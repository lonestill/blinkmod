package com.blinkmap.mod;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class WorldTourUi {
    private static final int BASE_BG = 0xFF0D0E11;
    private static final int SECONDARY = 0xFF8A8D98;
    private static final int DIVIDER = 0xFF22242D;
    private static final int PRIMARY = 0xFFF0F1F5;
    private static final int CARD_BG = 0xFF18191F;
    private static final int CARD_INNER_BG = 0xFF131418;
    private static final int OUTLINE = 0xFF353844;
    private static final int OUTLINE_VARIANT = 0xFF1C1E25;
    private static final int GOLD = 0xFFE5C07B;

    private static long currentDelay = 6000L; // default 6s for Blink backend ingestion

    private WorldTourUi() {}

    public static void attach(final LinearLayout parent, final Activity activity) {
        if (parent == null || activity == null) return;

        final Handler mainHandler = new Handler(Looper.getMainLooper());

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(CARD_BG);
        cardBg.setCornerRadius(dp(activity, 16));
        cardBg.setStroke(dp(activity, 1), OUTLINE_VARIANT);
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
        title.setTextColor(PRIMARY);
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

        // --- Visited Countries Stats Box ---
        final LinearLayout statsBox = new LinearLayout(activity);
        statsBox.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable statsBg = new GradientDrawable();
        statsBg.setColor(CARD_INNER_BG);
        statsBg.setCornerRadius(dp(activity, 12));
        statsBg.setStroke(dp(activity, 1), OUTLINE_VARIANT);
        statsBox.setBackground(statsBg);
        int sbPad = dp(activity, 12);
        statsBox.setPadding(sbPad, sbPad, sbPad, sbPad);
        LinearLayout.LayoutParams statsBoxLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statsBoxLp.topMargin = dp(activity, 8);
        statsBoxLp.bottomMargin = dp(activity, 4);
        statsBox.setLayoutParams(statsBoxLp);

        final TextView statsMain = new TextView(activity);
        statsMain.setTextSize(14);
        statsMain.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statsBox.addView(statsMain);

        final TextView statsSub = new TextView(activity);
        statsSub.setTextSize(11);
        statsSub.setTextColor(SECONDARY);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(activity, 3);
        statsBox.addView(statsSub, subLp);

        // MD3 Progress Bar for Visited Countries
        final Md3ProgressBar progressBar = new Md3ProgressBar(activity);
        progressBar.setColors(0xFF2C2F3A, PRIMARY, 0xFF5A5E70);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 8));
        pbLp.topMargin = dp(activity, 10);
        pbLp.bottomMargin = dp(activity, 4);
        progressBar.setLayoutParams(pbLp);
        statsBox.addView(progressBar);

        // Sub-buttons inside stats box: Sync & View missing list
        LinearLayout statsBtns = new LinearLayout(activity);
        statsBtns.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sbLp.topMargin = dp(activity, 10);
        statsBtns.setLayoutParams(sbLp);

        final TextView btnSync = createPillButton(activity, "Обновить");
        final TextView btnShowMissing = createPillButton(activity, "Осталось (25)");
        final TextView btnDump = createPillButton(activity, "Дамп");
        statsBtns.addView(btnSync);
        statsBtns.addView(btnShowMissing);
        statsBtns.addView(btnDump);
        statsBox.addView(statsBtns);
        card.addView(statsBox);

        // --- Current Tour Live Status View ---
        final TextView statusView = new TextView(activity);
        statusView.setText(WorldTour.getStatusText());
        statusView.setTextSize(13);
        statusView.setTextColor(PRIMARY);
        statusView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dp(activity, 10);
        statusLp.bottomMargin = dp(activity, 10);
        card.addView(statusView, statusLp);

        // --- Runnable to update both tour status and country statistics ---
        final Runnable updateVisitedStats = new Runnable() {
            @Override
            public void run() {
                VisitedCountriesManager.CountryStats stats = VisitedCountriesManager.getStats(activity);
                if (stats.missingCount == 0 && stats.visitedCount > 0) {
                    statsMain.setText("Все страны мира посещены (ТОП-1)");
                    statsMain.setTextColor(GOLD);
                } else if (stats.visitedCount > 0) {
                    statsMain.setText("Посещено: " + stats.visitedCount + " / " + stats.totalWorldCountries + " (осталось: " + stats.missingCount + ")");
                    statsMain.setTextColor(PRIMARY);
                } else {
                    statsMain.setText("Посещено: 0 / " + stats.totalWorldCountries + " (требуется синхронизация)");
                    statsMain.setTextColor(SECONDARY);
                }

                float pct = stats.totalWorldCountries > 0 ? (float) stats.visitedCount / (float) stats.totalWorldCountries : 0f;
                progressBar.setProgress(pct, true);

                String timeText = stats.lastSyncTime > 0L ?
                    android.text.format.DateUtils.getRelativeTimeSpanString(stats.lastSyncTime).toString() :
                    "ещё не обновлялось";
                String srcText = (stats.source != null && !stats.source.isEmpty()) ? " • " + stats.source : "";
                statsSub.setText("Синхронизация: " + timeText + srcText);

                btnShowMissing.setText("Осталось (" + stats.missingCount + ")");
            }
        };

        final Runnable updateStatus = new Runnable() {
            @Override
            public void run() {
                statusView.setText(WorldTour.getStatusText());
                updateVisitedStats.run();
            }
        };

        updateVisitedStats.run();
        VisitedCountriesManager.addListener(new Runnable() {
            @Override
            public void run() {
                mainHandler.post(updateStatus);
            }
        });

        // Sync button click
        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnSync.setText("Загрузка...");
                progressBar.setIndeterminate(true);
                VisitedCountriesManager.syncFromServerAsync(activity, new VisitedCountriesManager.SyncCallback() {
                    @Override
                    public void onSuccess(final VisitedCountriesManager.CountryStats stats) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                btnSync.setText("Обновить");
                                progressBar.setIndeterminate(false);
                                updateStatus.run();
                                Toast.makeText(activity, "Синхронизировано: " + stats.visitedCount + " стран!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(final String message) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                btnSync.setText("Обновить");
                                progressBar.setIndeterminate(false);
                                updateStatus.run();
                                Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
            }
        });

        // Show Missing dialog click
        btnShowMissing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMissingCountriesDialog(activity, new Runnable() {
                    @Override
                    public void run() {
                        mainHandler.post(updateStatus);
                    }
                });
            }
        });

        // Dump Map click
        btnDump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnDump.setText("Дамп...");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String res = VisitedCountriesManager.dumpMapData(activity);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                btnDump.setText("Дамп");
                                Toast.makeText(activity, res, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }, "BlinkMod-MapDumper").start();
            }
        });

        // --- Speed Controls ---
        TextView speedLabel = new TextView(activity);
        speedLabel.setText("Поездка на машине по стране (~50 км/ч, для зачёта 5-10 сек):");
        speedLabel.setTextSize(11);
        speedLabel.setTextColor(SECONDARY);
        card.addView(speedLabel);

        LinearLayout speedRow = new LinearLayout(activity);
        speedRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams speedRowLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        speedRowLp.topMargin = dp(activity, 6);
        speedRowLp.bottomMargin = dp(activity, 12);
        speedRow.setLayoutParams(speedRowLp);

        final TextView btn35s = createPillButton(activity, "3.5 сек");
        final TextView btn6s = createPillButton(activity, "6.0 сек");
        final TextView btn8s = createPillButton(activity, "8.0 сек");
        final TextView btn10s = createPillButton(activity, "10 сек");

        final Runnable refreshSpeeds = new Runnable() {
            @Override
            public void run() {
                setPillSelected(btn35s, currentDelay == 3500L);
                setPillSelected(btn6s, currentDelay == 6000L);
                setPillSelected(btn8s, currentDelay == 8000L);
                setPillSelected(btn10s, currentDelay == 10000L);
            }
        };
        refreshSpeeds.run();

        btn35s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDelay = 3500L;
                refreshSpeeds.run();
            }
        });
        btn6s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDelay = 6000L;
                refreshSpeeds.run();
            }
        });
        btn8s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDelay = 8000L;
                refreshSpeeds.run();
            }
        });
        btn10s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDelay = 10000L;
                refreshSpeeds.run();
            }
        });

        speedRow.addView(btn35s);
        speedRow.addView(btn6s);
        speedRow.addView(btn8s);
        speedRow.addView(btn10s);
        card.addView(speedRow);

        // --- DEV-ONLY ACTIONS (Gated behind ModConfig.isDevMode) ---
        if (ModConfig.isDevMode(activity)) {
            // Action 0: DYNAMIC TOUR OVER MISSING COUNTRIES ONLY
            View btnMissingTour = createActionButton(activity, "ВЗЯТЬ НЕДОСТАЮЩИЕ СТРАНЫ", GOLD, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    List<WorldTour.City> missing = VisitedCountriesManager.getMissingCountries(activity);
                    if (missing.isEmpty()) {
                        Toast.makeText(activity, "Все страны уже посещены! Топ-1 мира взят!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    WorldTour.start(activity, WorldTour.MODE_MISSING_ONLY, currentDelay, new Runnable() {
                        @Override
                        public void run() {
                            mainHandler.post(updateStatus);
                        }
                    });
                    updateStatus.run();
                    Toast.makeText(activity, "Рейд по " + missing.size() + " недостающим странам запущен!", Toast.LENGTH_SHORT).show();
                }
            });
            card.addView(btnMissingTour);

            // Action 0.5: SNIPER TOP-1 BOOST (42 sovereign guaranteed targets)
            View btnTop1 = createActionButton(activity, "СНАЙПЕР ТОП-1 (42 страны)", GOLD, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    WorldTour.start(activity, WorldTour.MODE_TOP1_BOOST, currentDelay, new Runnable() {
                        @Override
                        public void run() {
                            mainHandler.post(updateStatus);
                        }
                    });
                    updateStatus.run();
                    Toast.makeText(activity, "Снайперский тур ТОП-1 запущен (42 страны)!", Toast.LENGTH_SHORT).show();
                }
            });
            card.addView(btnTop1);
        }

        // Action 1: ALL 230 OFFICIAL COUNTRIES
        View btnCountries = createActionButton(activity, "Все страны (230 точек базы Blink)", PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorldTour.start(activity, WorldTour.MODE_COUNTRIES_ONLY, currentDelay, new Runnable() {
                    @Override
                    public void run() {
                        mainHandler.post(updateStatus);
                    }
                });
                updateStatus.run();
                Toast.makeText(activity, "Надёжный тур по 230 странам запущен!", Toast.LENGTH_SHORT).show();
            }
        });
        card.addView(btnCountries);

        // Action 2: All 249 countries and cities (1624 targets)
        View btnAll = createActionButton(activity, "Все страны и города (1624 точки)", PRIMARY, new View.OnClickListener() {
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
        View btnGrid = createActionButton(activity, "Облететь всю планету", PRIMARY, new View.OnClickListener() {
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
        View btnStop = createActionButton(activity, "Остановить турне", 0xFFF87171, new View.OnClickListener() {
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

    public static void showMissingCountriesDialog(final Activity activity, final Runnable onTourStarted) {
        if (activity == null || activity.isFinishing()) return;

        final List<WorldTour.City> missing = VisitedCountriesManager.getMissingCountries(activity);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        final AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.rgb(18, 18, 24));
        rootBg.setCornerRadius(dp(activity, 16));
        root.setBackground(rootBg);
        int pad = dp(activity, 16);
        root.setPadding(pad, pad, pad, pad);

        // Header Row: Title and Close button
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(activity);
        title.setText("Недостающие страны (" + missing.size() + ")");
        title.setTextSize(16);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        header.addView(title, titleLp);

        TextView closeBtn = new TextView(activity);
        closeBtn.setText("✕");
        closeBtn.setTextSize(18);
        closeBtn.setTextColor(SECONDARY);
        closeBtn.setPadding(dp(activity, 8), dp(activity, 4), dp(activity, 8), dp(activity, 4));
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        header.addView(closeBtn);
        root.addView(header);

        TextView sub = new TextView(activity);
        sub.setText("Нажмите на страну для телепорта или запустите тур по всем");
        sub.setTextSize(11);
        sub.setTextColor(SECONDARY);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(activity, 4);
        subLp.bottomMargin = dp(activity, 10);
        root.addView(sub, subLp);

        // Quick button inside dialog to start tour
        if (!missing.isEmpty()) {
            View startAllBtn = createActionButton(activity, "Запустить тур по недостающим (" + missing.size() + ")", GOLD, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    WorldTour.start(activity, WorldTour.MODE_MISSING_ONLY, currentDelay, onTourStarted);
                    if (onTourStarted != null) onTourStarted.run();
                    Toast.makeText(activity, "Тур по недостающим запущен (" + missing.size() + " стран)!", Toast.LENGTH_SHORT).show();
                }
            });
            root.addView(startAllBtn);
        } else {
            TextView congrats = new TextView(activity);
            congrats.setText("Все страны мира уже посещены");
            congrats.setTextColor(GOLD);
            congrats.setTextSize(14);
            congrats.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            congrats.setPadding(0, dp(activity, 16), 0, dp(activity, 16));
            root.addView(congrats);
        }

        // Scrollable list of missing countries
        ScrollView sv = new ScrollView(activity);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 380));
        svLp.topMargin = dp(activity, 8);
        sv.setLayoutParams(svLp);

        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < missing.size(); i++) {
            final WorldTour.City city = missing.get(i);
            String country = VisitedCountriesManager.extractCountryName(city.name);
            String flag = VisitedCountriesManager.getFlagForCountry(country);

            LinearLayout item = new LinearLayout(activity);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            int itemPad = dp(activity, 8);
            item.setPadding(itemPad, itemPad, itemPad, itemPad);

            TextView flagTv = new TextView(activity);
            flagTv.setText(flag);
            flagTv.setTextSize(20);
            flagTv.setPadding(0, 0, dp(activity, 10), 0);
            item.addView(flagTv);

            LinearLayout textCol = new LinearLayout(activity);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textColLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);

            TextView nameTv = new TextView(activity);
            nameTv.setText(city.name);
            nameTv.setTextSize(13);
            nameTv.setTextColor(Color.WHITE);
            nameTv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            textCol.addView(nameTv);

            TextView coordTv = new TextView(activity);
            coordTv.setText(String.format(java.util.Locale.US, "%.4f, %.4f", city.lat, city.lon));
            coordTv.setTextSize(11);
            coordTv.setTextColor(SECONDARY);
            textCol.addView(coordTv);

            item.addView(textCol, textColLp);

            // Click to teleport directly to this country
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StepHooks.setCustomLocation(city.lat, city.lon);
                    StepHooks.dispatchSyntheticLocation();
                    VisitedCountriesManager.markCountryVisited(city.name);
                    Toast.makeText(activity, "Переместились: " + city.name, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (onTourStarted != null) onTourStarted.run();
                }
            });

            list.addView(item);

            if (i < missing.size() - 1) {
                View div = new View(activity);
                div.setBackgroundColor(DIVIDER);
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 1));
                divLp.leftMargin = dp(activity, 32);
                list.addView(div, divLp);
            }
        }

        sv.addView(list);
        root.addView(sv);

        dialog.setView(root);
        dialog.show();
    }

    private static TextView createPillButton(Activity a, String text) {
        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextSize(11);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        int padH = dp(a, 10);
        int padV = dp(a, 7);
        tv.setPadding(padH, padV, padH, padV);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0xFF22242D);
        gd.setCornerRadius(dp(a, 14));
        gd.setStroke(dp(a, 1), OUTLINE);
        tv.setBackground(gd);
        tv.setTextColor(PRIMARY);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        lp.rightMargin = dp(a, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static void setPillSelected(TextView tv, boolean selected) {
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(dp(tv.getContext(), 14));
        if (selected) {
            gd.setColor(0xFF2B2E38);
            gd.setStroke(dp(tv.getContext(), 1), PRIMARY);
            tv.setTextColor(PRIMARY);
        } else {
            gd.setColor(0xFF22242D);
            gd.setStroke(dp(tv.getContext(), 1), OUTLINE);
            tv.setTextColor(SECONDARY);
        }
        tv.setBackground(gd);
    }

    private static View createActionButton(Activity a, String text, int color, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padH = dp(a, 14);
        int padV = dp(a, 12);
        row.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(a, 8);
        row.setLayoutParams(lp);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0xFF22242D);
        gd.setCornerRadius(dp(a, 12));
        gd.setStroke(dp(a, 1), OUTLINE_VARIANT);
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
