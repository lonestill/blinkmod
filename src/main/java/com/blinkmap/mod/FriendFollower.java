package com.blinkmap.mod;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Autonomous friend following & orbit engine for BlinkMod.
 *
 * Allows locking onto any friend from the local database, following their
 * coordinates, orbiting around them at configurable radiuses, or shadowing them,
 * while automatically generating realistic pedometer steps and speed.
 */
public final class FriendFollower {
    private static final String TAG = "FriendFollower";
    private static final String PREFS_NAME = "blinkmod_settings";

    public static final int MODE_ORBIT = 0;   // Walking in a circle around friend
    public static final int MODE_SHADOW = 1;  // Following behind friend based on direction
    public static final int MODE_POCKET = 2;  // Right at friend coordinates with natural jitter

    public static final float SPEED_WALK = 4.5f;   // ~4.5 km/h (1.25 m/s, generates ~1.7 steps/s)
    public static final float SPEED_RUN = 11.5f;   // ~11.5 km/h (3.2 m/s, generates ~3.1 steps/s)
    public static final float SPEED_BIKE = 22.0f;  // ~22 km/h (6.1 m/s, no steps)
    public static final float SPEED_CAR = 48.0f;   // ~48 km/h (13.3 m/s, vehicle avatar mode)

    public static final float RADIUS_CLOSE = 10f;     // 10 meters
    public static final float RADIUS_MEDIUM = 25f;    // 25 meters (standard)
    public static final float RADIUS_DISTANT = 50f;   // 50 meters
    public static final float RADIUS_WIDE = 100f;     // 100 meters

    public static final class FriendTarget {
        public final long id;
        public final String name;
        public final String nickname;
        public final String avatarUrl;
        public double latitude;
        public double longitude;
        public float bearing;
        public float speedKmh;
        public boolean isOnline;
        public long lastUpdateTs;
        public float distanceMeters = -1f;

        public FriendTarget(long id, String name, String nickname, String avatarUrl,
                            double lat, double lon, float bearing, float speedKmh,
                            boolean isOnline, long lastUpdateTs) {
            this.id = id;
            this.name = name != null ? name.trim() : "";
            this.nickname = nickname != null ? nickname.trim() : "";
            this.avatarUrl = avatarUrl != null ? avatarUrl.trim() : "";
            this.latitude = lat;
            this.longitude = lon;
            this.bearing = bearing;
            this.speedKmh = speedKmh;
            this.isOnline = isOnline;
            this.lastUpdateTs = lastUpdateTs;
        }

        public String getDisplayName() {
            if (!name.isEmpty()) return name;
            if (!nickname.isEmpty()) return "@" + nickname;
            return "ID " + id;
        }

        public String getSecondaryText() {
            StringBuilder sb = new StringBuilder();
            if (isOnline) {
                sb.append("В сети");
            } else {
                sb.append(formatTimeAge(lastUpdateTs));
            }
            if (distanceMeters >= 0) {
                sb.append(" · ");
                if (distanceMeters < 1000) {
                    sb.append(Math.round(distanceMeters)).append(" м");
                } else {
                    sb.append(String.format(Locale.US, "%.1f км", distanceMeters / 1000f));
                }
            }
            if (speedKmh > 3.0f) {
                sb.append(" · ").append(Math.round(speedKmh)).append(" км/ч");
            }
            return sb.toString();
        }
    }

    public interface FollowListener {
        void onFollowTick(FriendTarget target, double lat, double lon, int stepsAdded);
        void onFollowStopped();
    }

    private static volatile boolean running = false;
    private static volatile FriendTarget currentTarget = null;
    private static volatile int currentMode = MODE_ORBIT;
    private static volatile float currentRadius = RADIUS_MEDIUM;
    private static volatile float currentSpeedKmh = SPEED_WALK;
    private static volatile int sessionStepsAdded = 0;
    private static volatile Thread followerThread = null;
    private static volatile FollowListener activeListener = null;

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private FriendFollower() {}

