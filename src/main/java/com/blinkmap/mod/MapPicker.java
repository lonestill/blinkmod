package com.blinkmap.mod;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.maps.sdk.MapView;
import com.maps.sdk.controllers.CameraController;
import com.maps.sdk.data.LatLon;

import java.util.Locale;

public class MapPicker {
    public static Activity mainActivity;
    public static MapView activeMapView;
    public static View activeOverlay;

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static Runnable updateRunnable;
    private static double currentLat = 0.0;
    private static double currentLon = 0.0;
    private static boolean isPicking = false;

    public static class MapPickerOverlay extends FrameLayout {
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

    public static void showPicker() {
        showPicker(mainActivity);
    }

    public static void showPicker(final Activity activity) {
        if (activity == null) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    doShowPicker(activity);
                } catch (Throwable t) {
                    Toast.makeText(activity, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

        // 1. Center Reticle (Crosshair / Target Pin)
        FrameLayout reticleFrame = new FrameLayout(activity);
        int reticleSize = Math.round(48 * density);
        FrameLayout.LayoutParams reticleLp = new FrameLayout.LayoutParams(reticleSize, reticleSize);
        reticleLp.gravity = Gravity.CENTER;
        reticleFrame.setLayoutParams(reticleLp);
        reticleFrame.setClickable(false);
        reticleFrame.setFocusable(false);

        // Outer glowing ring
        View outerRing = new View(activity);
        int ringSize = Math.round(42 * density);
        FrameLayout.LayoutParams ringLp = new FrameLayout.LayoutParams(ringSize, ringSize);
        ringLp.gravity = Gravity.CENTER;
        GradientDrawable ringDrawable = new GradientDrawable();
        ringDrawable.setShape(GradientDrawable.OVAL);
        ringDrawable.setColor(0x288A5CFF);
        ringDrawable.setStroke(Math.round(2.5f * density), 0xFF8A5CFF);
        outerRing.setBackground(ringDrawable);
        reticleFrame.addView(outerRing, ringLp);

        // Center white dot
        View centerDot = new View(activity);
        int dotSize = Math.round(8 * density);
        FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dotSize, dotSize);
        dotLp.gravity = Gravity.CENTER;
        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);
        dotDrawable.setColor(0xFFFFFFFF);
        centerDot.setBackground(dotDrawable);
        reticleFrame.addView(centerDot, dotLp);

        overlay.addView(reticleFrame);

        // 2. Top Instruction Pill
        LinearLayout topPill = new LinearLayout(activity);
        topPill.setOrientation(LinearLayout.HORIZONTAL);
        topPill.setGravity(Gravity.CENTER);
        int pillPadH = Math.round(16 * density);
        int pillPadV = Math.round(8 * density);
        topPill.setPadding(pillPadH, pillPadV, pillPadH, pillPadV);
        GradientDrawable topDrawable = new GradientDrawable();
        topDrawable.setShape(GradientDrawable.RECTANGLE);
        topDrawable.setCornerRadius(20 * density);
        topDrawable.setColor(0xD816161C);
        topDrawable.setStroke(Math.round(1 * density), 0xFF2C2C38);
        topPill.setBackground(topDrawable);
        topPill.setElevation(8 * density);

        TextView topText = new TextView(activity);
        topText.setText("🎯 Наведите прицел на нужное место");
        topText.setTextSize(13);
        topText.setTextColor(0xFFEEEEEE);
        topText.setTypeface(Typeface.DEFAULT_BOLD);
        topPill.addView(topText);

        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        topLp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        topLp.topMargin = Math.round(54 * density);
        overlay.addView(topPill, topLp);

        // 3. Bottom Control Card
        final LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        int cardPadH = Math.round(18 * density);
        int cardPadV = Math.round(16 * density);
        card.setPadding(cardPadH, cardPadV, cardPadH, cardPadV);
        card.setElevation(14 * density);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(22 * density);
        cardBg.setColor(0xF516161C);
        cardBg.setStroke(Math.round(1 * density), 0xFF2C2C38);
        card.setBackground(cardBg);

        // Header inside card
        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView headerTitle = new TextView(activity);
        headerTitle.setText("ВЫБОР ТОЧКИ СПУФА");
        headerTitle.setTextSize(11);
        headerTitle.setTextColor(0xFF8E8EA0);
        headerTitle.setTypeface(Typeface.DEFAULT_BOLD);
        headerRow.addView(headerTitle);

        card.addView(headerRow);

        // Coordinates display
        final TextView coordsView = new TextView(activity);
        coordsView.setTextSize(17);
        coordsView.setTextColor(0xFFFFFFFF);
        coordsView.setTypeface(Typeface.DEFAULT_BOLD);
        coordsView.setText("0.000000, 0.000000");

        LinearLayout.LayoutParams coordsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        coordsLp.topMargin = Math.round(6 * density);
        coordsLp.bottomMargin = Math.round(14 * density);
        card.addView(coordsView, coordsLp);

        // Buttons row
        LinearLayout buttonsRow = new LinearLayout(activity);
        buttonsRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonsRow.setGravity(Gravity.CENTER_VERTICAL);

        // Cancel button
        TextView btnCancel = new TextView(activity);
        btnCancel.setText("Отмена");
        btnCancel.setTextSize(14);
        btnCancel.setTextColor(0xFF8E8EA0);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setBackground(createRipple(0xFF22222C, 14 * density));
        btnCancel.setClickable(true);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissPicker();
            }
        });

        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(
                0,
                Math.round(46 * density),
                1.0f
        );
        cancelLp.rightMargin = Math.round(6 * density);
        buttonsRow.addView(btnCancel, cancelLp);

        // Apply button
        TextView btnApply = new TextView(activity);
        btnApply.setText("✓ Установить");
        btnApply.setTextSize(14);
        btnApply.setTextColor(0xFFFFFFFF);
        btnApply.setTypeface(Typeface.DEFAULT_BOLD);
        btnApply.setGravity(Gravity.CENTER);
        btnApply.setBackground(createRipple(0xFF8A5CFF, 14 * density));
        btnApply.setClickable(true);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                Math.round(46 * density),
                1.5f
        );
        applyLp.leftMargin = Math.round(6 * density);
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

        // Request focus for Back key
        overlay.requestFocus();

        // Animations
        overlay.setAlpha(0.0f);
        overlay.animate().alpha(1.0f).setDuration(180).start();

        card.setTranslationY(80 * density);
        card.animate().translationY(0.0f).setDuration(220)
                .setInterpolator(new DecelerateInterpolator()).start();

        // Real-time coordinates update runnable
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

                handler.postDelayed(this, 120);
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
                View card = ((MapPickerOverlay) toRemove).bottomCard;
                if (card != null) {
                    card.animate().translationY(100f).alpha(0.0f).setDuration(160).start();
                }
            }

            toRemove.animate().alpha(0.0f).setDuration(160).withEndAction(new Runnable() {
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

    private static Drawable createRipple(int normalColor, float cornerRadius) {
        GradientDrawable content = new GradientDrawable();
        content.setShape(GradientDrawable.RECTANGLE);
        content.setCornerRadius(cornerRadius);
        content.setColor(normalColor);

        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.RECTANGLE);
        mask.setCornerRadius(cornerRadius);
        mask.setColor(0xFFFFFFFF);

        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, mask);
    }
}
