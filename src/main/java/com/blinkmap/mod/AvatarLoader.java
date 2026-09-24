package com.blinkmap.mod;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight, zero-dependency async avatar image loader and generator for BlinkMod.
 */
public final class AvatarLoader {
    private static final LruCache<String, Bitmap> memoryCache = new LruCache<String, Bitmap>(64);
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final int[] PALETTE_BG = {
        0xFF1D3557, 0xFF2B1E4A, 0xFF143628, 0xFF422415, 0xFF441220, 0xFF0E3842, 0xFF2D2A4A, 0xFF3D2645
    };
    private static final int[] PALETTE_TEXT = {
        0xFF8ECAE6, 0xFFC77DFF, 0xFF52B788, 0xFFFFB703, 0xFFE63946, 0xFF00B4D8, 0xFFA594F9, 0xFFE0AAFF
    };

    private AvatarLoader() {}

    public static Bitmap getInitialsAvatar(String name, long id, int sizePx) {
        String key = "initials_" + id + "_" + name;
        Bitmap cached = memoryCache.get(key);
        if (cached != null) return cached;

        int colorIdx = (int) (Math.abs(id ^ (name != null ? name.hashCode() : 0)) % PALETTE_BG.length);
        int bgColor = PALETTE_BG[colorIdx];
        int textColor = PALETTE_TEXT[colorIdx];

        String initial = "?";
        if (name != null && !name.isEmpty()) {
            String clean = name.replaceAll("^[@_\\s]+", "");
            if (!clean.isEmpty()) {
                initial = clean.substring(0, 1).toUpperCase(Locale.US);
            }
        }

        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(bgColor);
        canvas.drawCircle(sizePx / 2.0f, sizePx / 2.0f, sizePx / 2.0f, p);

        p.setColor(textColor);
        p.setTextSize(sizePx * 0.44f);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fm = p.getFontMetrics();
        float y = (sizePx / 2.0f) - ((fm.ascent + fm.descent) / 2.0f);
        canvas.drawText(initial, sizePx / 2.0f, y, p);

        memoryCache.put(key, bmp);
        return bmp;
    }

    public static void load(final String url, final ImageView target, final Bitmap fallback) {
        if (target == null) return;
        if (url == null || url.isEmpty()) {
            target.setImageBitmap(fallback);
            return;
        }

        Bitmap cached = memoryCache.get(url);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }

        target.setImageBitmap(fallback);
        target.setTag(url);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = null;
                HttpURLConnection conn = null;
                try {
                    URL u = new URL(url);
                    conn = (HttpURLConnection) u.openConnection();
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    Bitmap raw = BitmapFactory.decodeStream(is);
                    is.close();
                    if (raw != null) {
                        bmp = toCircleBitmap(raw);
                        memoryCache.put(url, bmp);
                    }
                } catch (Throwable ignored) {
                } finally {
                    if (conn != null) conn.disconnect();
                }

                final Bitmap result = bmp;
                if (result != null) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (url.equals(target.getTag())) {
                                target.setImageBitmap(result);
                            }
                        }
                    });
                }
            }
        });
    }

    public static Bitmap toCircleBitmap(Bitmap src) {
        if (src == null) return null;
        int size = Math.min(src.getWidth(), src.getHeight());
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader shader = new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        float r = size / 2.0f;
        canvas.drawCircle(r, r, r, paint);
        return out;
    }
}