    public static boolean isRunning() {
        return running;
    }

    public static FriendTarget getCurrentTarget() {
        return currentTarget;
    }

    public static int getMode() {
        return currentMode;
    }

    public static float getRadius() {
        return currentRadius;
    }

    public static float getSpeedKmh() {
        return currentSpeedKmh;
    }

    public static int getSessionStepsAdded() {
        return sessionStepsAdded;
    }

    public static void setListener(FollowListener listener) {
        activeListener = listener;
    }

    public static String getStatusText() {
        if (!running || currentTarget == null) {
            return "Выключено";
        }
        String modeName;
        switch (currentMode) {
            case MODE_SHADOW:
                modeName = "Тень (" + Math.round(currentRadius) + "м)";
                break;
            case MODE_POCKET:
                modeName = "В кармане";
                break;
            case MODE_ORBIT:
            default:
                modeName = "Хоровод (" + Math.round(currentRadius) + "м)";
                break;
        }
        return modeName + " · " + Math.round(currentSpeedKmh) + " км/ч (+" + sessionStepsAdded + " шаг)";
    }

    public static List<FriendTarget> loadFriends(Context context) {
        ArrayList<FriendTarget> list = new ArrayList<FriendTarget>();
        if (context == null) return list;

        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            File f = context.getDatabasePath("com.blinkmap.database");
            if (f == null || !f.exists()) {
                f = new File("/data/data/app.blinkmod/databases/com.blinkmap.database");
            }
            if (!f.exists()) {
                return list;
            }

            db = SQLiteDatabase.openDatabase(f.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            // Try querying MarkerDatabaseView first
            try {
                c = db.rawQuery(
                    "SELECT profile_id, profile_nickname, profile_name, profile_avatar_thumbnail_url, " +
                    "latitude, longitude, bearing, speed_km_h, is_online, last_update_ts " +
                    "FROM MarkerDatabaseView WHERE latitude IS NOT NULL AND longitude IS NOT NULL", null);
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    String nick = c.getString(1);
                    String name = c.getString(2);
                    String avatar = c.getString(3);
                    double lat = c.getDouble(4);
                    double lon = c.getDouble(5);
                    float bearing = c.isNull(6) ? 0f : c.getFloat(6);
                    float speed = c.isNull(7) ? -1f : c.getFloat(7);
                    boolean isOnline = !c.isNull(8) && c.getInt(8) == 1;
                    long updateTs = c.isNull(9) ? 0L : c.getLong(9);
                    list.add(new FriendTarget(id, name, nick, avatar, lat, lon, bearing, speed, isOnline, updateTs));
                }
            } catch (Throwable e) {
                if (c != null) c.close();
                c = db.rawQuery(
                    "SELECT profile_id, profile_nickname, profile_name, profile_avatar_thumbnail_url, " +
                    "latitude, longitude, bearing, speed_km_h, 0, last_update_ts " +
                    "FROM MarkerEntity WHERE latitude IS NOT NULL AND longitude IS NOT NULL", null);
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    String nick = c.getString(1);
                    String name = c.getString(2);
                    String avatar = c.getString(3);
                    double lat = c.getDouble(4);
                    double lon = c.getDouble(5);
                    float bearing = c.isNull(6) ? 0f : c.getFloat(6);
                    float speed = c.isNull(7) ? -1f : c.getFloat(7);
                    long updateTs = c.isNull(9) ? 0L : c.getLong(9);
                    list.add(new FriendTarget(id, name, nick, avatar, lat, lon, bearing, speed, false, updateTs));
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
            if (db != null) try { db.close(); } catch (Throwable ignored) {}
        }

        // Calculate distances relative to current location
        double myLat = StepHooks.driftCurLat != 0.0 ? StepHooks.driftCurLat : StepHooks.customLat;
        double myLon = StepHooks.driftCurLon != 0.0 ? StepHooks.driftCurLon : StepHooks.customLon;
        if (myLat != 0.0 && myLon != 0.0) {
            float[] res = new float[1];
            for (FriendTarget target : list) {
                try {
                    Location.distanceBetween(myLat, myLon, target.latitude, target.longitude, res);
                    target.distanceMeters = res[0];
                } catch (Throwable ignored) {}
            }
        }

        // Sort: Online first, then closest distance, then most recent update
        Collections.sort(list, new Comparator<FriendTarget>() {
            @Override
            public int compare(FriendTarget a, FriendTarget b) {
                if (a.isOnline != b.isOnline) {
                    return a.isOnline ? -1 : 1;
                }
                if (a.distanceMeters >= 0 && b.distanceMeters >= 0) {
                    return Float.compare(a.distanceMeters, b.distanceMeters);
                }
                return Long.compare(b.lastUpdateTs, a.lastUpdateTs);
            }
        });

        return list;
    }

