package com.blinkmap.mod;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Native token-login UI shown only while Blink has no local bearer session. */
public final class TokenLoginOverlay {
    private static final String BUTTON_TAG = "blinkmod_token_login_button";
    private static final String FLOATING_BUTTON_TAG = "blinkmod_floating_button";
    private static final int SECURE_FLAG = WindowManager.LayoutParams.FLAG_SECURE;
    private static final int MAX_PROFILE_BYTES = 1024 * 1024;

    private static AlertDialog activeDialog;
    private static EditText activeInput;
    private static TextView activeStatus;

    private TokenLoginOverlay() {
    }

    public static void onSessionChecked(final Activity activity, Object value) {
        final String token = value instanceof String ? ((String) value).trim() : "";
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (token.length() == 0) {
                    showButton(activity);
                    GmsCoreHelper.onSessionChecked(activity);
                } else {
                    hideButton(activity);
                    GmsCoreHelper.hidePill(activity);
                }
            }
        });
    }

    public static void onSessionCheckFailed(Activity activity) {
        // Fail closed: do not expose an auth control when the native session state is unknown.
        hideButton(activity);
    }

    public static void showButton(final Activity activity) {
        // Token actions live in BlinkMod settings and must not cover stock auth controls.
        hideButton(activity);
    }

    public static void hideButton(Activity activity) {
        GmsCoreHelper.hidePill(activity);
        View decor = activity.getWindow().getDecorView();
        View button = decor.findViewWithTag(BUTTON_TAG);
        if (button != null && button.getParent() instanceof ViewGroup) {
            ((ViewGroup) button.getParent()).removeView(button);
        }
    }

    public static void showTokenDialog(final Activity activity) {
        if (activeDialog != null && activeDialog.isShowing()) {
            return;
        }

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int horizontal = dp(activity, 24);
        content.setPadding(horizontal, dp(activity, 6), horizontal, 0);

        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setHint("Токен Blink");
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        input.setSaveEnabled(false);
        input.setSelectAllOnFocus(false);
        content.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView status = new TextView(activity);
        status.setTextSize(13.0f);
        status.setTextColor(Color.rgb(190, 45, 60));
        status.setPadding(0, dp(activity, 8), 0, 0);
        status.setVisibility(View.GONE);
        content.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Вход по токену")
                .setMessage("Вставь токен своего аккаунта. Он будет проверен сервером Blink и сохранён в штатной сессии приложения.")
                .setView(content)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Войти", null)
                .create();

        activeDialog = dialog;
        activeInput = input;
        activeStatus = status;

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface ignored) {
                clearSensitiveUi(activity);
            }
        });
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface ignored) {
                Window window = dialog.getWindow();
                if (window != null) {
                    window.addFlags(SECURE_FLAG);
                }
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                submit(activity, dialog);
                            }
                        }
                );
                input.requestFocus();
                if (window != null) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        dialog.show();
    }

    private static void submit(final Activity activity, final AlertDialog dialog) {
        final String token = normalizeToken(activeInput == null ? "" : activeInput.getText().toString());
        if (token.length() < 12) {
            setStatus("Похоже, это не токен Blink");
            return;
        }

        setBusy(dialog, true);
        setStatus("Проверяю токен…", Color.rgb(90, 90, 100));

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final long accountId = verifyToken(token);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setStatus("Сохраняю сессию…", Color.rgb(90, 90, 100));
                            try {
                                Class<?> bridge = Class.forName("com.blinkmap.mod.TokenAuthBridge");
                                bridge.getMethod("save", Activity.class, String.class, Long.TYPE).invoke(null, activity, token, accountId);
                            } catch (Throwable t) {
                                onSaveFailed(activity);
                            }
                        }
                    });
                } catch (final InvalidTokenException error) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setBusy(dialog, false);
                            setStatus(error.getMessage());
                        }
                    });
                } catch (Throwable ignored) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setBusy(dialog, false);
                            setStatus("Не удалось связаться с Blink. Проверь интернет и попробуй ещё раз");
                        }
                    });
                }
            }
        }, "blink-token-check");
        worker.start();
    }

    private static long verifyToken(String token) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(
                    "https://pin.apiblink.ru/api/account/profile"
            ).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(12000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", token);

            int status = connection.getResponseCode();
            if (status == 401 || status == 403) {
                throw new InvalidTokenException("Токен недействителен или истёк");
            }
            if (status != 200) {
                throw new InvalidTokenException("Blink отклонил вход (код " + status + ")");
            }

            String response = readLimited(connection.getInputStream());
            JSONObject profile = new JSONObject(response);
            long accountId = readAccountId(profile);
            if (accountId <= 0) {
                throw new InvalidTokenException("Токен принят, но профиль аккаунта не распознан");
            }
            return accountId;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static long readAccountId(JSONObject profile) {
        long id = profile.optLong("account_id", 0L);
        if (id <= 0) {
            id = profile.optLong("id", 0L);
        }
        if (id <= 0) {
            JSONObject account = profile.optJSONObject("account");
            if (account != null) {
                id = account.optLong("id", 0L);
            }
        }
        return id;
    }

    private static String readLimited(InputStream input) throws Exception {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > MAX_PROFILE_BYTES) {
                    throw new InvalidTokenException("Ответ Blink слишком большой");
                }
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            input.close();
        }
    }

    private static String normalizeToken(String value) {
        String token = value == null ? "" : value.trim();
        String lower = token.toLowerCase(Locale.ROOT);
        if (lower.startsWith("authorization:")) {
            token = token.substring("authorization:".length()).trim();
            lower = token.toLowerCase(Locale.ROOT);
        }
        if (lower.startsWith("bearer ")) {
            token = token.substring("bearer ".length()).trim();
        }
        return token;
    }

    public static void onSaveFinished(final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = activeDialog;
                if (activeInput != null) {
                    activeInput.setText("");
                }
                if (dialog != null) {
                    dialog.dismiss();
                }
                hideButton(activity);
                Toast.makeText(activity, "Аккаунт подключён", Toast.LENGTH_SHORT).show();

                Intent restart = activity.getPackageManager()
                        .getLaunchIntentForPackage(activity.getPackageName());
                if (restart != null) {
                    restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(restart);
                }
                activity.finish();
            }
        });
    }

    public static void onSaveFailed(final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = activeDialog;
                if (dialog != null && dialog.isShowing()) {
                    setBusy(dialog, false);
                    setStatus("Не удалось сохранить сессию. Попробуй ещё раз");
                }
            }
        });
    }

    private static void setBusy(AlertDialog dialog, boolean busy) {
        Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (positive != null) {
            positive.setEnabled(!busy);
        }
        if (negative != null) {
            negative.setEnabled(!busy);
        }
        if (activeInput != null) {
            activeInput.setEnabled(!busy);
        }
    }

    private static void setStatus(String message) {
        setStatus(message, Color.rgb(190, 45, 60));
    }

    private static void setStatus(String message, int color) {
        if (activeStatus != null) {
            activeStatus.setText(message);
            activeStatus.setTextColor(color);
            activeStatus.setVisibility(View.VISIBLE);
        }
    }

    private static void clearSensitiveUi(Activity activity) {
        if (activeInput != null) {
            activeInput.setText("");
        }
        activeInput = null;
        activeStatus = null;
        activeDialog = null;
        Window window = activity.getWindow();
        if (window != null) {
            window.clearFlags(SECURE_FLAG);
        }
    }

    private static GradientDrawable rounded(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(radius);
        return background;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static final class InvalidTokenException extends Exception {
        InvalidTokenException(String message) {
            super(message);
        }
    }
}
