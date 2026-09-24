package com.blinkmap.mod;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

public final class BlinkModButton {
    private static final String TAG = "blinkmod_floating_button";
    public static volatile Activity currentActivity = null;

    private BlinkModButton() {}

    public static void inject(final Activity activity) {
        currentActivity = activity;
        MapPicker.mainActivity = activity;
        StepHooks.init(activity.getApplicationContext());

        View decor = activity.getWindow().getDecorView();
        if (!(decor instanceof ViewGroup)) return;
        View old = decor.findViewWithTag(TAG);
        if (old != null) ((ViewGroup) decor).removeView(old);

        final android.content.SharedPreferences prefs = activity.getSharedPreferences("blinkmod_settings", Activity.MODE_PRIVATE);
        // Clean native bottom bar button is now primary.
        // Floating button (2015 mode) is disabled by default, toggleable in settings.
        if (!prefs.getBoolean("floating_enabled", false)) {
            return;
        }

        final int size = Math.max(34, Math.min(56, prefs.getInt("floating_size", 44)));
        ImageButton button = new ImageButton(activity);
        button.setTag(TAG);
        button.setContentDescription("BlinkMod");
        try { button.setImageDrawable(activity.getPackageManager().getApplicationIcon(activity.getPackageName())); } catch (Exception ignored) {}
        button.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        button.setPadding(dp(activity, 2), dp(activity, 2), dp(activity, 2), dp(activity, 2));
        button.setElevation(dp(activity, 8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(225, 8, 8, 10));
        bg.setStroke(dp(activity, 1), Color.rgb(128, 75, 246));
        bg.setShape(GradientDrawable.OVAL);
        button.setBackground(bg);
        button.setAlpha(Math.max(.45f, Math.min(1f, prefs.getInt("floating_opacity", 90) / 100f)));
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(activity, size), dp(activity, size));
        lp.gravity = (prefs.getBoolean("floating_left", false) ? Gravity.START : Gravity.END) | Gravity.TOP;
        lp.leftMargin = lp.rightMargin = dp(activity, 14);
        int savedY = prefs.getInt("floating_y", -1);
        lp.topMargin = savedY < 0 ? Math.max(dp(activity, 84), activity.getResources().getDisplayMetrics().heightPixels / 2 - dp(activity, size / 2)) : savedY;
        final ImageButton dragButton = button;
        button.setOnTouchListener(new View.OnTouchListener() {
            float downY; int startTop; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    downY = e.getRawY(); startTop = lp.topMargin; moved = false; return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    int dy = Math.round(e.getRawY() - downY); if (Math.abs(dy) > dp(activity, 6)) moved = true;
                    int max = activity.getResources().getDisplayMetrics().heightPixels - dp(activity, size + 54);
                    lp.topMargin = Math.max(dp(activity, 54), Math.min(max, startTop + dy));
                    dragButton.setLayoutParams(lp);
                    return true;
                }
                if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                    if (moved) {
                        prefs.edit().putInt("floating_y", lp.topMargin).apply();
                        v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    } else {
                        openMenu();
                    }
                    return true;
                }
                return false;
            }
        });
        ((ViewGroup) decor).addView(button, lp);
    }

    public static boolean interceptTabClick(Object tab) {
        if (tab == null) return false;
        try {
            java.lang.reflect.Method m = tab.getClass().getMethod("b");
            Object res = m.invoke(tab);
            if (res instanceof Integer && ((Integer) res) == 0x7f130767) {
                openMenu();
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static void openMenu() {
        Activity act = currentActivity != null ? currentActivity : MapPicker.mainActivity;
        if (act == null || act.isFinishing()) {
            act = MapPicker.getTopActivity();
        }
        if (act == null) return;
        final Activity finalAct = act;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    View decor = finalAct.getWindow().getDecorView();
                    decor.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    Intent intent = new Intent(finalAct, ModSettingsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    finalAct.startActivity(intent);
                    finalAct.overridePendingTransition(0, 0);
                } catch (Throwable ignored) {}
            }
        });
    }

    private static int dp(Activity a, int v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }
}