    public static synchronized void startFollow(final Context context, final FriendTarget target,
                                                final int mode, final float radiusMeters,
                                                final float speedKmh, final FollowListener listener) {
        stopFollow(context);
        if (target == null) return;

        currentTarget = target;
        currentMode = mode;
        currentRadius = radiusMeters > 0 ? radiusMeters : RADIUS_MEDIUM;
        currentSpeedKmh = speedKmh > 0 ? speedKmh : SPEED_WALK;
        sessionStepsAdded = 0;
        activeListener = listener;
        running = true;

        // Persist preferences
        if (context != null) {
            try {
                SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, 0);
                sp.edit()
                    .putLong("follower_target_id", target.id)
                    .putInt("follower_mode", mode)
                    .putFloat("follower_radius", currentRadius)
                    .putFloat("follower_speed", currentSpeedKmh)
                    .apply();
            } catch (Throwable ignored) {}
        }

        followerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                PowerManager.WakeLock wakeLock = null;
                if (context != null) {
                    try {
                        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                        if (pm != null) {
                            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BlinkMod:FriendFollower");
                            wakeLock.acquire(2 * 60 * 60 * 1000L); // 2 hours max
                        }
                    } catch (Throwable ignored) {}
                }

                double centerLat = target.latitude;
                double centerLon = target.longitude;
                float friendBearing = target.bearing;
                float friendSpeed = target.speedKmh;
                double currentOrbitAngle = 0.0;
                long lastDbCheckTime = 0;
                final long TICK_INTERVAL_MS = 500L; // 2 Hz updates for silky smooth tracking
                double accumulatedSteps = 0.0;

