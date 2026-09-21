package com.blinkmap.mod;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class WorldTourUi {
    private static final int BASE_BG = Color.rgb(22, 22, 28);
    private static final int SECONDARY = Color.rgb(142, 142, 160);
    private static final int DIVIDER = Color.rgb(44, 44, 56);
    private static final int PURPLE = Color.rgb(138, 92, 255);
    private static final int CARD_BG = Color.rgb(32, 32, 40);

    private static long currentDelay = 500L; // default 500ms for fast countries tour

    private WorldTourUi() {}

    public static void attach(final LinearLayout parent, final Activity activity) {
        if (parent == null || activity == null) return;

        final Handler mainHandler = new Handler(Looper.getMainLooper());

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(CARD_BG);
        cardBg.setCornerRadius(dp(activity, 16));
        card.setBackground(cardBg);
        int pad = dp(activity, 16);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardLp.topMargin = dp(activity, 12);
        cardLp.bottomMargin = dp(activity, 8);
        card.setLayoutParams(cardLp);

        TextView title = new TextView(activity);
        title.setText("МИРОВОЕ ТУРНЕ");
        title.setTextSize(14);
        title.setTextColor(PURPLE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(title);

        TextView desc = new TextView(activity);
        desc.setText("Быстрое перемещение по всем странам планеты");
        desc.setTextSize(12);
        desc.setTextColor(SECONDARY);
        LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        descLp.topMargin = dp(activity, 4);
        card.addView(desc, descLp);

        final TextView statusView = new TextView(activity);
        statusView.setText(WorldTour.getStatusText());
        statusView.setTextSize(13);
        statusView.setTextColor(Color.WHITE);
        statusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dp(activity, 10);
        statusLp.bottomMargin = dp(activity, 10);
        card.addView(statusView, statusLp);

        final Runnable updateStatus = new Runnable() {
            @Override
            public void run() {
                statusView.setText(WorldTour.getStatusText());
            }
        };

        TextView speedLabel = new TextView(activity);
        speedLabel.setText("Интервал между точками:");
        speedLabel.setTextSize(12);
        speedLabel.setTextColor(SECONDARY);
        card.addView(speedLabel);

        LinearLayout speedRow = new LinearLayout(activity);
        speedRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams speedRowLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        speedRowLp.topMargin = dp(activity, 6);
        speedRowLp.bottomMargin = dp(activity, 12);
        speedRow.setLayoutParams(speedRowLp);

        final TextView btn250 = createPillButton(activity, "250 мс");
        final TextView btn500 = createPillButton(activity, "500 мс");
        final TextView btn15s = createPillButton(activity, "1.5 сек");

        final Runnable refreshSpeeds = new Runnable() {
            @Override
            public void run() {
                setPillSelected(btn250, currentDelay == 250L);
                setPillSelected(btn500, currentDelay == 500L);
                setPillSelected(btn15s, currentDelay == 1500L);
            }
        };
        refreshSpeeds.run();

        btn250.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDelay = 250L;
                refreshSpeeds.run();
            }
        });
        btn500.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDelay = 500L;
                refreshSpeeds.run();
            }
        });
        btn15s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDelay = 1500L;
                refreshSpeeds.run();
            }
        });

        speedRow.addView(btn250);
        speedRow.addView(btn500);
        speedRow.addView(btn15s);
        card.addView(speedRow);

        // Action 1: ALL 250 COUNTRIES + 50 US STATES (FAST: ~2.5 min)
        View btnCountries = createActionButton(activity, "🌍 Все страны + 50 штатов США (299 точек)", PURPLE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorldTour.start(activity, WorldTour.MODE_COUNTRIES_ONLY, currentDelay, new Runnable() {
                    @Override
                    public void run() {
                        mainHandler.post(updateStatus);
                    }
                });
                updateStatus.run();
                Toast.makeText(activity, "Тур по 250 странам и 50 штатам запущен!", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(btnCountries);

        // Action 2: All 249 countries and cities (1624 targets)
        View btnAll = createActionButton(activity, "🏙 Все страны и города (1624 точки)", PURPLE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorldTour.start(activity, WorldTour.MODE_ALL_CITIES, currentDelay, new Runnable() {
                    @Override
                    public void run() {
                        mainHandler.post(updateStatus);
                    }
                });
                updateStatus.run();
                Toast.makeText(activity, "Турне по городам запущено!", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(btnAll);

        // Action 3: Globe Grid (180 coords covering land & ocean)
        View btnGrid = createActionButton(activity, "🌊 Облететь планету (сетка + океаны)", PURPLE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorldTour.start(activity, WorldTour.MODE_GLOBE_GRID, currentDelay, new Runnable() {
                    @Override
                    public void run() {
                        mainHandler.post(updateStatus);
                    }
                });
                updateStatus.run();
                Toast.makeText(activity, "Глобальная сетка запущена!", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(btnGrid);

        // Action 4: Stop
        View btnStop = createActionButton(activity, "⏹ Остановить турне", Color.rgb(255, 80, 80), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorldTour.stop();
                updateStatus.run();
                Toast.makeText(activity, "Турне остановлено", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(btnStop);

        parent.addView(card);
    }

    private static TextView createPillButton(Activity a, String text) {
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextSize(11);
        tv.setGravity(Gravity.CENTER);
        int padH = dp(a, 8);
        int padV = dp(a, 6);
        tv.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.rightMargin = dp(a, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static void setPillSelected(TextView tv, boolean selected) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dp(tv.getContext(), 8));
        if (selected) {
            gd.setColor(PURPLE);
            tv.setTextColor(Color.WHITE);
        } else {
            gd.setColor(DIVIDER);
            tv.setTextColor(SECONDARY);
        }
        tv.setBackground(gd);
    }

    private static View createActionButton(Activity a, String text, int color, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(a, 10);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(a, 6);
        row.setLayoutParams(lp);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.rgb(40, 40, 52));
        gd.setCornerRadius(dp(a, 10));
        row.setBackground(gd);

        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(color);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(tv);

        row.setOnClickListener(listener);
        return row;
    }

    private static int dp(android.content.Context c, int val) {
        float d = c.getResources().getDisplayMetrics().density;
        return (int) (val * d + 0.5f);
    }
}
