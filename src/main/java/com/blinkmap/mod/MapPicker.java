package com.blinkmap.mod;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.maps.sdk.MapView;
import com.maps.sdk.controllers.CameraController;
import com.maps.sdk.data.LatLon;

import java.util.Locale;

/**
 * High-precision tactical Map Picker overlay for BlinkMod 2.0.
 * Styled in OLED Stealth / Titanium Monochrome palette.
 */
public class MapPicker {
    public static Activity mainActivity;
    public static MapView activeMapView;
    public static View activeOverlay;

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static Runnable updateRunnable;
    private static double currentLat = 0.0;
    private static double currentLon = 0.0;
    private static boolean isPicking = false;

    // Tokens matching ModSettingsActivity OLED Stealth Palette
    private static final int COLOR_CARD_BG = 0xF518191F;
    private static final int COLOR_PILL_BG = 0xEE18191F;
    private static final int COLOR_OUTLINE = 0xFF353844;
    private static final int COLOR_PRIMARY = 0xFFF0F1F5;
    private static final int COLOR_ON_PRIMARY = 0xFF0D0E11;
    private static final int COLOR_CONTAINER = 0xFF22242D;
    private static final int COLOR_TEXT_SECONDARY = 0xFF9E9EA8;

    public static class MapPickerOverlay extends FrameLayout {
        public View topPill;
        public View bottomCard;

        public MapPickerOverlay(Context context) {
            super(context);
            setFocusableInTouchMode(true);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            if (bottomCard != null && isPointInside(ev.getRawX(), ev.getRawY(), bottomCard)) {
                return super.dispatchTouchEvent(ev);
            }
            if (topPill != null && isPointInside(ev.getRawX(), ev.getRawY(), topPill)) {
                return super.dispatchTouchEvent(ev);
            }
            return false;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                MapPicker.dismissPicker();
                return true;
            }
            return super.dispatchKeyEvent(event);
        }

