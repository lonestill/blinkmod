package com.blinkmap.mod;

import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class ChatFilter {
    private static final String PREFS_NAME = "blinkmod_settings";
    public static final String KEY_HIDE_BLINK_NEWS = "hide_blink_news";
    public static final String KEY_REPLACE_BLINK_NEWS = "replace_blink_news";
    public static final long BLINK_NEWS_CHAT_ID = 82632431210250275L;

    public static volatile Object currentQ8g;

    private ChatFilter() {}

    public static boolean isHideBlinkNewsEnabled() {
        SharedPreferences p = getPrefs();
        return p != null && p.getBoolean(KEY_HIDE_BLINK_NEWS, false);
    }

    public static boolean isReplaceBlinkNewsEnabled() {
        SharedPreferences p = getPrefs();
        return p == null || p.getBoolean(KEY_REPLACE_BLINK_NEWS, true);
    }

    public static String filterQuery(String sql) {
        if (sql == null) return sql;

        if (isReplaceBlinkNewsEnabled()) {
            if (sql.contains("lm.content_status AS last_message_content_status")) {
                sql = sql.replace("lm.content_status AS last_message_content_status",
                        "CASE WHEN c.chat_id = 82632431210250275 THEN '{\"type\":\"synchronized_content\",\"remote\":{\"type\":\"text\",\"value\":\"скоро тут что то будет\",\"style\":0,\"scale\":1.0,\"embeds\":[]}}' ELSE lm.content_status END AS last_message_content_status");
            }
        }

        if (isHideBlinkNewsEnabled()) {
            if (sql.contains("c.name != 'Blink News'")) {
                return sql;
            }
            if (sql.contains("LEFT JOIN UserEntity u ON lm.sender_id = u.id")) {
                return sql.replace("LEFT JOIN UserEntity u ON lm.sender_id = u.id\n            ORDER BY",
                        "LEFT JOIN UserEntity u ON lm.sender_id = u.id\n            WHERE c.type != 3 AND c.name != 'Blink News'\n            ORDER BY");
            }
            int lastOrder = sql.lastIndexOf("ORDER BY");
            if (lastOrder != -1) {
                return sql.substring(0, lastOrder)
                        + "WHERE c.type != 3 AND c.name != 'Blink News'\n            "
                        + sql.substring(lastOrder);
            }
        }
        return sql;
    }

    public static String transformName(long chatId, String original) {
        if (chatId == BLINK_NEWS_CHAT_ID || "Blink News".equals(original)) {
            if (isReplaceBlinkNewsEnabled()) {
                return "BlinkMod News";
            }
        }
        return original;
    }

    public static String transformPreviewText(long chatId, String original) {
        if (chatId == BLINK_NEWS_CHAT_ID) {
            if (isReplaceBlinkNewsEnabled()) {
                return "скоро тут что то будет";
            }
        }
        return original;
    }

    public static List transformMessageList(List list) {
        if (list == null || list.isEmpty() || !isReplaceBlinkNewsEnabled()) {
            currentQ8g = null;
            return list;
        }
        try {
            Object first = list.get(0);
            Field fieldA = first.getClass().getField("a");
            Object lag = fieldA.get(first);
            Field fieldB = lag.getClass().getField("b");
            long chatId = fieldB.getLong(lag);

            if (chatId == BLINK_NEWS_CHAT_ID) {
                Object q8g = currentQ8g;
                currentQ8g = null;

                if (q8g != null) {
                    try {
                        Field fieldE = q8g.getClass().getField("e");
                        Object cursor = fieldE.get(q8g);
                        if (cursor != null) {
                            list.clear();
                            return list;
                        }
                    } catch (Throwable ignored) {}
                }

                while (list.size() > 1) {
                    list.remove(list.size() - 1);
                }

                try {
                    Field fieldSender = first.getClass().getField("b");
                    fieldSender.setAccessible(true);
                    fieldSender.set(first, "BlinkMod News");
                } catch (Throwable ignored) {}

                try {
                    Field fieldReply = first.getClass().getField("e");
                    fieldReply.setAccessible(true);
                    fieldReply.set(first, null);

                    Field fieldFwd = first.getClass().getField("f");
                    fieldFwd.setAccessible(true);
                    fieldFwd.set(first, null);

                    Field fieldReaction = first.getClass().getField("g");
                    fieldReaction.setAccessible(true);
                    fieldReaction.set(first, null);
                } catch (Throwable ignored) {}

                try {
                    String json = "{\"type\":\"synchronized_content\",\"remote\":{\"type\":\"text\",\"value\":\"скоро тут что то будет\",\"style\":0,\"scale\":1.0,\"embeds\":[]}}";
                    Class<?> g8gClass = Class.forName("g8g");
                    Method methodA = g8gClass.getMethod("a", String.class);
                    Object e8g = methodA.invoke(null, json);
                    if (e8g != null) {
                        Field fieldL = lag.getClass().getField("l");
                        fieldL.setAccessible(true);
                        fieldL.set(lag, e8g);

                        Field fieldAInE8g = e8g.getClass().getField("a");
                        Object q7g = fieldAInE8g.get(e8g);

                        Field fieldP = lag.getClass().getField("p");
                        fieldP.setAccessible(true);
                        fieldP.set(lag, q7g);
                    }
                } catch (Throwable t) {
                    android.util.Log.e("BlinkMod", "Failed to deserialize fake message content", t);
                }
            }
        } catch (Throwable t) {
            android.util.Log.e("BlinkMod", "transformMessageList error", t);
        } finally {
            currentQ8g = null;
        }
        return list;
    }

    public static List transformForwardMessageList(List list) {
        if (list == null || list.isEmpty() || !isReplaceBlinkNewsEnabled()) {
            return list;
        }
        try {
            Object first = list.get(0);
            Field fieldA = first.getClass().getField("a");
            Object lag = fieldA.get(first);
            Field fieldB = lag.getClass().getField("b");
            long chatId = fieldB.getLong(lag);
            if (chatId == BLINK_NEWS_CHAT_ID) {
                list.clear();
            }
        } catch (Throwable ignored) {}
        return list;
    }

    private static SharedPreferences getPrefs() {
        Context c = StepHooks.getApp();
        if (c != null) {
            return c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object appObj = at.getMethod("currentApplication").invoke(null);
            if (appObj instanceof Context) {
                return ((Context) appObj).getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
