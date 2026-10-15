package com.blinkmap.mod;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class GmsCoreHelper {
    public static final String GMS_PACKAGE = "app.revanced.android.gms";
    public static final String GMS_LATEST_RELEASE_URL = "https://github.com/ReVanced/GmsCore/releases/latest";
    public static final String GMS_DIRECT_APK_URL = "https://github.com/ReVanced/GmsCore/releases/download/v0.3.1.4.240913/app.revanced.android.gms-240913008-signed.apk";

    private static final String PILL_TAG = "blinkmod_gmscore_pill";
    private static final String PREF_NAME = "blinkmod_gmscore";
    private static final String KEY_SHOWN_PROMPT = "shown_prompt_v2_0";

    private static AlertDialog activeDialog;

    private GmsCoreHelper() {}

    public static boolean isGmsCoreInstalled(Context context) {
        if (context == null) return false;
        try {
            android.content.pm.PackageInfo pi = context.getPackageManager().getPackageInfo(GMS_PACKAGE, 0);
            return pi != null && pi.applicationInfo != null && pi.applicationInfo.enabled;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void openGmsCoreSettings(Context context) {
        if (context == null) return;
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(GMS_PACKAGE);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            }
        } catch (Throwable ignored) {}

        try {
            Intent intent = new Intent();
            intent.setClassName(GMS_PACKAGE, "org.microg.gms.ui.SettingsActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Throwable t) {
            Toast.makeText(context, "Не удалось открыть настройки GmsCore", Toast.LENGTH_SHORT).show();
        }
    }

    public static void downloadGmsCore(Activity activity) {
        if (activity == null) return;
        try {
            DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                DownloadManager.Request req = new DownloadManager.Request(Uri.parse(GMS_DIRECT_APK_URL));
                req.setTitle("ReVanced GmsCore (microG)");
                req.setDescription("Загрузка сервисов для авторизации в моде...");
                req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app.revanced.android.gms.apk");
                req.setMimeType("application/vnd.android.package-archive");
                dm.enqueue(req);
                Toast.makeText(activity, "Загрузка GmsCore началась (см. шторку)", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Throwable ignored) {}

        // Fallback to browser
        try {
            Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(GMS_DIRECT_APK_URL));
            browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(browser);
        } catch (Throwable t) {
            try {
                Intent browser = new Intent(Intent.ACTION_VIEW, Uri.parse(GMS_LATEST_RELEASE_URL));
                browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(browser);
            } catch (Throwable ignored2) {}
        }
    }

    public static void showGuideDialog(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        if (activeDialog != null && activeDialog.isShowing()) return;

        final boolean installed = isGmsCoreInstalled(activity);

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(activity, 22);
        int padV = dp(activity, 16);
        layout.setPadding(padH, padV, padH, padV);

        // Header status badge
        TextView statusBadge = new TextView(activity);
        statusBadge.setText(installed ? "GmsCore установлен" : "GmsCore не установлен");
        statusBadge.setTextSize(12.5f);
        statusBadge.setTypeface(Typeface.DEFAULT_BOLD);
        statusBadge.setTextColor(installed ? Color.rgb(76, 217, 100) : Color.rgb(255, 149, 0));
        statusBadge.setPadding(dp(activity, 10), dp(activity, 5), dp(activity, 10), dp(activity, 5));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(installed ? Color.argb(40, 76, 217, 100) : Color.argb(40, 255, 149, 0));
        badgeBg.setCornerRadius(dp(activity, 8));
        statusBadge.setBackground(badgeBg);
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.bottomMargin = dp(activity, 12);
        layout.addView(statusBadge, badgeLp);

        // Body explanation
        TextView body = new TextView(activity);
        body.setTextSize(14.0f);
        body.setTextColor(Color.rgb(230, 230, 235));
        body.setLineSpacing(dp(activity, 4), 1.0f);
        body.setText(
                "Оригинальный Google Play блокирует вход в модифицированных приложениях из-за сторонней цифровой подписи.\n\n" +
                "Для входа через Google используется ReVanced GmsCore (microG).\n\n" +
                "Порядок действий:\n" +
                "1. Установите ReVanced GmsCore (кнопка ниже).\n" +
                "2. ОБЯЗАТЕЛЬНО откройте GmsCore и добавьте ваш Google-аккаунт внутри него («Настройки Google аккаунта»).\n" +
                "3. Только после этого нажимайте «Войти с Google» в Blink.\n\n" +
                "Если вход через Google не срабатывает — используйте «Вход по токену»."
        );
        layout.addView(body);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("Вход через Google (GmsCore)")
                .setView(layout)
                .setNegativeButton("Закрыть", null);

        builder.setNeutralButton("Вход по токену", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TokenLoginOverlay.showTokenDialog(activity);
            }
        });

        if (!installed) {
            builder.setPositiveButton("Скачать GmsCore", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    downloadGmsCore(activity);
                }
            });
        } else {
            builder.setPositiveButton("Настройки Google аккаунта", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openGmsCoreSettings(activity);
                }
            });
        }

        activeDialog = builder.create();
        activeDialog.show();
    }

    public static void onSessionChecked(final Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        showPill(activity);

        // Auto-show prompt once if microG is not installed
        SharedPreferences sp = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!isGmsCoreInstalled(activity) && !sp.getBoolean(KEY_SHOWN_PROMPT, false)) {
            sp.edit().putBoolean(KEY_SHOWN_PROMPT, true).apply();
            activity.getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!activity.isFinishing()) {
                        showGuideDialog(activity);
                    }
                }
            }, 800);
        }
    }

    public static void showPill(final Activity activity) {
        if (activity == null) return;
        View decor = activity.getWindow().getDecorView();
        if (!(decor instanceof ViewGroup)) return;

        ViewGroup root = (ViewGroup) decor;
        View old = root.findViewWithTag(PILL_TAG);
        if (old != null) root.removeView(old);

        final boolean installed = isGmsCoreInstalled(activity);

        TextView pill = new TextView(activity);
        pill.setTag(PILL_TAG);
        pill.setText(installed ? "microG готов (инфо/настройки)" : "Вход через Google (microG)");
        pill.setTextSize(12.5f);
        pill.setTypeface(Typeface.DEFAULT_BOLD);
        pill.setTextColor(Color.WHITE);
        pill.setPadding(dp(activity, 14), dp(activity, 8), dp(activity, 14), dp(activity, 8));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(installed ? Color.argb(220, 20, 110, 45) : Color.argb(220, 105, 55, 215));
        bg.setCornerRadius(dp(activity, 20));
        bg.setStroke(dp(activity, 1), Color.argb(120, 255, 255, 255));
        pill.setBackground(bg);
        pill.setElevation(dp(activity, 8));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.topMargin = dp(activity, 48);
        pill.setLayoutParams(lp);

        pill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showGuideDialog(activity);
            }
        });

        root.addView(pill);

        if (installed) {
            pill.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hidePill(activity);
                }
            }, 6000);
        }
    }

    public static void hidePill(Activity activity) {
        if (activity == null) return;
        View decor = activity.getWindow().getDecorView();
        if (decor instanceof ViewGroup) {
            View pill = decor.findViewWithTag(PILL_TAG);
            if (pill != null) {
                ((ViewGroup) decor).removeView(pill);
            }
        }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