        private static boolean isPointInside(float x, float y, View v) {
            if (v == null || v.getVisibility() != View.VISIBLE) return false;
            int[] loc = new int[2];
            v.getLocationOnScreen(loc);
            int vx = loc[0];
            int vy = loc[1];
            return (x >= vx && x <= (vx + v.getWidth()) &&
                    y >= vy && y <= (vy + v.getHeight()));
        }
    }

    /**
     * Precision Tactical Crosshair View with sub-pixel alignment and contrast outline.
     */
    public static class TacticalReticleView extends View {
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public TacticalReticleView(Context context) {
            super(context);
            float d = getResources().getDisplayMetrics().density;

            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(1.5f * d);
            ringPaint.setColor(COLOR_PRIMARY);

            tickPaint.setStyle(Paint.Style.STROKE);
            tickPaint.setStrokeWidth(2.0f * d);
            tickPaint.setStrokeCap(Paint.Cap.ROUND);
            tickPaint.setColor(COLOR_PRIMARY);

            dotPaint.setStyle(Paint.Style.FILL);
            dotPaint.setColor(0xFFFFFFFF);

            shadowPaint.setStyle(Paint.Style.STROKE);
            shadowPaint.setStrokeWidth(3.5f * d);
            shadowPaint.setColor(0x66000000);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float d = getResources().getDisplayMetrics().density;

            float r = 20f * d;
            // High contrast halo for visibility over white & satellite map layers
            canvas.drawCircle(cx, cy, r, shadowPaint);
            canvas.drawCircle(cx, cy, r, ringPaint);

            // Center targeting dot
            canvas.drawCircle(cx, cy, 3.5f * d, dotPaint);

            // 4 tactical crosshair marks
            float tickInner = 12f * d;
            float tickOuter = 26f * d;

            // Top
            canvas.drawLine(cx, cy - tickOuter, cx, cy - tickInner, tickPaint);
            // Bottom
            canvas.drawLine(cx, cy + tickInner, cx, cy + tickOuter, tickPaint);
            // Left
            canvas.drawLine(cx - tickOuter, cy, cx - tickInner, cy, tickPaint);
            // Right
            canvas.drawLine(cx + tickInner, cy, cx + tickOuter, cy, tickPaint);
        }
    }

    public static Activity getTopActivity() {
        if (mainActivity != null && !mainActivity.isFinishing() && !mainActivity.isDestroyed()) {
            return mainActivity;
        }
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            java.lang.reflect.Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Object activities = activitiesField.get(activityThread);
            if (activities instanceof java.util.Map) {
                for (Object activityRecord : ((java.util.Map<?, ?>) activities).values()) {
                    Class<?> recordClass = activityRecord.getClass();
                    java.lang.reflect.Field pausedField = recordClass.getDeclaredField("paused");
                    pausedField.setAccessible(true);
                    if (!pausedField.getBoolean(activityRecord)) {
                        java.lang.reflect.Field activityField = recordClass.getDeclaredField("activity");
                        activityField.setAccessible(true);
                        Activity act = (Activity) activityField.get(activityRecord);
                        if (act != null && !act.getClass().getSimpleName().contains("ModSettingsActivity") && !act.isFinishing()) {
                            mainActivity = act;
                            return act;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return mainActivity;
    }

    public static void showPicker() {
        showPicker(getTopActivity());
    }

    public static void showPicker(Activity activity) {
        if (activity == null) {
            activity = getTopActivity();
        }
        if (activity == null) return;
        final Activity act = activity;

        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    doShowPicker(act);
                } catch (Throwable t) {
                    Toast.makeText(act, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private static void doShowPicker(final Activity activity) {
        if (isPicking) {
            dismissPicker();
        }

        mainActivity = activity;
        isPicking = true;

        final DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        final float density = dm.density;

        final MapPickerOverlay overlay = new MapPickerOverlay(activity);
        activeOverlay = overlay;

        // 1. Center Tactical Reticle
        TacticalReticleView reticle = new TacticalReticleView(activity);
        int reticleSize = Math.round(64 * density);
        FrameLayout.LayoutParams reticleLp = new FrameLayout.LayoutParams(reticleSize, reticleSize);
        reticleLp.gravity = Gravity.CENTER;
        reticle.setLayoutParams(reticleLp);
        reticle.setClickable(false);
        reticle.setFocusable(false);
        overlay.addView(reticle);

        // 2. Top Instruction Pill
        LinearLayout topPill = new LinearLayout(activity);
        topPill.setOrientation(LinearLayout.HORIZONTAL);
        topPill.setGravity(Gravity.CENTER);
        int pillPadH = Math.round(18 * density);
        int pillPadV = Math.round(8 * density);
        topPill.setPadding(pillPadH, pillPadV, pillPadH, pillPadV);

        GradientDrawable topBg = new GradientDrawable();
        topBg.setShape(GradientDrawable.RECTANGLE);
        topBg.setCornerRadius(20 * density);
        topBg.setColor(COLOR_PILL_BG);
        topBg.setStroke(Math.round(1 * density), COLOR_OUTLINE);
        topPill.setBackground(topBg);
        topPill.setElevation(8 * density);

        TextView dotView = new TextView(activity);
        dotView.setText("● ");
        dotView.setTextSize(10);
        dotView.setTextColor(0xFF38E54D);
        topPill.addView(dotView);

        TextView topText = new TextView(activity);
        topText.setText("Наведите перекрестие на точку");
        topText.setTextSize(12);
        topText.setTextColor(COLOR_PRIMARY);
        topText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        topPill.addView(topText);

        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        topLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        topLp.topMargin = Math.round(52 * density);
        overlay.addView(topPill, topLp);
        overlay.topPill = topPill;

        // 3. Bottom Titanium Control Card
        final LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        int cardPadH = Math.round(20 * density);
        int cardPadV = Math.round(18 * density);
        card.setPadding(cardPadH, cardPadV, cardPadH, cardPadV);
        card.setElevation(16 * density);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(24 * density);
        cardBg.setColor(COLOR_CARD_BG);
        cardBg.setStroke(Math.round(1 * density), COLOR_OUTLINE);
        card.setBackground(cardBg);

        // Header inside card
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView headerTitle = new TextView(activity);
        headerTitle.setText("ВЫБОР ГЕОПОЗИЦИИ");
        headerTitle.setTextSize(11);
        headerTitle.setTextColor(COLOR_TEXT_SECONDARY);
        headerTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        headerTitle.setLetterSpacing(0.08f);
        headerRow.addView(headerTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView badge = new TextView(activity);
        badge.setText("SPOOF GPS");
        badge.setTextSize(10);
        badge.setTextColor(COLOR_PRIMARY);
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badge.setPadding(Math.round(8 * density), Math.round(3 * density), Math.round(8 * density), Math.round(3 * density));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(COLOR_CONTAINER);
        badgeBg.setCornerRadius(10 * density);
        badge.setBackground(badgeBg);
        headerRow.addView(badge);

        card.addView(headerRow);

        // Coordinates display
        final TextView coordsView = new TextView(activity);
        coordsView.setTextSize(18);
        coordsView.setTextColor(0xFFFFFFFF);
        coordsView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        coordsView.setText("0.000000, 0.000000");

        LinearLayout.LayoutParams coordsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        coordsLp.topMargin = Math.round(8 * density);
        coordsLp.bottomMargin = Math.round(16 * density);
        card.addView(coordsView, coordsLp);

        coordsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("coords", coordsView.getText().toString()));
                    Toast.makeText(activity, "Координаты скопированы", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Buttons row
        LinearLayout buttonsRow = new LinearLayout(activity);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonsRow.setGravity(Gravity.CENTER_VERTICAL);

        // Cancel button (M3 Outlined Pill)
        TextView btnCancel = new TextView(activity);
        btnCancel.setText("Отмена");
        btnCancel.setTextSize(13);
        btnCancel.setTextColor(COLOR_PRIMARY);
        btnCancel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setColor(Color.TRANSPARENT);
        cancelBg.setCornerRadius(22 * density);
        cancelBg.setStroke(Math.round(1 * density), COLOR_OUTLINE);
        btnCancel.setBackground(cancelBg);
        addTouchSpring(btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                dismissPicker();
            }
        });

        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(
                0,
                Math.round(44 * density),
                1.0f
        );
        cancelLp.rightMargin = Math.round(8 * density);
        buttonsRow.addView(btnCancel, cancelLp);

        // Apply button (M3 Primary Pill)
        TextView btnApply = new TextView(activity);
        btnApply.setText("Установить точку");
        btnApply.setTextSize(13);
        btnApply.setTextColor(COLOR_ON_PRIMARY);
        btnApply.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btnApply.setGravity(Gravity.CENTER);
        GradientDrawable applyBg = new GradientDrawable();
        applyBg.setColor(COLOR_PRIMARY);
        applyBg.setCornerRadius(22 * density);
        btnApply.setBackground(applyBg);
        addTouchSpring(btnApply);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                if (currentLat != 0.0 || currentLon != 0.0) {
                    StepHooks.setCustomLocation(currentLat, currentLon);
                    String msg = String.format(Locale.US, "Точка установлена: %.5f, %.5f", currentLat, currentLon);
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "Координаты не получены", Toast.LENGTH_SHORT).show();
                }
                dismissPicker();
            }
        });

        LinearLayout.LayoutParams applyLp = new LinearLayout.LayoutParams(
                0,
                Math.round(44 * density),
                1.5f
        );
        buttonsRow.addView(btnApply, applyLp);

        card.addView(buttonsRow);

        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        cardLp.leftMargin = Math.round(16 * density);
        cardLp.rightMargin = Math.round(16 * density);
        cardLp.bottomMargin = Math.round(34 * density);
        overlay.addView(card, cardLp);
        overlay.bottomCard = card;

        // Add to DecorView
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        decor.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        overlay.requestFocus();

        // Animations: Smooth spring entry
        overlay.setAlpha(0.0f);
        overlay.animate().alpha(1.0f).setDuration(160).start();

        topPill.setTranslationY(-40 * density);
        topPill.animate().translationY(0.0f).setDuration(260)
                .setInterpolator(new DecelerateInterpolator()).start();

        card.setTranslationY(120 * density);
        card.animate().translationY(0.0f).setDuration(320)
                .setInterpolator(new OvershootInterpolator(0.7f)).start();

        // Real-time coordinates update loop
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPicking || activeOverlay == null) return;

                MapView mv = getMapView();
                if (mv != null) {
                    try {
                        CameraController cc = mv.getCameraController();
                        if (cc != null) {
                            LatLon ll = cc.getCenterCoordinates();
                            if (ll != null) {
                                currentLat = ll.getLatitude();
                                currentLon = ll.getLongitude();
                                coordsView.setText(String.format(Locale.US, "%.6f, %.6f", currentLat, currentLon));
                            }
                        }
                    } catch (Throwable ignored) {}
                }

                handler.postDelayed(this, 100);
            }
        };

        handler.post(updateRunnable);
    }

    public static void dismissPicker() {
        if (!isPicking && activeOverlay == null) return;
        isPicking = false;

        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }

        if (activeOverlay != null) {
            final View toRemove = activeOverlay;
            activeOverlay = null;

            if (toRemove instanceof MapPickerOverlay) {
                MapPickerOverlay mpo = (MapPickerOverlay) toRemove;
                if (mpo.bottomCard != null) {
                    mpo.bottomCard.animate().translationY(140f).alpha(0.0f).setDuration(180).start();
                }
                if (mpo.topPill != null) {
                    mpo.topPill.animate().translationY(-40f).alpha(0.0f).setDuration(180).start();
                }
            }

            toRemove.animate().alpha(0.0f).setDuration(180).withEndAction(new Runnable() {
                @Override
                public void run() {
                    try {
                        ViewGroup parent = (ViewGroup) toRemove.getParent();
                        if (parent != null) {
                            parent.removeView(toRemove);
                        }
                    } catch (Throwable ignored) {}
                }
            }).start();
        }
    }

    public static MapView getMapView() {
        if (activeMapView != null && activeMapView.isAttachedToWindow()) {
            return activeMapView;
        }
        if (mainActivity != null) {
            try {
                View v = findMapView(mainActivity.getWindow().getDecorView());
                if (v instanceof MapView) {
                    activeMapView = (MapView) v;
                    return activeMapView;
                }
            } catch (Throwable ignored) {}
        }
        return activeMapView;
    }

    public static MapView findMapView(View root) {
        if (root == null) return null;
        if (root instanceof MapView) return (MapView) root;
        if (root.getClass().getName().contains("MapView")) {
            try {
                return (MapView) root;
            } catch (Throwable ignored) {}
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                MapView found = findMapView(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void addTouchSpring(final View view) {
        if (view == null) return;
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(240)
                                .setInterpolator(new OvershootInterpolator(1.3f)).start();
                        break;
                }
                return false;
            }
        });
    }

    private static void haptic(View v) {
        if (v != null) {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }
}
