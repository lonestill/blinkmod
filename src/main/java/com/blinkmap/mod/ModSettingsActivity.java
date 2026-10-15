package com.blinkmap.mod;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ModSettingsActivity extends Activity {
    private static final String PREFS = "blinkmod_settings";

    // Material Design 3 OLED Stealth / Titanium Monochrome Tokens
    private static final int COLOR_SURFACE = 0xFF0D0E11;
    private static final int COLOR_SURFACE_CONTAINER_LOW = 0xFF131418;
    private static final int COLOR_SURFACE_CONTAINER = 0xFF18191F;
    private static final int COLOR_SURFACE_CONTAINER_HIGH = 0xFF22242D;
    private static final int COLOR_SURFACE_CONTAINER_HIGHEST = 0xFF2C2F3A;
    private static final int COLOR_PRIMARY = 0xFFF0F1F5;
    private static final int COLOR_ON_PRIMARY = 0xFF0E0F12;
    private static final int COLOR_PRIMARY_CONTAINER = 0xFF2B2E38;
    private static final int COLOR_ON_PRIMARY_CONTAINER = 0xFFF0F1F5;
    private static final int COLOR_SECONDARY_CONTAINER = 0xFF20222A;
    private static final int COLOR_ON_SURFACE = 0xFFEDEDF2;
    private static final int COLOR_ON_SURFACE_VARIANT = 0xFF8A8D98;
    private static final int COLOR_OUTLINE = 0xFF353844;
    private static final int COLOR_OUTLINE_VARIANT = 0xFF1C1E25;
    private static final int COLOR_DRAG_HANDLE = 0xFF4A4E5C;
    private static final int COLOR_SWITCH_TRACK_OFF = 0xFF242630;
    private static final int COLOR_SWITCH_THUMB_OFF = 0xFF6D7180;
    private static final int COLOR_SUCCESS = 0xFF34D399;
    private static final int COLOR_SCRIM = 0xD8000000;

    // Tabs
    private static final int TAB_BOOST = 0;
    private static final int TAB_GHOST = 1;
    private static final int TAB_PROFILE = 2;
    private static final int TAB_CUSTOM = 3;

    private int activeTab = TAB_BOOST;
    private int customSubTab = 0;
    private FrameLayout rootLayout;
    private View scrimView;
    private LinearLayout sheetContainer;
    private ScrollView scrollView;
    private LinearLayout contentContainer;
    private LinearLayout tabsRow;
    private TextView[] tabButtons;

    private int devTaps = 0;
    private long lastTapTime = 0;
    private boolean isClosing = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(COLOR_SURFACE_CONTAINER_LOW);
        }

        activeTab = getSharedPreferences(PREFS, 0).getInt("last_active_tab", TAB_BOOST);
        buildBottomSheetView();
        renderTab(activeTab, false);
        animateEnter();
    }

    @Override
    public void onBackPressed() {
        closeAnimated();
    }

    private static final PathInterpolator INTERPOLATOR_EMPHASIZED = new PathInterpolator(0.05f, 0.7f, 0.1f, 1.0f);
    private static final PathInterpolator INTERPOLATOR_ACCELERATE = new PathInterpolator(0.3f, 0.0f, 0.8f, 0.15f);
    private static final OvershootInterpolator INTERPOLATOR_SPRING = new OvershootInterpolator(1.3f);
    private static final OvershootInterpolator INTERPOLATOR_SHEET_SPRING = new OvershootInterpolator(0.6f);

    private static void addTouchSpring(final View view) {
        if (view == null) return;
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate()
                                .scaleX(0.95f)
                                .scaleY(0.95f)
                                .setDuration(100)
                                .setInterpolator(INTERPOLATOR_ACCELERATE)
                                .start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(260)
                                .setInterpolator(INTERPOLATOR_SPRING)
                                .start();
                        break;
                }
                return false;
            }
        });
    }

    public void closeAnimated() {
        if (isClosing) return;
        isClosing = true;

        scrimView.animate()
                .alpha(0f)
                .setDuration(220)
                .setInterpolator(INTERPOLATOR_ACCELERATE)
                .start();

        float targetY = sheetContainer.getHeight() > 0 ? sheetContainer.getHeight() : dp(650);
        sheetContainer.animate()
                .translationY(targetY)
                .scaleX(0.96f)
                .scaleY(0.96f)
                .alpha(0.0f)
                .setInterpolator(INTERPOLATOR_ACCELERATE)
                .setDuration(220)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                        overridePendingTransition(0, 0);
                    }
                })
                .start();
    }

    private void animateEnter() {
        scrimView.setAlpha(0f);
        scrimView.animate()
                .alpha(1f)
                .setDuration(280)
                .setInterpolator(INTERPOLATOR_EMPHASIZED)
                .start();

        final DisplayMetrics dm = getResources().getDisplayMetrics();
        sheetContainer.setTranslationY(dm.heightPixels * 0.80f);
        sheetContainer.setScaleX(0.94f);
        sheetContainer.setScaleY(0.94f);
        sheetContainer.setAlpha(0.2f);

        sheetContainer.animate()
                .translationY(0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setInterpolator(INTERPOLATOR_SHEET_SPRING)
                .setDuration(400)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        animateCardsCascade();
                    }
                })
                .start();
    }

    private void buildBottomSheetView() {
        rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

        // 1. Scrim
        scrimView = new View(this);
        scrimView.setBackgroundColor(COLOR_SCRIM);
        scrimView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeAnimated();
            }
        });
        rootLayout.addView(scrimView, new FrameLayout.LayoutParams(-1, -1));

        // 2. Bottom Sheet Container (M3 Extra Large shape: 28dp top corners)
        sheetContainer = new LinearLayout(this);
        sheetContainer.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(COLOR_SURFACE_CONTAINER_LOW);
        sheetBg.setCornerRadii(new float[]{dp(28), dp(28), dp(28), dp(28), 0, 0, 0, 0});
        sheetBg.setStroke(dp(1), COLOR_OUTLINE);
        sheetContainer.setBackground(sheetBg);
        sheetContainer.setClickable(true);

        final DisplayMetrics dm = getResources().getDisplayMetrics();
        final int maxHeight = (int) (dm.heightPixels * 0.88f);
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(-1, maxHeight);
        sheetLp.gravity = Gravity.BOTTOM;
        rootLayout.addView(sheetContainer, sheetLp);

        // 3. Top Drag Handle (M3 standard: 32x4dp, corner 2dp, #79747E)
        FrameLayout handleArea = new FrameLayout(this);
        handleArea.setPadding(0, dp(12), 0, dp(6));

        View handle = new View(this);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(COLOR_DRAG_HANDLE);
        handleBg.setCornerRadius(dp(2));
        handle.setBackground(handleBg);
        FrameLayout.LayoutParams handleLp = new FrameLayout.LayoutParams(dp(32), dp(4));
        handleLp.gravity = Gravity.CENTER;
        handleArea.addView(handle, handleLp);

        sheetContainer.addView(handleArea, new LinearLayout.LayoutParams(-1, -2));
        setupDragToClose(sheetContainer, handleArea);

        // 4. Header Bar
        sheetContainer.addView(buildHeaderBar(), new LinearLayout.LayoutParams(-1, -2));

        // 5. Tabs
        View tabs = buildTabsBar();
        sheetContainer.addView(tabs, tabs.getLayoutParams());

        // 6. Scrollable Content Area
        scrollView = new ScrollView(this);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scrollView.setClipToPadding(false);

        contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(dp(16), dp(4), dp(16), dp(36));

        scrollView.addView(contentContainer, new ScrollView.LayoutParams(-1, -2));
        sheetContainer.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Window insets
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            rootLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    int bottom = 0;
                    if (Build.VERSION.SDK_INT >= 30) {
                        bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                    } else {
                        bottom = insets.getSystemWindowInsetBottom();
                    }
                    contentContainer.setPadding(dp(16), dp(4), dp(16), bottom + dp(24));
                    return insets;
                }
            });
        }

        setContentView(rootLayout);
    }

    private void setupDragToClose(final View sheet, View touchArea) {
        touchArea.setOnTouchListener(new View.OnTouchListener() {
            float startY;
            float initialTransY;
            boolean isDragging;
            VelocityTracker velocityTracker;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                }
                velocityTracker.addMovement(event);

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getRawY();
                        initialTransY = sheet.getTranslationY();
                        isDragging = true;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (!isDragging) return false;
                        float dy = event.getRawY() - startY;
                        if (dy < 0) {
                            sheet.setTranslationY(initialTransY + dy * 0.2f);
                        } else {
                            sheet.setTranslationY(initialTransY + dy);
                            float progress = Math.min(1f, dy / (float) Math.max(1, sheet.getHeight()));
                            scrimView.setAlpha(Math.max(0.15f, 1f - progress * 0.85f));
                            float scale = 1.0f - progress * 0.04f;
                            sheet.setScaleX(scale);
                            sheet.setScaleY(scale);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!isDragging) return false;
                        isDragging = false;
                        velocityTracker.computeCurrentVelocity(1000);
                        float vy = velocityTracker.getYVelocity();
                        velocityTracker.recycle();
                        velocityTracker = null;

                        float totalDy = event.getRawY() - startY;
                        if (totalDy > dp(90) || vy > 1200) {
                            closeAnimated();
                        } else {
                            sheet.animate()
                                    .translationY(0f)
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setInterpolator(INTERPOLATOR_EMPHASIZED)
                                    .setDuration(260)
                                    .start();
                            scrimView.animate()
                                    .alpha(1.0f)
                                    .setInterpolator(INTERPOLATOR_EMPHASIZED)
                                    .setDuration(260)
                                    .start();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private View buildHeaderBar() {
        FrameLayout header = new FrameLayout(this);
        header.setPadding(dp(16), dp(2), dp(16), dp(8));

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(this);
        try {
            icon.setImageDrawable(getPackageManager().getApplicationIcon(getPackageName()));
        } catch (Exception ignored) {}
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        brand.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, 0, 0);

        TextView title = new TextView(this);
        title.setText("BlinkMod 2.0");
        title.setTextSize(18);
        title.setTextColor(COLOR_ON_SURFACE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleBox.addView(title);

        TextView version = new TextView(this);
        version.setText(ModConfig.getVersionDisplay(this));
        version.setTextSize(11);
        version.setTextColor(COLOR_ON_SURFACE_VARIANT);
        titleBox.addView(version);

        brand.addView(titleBox);

        brand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
            }
        });

        addTouchSpring(brand);
        FrameLayout.LayoutParams brandLp = new FrameLayout.LayoutParams(-2, -2);
        brandLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        header.addView(brand, brandLp);

        TextView closeBtn = new TextView(this);
        closeBtn.setText("×");
        closeBtn.setTextSize(20);
        closeBtn.setTextColor(COLOR_ON_SURFACE);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setColor(COLOR_SURFACE_CONTAINER_HIGH);
        closeBg.setShape(GradientDrawable.OVAL);
        closeBtn.setBackground(closeBg);
        addTouchSpring(closeBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                closeAnimated();
            }
        });

        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(dp(32), dp(32));
        closeLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        header.addView(closeBtn, closeLp);

        return header;
    }

    private View buildTabsBar() {
        tabsRow = new LinearLayout(this);
        tabsRow.setOrientation(LinearLayout.HORIZONTAL);
        tabsRow.setPadding(dp(4), dp(4), dp(4), dp(4));
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(COLOR_SURFACE_CONTAINER);
        barBg.setCornerRadius(dp(22));
        barBg.setStroke(dp(1), COLOR_OUTLINE);
        tabsRow.setBackground(barBg);

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.leftMargin = dp(16);
        rowLp.rightMargin = dp(16);
        rowLp.topMargin = dp(2);
        rowLp.bottomMargin = dp(10);
        tabsRow.setLayoutParams(rowLp);

        rebuildTabs();
        return tabsRow;
    }

    private void rebuildTabs() {
        tabsRow.removeAllViews();
        String[] tabNames = new String[]{"Буст", "Фантом", "Профиль", "Кастомизация"};
        int[] tabIds = new int[]{TAB_BOOST, TAB_GHOST, TAB_PROFILE, TAB_CUSTOM};
        tabButtons = new TextView[4];

        for (int i = 0; i < 4; i++) {
            final int id = tabIds[i];
            TextView tab = new TextView(this);
            tab.setText(tabNames[i]);
            tab.setTextSize(11);
            tab.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            tab.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1f);
            if (i > 0) lp.leftMargin = dp(4);
            tabsRow.addView(tab, lp);
            tabButtons[i] = tab;
            addTouchSpring(tab);

            tab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    haptic(v);
                    renderTab(id, true);
                }
            });
        }
        updateTabStyles();
    }

    private void updateTabStyles() {
        if (tabButtons == null) return;
        int[] tabIds = new int[]{TAB_BOOST, TAB_GHOST, TAB_PROFILE, TAB_CUSTOM};
        for (int i = 0; i < tabButtons.length; i++) {
            TextView tab = tabButtons[i];
            boolean selected = (tabIds[i] == activeTab);
            tab.setTextColor(selected ? COLOR_ON_PRIMARY_CONTAINER : COLOR_ON_SURFACE_VARIANT);
            GradientDrawable tabBg = new GradientDrawable();
            if (selected) {
                tabBg.setColor(COLOR_PRIMARY_CONTAINER);
                tabBg.setCornerRadius(dp(18));
            } else {
                tabBg.setColor(Color.TRANSPARENT);
            }
            tab.setBackground(tabBg);
        }
    }

    private void renderTab(final int tab, boolean animate) {
        final int prevTab = activeTab;
        activeTab = tab;
        getSharedPreferences(PREFS, 0).edit().putInt("last_active_tab", tab).apply();
        updateTabStyles();

        if (animate && prevTab != tab) {
            final int direction = (tab > prevTab) ? 1 : -1;
            contentContainer.animate()
                    .alpha(0.0f)
                    .translationX(-direction * dp(24))
                    .setDuration(110)
                    .setInterpolator(INTERPOLATOR_ACCELERATE)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            populateTabContent(tab);
                            scrollView.scrollTo(0, 0);
                            contentContainer.setTranslationX(direction * dp(28));
                            contentContainer.setAlpha(0f);
                            contentContainer.animate()
                                    .alpha(1f)
                                    .translationX(0f)
                                    .setDuration(220)
                                    .setInterpolator(INTERPOLATOR_EMPHASIZED)
                                    .start();
                            animateCardsCascade();
                        }
                    })
                    .start();
        } else {
            populateTabContent(tab);
            scrollView.scrollTo(0, 0);
            contentContainer.setTranslationX(0f);
            contentContainer.setAlpha(1f);
            if (animate) animateCardsCascade();
        }
    }

    private void animateCardsCascade() {
        int count = contentContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = contentContainer.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(dp(16));
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(280)
                    .setStartDelay(i * 35)
                    .setInterpolator(INTERPOLATOR_EMPHASIZED)
                    .start();
        }
    }

    private void populateTabContent(int tab) {
        contentContainer.removeAllViews();
        switch (tab) {
            case TAB_BOOST:
                renderBoostTab(contentContainer);
                break;
            case TAB_GHOST:
                renderGhostTab(contentContainer);
                break;
            case TAB_PROFILE:
                renderProfileTab(contentContainer);
                break;
            case TAB_CUSTOM:
                renderCustomTab(contentContainer);
                break;
        }
    }

    // =========================================================================
    // 1. БУСТ (BOOST TAB)
    // =========================================================================
    private void renderBoostTab(LinearLayout container) {
        // --- Flexible Steps Card ---
        LinearLayout stepsCard = card();
        sectionTitle(stepsCard, "ШАГИ");

        final SharedPreferences sp = getSharedPreferences(PREFS, 0);
        final int targetSteps = sp.getInt("target_steps_count", 100000);
        String formattedSteps = String.format(Locale.US, "%,d", targetSteps).replace(',', ' ');

        TextView postStepsBtn = buttonPrimary("Отправить " + formattedSteps + " шагов");
        postStepsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                haptic(v);
                try {
                    int stepsToSend = sp.getInt("target_steps_count", 100000);
                    long now = System.currentTimeMillis();
                    String iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(new Date(now));
                    StepHooks.onApiStarted();
                    Class<?> c = Class.forName("com.blinkmap.mod.WorkBridge");
                    try {
                        c.getMethod("postSteps", Context.class, Integer.TYPE, Long.TYPE, String.class)
                                .invoke(null, getApplicationContext(), stepsToSend, now, iso);
                    } catch (NoSuchMethodException nsme) {
                        c.getMethod("post100k", Context.class, Long.TYPE, String.class)
                                .invoke(null, getApplicationContext(), now, iso);
                    }

                    Toast.makeText(ModSettingsActivity.this, "Запрос отправлен", Toast.LENGTH_SHORT).show();
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (activeTab == TAB_BOOST) renderTab(TAB_BOOST, false);
                        }
                    }, 1800);
                } catch (Throwable e) {
                    StepHooks.onApiError(e);
                    Toast.makeText(ModSettingsActivity.this, "Ошибка: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
                }
            }
        });
        stepsCard.addView(postStepsBtn);

        // Presets: 10k, 25k, 50k, 100k, Своё
        LinearLayout stepsPills = new LinearLayout(this);
        stepsPills.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(-1, -2);
        spLp.topMargin = dp(8);
        stepsPills.setLayoutParams(spLp);

        final int[] presetSteps = {10000, 25000, 50000, 100000};
        final String[] presetLabels = {"10k", "25k", "50k", "100k"};

        boolean isCustom = true;
        for (int pVal : presetSteps) {
            if (targetSteps == pVal) {
                isCustom = false;
                break;
            }
        }

        final TextView[] presetPillsList = new TextView[presetSteps.length];
        final TextView[] customPillRef = new TextView[1];

        for (int i = 0; i < presetSteps.length; i++) {
            final int index = i;
            final int sVal = presetSteps[i];
            boolean selected = (targetSteps == sVal);
            TextView pill = createPill(presetLabels[i], selected);
            presetPillsList[i] = pill;
            if (i > 0) ((LinearLayout.LayoutParams) pill.getLayoutParams()).leftMargin = dp(6);
            stepsPills.addView(pill);

            pill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    haptic(v);
                    sp.edit().putInt("target_steps_count", sVal).apply();
                    postStepsBtn.setText("Отправить " + String.format(Locale.US, "%,d", sVal).replace(',', ' ') + " шагов");
                    for (int j = 0; j < presetPillsList.length; j++) {
                        updatePillStyle(presetPillsList[j], j == index);
                    }
                    if (customPillRef[0] != null) {
                        customPillRef[0].setText("Своё");
                        updatePillStyle(customPillRef[0], false);
                    }
                }
            });
        }

        String customLabel = isCustom ? (targetSteps >= 1000 ? (targetSteps / 1000 + "k") : String.valueOf(targetSteps)) : "Своё";
        final TextView customPill = createPill(customLabel, isCustom);
        customPillRef[0] = customPill;
        ((LinearLayout.LayoutParams) customPill.getLayoutParams()).leftMargin = dp(6);
        customPill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                showCustomStepsDialog(postStepsBtn, presetPillsList, customPill);
            }
        });
        stepsPills.addView(customPill);
        stepsCard.addView(stepsPills);

        String lastStepState = getSharedPreferences(PREFS, 0).getString("api_steps_state", "Запросов пока не было");
        int verifiedSteps = getSharedPreferences(PREFS, 0).getInt("api_steps_verified", 0);

        Md3ProgressBar stepsProgress = new Md3ProgressBar(this);
        stepsProgress.setColors(COLOR_SURFACE_CONTAINER_HIGHEST, COLOR_PRIMARY, COLOR_DRAG_HANDLE);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(8));
        pbLp.topMargin = dp(14);
        pbLp.bottomMargin = dp(6);
        stepsProgress.setLayoutParams(pbLp);

        if (lastStepState.contains("Отправляется") || lastStepState.contains("Проверяем")) {
            stepsProgress.setIndeterminate(true);
        } else {
            float p = Math.min(1.0f, (float) verifiedSteps / (float) Math.max(1, targetSteps));
            stepsProgress.setProgress(p, false);
        }
        stepsCard.addView(stepsProgress);

        stepsCard.addView(statusInfoRow("Статус шагов", lastStepState));
        stepsCard.addView(line());

        // Multiplier switch & selector
        stepsCard.addView(toggleRow("Умножать шаги при синхронизации",
                "Автоматически масштабирует входящие шаги",
                "network_steps_enabled", false, null));
        stepsCard.addView(line());

        stepsCard.addView(multiplierSelector("Множитель отправки", "network_steps_multiplier", new int[]{1, 2, 5, 10}));
        container.addView(stepsCard, bottomMargin(14));
    }

    // =========================================================================
    // 2. ФАНТОМ (GHOST / PRIVACY TAB)
    // =========================================================================
    private void renderGhostTab(LinearLayout container) {
        // --- Location Spoofing Card ---
        LinearLayout geoCard = card();
        sectionTitle(geoCard, "ПОДМЕНА ГЕОЛОКАЦИИ");

        boolean isSpoofing = StepHooks.isSpoofingActive();
        View badgeRow = badgeStatusRow("Режим координат",
                isSpoofing ? "Фейк активен" : "Реальный GPS",
                isSpoofing ? COLOR_SUCCESS : COLOR_ON_SURFACE_VARIANT);
        final TextView badgeModeText = (TextView) ((LinearLayout) badgeRow).getChildAt(1);
        geoCard.addView(badgeRow);

        View coordsRow = statusInfoRow("Координаты", StepHooks.customCoordsText());
        final TextView coordsValText = (TextView) ((LinearLayout) coordsRow).getChildAt(1);
        geoCard.addView(coordsRow);
        geoCard.addView(line());

        final Runnable updateGeoHeader = new Runnable() {
            @Override
            public void run() {
                boolean active = StepHooks.isSpoofingActive();
                badgeModeText.setText(active ? "Фейк активен" : "Реальный GPS");
                updateBadgeStyle(badgeModeText, active ? COLOR_SUCCESS : COLOR_ON_SURFACE_VARIANT);
                coordsValText.setText(StepHooks.customCoordsText());
            }
        };

        // Big Map Picker Button
        TextView mapPickerBtn = buttonPrimary("Выбрать точку на карте");
        mapPickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                final Activity target = MapPicker.getTopActivity();
                closeAnimated();
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MapPicker.showPicker(target);
                    }
                }, 180);
            }
        });
        geoCard.addView(mapPickerBtn);

        // Sub-actions row: Paste, Pin current, Reset (M3 Outlined buttons)
        LinearLayout quickBtns = new LinearLayout(this);
        quickBtns.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(-1, -2);
        qp.topMargin = dp(8);
        quickBtns.setLayoutParams(qp);

        TextView btnPaste = buttonOutlined("Из буфера");
        btnPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                if (StepHooks.setCustomLocationFromClipboard(ModSettingsActivity.this)) {
                    Toast.makeText(ModSettingsActivity.this, "Координаты применены", Toast.LENGTH_SHORT).show();
                    updateGeoHeader.run();
                } else {
                    Toast.makeText(ModSettingsActivity.this, "В буфере нет координат", Toast.LENGTH_SHORT).show();
                }
            }
        });

        TextView btnPin = buttonOutlined("Зафиксировать");
        btnPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                StepHooks.setCustomLocationFromCurrent(ModSettingsActivity.this);
                Toast.makeText(ModSettingsActivity.this, "Позиция зафиксирована", Toast.LENGTH_SHORT).show();
                updateGeoHeader.run();
            }
        });

        TextView btnReset = buttonOutlined("Сброс GPS");
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                StepHooks.resetCustomLocation(ModSettingsActivity.this);
                Toast.makeText(ModSettingsActivity.this, "Возврат к реальному GPS", Toast.LENGTH_SHORT).show();
                updateGeoHeader.run();
            }
        });

        quickBtns.addView(btnPaste, new LinearLayout.LayoutParams(0, dp(40), 1f));
        quickBtns.addView(btnPin, createMarginedParams(dp(6)));
        quickBtns.addView(btnReset, createMarginedParams(dp(6)));
        geoCard.addView(quickBtns);

        container.addView(geoCard, bottomMargin(14));

        renderFollowerCard(container, updateGeoHeader);

        // --- Smart Drift Card ---
        LinearLayout driftCard = card();
        sectionTitle(driftCard, "УМНЫЙ ДРЕЙФ");
        TextView driftDesc = textSmall("Имитирует живую ходьбу с шагами и курсом, исключая подозрения в статичной позиции.");
        driftCard.addView(driftDesc);

        LinearLayout driftPills = new LinearLayout(this);
        driftPills.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams dpLp = new LinearLayout.LayoutParams(-1, -2);
        dpLp.topMargin = dp(8);
        driftPills.setLayoutParams(dpLp);

        int currentDrift = StepHooks.driftMode;
        final int[] driftValues = {0, 1, 2, 3, 4};
        final String[] driftLabels = {"Выкл", "±10м", "Ходьба", "Бег", "Авто"};
        final TextView[] driftPillsList = new TextView[driftValues.length];

        for (int i = 0; i < driftValues.length; i++) {
            final int index = i;
            final int mode = driftValues[i];
            TextView pill = createPill(driftLabels[i], currentDrift == mode);
            driftPillsList[i] = pill;
            if (i > 0) ((LinearLayout.LayoutParams) pill.getLayoutParams()).leftMargin = dp(6);
            driftPills.addView(pill);

            pill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    haptic(v);
                    StepHooks.setDriftMode(mode);
                    for (int j = 0; j < driftPillsList.length; j++) {
                        updatePillStyle(driftPillsList[j], j == index);
                    }
                    updateGeoHeader.run();
                }
            });
        }
        driftCard.addView(driftPills);
        container.addView(driftCard, bottomMargin(14));

        // --- Freeze / Pause Card ---
        LinearLayout pauseCard = card();
        sectionTitle(pauseCard, "ЗАМОРОЗКА ГЕОЛОКАЦИИ");
        View pauseStatusRow = statusInfoRow("Состояние", StepHooks.pauseStatus());
        final TextView pauseStatusVal = (TextView) ((LinearLayout) pauseStatusRow).getChildAt(1);
        pauseCard.addView(pauseStatusRow);
        pauseCard.addView(line());

        LinearLayout pausePills = new LinearLayout(this);
        pausePills.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ppLp = new LinearLayout.LayoutParams(-1, -2);
        ppLp.topMargin = dp(8);
        pausePills.setLayoutParams(ppLp);

        final long[] pauseTimes = {15 * 60 * 1000L, 60 * 60 * 1000L};
        final String[] pauseLabels = {"15 мин", "1 час"};

        for (int i = 0; i < pauseTimes.length; i++) {
            final long dur = pauseTimes[i];
            TextView pill = buttonOutlined(pauseLabels[i]);
            if (i > 0) ((LinearLayout.LayoutParams) pill.getLayoutParams()).leftMargin = dp(6);
            pausePills.addView(pill);
            pill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    haptic(v);
                    StepHooks.pauseFor(dur);
                    pauseStatusVal.setText(StepHooks.pauseStatus());
                    updateGeoHeader.run();
                }
            });
        }

        TextView pillForever = buttonOutlined("Навсегда");
        ((LinearLayout.LayoutParams) pillForever.getLayoutParams()).leftMargin = dp(6);
        pillForever.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                StepHooks.setPauseLocation(true);
                pauseStatusVal.setText(StepHooks.pauseStatus());
                updateGeoHeader.run();
            }
        });
        pausePills.addView(pillForever);

        TextView pillResume = buttonTonal("Снять");
        ((LinearLayout.LayoutParams) pillResume.getLayoutParams()).leftMargin = dp(6);
        pillResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                StepHooks.resumeLocation();
                pauseStatusVal.setText(StepHooks.pauseStatus());
                updateGeoHeader.run();
            }
        });
        pausePills.addView(pillResume);

        pauseCard.addView(pausePills);
        container.addView(pauseCard, bottomMargin(14));

        // --- Battery Spoofing Card ---
        LinearLayout batteryCard = card();
        sectionTitle(batteryCard, "ЗАРЯД БАТАРЕИ");

        final LinearLayout batPills = new LinearLayout(this);
        batPills.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bpLp = new LinearLayout.LayoutParams(-1, -2);
        bpLp.topMargin = dp(6);
        batPills.setLayoutParams(bpLp);

        final int[] batVals = {-1, 100, 50, 1};
        final String[] batNames = {"Реальный", "100%", "50%", "1%"};
        int curCharge = StepHooks.forcedCharge;
        boolean hideCharge = StepHooks.hideCharge;

        final TextView[] batPillsList = new TextView[batVals.length];
        for (int i = 0; i < batVals.length; i++) {
            final int index = i;
            final int val = batVals[i];
            boolean selected = !hideCharge && (curCharge == val);
            TextView pill = createPill(batNames[i], selected);
            batPillsList[i] = pill;
            if (i > 0) ((LinearLayout.LayoutParams) pill.getLayoutParams()).leftMargin = dp(6);
            batPills.addView(pill);

            pill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    haptic(v);
                    StepHooks.setForcedCharge(val);
                    for (int j = 0; j < batPillsList.length; j++) {
                        updatePillStyle(batPillsList[j], j == index);
                    }
                }
            });
        }

        batteryCard.addView(toggleRow("Скрыть заряд батареи",
                "Передаёт значение заряда как null",
                "hide_charge", false, new ToggleSwitchView.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(ToggleSwitchView view, boolean isChecked) {
                        StepHooks.setHideCharge(isChecked);
                        if (isChecked) {
                            for (TextView p : batPillsList) {
                                updatePillStyle(p, false);
                            }
                        } else {
                            int charge = StepHooks.forcedCharge;
                            for (int j = 0; j < batVals.length; j++) {
                                updatePillStyle(batPillsList[j], batVals[j] == charge);
                            }
                        }
                    }
                }));
        batteryCard.addView(line());

        TextView batLabel = textSmall("Фиксированный уровень:");
        batteryCard.addView(batLabel);
        batteryCard.addView(batPills);
        container.addView(batteryCard, bottomMargin(14));

        // --- Ghost Suite Card ---
        LinearLayout ghostCard = card();
        sectionTitle(ghostCard, "GHOST SUITE (СТЕЛС И МЕССЕНДЖЕР)");

        ghostCard.addView(toggleRow("Инкогнито войсов и кружков",
                "Не отправлять отчет о прослушивании/просмотре на сервер",
                "ghost_voice_active", StepHooks.isGhostVoiceActive(), new ToggleSwitchView.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(ToggleSwitchView view, boolean isChecked) {
                        StepHooks.setGhostVoiceActive(isChecked);
                    }
                }));
        ghostCard.addView(line());

        ghostCard.addView(toggleRow("Анти-спуф щит (Jammer Shield)",
                "Блокирует маркер фейк-GPS от отображения у друзей на карте",
                "anti_spoof_shield", StepHooks.isAntiSpoofShieldActive(), new ToggleSwitchView.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(ToggleSwitchView view, boolean isChecked) {
                        StepHooks.setAntiSpoofShieldActive(isChecked);
                    }
                }));
        container.addView(ghostCard, bottomMargin(14));

        // --- Abuse Suite Card ---
        LinearLayout abuseCard = card();
        sectionTitle(abuseCard, "ABUSE SUITE");
        abuseCard.addView(textSmall("Удаленный вызов серверных механик без физических действий."));

        TextView bumpBtn = buttonPrimary("Дистанционный бамп (POST /api/bump)");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(8);
        bumpBtn.setLayoutParams(bp);
        bumpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                try {
                    Class<?> c = Class.forName("com.blinkmap.mod.WorkBridge");
                    c.getMethod("triggerBump", Context.class).invoke(null, ModSettingsActivity.this);
                } catch (Throwable t) {
                    Toast.makeText(ModSettingsActivity.this, "Ошибка: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        abuseCard.addView(bumpBtn);
        container.addView(abuseCard, bottomMargin(14));
    }

    // =========================================================================
    // 3. ПРОФИЛЬ (PROFILE TAB)
    // =========================================================================
    private void renderProfileTab(LinearLayout container) {
        SharedPreferences p = getSharedPreferences(PREFS, 0);

        // --- Profile Card ---
        LinearLayout profCard = card();
        sectionTitle(profCard, "АККАУНТ");

        String name = p.getString("profile_name", "—");
        String username = p.getString("profile_username", "");
        String accountId = p.getString("profile_account_id", "—");
        String badge = BadgeManager.getBadge(username, accountId);

        profCard.addView(copyRow("Имя", name, "Имя скопировано"));
        profCard.addView(line());
        profCard.addView(copyRow("Username", username.isEmpty() ? "—" : "@" + username, "Username скопирован"));
        profCard.addView(line());
        profCard.addView(copyRow("Account ID", accountId, "ID скопирован"));
        profCard.addView(line());
        profCard.addView(copyRow("Бейдж", badge == null ? "Обычный" : badge, "Бейдж скопирован"));
        profCard.addView(line());

        LinearLayout actRow = new LinearLayout(this);
        actRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = dp(10);
        actRow.setLayoutParams(ap);

        TextView btnRefresh = buttonOutlined("Обновить данные");
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                haptic(v);
                StepHooks.onProfileStarted();
                try {
                    Class<?> c = Class.forName("com.blinkmap.mod.WorkBridge");
                    c.getMethod("loadMyProfile", Context.class).invoke(null, getApplicationContext());
                    Toast.makeText(ModSettingsActivity.this, "Запрос профиля...", Toast.LENGTH_SHORT).show();
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (activeTab == TAB_PROFILE) renderTab(TAB_PROFILE, false);
                        }
                    }, 1400);
                } catch (Throwable e) {
                    StepHooks.onProfileError(e);
                }
            }
        });

        TextView btnToken = buttonPrimary("Вход по токену");
        btnToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                TokenLoginOverlay.showTokenDialog(ModSettingsActivity.this);
            }
        });

        TextView btnGms = buttonSecondary("Google (microG)");
        btnGms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                GmsCoreHelper.showGuideDialog(ModSettingsActivity.this);
            }
        });

        actRow.addView(btnRefresh, new LinearLayout.LayoutParams(0, dp(44), 1f));
        actRow.addView(btnToken, createMarginedParams(dp(8)));
        actRow.addView(btnGms, createMarginedParams(dp(8)));
        profCard.addView(actRow);

        container.addView(profCard, bottomMargin(14));

        // --- About Mod Card ---
        LinearLayout aboutCard = card();
        sectionTitle(aboutCard, "О МОДЕ");

        TextView aboutDesc = textSmall("BlinkMod " + ModConfig.VERSION_PUBLIC + " — приватность, шаги и путешествия.");
        aboutCard.addView(aboutDesc);

        TextView tgBtn = buttonPrimary("Канал проекта @blinkmod");
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, dp(48));
        tp.topMargin = dp(10);
        tgBtn.setLayoutParams(tp);
        tgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/blinkmod")));
                } catch (Throwable e) {
                    Toast.makeText(ModSettingsActivity.this, "Не удалось открыть Telegram", Toast.LENGTH_SHORT).show();
                }
            }
        });
        aboutCard.addView(tgBtn);
        container.addView(aboutCard, bottomMargin(14));

        // --- Creator Card ---
        BadgeManager.TeamMember creator = BadgeManager.getCreator();
        if (creator != null) {
            LinearLayout creatorCard = card();
            sectionTitle(creatorCard, "СОЗДАТЕЛЬ");
            creatorCard.addView(teamMemberRow(creator));
            container.addView(creatorCard, bottomMargin(14));
        }

        // --- Helpers & Credits Card ---
        java.util.List<BadgeManager.TeamMember> helpers = BadgeManager.getHelpers();
        if (helpers != null && !helpers.isEmpty()) {
            LinearLayout helpersCard = card();
            sectionTitle(helpersCard, BadgeManager.getHelpersTitle());
            String sub = BadgeManager.getHelpersSubtitle();
            if (sub != null && !sub.isEmpty()) {
                TextView subTv = textSmall(sub);
                subTv.setPadding(0, 0, 0, dp(12));
                helpersCard.addView(subTv);
            }
            for (int i = 0; i < helpers.size(); i++) {
                helpersCard.addView(teamMemberRow(helpers.get(i)));
                if (i < helpers.size() - 1) {
                    helpersCard.addView(line());
                }
            }
            container.addView(helpersCard, bottomMargin(14));
        }
    }

    // =========================================================================
    // 4. КАСТОМИЗАЦИЯ (CUSTOM TAB)
    // =========================================================================
    private void renderCustomTab(LinearLayout container) {
        LinearLayout subTabsRow = new LinearLayout(this);
        subTabsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(-1, -2);
        stLp.bottomMargin = dp(14);
        subTabsRow.setLayoutParams(stLp);

        String[] subTabNames = new String[]{"Интерфейс", "Реклама и чаты"};
        if (customSubTab > 1) customSubTab = 0;

        final TextView[] pills = new TextView[subTabNames.length];
        for (int i = 0; i < subTabNames.length; i++) {
            final int subIdx = i;
            TextView pill = createPill(subTabNames[i], customSubTab == subIdx);
            pills[i] = pill;
            if (i > 0) ((LinearLayout.LayoutParams) pill.getLayoutParams()).leftMargin = dp(6);
            subTabsRow.addView(pill);

            pill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    haptic(v);
                    customSubTab = subIdx;
                    renderTab(TAB_CUSTOM, false);
                }
            });
        }
        container.addView(subTabsRow);

        if (customSubTab == 0) {
            // === ПОДВКЛАДКА: ИНТЕРФЕЙС ===
            LinearLayout uiCard = card();
            sectionTitle(uiCard, "ИНТЕРФЕЙС И НАВИГАЦИЯ");

            TextView navNote = textSmall("Кнопка мода нативно встроена в нижний бар. Можно скрыть отдельные вкладки — приложение не сломается.");
            uiCard.addView(navNote);

            uiCard.addView(line());
            sectionTitle(uiCard, "ВКЛАДКИ НИЖНЕГО БАРА");

            final String[] tabKeys  = BlinkModButton.TAB_KEYS;
            final String[] tabNames = BlinkModButton.TAB_NAMES;
            for (int i = 0; i < tabKeys.length; i++) {
                String sub = (i == 5) ? "Если скрыть — открывать мод можно через плавающую кнопку" : null;
                uiCard.addView(toggleRow(tabNames[i], sub, tabKeys[i], true,
                        new ToggleSwitchView.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(ToggleSwitchView view, boolean isChecked) {
                                BlinkModButton.applyTabVisibility(getApplicationContext());
                            }
                        }));
                if (i < tabKeys.length - 1) uiCard.addView(line());
            }
            container.addView(uiCard, bottomMargin(14));

            LinearLayout floatCard = card();
            sectionTitle(floatCard, "ПЛАВАЮЩАЯ КНОПКА");
            floatCard.addView(toggleRow("Плавающая кнопка",
                    "Включить дополнительную плавающую кнопку на экране",
                    "floating_enabled", false, new ToggleSwitchView.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(ToggleSwitchView view, boolean isChecked) {
                            if (MapPicker.mainActivity != null) {
                                BlinkModButton.inject(MapPicker.mainActivity);
                            }
                        }
                    }));
            floatCard.addView(line());
            floatCard.addView(toggleRow("Плавающая кнопка слева",
                    "Разместить плавающую кнопку у левого края экрана",
                    "floating_left", false, new ToggleSwitchView.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(ToggleSwitchView view, boolean isChecked) {
                            if (MapPicker.mainActivity != null) {
                                BlinkModButton.inject(MapPicker.mainActivity);
                            }
                        }
                    }));
            container.addView(floatCard, bottomMargin(14));

        } else if (customSubTab == 1) {
            // === ПОДВКЛАДКА: РЕКЛАМА И ЧАТЫ ===
            LinearLayout chatCard = card();
            sectionTitle(chatCard, "СИСТЕМНЫЕ КАНАЛЫ");
            chatCard.addView(toggleRow("Канал «BlinkMod News»", "Заменить скучный системный чат на BlinkMod News («скоро тут что то будет»)", "replace_blink_news", true, null));
            chatCard.addView(line());
            chatCard.addView(toggleRow("Скрыть новостной канал", "Полностью скрыть системный/новостной канал из списка чатов", "hide_blink_news", false, null));
            container.addView(chatCard, bottomMargin(14));

            LinearLayout adCard = card();
            sectionTitle(adCard, "БЛОКИРОВКА И ПРИВАТНОСТЬ");
            adCard.addView(toggleRow("Блокировка рекламы", "Скрыть рекламные баннеры Aviasales, шторки и промо-слои на карте", "adblock_enabled", true, null));
            adCard.addView(line());
            adCard.addView(toggleRow("Отключить трекеры", "Блокировать сбор аналитики и телеметрии (AppsFlyer, MyTracker)", "block_analytics_enabled", true, null));
            container.addView(adCard, bottomMargin(14));
        }
    }

    // =========================================================================
    // MATERIAL DESIGN 3 SWITCH VIEW (52x32dp, track #36343B / Primary, thumb #938F99 / OnPrimary)
    // =========================================================================
    public static final class ToggleSwitchView extends View {
        private boolean isChecked;
        private float thumbProgress = 0f;
        private ValueAnimator animator;
        private OnCheckedChangeListener listener;

        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint trackBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF trackRect = new RectF();
        private final RectF borderRect = new RectF();

        public interface OnCheckedChangeListener {
            void onCheckedChanged(ToggleSwitchView view, boolean checked);
        }

        public ToggleSwitchView(Context context) {
            super(context);
            trackBorderPaint.setStyle(Paint.Style.STROKE);
            setClickable(false);
            setFocusable(false);
        }

        public void setChecked(boolean checked, boolean animate) {
            if (this.isChecked == checked && animator == null) return;
            this.isChecked = checked;
            float target = checked ? 1f : 0f;
            if (animate) {
                if (animator != null) animator.cancel();
                animator = ValueAnimator.ofFloat(thumbProgress, target);
                animator.setDuration(180);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        thumbProgress = (float) animation.getAnimatedValue();
                        invalidate();
                    }
                });
                animator.start();
            } else {
                thumbProgress = target;
                invalidate();
            }
        }

        public boolean isChecked() {
            return isChecked;
        }

        public void toggle(boolean animate) {
            setChecked(!isChecked, animate);
            if (listener != null) {
                listener.onCheckedChanged(this, isChecked);
            }
        }

        public void setOnCheckedChangeListener(OnCheckedChangeListener l) {
            this.listener = l;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            float d = getResources().getDisplayMetrics().density;
            setMeasuredDimension(Math.round(52 * d), Math.round(32 * d));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float d = getResources().getDisplayMetrics().density;
            float w = getWidth();
            float h = getHeight();
            float radius = h / 2f;

            // Track background interpolation: #36343B (off) -> Primary #D0BCFF (on)
            int rTrack = (int) (Color.red(COLOR_SWITCH_TRACK_OFF) + thumbProgress * (Color.red(COLOR_PRIMARY) - Color.red(COLOR_SWITCH_TRACK_OFF)));
            int gTrack = (int) (Color.green(COLOR_SWITCH_TRACK_OFF) + thumbProgress * (Color.green(COLOR_PRIMARY) - Color.green(COLOR_SWITCH_TRACK_OFF)));
            int bTrack = (int) (Color.blue(COLOR_SWITCH_TRACK_OFF) + thumbProgress * (Color.blue(COLOR_PRIMARY) - Color.blue(COLOR_SWITCH_TRACK_OFF)));
            trackPaint.setColor(Color.rgb(rTrack, gTrack, bTrack));

            trackRect.set(0, 0, w, h);
            canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

            // Subtle outline for unchecked state (fades out as it turns on)
            if (thumbProgress < 0.99f) {
                int outlineAlpha = Math.round((1f - thumbProgress) * 255f);
                trackBorderPaint.setColor((outlineAlpha << 24) | (COLOR_OUTLINE & 0x00FFFFFF));
                trackBorderPaint.setStrokeWidth(d);
                float halfStroke = d / 2f;
                borderRect.set(halfStroke, halfStroke, w - halfStroke, h - halfStroke);
                canvas.drawRoundRect(borderRect, radius - halfStroke, radius - halfStroke, trackBorderPaint);
            }

            // Thumb color interpolation: #938F99 (off) -> OnPrimary #381E72 (on)
            int rThumb = (int) (Color.red(COLOR_SWITCH_THUMB_OFF) + thumbProgress * (Color.red(COLOR_ON_PRIMARY) - Color.red(COLOR_SWITCH_THUMB_OFF)));
            int gThumb = (int) (Color.green(COLOR_SWITCH_THUMB_OFF) + thumbProgress * (Color.green(COLOR_ON_PRIMARY) - Color.green(COLOR_SWITCH_THUMB_OFF)));
            int bThumb = (int) (Color.blue(COLOR_SWITCH_THUMB_OFF) + thumbProgress * (Color.blue(COLOR_ON_PRIMARY) - Color.blue(COLOR_SWITCH_THUMB_OFF)));
            thumbPaint.setColor(Color.rgb(rThumb, gThumb, bThumb));

            // Thumb size: M3 specs: off = 16dp diameter (radius 8dp), on = 24dp diameter (radius 12dp)
            float thumbRadius = (8f + 4f * thumbProgress) * d;
            // Center X: off = 16dp, on = 36dp (w - 16dp)
            float startX = 16f * d;
            float endX = w - 16f * d;
            float curX = startX + thumbProgress * (endX - startX);

            canvas.drawCircle(curX, h / 2f, thumbRadius, thumbPaint);
        }
    }

    // =========================================================================
    // UI BUILDER HELPERS (MATERIAL DESIGN 3)
    // =========================================================================
    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_SURFACE_CONTAINER);
        bg.setCornerRadius(dp(16)); // M3 Large token (16dp)
        bg.setStroke(dp(1), COLOR_OUTLINE_VARIANT); // M3 Subtle hairline border (1dp)
        card.setBackground(bg);
        int pad = dp(16);
        card.setPadding(pad, pad, pad, pad);
        return card;
    }

    private void sectionTitle(LinearLayout parent, String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(12);
        tv.setTextColor(COLOR_PRIMARY);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tv.setLetterSpacing(0.08f);
        tv.setPadding(0, 0, 0, dp(10));
        parent.addView(tv);
    }

    private TextView textSmall(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(COLOR_ON_SURFACE_VARIANT);
        tv.setLineSpacing(dp(2), 1.15f);
        return tv;
    }

    // M3 FilledButton: Primary (#D0BCFF) background, OnPrimary (#381E72) text, Full Pill
    private TextView buttonPrimary(String text) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(14);
        btn.setTextColor(COLOR_ON_PRIMARY);
        btn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setMinimumHeight(dp(48));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_PRIMARY);
        bg.setCornerRadius(dp(24));
        btn.setBackground(bg);
        addTouchSpring(btn);
        return btn;
    }

    // M3 TonalButton: Primary Container (#2B2E38) background, OnPrimary Container (#F0F1F5) text, Full Pill
    private TextView buttonTonal(String text) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(12);
        btn.setSingleLine(true);
        btn.setEllipsize(android.text.TextUtils.TruncateAt.END);
        btn.setTextColor(COLOR_ON_PRIMARY_CONTAINER);
        btn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setMinimumHeight(dp(40));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_PRIMARY_CONTAINER);
        bg.setCornerRadius(dp(20));
        btn.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        btn.setLayoutParams(lp);
        addTouchSpring(btn);
        return btn;
    }

    // M3 OutlinedButton: Transparent background, Outline (#353844) 1dp border, OnSurface (#EDEDF2) text, Full Pill
    private TextView buttonOutlined(String text) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextSize(12);
        btn.setSingleLine(true);
        btn.setEllipsize(android.text.TextUtils.TruncateAt.END);
        btn.setTextColor(COLOR_ON_SURFACE);
        btn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setMinimumHeight(dp(40));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.TRANSPARENT);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), COLOR_OUTLINE);
        btn.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        btn.setLayoutParams(lp);
        addTouchSpring(btn);
        return btn;
    }

    private TextView buttonSecondary(String text) {
        return buttonOutlined(text);
    }

    // M3 Segmented Button / Filter Chip Pill
    private TextView createPill(String text, boolean selected) {
        TextView pill = new TextView(this);
        pill.setText(text);
        pill.setTextSize(13);
        pill.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pill.setGravity(Gravity.CENTER);
        pill.setTextColor(selected ? COLOR_ON_PRIMARY_CONTAINER : COLOR_ON_SURFACE_VARIANT);

        GradientDrawable bg = new GradientDrawable();
        if (selected) {
            bg.setColor(COLOR_PRIMARY_CONTAINER);
            bg.setStroke(dp(1), COLOR_PRIMARY);
        } else {
            bg.setColor(COLOR_SURFACE_CONTAINER_HIGH);
            bg.setStroke(dp(1), COLOR_OUTLINE);
        }
        bg.setCornerRadius(dp(19));
        pill.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        pill.setLayoutParams(lp);
        addTouchSpring(pill);
        return pill;
    }

    private void updatePillStyle(TextView pill, boolean selected) {
        if (pill == null) return;
        pill.setTextColor(selected ? COLOR_ON_PRIMARY_CONTAINER : COLOR_ON_SURFACE_VARIANT);
        GradientDrawable bg = new GradientDrawable();
        if (selected) {
            bg.setColor(COLOR_PRIMARY_CONTAINER);
            bg.setStroke(dp(1), COLOR_PRIMARY);
        } else {
            bg.setColor(COLOR_SURFACE_CONTAINER_HIGH);
            bg.setStroke(dp(1), COLOR_OUTLINE);
        }
        bg.setCornerRadius(dp(19));
        pill.setBackground(bg);
    }

    private void updateBadgeStyle(TextView b, int badgeColor) {
        if (b == null) return;
        b.setTextColor(badgeColor == COLOR_SUCCESS ? COLOR_ON_PRIMARY : COLOR_ON_SURFACE_VARIANT);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(badgeColor == COLOR_SUCCESS ? COLOR_SUCCESS : COLOR_SURFACE_CONTAINER_HIGH);
        bg.setCornerRadius(dp(12));
        if (badgeColor != COLOR_SUCCESS) {
            bg.setStroke(dp(1), COLOR_OUTLINE);
        }
        b.setBackground(bg);
    }

    private View statusInfoRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(14);
        l.setTextColor(COLOR_ON_SURFACE);
        row.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(13);
        v.setTextColor(COLOR_ON_SURFACE_VARIANT);
        v.setGravity(Gravity.END);
        row.addView(v, new LinearLayout.LayoutParams(-2, -2));
        return row;
    }

    private View badgeStatusRow(String label, String badgeText, int badgeColor) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(14);
        l.setTextColor(COLOR_ON_SURFACE);
        row.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView b = new TextView(this);
        b.setText(badgeText);
        b.setTextSize(12);
        b.setTextColor(badgeColor == COLOR_SUCCESS ? COLOR_ON_PRIMARY : COLOR_ON_SURFACE_VARIANT);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setPadding(dp(12), dp(4), dp(12), dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(badgeColor == COLOR_SUCCESS ? COLOR_SUCCESS : COLOR_SURFACE_CONTAINER_HIGH);
        bg.setCornerRadius(dp(12));
        if (badgeColor != COLOR_SUCCESS) {
            bg.setStroke(dp(1), COLOR_OUTLINE);
        }
        b.setBackground(bg);
        row.addView(b, new LinearLayout.LayoutParams(-2, -2));
        return row;
    }

    private void openBlinkProfile(BadgeManager.TeamMember m) {
        if (m == null) return;
        try {
            Intent intent = null;
            if (m.id != null && !m.id.isEmpty() && !"0".equals(m.id)) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("blink:///profile/?id=" + m.id));
            } else if (m.username != null && !m.username.isEmpty()) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://blinkmap.com/@" + m.username));
            }
            if (intent != null) {
                intent.setPackage(getPackageName());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            }
        } catch (Throwable t) {
            try {
                Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse("blink:///profile/?id=" + m.id));
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallback);
                return;
            } catch (Throwable ignored) {}
        }
        Toast.makeText(this, "Не удалось открыть профиль", Toast.LENGTH_SHORT).show();
    }

    private View teamMemberRow(final BadgeManager.TeamMember m) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);

        String mainName = (m.name != null && !m.name.isEmpty()) ? m.name : ("ID " + m.id);
        TextView nameView = new TextView(this);
        nameView.setText(mainName);
        nameView.setTextSize(14);
        nameView.setTextColor(COLOR_ON_SURFACE);
        nameView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        infoCol.addView(nameView);

        String sub = "";
        if (m.username != null && !m.username.isEmpty()) {
            sub = "@" + m.username + " • ";
        }
        sub += "ID: " + m.id;

        TextView subView = new TextView(this);
        subView.setText(sub);
        subView.setTextSize(11);
        subView.setTextColor(COLOR_ON_SURFACE_VARIANT);
        infoCol.addView(subView);

        row.addView(infoCol, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView badgeView = new TextView(this);
        badgeView.setText(m.role);
        badgeView.setTextSize(11);
        badgeView.setTextColor(COLOR_PRIMARY);
        badgeView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        badgeView.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_SURFACE_CONTAINER_HIGH);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), COLOR_OUTLINE);
        badgeView.setBackground(bg);
        row.addView(badgeView);

        TextView arrow = new TextView(this);
        arrow.setText(" ›");
        arrow.setTextSize(16);
        arrow.setTextColor(COLOR_ON_SURFACE_VARIANT);
        arrow.setPadding(dp(4), 0, 0, 0);
        row.addView(arrow);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                openBlinkProfile(m);
            }
        });
        addTouchSpring(row);
        return row;
    }

    private View copyRow(String label, final String value, final String toastMsg) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(14);
        l.setTextColor(COLOR_ON_SURFACE);
        row.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(13);
        v.setTextColor(COLOR_PRIMARY);
        v.setGravity(Gravity.END);
        row.addView(v, new LinearLayout.LayoutParams(-2, -2));

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                if (!"—".equals(value)) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("BlinkMod", value));
                    Toast.makeText(ModSettingsActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });
        return row;
    }

    private View toggleRow(String title, String subtitle, final String prefKey, boolean defValue,
                           final ToggleSwitchView.OnCheckedChangeListener extraListener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(14);
        t.setTextColor(COLOR_ON_SURFACE);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textCol.addView(t);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView s = new TextView(this);
            s.setText(subtitle);
            s.setTextSize(11);
            s.setTextColor(COLOR_ON_SURFACE_VARIANT);
            s.setPadding(0, dp(2), 0, 0);
            textCol.addView(s);
        }
        row.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1f));

        final ToggleSwitchView sw = new ToggleSwitchView(this);
        boolean isChecked = getSharedPreferences(PREFS, 0).getBoolean(prefKey, defValue);
        sw.setChecked(isChecked, false);

        sw.setOnCheckedChangeListener(new ToggleSwitchView.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ToggleSwitchView view, boolean checked) {
                getSharedPreferences(PREFS, 0).edit().putBoolean(prefKey, checked).apply();
                if (extraListener != null) {
                    extraListener.onCheckedChanged(view, checked);
                }
            }
        });

        row.addView(sw, new LinearLayout.LayoutParams(-2, -2));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                sw.toggle(true);
            }
        });
        return row;
    }

    private View multiplierSelector(String title, final String prefKey, int[] multipliers) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(6), 0, dp(4));

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(13);
        tv.setTextColor(COLOR_ON_SURFACE_VARIANT);
        box.addView(tv);

        LinearLayout pills = new LinearLayout(this);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, -2);
        plp.topMargin = dp(8);
        pills.setLayoutParams(plp);

        int selected = getSharedPreferences(PREFS, 0).getInt(prefKey, 2);
        final TextView[] pillsList = new TextView[multipliers.length];

        for (int i = 0; i < multipliers.length; i++) {
            final int index = i;
            final int m = multipliers[i];
            TextView pill = createPill("×" + m, selected == m);
            pillsList[i] = pill;
            if (i > 0) ((LinearLayout.LayoutParams) pill.getLayoutParams()).leftMargin = dp(6);
            pills.addView(pill);

            pill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    haptic(v);
                    getSharedPreferences(PREFS, 0).edit().putInt(prefKey, m).apply();
                    for (int j = 0; j < pillsList.length; j++) {
                        updatePillStyle(pillsList[j], j == index);
                    }
                }
            });
        }
        box.addView(pills);
        return box;
    }

    private void showCustomStepsDialog(final TextView postStepsBtn, final TextView[] presetPills, final TextView customPill) {
        final SharedPreferences sp = getSharedPreferences(PREFS, 0);
        int current = sp.getInt("target_steps_count", 100000);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(22), dp(20), dp(22), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_SURFACE_CONTAINER_HIGH);
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1), COLOR_OUTLINE);
        layout.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("Количество шагов");
        title.setTextSize(18);
        title.setTextColor(COLOR_ON_SURFACE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Введите целевое число шагов (от 100 до 500 000):");
        subtitle.setTextSize(13);
        subtitle.setTextColor(COLOR_ON_SURFACE_VARIANT);
        subtitle.setPadding(0, dp(4), 0, dp(14));
        layout.addView(subtitle);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.setTextColor(COLOR_ON_SURFACE);
        input.setTextSize(17);
        input.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(COLOR_SURFACE_CONTAINER);
        inputBg.setCornerRadius(dp(12));
        inputBg.setStroke(dp(1), COLOR_OUTLINE_VARIANT);
        input.setBackground(inputBg);
        layout.addView(input);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, dp(18), 0, 0);

        TextView cancelBtn = buttonOutlined("Отмена");
        TextView okBtn = buttonPrimary("Применить");

        buttons.addView(cancelBtn, new LinearLayout.LayoutParams(0, dp(42), 1f));
        LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        okLp.leftMargin = dp(8);
        buttons.addView(okBtn, okLp);
        layout.addView(buttons);

        builder.setView(layout);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                dialog.dismiss();
            }
        });

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                try {
                    String s = input.getText().toString().trim();
                    if (!s.isEmpty()) {
                        int val = Integer.parseInt(s);
                        if (val < 100) val = 100;
                        if (val > 1000000) val = 1000000;
                        sp.edit().putInt("target_steps_count", val).apply();
                        dialog.dismiss();
                        if (postStepsBtn != null) {
                            postStepsBtn.setText("Отправить " + String.format(Locale.US, "%,d", val).replace(',', ' ') + " шагов");
                        }
                        if (presetPills != null) {
                            for (TextView p : presetPills) updatePillStyle(p, false);
                        }
                        if (customPill != null) {
                            customPill.setText(val >= 1000 ? (val / 1000 + "k") : String.valueOf(val));
                            updatePillStyle(customPill, true);
                        }
                    }
                } catch (Throwable e) {
                    Toast.makeText(ModSettingsActivity.this, "Введите корректное число", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void renderFollowerCard(LinearLayout container, final Runnable updateGeoHeader) {
        final SharedPreferences sp = getSharedPreferences(PREFS, 0);
        final LinearLayout stalkerCard = card();
        sectionTitle(stalkerCard, "ПРЕСЛЕДОВАНИЕ И ОРБИТА ДРУГА");

        TextView desc = textSmall("Автоматическое преследование выбранного друга: хоровод по орбите вокруг него, тень сзади или режим в кармане с генерацией шагов.");
        stalkerCard.addView(desc);

        boolean isFollowing = FriendFollower.isRunning();
        FriendFollower.FriendTarget current = FriendFollower.getCurrentTarget();
        String targetName = (isFollowing && current != null) ? current.getDisplayName() : "Выключено";

        View statusBadge = badgeStatusRow("Статус преследования",
                targetName,
                isFollowing ? COLOR_SUCCESS : COLOR_ON_SURFACE_VARIANT);
        final TextView statusBadgeText = (TextView) ((LinearLayout) statusBadge).getChildAt(1);
        stalkerCard.addView(statusBadge);

        View detailsRow = statusInfoRow("Параметры", FriendFollower.getStatusText());
        final TextView detailsValText = (TextView) ((LinearLayout) detailsRow).getChildAt(1);
        stalkerCard.addView(detailsRow);
        stalkerCard.addView(line());

        // Mode Selector Pills: [Хоровод] [Тень] [В кармане]
        TextView modeLabel = textSmall("Режим перемещения");
        modeLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        stalkerCard.addView(modeLabel);

        LinearLayout modePills = new LinearLayout(this);
        modePills.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams mpLp = new LinearLayout.LayoutParams(-1, -2);
        mpLp.topMargin = dp(6);
        mpLp.bottomMargin = dp(10);
        modePills.setLayoutParams(mpLp);

        final int savedMode = sp.getInt("follower_mode", FriendFollower.MODE_ORBIT);
        final int[] modes = {FriendFollower.MODE_ORBIT, FriendFollower.MODE_SHADOW, FriendFollower.MODE_POCKET};
        final String[] modeLabels = {"Хоровод", "Тень", "В кармане"};
        final TextView[] modePillsList = new TextView[modes.length];

        for (int i = 0; i < modes.length; i++) {
            final int index = i;
            final int m = modes[i];
            TextView pill = createPill(modeLabels[i], savedMode == m);
            modePillsList[i] = pill;
            if (i > 0) ((LinearLayout.LayoutParams) pill.getLayoutParams()).leftMargin = dp(6);
            modePills.addView(pill);

            pill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    haptic(v);
                    sp.edit().putInt("follower_mode", m).apply();
                    for (int j = 0; j < modePillsList.length; j++) {
                        updatePillStyle(modePillsList[j], j == index);
                    }
                    if (FriendFollower.isRunning()) {
                        FriendFollower.startFollow(ModSettingsActivity.this,
                                FriendFollower.getCurrentTarget(),
                                m,
                                sp.getFloat("follower_radius", FriendFollower.RADIUS_MEDIUM),
                                sp.getFloat("follower_speed", FriendFollower.SPEED_WALK),
                                createFollowerListener(statusBadgeText, detailsValText, updateGeoHeader));
                    }
                    detailsValText.setText(FriendFollower.getStatusText());
                    updateGeoHeader.run();
                }
            });
        }
        stalkerCard.addView(modePills);

        // Radius Selector Pills: [10м] [25м] [50м] [100м]
        TextView radiusLabel = textSmall("Радиус орбиты / дистанция");
        radiusLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        stalkerCard.addView(radiusLabel);

        LinearLayout radiusPills = new LinearLayout(this);
        radiusPills.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rpLp = new LinearLayout.LayoutParams(-1, -2);
        rpLp.topMargin = dp(6);
        rpLp.bottomMargin = dp(10);
        radiusPills.setLayoutParams(rpLp);

        final float savedRadius = sp.getFloat("follower_radius", FriendFollower.RADIUS_MEDIUM);
        final float[] radiuses = {FriendFollower.RADIUS_CLOSE, FriendFollower.RADIUS_MEDIUM, FriendFollower.RADIUS_DISTANT, FriendFollower.RADIUS_WIDE};
        final String[] radiusLabels = {"10 м", "25 м", "50 м", "100 м"};
        final TextView[] radiusPillsList = new TextView[radiuses.length];

        for (int i = 0; i < radiuses.length; i++) {
            final int index = i;
            final float r = radiuses[i];
            TextView pill = createPill(radiusLabels[i], Math.abs(savedRadius - r) < 1f);
            radiusPillsList[i] = pill;
            if (i > 0) ((LinearLayout.LayoutParams) pill.getLayoutParams()).leftMargin = dp(6);
            radiusPills.addView(pill);

            pill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    haptic(v);
                    sp.edit().putFloat("follower_radius", r).apply();
                    for (int j = 0; j < radiusPillsList.length; j++) {
                        updatePillStyle(radiusPillsList[j], j == index);
                    }
                    if (FriendFollower.isRunning()) {
                        FriendFollower.startFollow(ModSettingsActivity.this,
                                FriendFollower.getCurrentTarget(),
                                sp.getInt("follower_mode", FriendFollower.MODE_ORBIT),
                                r,
                                sp.getFloat("follower_speed", FriendFollower.SPEED_WALK),
                                createFollowerListener(statusBadgeText, detailsValText, updateGeoHeader));
                    }
                    detailsValText.setText(FriendFollower.getStatusText());
                    updateGeoHeader.run();
                }
            });
        }
        stalkerCard.addView(radiusPills);

        // Speed Selector Pills: [Шаг 4.5] [Бег 12] [Вело 22] [Авто 48]
        TextView speedLabel = textSmall("Скорость движения");
        speedLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        stalkerCard.addView(speedLabel);

        LinearLayout speedPills = new LinearLayout(this);
        speedPills.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(-1, -2);
        spLp.topMargin = dp(6);
        spLp.bottomMargin = dp(12);
        speedPills.setLayoutParams(spLp);

        final float savedSpeed = sp.getFloat("follower_speed", FriendFollower.SPEED_WALK);
        final float[] speeds = {FriendFollower.SPEED_WALK, FriendFollower.SPEED_RUN, FriendFollower.SPEED_BIKE, FriendFollower.SPEED_CAR};
        final String[] speedLabels = {"Шаг 4.5", "Бег 12", "Вело 22", "Авто 48"};
        final TextView[] speedPillsList = new TextView[speeds.length];

        for (int i = 0; i < speeds.length; i++) {
            final int index = i;
            final float s = speeds[i];
            TextView pill = createPill(speedLabels[i], Math.abs(savedSpeed - s) < 1f);
            speedPillsList[i] = pill;
            if (i > 0) ((LinearLayout.LayoutParams) pill.getLayoutParams()).leftMargin = dp(6);
            speedPills.addView(pill);

            pill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    haptic(v);
                    sp.edit().putFloat("follower_speed", s).apply();
                    for (int j = 0; j < speedPillsList.length; j++) {
                        updatePillStyle(speedPillsList[j], j == index);
                    }
                    if (FriendFollower.isRunning()) {
                        FriendFollower.startFollow(ModSettingsActivity.this,
                                FriendFollower.getCurrentTarget(),
                                sp.getInt("follower_mode", FriendFollower.MODE_ORBIT),
                                sp.getFloat("follower_radius", FriendFollower.RADIUS_MEDIUM),
                                s,
                                createFollowerListener(statusBadgeText, detailsValText, updateGeoHeader));
                    }
                    detailsValText.setText(FriendFollower.getStatusText());
                    updateGeoHeader.run();
                }
            });
        }
        stalkerCard.addView(speedPills);

        // Action Buttons Row
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams apLp = new LinearLayout.LayoutParams(-1, -2);
        apLp.topMargin = dp(4);
        actionRow.setLayoutParams(apLp);

        final TextView btnChoose = buttonPrimary(isFollowing ? "Сменить цель" : "Выбрать друга для слежки");
        actionRow.addView(btnChoose, new LinearLayout.LayoutParams(0, dp(44), 1f));

        final TextView btnStop = buttonOutlined("Остановить");
        btnStop.setVisibility(isFollowing ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams stopLp = new LinearLayout.LayoutParams(0, dp(44), 0.6f);
        stopLp.leftMargin = dp(8);
        actionRow.addView(btnStop, stopLp);

        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                showFriendChooserDialog(new Runnable() {
                    @Override
                    public void run() {
                        boolean active = FriendFollower.isRunning();
                        FriendFollower.FriendTarget tgt = FriendFollower.getCurrentTarget();
                        statusBadgeText.setText(active && tgt != null ? ("В орбите: " + tgt.getDisplayName()) : "Выключено");
                        updateBadgeStyle(statusBadgeText, active ? COLOR_SUCCESS : COLOR_ON_SURFACE_VARIANT);
                        detailsValText.setText(FriendFollower.getStatusText());
                        btnChoose.setText(active ? "Сменить цель" : "Выбрать друга для слежки");
                        btnStop.setVisibility(active ? View.VISIBLE : View.GONE);
                        updateGeoHeader.run();
                    }
                }, updateGeoHeader, statusBadgeText, detailsValText);
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                FriendFollower.stopFollow(ModSettingsActivity.this);
                statusBadgeText.setText("Выключено");
                updateBadgeStyle(statusBadgeText, COLOR_ON_SURFACE_VARIANT);
                detailsValText.setText("Выключено");
                btnChoose.setText("Выбрать друга для слежки");
                btnStop.setVisibility(View.GONE);
                Toast.makeText(ModSettingsActivity.this, "Преследование остановлено", Toast.LENGTH_SHORT).show();
                updateGeoHeader.run();
            }
        });

        stalkerCard.addView(actionRow);
        container.addView(stalkerCard, bottomMargin(14));

        if (FriendFollower.isRunning()) {
            FriendFollower.setListener(createFollowerListener(statusBadgeText, detailsValText, updateGeoHeader));
        }
    }

    private FriendFollower.FollowListener createFollowerListener(final TextView badgeText, final TextView detailsText, final Runnable updateGeoHeader) {
        return new FriendFollower.FollowListener() {
            @Override
            public void onFollowTick(FriendFollower.FriendTarget target, double lat, double lon, int stepsAdded) {
                if (!isFinishing() && activeTab == TAB_GHOST) {
                    if (detailsText != null) {
                        detailsText.setText(FriendFollower.getStatusText());
                    }
                    if (updateGeoHeader != null) {
                        updateGeoHeader.run();
                    }
                }
            }

            @Override
            public void onFollowStopped() {
                if (!isFinishing() && activeTab == TAB_GHOST) {
                    if (badgeText != null) {
                        badgeText.setText("Выключено");
                        updateBadgeStyle(badgeText, COLOR_ON_SURFACE_VARIANT);
                    }
                    if (detailsText != null) {
                        detailsText.setText("Выключено");
                    }
                    if (updateGeoHeader != null) {
                        updateGeoHeader.run();
                    }
                }
            }
        };
    }

    private void showFriendChooserDialog(final Runnable onChosen, final Runnable updateGeoHeader, final TextView badgeText, final TextView detailsText) {
        final List<FriendFollower.FriendTarget> allFriends = FriendFollower.loadFriends(this);
        if (allFriends.isEmpty()) {
            Toast.makeText(this, "Друзья с координатами не найдены в базе. Откройте карту Blink!", Toast.LENGTH_LONG).show();
            return;
        }

        final Dialog dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        final FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));

        final View scrim = new View(this);
        scrim.setBackgroundColor(COLOR_SCRIM);
        rootFrame.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        final LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setClickable(true);

        GradientDrawable sheetBg = new GradientDrawable();
        sheetBg.setColor(COLOR_SURFACE_CONTAINER_LOW);
        sheetBg.setCornerRadii(new float[]{dp(28), dp(28), dp(28), dp(28), 0, 0, 0, 0});
        sheetBg.setStroke(dp(1), COLOR_OUTLINE);
        sheet.setBackground(sheetBg);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        final int maxHeight = (int) (dm.heightPixels * 0.85f);
        FrameLayout.LayoutParams sheetLp = new FrameLayout.LayoutParams(-1, maxHeight);
        sheetLp.gravity = Gravity.BOTTOM;
        rootFrame.addView(sheet, sheetLp);

        // 1. Top Drag Handle
        FrameLayout handleArea = new FrameLayout(this);
        handleArea.setPadding(0, dp(12), 0, dp(8));
        View handle = new View(this);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(COLOR_DRAG_HANDLE);
        handleBg.setCornerRadius(dp(2));
        handle.setBackground(handleBg);
        FrameLayout.LayoutParams hLp = new FrameLayout.LayoutParams(dp(36), dp(4));
        hLp.gravity = Gravity.CENTER;
        handleArea.addView(handle, hLp);
        sheet.addView(handleArea, new LinearLayout.LayoutParams(-1, -2));

        // 2. Header Bar
        FrameLayout headerBar = new FrameLayout(this);
        headerBar.setPadding(dp(20), dp(4), dp(20), dp(10));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("Выберите цель");
        title.setTextSize(20);
        title.setTextColor(COLOR_ON_SURFACE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleBox.addView(title);

        int countOnline = 0;
        for (FriendFollower.FriendTarget f : allFriends) {
            if (f.isOnline) countOnline++;
        }

        final TextView subtitle = new TextView(this);
        subtitle.setText("Доступно: " + allFriends.size() + (countOnline > 0 ? (" · " + countOnline + " в сети") : ""));
        subtitle.setTextSize(12);
        subtitle.setTextColor(COLOR_ON_SURFACE_VARIANT);
        subtitle.setPadding(0, dp(2), 0, 0);
        titleBox.addView(subtitle);

        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(-2, -2);
        titleLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
        headerBar.addView(titleBox, titleLp);

        // Close Button
        TextView closeBtn = new TextView(this);
        closeBtn.setText("×");
        closeBtn.setTextSize(20);
        closeBtn.setTextColor(COLOR_ON_SURFACE);
        closeBtn.setGravity(Gravity.CENTER);
        closeBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setColor(COLOR_SURFACE_CONTAINER_HIGH);
        closeBg.setShape(GradientDrawable.OVAL);
        closeBtn.setBackground(closeBg);
        addTouchSpring(closeBtn);
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(dp(32), dp(32));
        closeLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        headerBar.addView(closeBtn, closeLp);

        sheet.addView(headerBar, new LinearLayout.LayoutParams(-1, -2));

        // 3. Search Bar
        LinearLayout searchContainer = new LinearLayout(this);
        searchContainer.setOrientation(LinearLayout.HORIZONTAL);
        searchContainer.setGravity(Gravity.CENTER_VERTICAL);
        searchContainer.setPadding(dp(14), 0, dp(14), 0);

        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(COLOR_SURFACE_CONTAINER_HIGH);
        searchBg.setCornerRadius(dp(16));
        searchBg.setStroke(dp(1), COLOR_OUTLINE);
        searchContainer.setBackground(searchBg);

        final EditText searchEdit = new EditText(this);
        searchEdit.setHint("Поиск по имени или @нику...");
        searchEdit.setHintTextColor(COLOR_ON_SURFACE_VARIANT);
        searchEdit.setTextColor(COLOR_ON_SURFACE);
        searchEdit.setTextSize(14);
        searchEdit.setSingleLine(true);
        searchEdit.setBackground(null);
        searchEdit.setPadding(dp(4), dp(10), dp(4), dp(10));
        searchContainer.addView(searchEdit, new LinearLayout.LayoutParams(0, -2, 1f));

        final TextView clearBtn = new TextView(this);
        clearBtn.setText("×");
        clearBtn.setTextSize(14);
        clearBtn.setTextColor(COLOR_ON_SURFACE_VARIANT);
        clearBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        clearBtn.setVisibility(View.GONE);
        searchContainer.addView(clearBtn);

        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(-1, dp(44));
        searchLp.leftMargin = dp(16);
        searchLp.rightMargin = dp(16);
        searchLp.bottomMargin = dp(10);
        sheet.addView(searchContainer, searchLp);

        // 4. Category Filter Chips: [Все] [В сети] [Рядом < 5км]
        LinearLayout chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setPadding(dp(16), 0, dp(16), dp(10));

        final int FILTER_ALL = 0;
        final int FILTER_ONLINE = 1;
        final int FILTER_NEARBY = 2;
        final int[] activeFilter = {FILTER_ALL};

        final TextView chipAll = createPill("Все (" + allFriends.size() + ")", true);
        final TextView chipOnline = createPill("В сети (" + countOnline + ")", false);
        final TextView chipNearby = createPill("Рядом < 5 км", false);

        ((LinearLayout.LayoutParams) chipOnline.getLayoutParams()).leftMargin = dp(6);
        ((LinearLayout.LayoutParams) chipNearby.getLayoutParams()).leftMargin = dp(6);

        chipsRow.addView(chipAll);
        chipsRow.addView(chipOnline);
        chipsRow.addView(chipNearby);
        sheet.addView(chipsRow, new LinearLayout.LayoutParams(-1, -2));

        // 5. ScrollView with Friends List
        final ScrollView listScroll = new ScrollView(this);
        listScroll.setClipToPadding(false);
        listScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        final LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(16), dp(4), dp(16), dp(24));
        listScroll.addView(listContainer, new ScrollView.LayoutParams(-1, -2));

        sheet.addView(listScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        final SharedPreferences sp = getSharedPreferences(PREFS, 0);

        final Runnable closeSheet = new Runnable() {
            @Override
            public void run() {
                scrim.animate().alpha(0f).setDuration(200).start();
                sheet.animate().translationY(maxHeight).setDuration(220)
                        .setInterpolator(INTERPOLATOR_ACCELERATE)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    dialog.dismiss();
                                } catch (Throwable ignored) {}
                            }
                        }).start();
            }
        };

        scrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSheet.run();
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                closeSheet.run();
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEdit.setText("");
            }
        });

        // Filter & List render logic
        final Runnable renderList = new Runnable() {
            @Override
            public void run() {
                listContainer.removeAllViews();
                String query = searchEdit.getText().toString().trim().toLowerCase(Locale.US);
                int filter = activeFilter[0];

                long currentTargetId = -1;
                if (FriendFollower.isRunning() && FriendFollower.getCurrentTarget() != null) {
                    currentTargetId = FriendFollower.getCurrentTarget().id;
                }

                ArrayList<FriendFollower.FriendTarget> filtered = new ArrayList<FriendFollower.FriendTarget>();
                for (FriendFollower.FriendTarget f : allFriends) {
                    if (filter == FILTER_ONLINE && !f.isOnline) continue;
                    if (filter == FILTER_NEARBY && (f.distanceMeters < 0 || f.distanceMeters > 5000f)) continue;

                    if (!query.isEmpty()) {
                        String name = f.name.toLowerCase(Locale.US);
                        String nick = f.nickname.toLowerCase(Locale.US);
                        String disp = f.getDisplayName().toLowerCase(Locale.US);
                        if (!name.contains(query) && !nick.contains(query) && !disp.contains(query)) {
                            continue;
                        }
                    }
                    filtered.add(f);
                }

                if (filtered.isEmpty()) {
                    LinearLayout emptyView = new LinearLayout(ModSettingsActivity.this);
                    emptyView.setOrientation(LinearLayout.VERTICAL);
                    emptyView.setGravity(Gravity.CENTER);
                    emptyView.setPadding(0, dp(48), 0, dp(48));

                    TextView emptyText = new TextView(ModSettingsActivity.this);
                    emptyText.setText("Никого не найдено");
                    emptyText.setTextSize(16);
                    emptyText.setTextColor(COLOR_ON_SURFACE);
                    emptyText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    emptyText.setGravity(Gravity.CENTER);
                    emptyText.setPadding(0, dp(8), 0, dp(4));
                    emptyView.addView(emptyText);

                    TextView emptySub = new TextView(ModSettingsActivity.this);
                    emptySub.setText("Попробуйте изменить поисковый запрос или фильтр");
                    emptySub.setTextSize(12);
                    emptySub.setTextColor(COLOR_ON_SURFACE_VARIANT);
                    emptySub.setGravity(Gravity.CENTER);
                    emptyView.addView(emptySub);

                    listContainer.addView(emptyView);
                    return;
                }

                for (final FriendFollower.FriendTarget friend : filtered) {
                    final boolean isActiveTarget = (friend.id == currentTargetId);

                    LinearLayout card = new LinearLayout(ModSettingsActivity.this);
                    card.setOrientation(LinearLayout.HORIZONTAL);
                    card.setGravity(Gravity.CENTER_VERTICAL);
                    card.setPadding(dp(14), dp(11), dp(14), dp(11));

                    GradientDrawable cardBg = new GradientDrawable();
                    if (isActiveTarget) {
                        cardBg.setColor(0xFF14241B); // Subtle emerald tint
                        cardBg.setStroke(dp(1), COLOR_SUCCESS);
                    } else {
                        cardBg.setColor(COLOR_SURFACE_CONTAINER);
                        cardBg.setStroke(dp(1), COLOR_OUTLINE_VARIANT);
                    }
                    cardBg.setCornerRadius(dp(18));
                    card.setBackground(cardBg);

                    // 1. Avatar Container with online dot
                    FrameLayout avatarFrame = new FrameLayout(ModSettingsActivity.this);
                    ImageView avatarImg = new ImageView(ModSettingsActivity.this);
                    avatarImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    Bitmap fallbackBmp = AvatarLoader.getInitialsAvatar(friend.getDisplayName(), friend.id, dp(46));
                    avatarImg.setImageBitmap(fallbackBmp);
                    AvatarLoader.load(friend.avatarUrl, avatarImg, fallbackBmp);
                    avatarFrame.addView(avatarImg, new FrameLayout.LayoutParams(dp(44), dp(44)));

                    if (friend.isOnline) {
                        View onlineDot = new View(ModSettingsActivity.this);
                        GradientDrawable dotBg = new GradientDrawable();
                        dotBg.setColor(COLOR_SUCCESS);
                        dotBg.setShape(GradientDrawable.OVAL);
                        dotBg.setStroke(dp(2), COLOR_SURFACE_CONTAINER_LOW);
                        onlineDot.setBackground(dotBg);

                        FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dp(12), dp(12));
                        dotLp.gravity = Gravity.BOTTOM | Gravity.END;
                        avatarFrame.addView(onlineDot, dotLp);
                    }

                    card.addView(avatarFrame, new LinearLayout.LayoutParams(dp(46), dp(46)));

                    // 2. Info Box
                    LinearLayout info = new LinearLayout(ModSettingsActivity.this);
                    info.setOrientation(LinearLayout.VERTICAL);
                    info.setPadding(dp(12), 0, dp(10), 0);

                    // Name line
                    LinearLayout nameLine = new LinearLayout(ModSettingsActivity.this);
                    nameLine.setOrientation(LinearLayout.HORIZONTAL);
                    nameLine.setGravity(Gravity.CENTER_VERTICAL);

                    TextView nameView = new TextView(ModSettingsActivity.this);
                    nameView.setText(friend.getDisplayName());
                    nameView.setTextSize(15);
                    nameView.setTextColor(COLOR_ON_SURFACE);
                    nameView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                    nameLine.addView(nameView);

                    if (!friend.name.isEmpty() && !friend.nickname.isEmpty()) {
                        TextView nickView = new TextView(ModSettingsActivity.this);
                        nickView.setText(" @" + friend.nickname);
                        nickView.setTextSize(12);
                        nickView.setTextColor(COLOR_ON_SURFACE_VARIANT);
                        nameLine.addView(nickView);
                    }
                    info.addView(nameLine);

                    // Subtitle line (Status, Distance, Speed)
                    TextView subView = new TextView(ModSettingsActivity.this);
                    subView.setText(friend.getSecondaryText());
                    subView.setTextSize(12);
                    subView.setTextColor(friend.isOnline ? COLOR_SUCCESS : COLOR_ON_SURFACE_VARIANT);
                    subView.setPadding(0, dp(3), 0, 0);
                    info.addView(subView);

                    card.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));

                    // 3. Right Status / Action
                    if (isActiveTarget) {
                        TextView activeBadge = new TextView(ModSettingsActivity.this);
                        activeBadge.setText("В ОРБИТЕ");
                        activeBadge.setTextSize(10);
                        activeBadge.setTextColor(COLOR_SUCCESS);
                        activeBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                        activeBadge.setPadding(dp(8), dp(4), dp(8), dp(4));

                        GradientDrawable abBg = new GradientDrawable();
                        abBg.setColor(0x2234D399);
                        abBg.setStroke(dp(1), COLOR_SUCCESS);
                        abBg.setCornerRadius(dp(10));
                        activeBadge.setBackground(abBg);

                        card.addView(activeBadge, new LinearLayout.LayoutParams(-2, -2));
                    } else {
                        TextView selectArrow = new TextView(ModSettingsActivity.this);
                        selectArrow.setText("›");
                        selectArrow.setTextSize(20);
                        selectArrow.setTextColor(COLOR_ON_SURFACE_VARIANT);
                        selectArrow.setGravity(Gravity.CENTER);
                        card.addView(selectArrow, new LinearLayout.LayoutParams(dp(24), dp(24)));
                    }

                    addTouchSpring(card);
                    card.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            haptic(v);
                            closeSheet.run();
                            int mode = sp.getInt("follower_mode", FriendFollower.MODE_ORBIT);
                            float radius = sp.getFloat("follower_radius", FriendFollower.RADIUS_MEDIUM);
                            float speed = sp.getFloat("follower_speed", FriendFollower.SPEED_WALK);

                            FriendFollower.startFollow(ModSettingsActivity.this, friend, mode, radius, speed,
                                    createFollowerListener(badgeText, detailsText, updateGeoHeader));

                            Toast.makeText(ModSettingsActivity.this, "Преследуем " + friend.getDisplayName(), Toast.LENGTH_SHORT).show();
                            if (onChosen != null) onChosen.run();
                        }
                    });

                    LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
                    cardLp.bottomMargin = dp(8);
                    listContainer.addView(card, cardLp);
                }
            }
        };

        // Filter chip clicks
        chipAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                activeFilter[0] = FILTER_ALL;
                updatePillStyle(chipAll, true);
                updatePillStyle(chipOnline, false);
                updatePillStyle(chipNearby, false);
                renderList.run();
            }
        });

        chipOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                activeFilter[0] = FILTER_ONLINE;
                updatePillStyle(chipAll, false);
                updatePillStyle(chipOnline, true);
                updatePillStyle(chipNearby, false);
                renderList.run();
            }
        });

        chipNearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haptic(v);
                activeFilter[0] = FILTER_NEARBY;
                updatePillStyle(chipAll, false);
                updatePillStyle(chipOnline, false);
                updatePillStyle(chipNearby, true);
                renderList.run();
            }
        });

        // Search text watcher
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearBtn.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                renderList.run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Initial render
        renderList.run();

        dialog.setContentView(rootFrame);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        // Enter animation
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(220).start();
        sheet.setTranslationY(maxHeight);
        sheet.animate().translationY(0f).setDuration(280)
                .setInterpolator(INTERPOLATOR_EMPHASIZED)
                .start();

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        FriendFollower.setListener(null);
        super.onDestroy();
    }

    private View line() {
        View v = new View(this);
        v.setBackgroundColor(COLOR_OUTLINE_VARIANT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(8);
        v.setLayoutParams(lp);
        return v;
    }

    private LinearLayout.LayoutParams bottomMargin(int marginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(marginDp);
        return lp;
    }

    private LinearLayout.LayoutParams createMarginedParams(int leftMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        lp.leftMargin = leftMarginDp;
        return lp;
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    private void haptic(View v) {
        if (v != null) {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }
}