                try {
                    while (running) {
                        long now = System.currentTimeMillis();

                        // 1. Periodically (every 1.5s) re-check friend coordinates in database
                        if (now - lastDbCheckTime >= 1500L) {
                            lastDbCheckTime = now;
                            FriendTarget fresh = queryFriendCoordinates(context, target.id);
                            if (fresh != null) {
                                target.latitude = fresh.latitude;
                                target.longitude = fresh.longitude;
                                target.bearing = fresh.bearing;
                                target.speedKmh = fresh.speedKmh;
                                target.isOnline = fresh.isOnline;
                                target.lastUpdateTs = fresh.lastUpdateTs;
                                friendBearing = fresh.bearing;
                                friendSpeed = fresh.speedKmh;
                            }
                        }

                        // 2. Smoothly follow the center point towards friend's actual coordinates
                        float[] distResult = new float[1];
                        Location.distanceBetween(centerLat, centerLon, target.latitude, target.longitude, distResult);
                        float distToFriend = distResult[0];

                        if (distToFriend > 1000f) {
                            // Teleport center if friend jumped over 1 km away
                            centerLat = target.latitude;
                            centerLon = target.longitude;
                        } else if (distToFriend > 1.0f) {
                            // Smoothly move center toward friend at matching speed or catch-up speed
                            double stepDist = (Math.max(currentSpeedKmh, friendSpeed > 0 ? friendSpeed + 2.0f : currentSpeedKmh) / 3.6) * (TICK_INTERVAL_MS / 1000.0);
                            double fraction = Math.min(1.0, stepDist / Math.max(0.1, distToFriend));
                            centerLat += (target.latitude - centerLat) * fraction;
                            centerLon += (target.longitude - centerLon) * fraction;
                        }

                        // 3. Compute synthetic location and bearing based on mode
                        double synLat = centerLat;
                        double synLon = centerLon;
                        float synBearing = 0f;
                        double speedMps = currentSpeedKmh / 3.6;

                        if (currentMode == MODE_ORBIT) {
                            // Angular velocity: omega = v / r
                            double r = Math.max(5.0, currentRadius);
                            double omega = speedMps / r;
                            currentOrbitAngle += omega * (TICK_INTERVAL_MS / 1000.0);
                            if (currentOrbitAngle > 2 * Math.PI) {
                                currentOrbitAngle -= 2 * Math.PI;
                            }

                            double dLat = (r * Math.cos(currentOrbitAngle)) / 111139.0;
                            double dLon = (r * Math.sin(currentOrbitAngle)) / (111139.0 * Math.max(0.15, Math.cos(Math.toRadians(centerLat))));

                            synLat = centerLat + dLat;
                            synLon = centerLon + dLon;

                            // Tangent heading along circular trajectory
                            synBearing = (float) ((Math.toDegrees(currentOrbitAngle) + 90.0) % 360.0);
                            if (synBearing < 0) synBearing += 360f;

                        } else if (currentMode == MODE_SHADOW) {
                            // Follow strictly behind friend's bearing, or behind current drift angle
                            float heading = (friendBearing != 0f) ? friendBearing : (float) Math.toDegrees(currentOrbitAngle);
                            double behindAngle = Math.toRadians((heading + 180f) % 360f);
                            double r = Math.max(4.0, currentRadius);

                            // Subtle left/right sway (±1.5m) to simulate realistic shadowing
                            double sway = Math.sin(now / 1200.0) * 1.5;
                            double swayAngle = Math.toRadians((heading + 90f) % 360f);

                            double dLat = (r * Math.cos(behindAngle) + sway * Math.cos(swayAngle)) / 111139.0;
                            double dLon = (r * Math.sin(behindAngle) + sway * Math.sin(swayAngle)) / (111139.0 * Math.max(0.15, Math.cos(Math.toRadians(centerLat))));

                            synLat = centerLat + dLat;
                            synLon = centerLon + dLon;
                            synBearing = heading; // Facing forward following friend

                        } else if (currentMode == MODE_POCKET) {
                            // Pocket mode: right on top with tiny realistic GPS jitter (±1.2m)
                            double jitterDist = 1.2 * Math.sin(now / 1500.0);
                            double jitterAngle = (now / 2000.0) % (2 * Math.PI);

                            double dLat = (jitterDist * Math.cos(jitterAngle)) / 111139.0;
                            double dLon = (jitterDist * Math.sin(jitterAngle)) / (111139.0 * Math.max(0.15, Math.cos(Math.toRadians(centerLat))));

                            synLat = centerLat + dLat;
                            synLon = centerLon + dLon;
                            synBearing = friendBearing;
                            speedMps = 0.4;
                        }

                        // 4. Generate realistic pedometer steps
                        double stepsPerSec = 0.0;
                        if (currentSpeedKmh <= 6.5f) {
                            stepsPerSec = 1.75; // Walking pace
                        } else if (currentSpeedKmh <= 16.0f) {
                            stepsPerSec = 3.20; // Running pace
                        }

                        accumulatedSteps += stepsPerSec * (TICK_INTERVAL_MS / 1000.0);
                        if (accumulatedSteps >= 1.0) {
                            int wholeSteps = (int) accumulatedSteps;
                            accumulatedSteps -= wholeSteps;
                            sessionStepsAdded += wholeSteps;
                            StepHooks.driftStepsAdded += wholeSteps;
                        }

                        // 5. Inject into StepHooks & dispatch to Blink map engine
                        StepHooks.customLat = synLat;
                        StepHooks.customLon = synLon;
                        StepHooks.driftCurLat = synLat;
                        StepHooks.driftCurLon = synLon;
                        StepHooks.driftSpeed = (float) speedMps;
                        StepHooks.driftBearing = synBearing;
                        StepHooks.dispatchSyntheticLocation();

                        // 6. Notify active UI listener on main thread
                        final double fLat = synLat;
                        final double fLon = synLon;
                        final int fSteps = sessionStepsAdded;
                        final FollowListener l = activeListener;
                        if (l != null) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (running && activeListener != null) {
                                        activeListener.onFollowTick(target, fLat, fLon, fSteps);
                                    }
                                }
                            });
                        }

                        Thread.sleep(TICK_INTERVAL_MS);
                    }
                } catch (InterruptedException ignored) {
                } catch (Throwable e) {
                    // Safe error logging
                } finally {
                    running = false;
                    if (wakeLock != null && wakeLock.isHeld()) {
                        try {
                            wakeLock.release();
                        } catch (Throwable ignored) {}
                    }
                    final FollowListener l = activeListener;
                    if (l != null) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (activeListener != null) {
                                    activeListener.onFollowStopped();
                                }
                            }
                        });
                    }
                }
            }
        }, "BlinkMod-FriendFollower");

        followerThread.start();
    }

    public static synchronized void stopFollow(Context context) {
        running = false;
        if (followerThread != null && followerThread.isAlive()) {
            followerThread.interrupt();
            followerThread = null;
        }
        if (currentTarget != null && context != null) {
            // Keep the last followed coordinates as the fixed spoofed location
            try {
                if (StepHooks.driftCurLat != 0.0 && StepHooks.driftCurLon != 0.0) {
                    StepHooks.setCustomLocation(StepHooks.driftCurLat, StepHooks.driftCurLon);
                }
            } catch (Throwable ignored) {}
        }
        final FollowListener l = activeListener;
        if (l != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (activeListener != null) {
                        activeListener.onFollowStopped();
                    }
                }
            });
        }
    }

    private static FriendTarget queryFriendCoordinates(Context context, long friendId) {
        if (context == null) return null;
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            File f = context.getDatabasePath("com.blinkmap.database");
            if (f == null || !f.exists()) {
                f = new File("/data/data/app.blinkmod/databases/com.blinkmap.database");
            }
            if (!f.exists()) return null;

            db = SQLiteDatabase.openDatabase(f.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            c = db.rawQuery(
                "SELECT profile_id, profile_nickname, profile_name, profile_avatar_thumbnail_url, " +
                "latitude, longitude, bearing, speed_km_h, 1, last_update_ts " +
                "FROM MarkerEntity WHERE profile_id = ? AND latitude IS NOT NULL AND longitude IS NOT NULL LIMIT 1",
                new String[]{String.valueOf(friendId)});

            if (c.moveToFirst()) {
                long id = c.getLong(0);
                String nick = c.getString(1);
                String name = c.getString(2);
                String avatar = c.getString(3);
                double lat = c.getDouble(4);
                double lon = c.getDouble(5);
                float bearing = c.isNull(6) ? 0f : c.getFloat(6);
                float speed = c.isNull(7) ? -1f : c.getFloat(7);
                long updateTs = c.isNull(9) ? 0L : c.getLong(9);
                return new FriendTarget(id, name, nick, avatar, lat, lon, bearing, speed, true, updateTs);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
            if (db != null) try { db.close(); } catch (Throwable ignored) {}
        }
        return null;
    }

    private static String formatTimeAge(long value) {
        long t = value > 0 && value < 100000000000L ? value * 1000L : value;
        long d = System.currentTimeMillis() - t;
        if (t <= 0) return "Офлайн";
        if (d < 0) d = 0;
        if (d < 60000) return "Только что";
        long m = d / 60000;
        if (m < 60) return m + " мин назад";
        long h = m / 60;
        if (h < 24) return h + " ч назад";
        return h / 24 + " дн назад";
    }
}
