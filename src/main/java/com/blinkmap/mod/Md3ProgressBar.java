package com.blinkmap.mod;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.PathInterpolator;

/**
 * Material Design 3 Expressive Linear Progress Indicator.
 * Supports:
 * - Rounded track and active indicator caps
 * - M3 signature gap between active indicator and track
 * - M3 signature Stop Indicator dot at the track end
 * - Smooth ValueAnimator with M3 Emphasized Decelerate easing
 * - Seamless indeterminate wave animation
 */
public class Md3ProgressBar extends View {
    private static final PathInterpolator EMPHASIZED_DECEL = new PathInterpolator(0.05f, 0.7f, 0.1f, 1.0f);

    private float progress = 0f; // 0.0 to 1.0
    private float animatedProgress = 0f;
    private boolean indeterminate = false;

    private int trackColor = 0xFF22252C;
    private int indicatorColor = 0xFFEDEDF2;
    private int stopDotColor = 0xFF505460;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stopDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private ValueAnimator progressAnimator;
    private ValueAnimator indeterminateAnimator;
    private float indetPhase = 0f;

    public Md3ProgressBar(Context context) {
        super(context);
        init();
    }

    private void init() {
        trackPaint.setStyle(Paint.Style.FILL);
        indicatorPaint.setStyle(Paint.Style.FILL);
        stopDotPaint.setStyle(Paint.Style.FILL);
        updateColors();
    }

    public void setColors(int track, int indicator, int stopDot) {
        this.trackColor = track;
        this.indicatorColor = indicator;
        this.stopDotColor = stopDot;
        updateColors();
        invalidate();
    }

    private void updateColors() {
        trackPaint.setColor(trackColor);
        indicatorPaint.setColor(indicatorColor);
        stopDotPaint.setColor(stopDotColor);
    }

    public void setProgress(float p, boolean animate) {
        p = Math.max(0f, Math.min(1f, p));
        this.progress = p;
        if (indeterminate) {
            setIndeterminate(false);
        }

        if (progressAnimator != null) {
            progressAnimator.cancel();
        }

        if (animate && isAttachedToWindow()) {
            progressAnimator = ValueAnimator.ofFloat(animatedProgress, p);
            progressAnimator.setDuration(420);
            progressAnimator.setInterpolator(EMPHASIZED_DECEL);
            progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    animatedProgress = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            progressAnimator.start();
        } else {
            animatedProgress = p;
            invalidate();
        }
    }

    public float getProgress() {
        return progress;
    }

    public void setIndeterminate(boolean indet) {
        if (this.indeterminate == indet) return;
        this.indeterminate = indet;

        if (indet) {
            if (progressAnimator != null) progressAnimator.cancel();
            if (indeterminateAnimator == null) {
                indeterminateAnimator = ValueAnimator.ofFloat(0f, 1f);
                indeterminateAnimator.setDuration(1200);
                indeterminateAnimator.setRepeatCount(ValueAnimator.INFINITE);
                indeterminateAnimator.setInterpolator(EMPHASIZED_DECEL);
                indeterminateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        indetPhase = (float) animation.getAnimatedValue();
                        invalidate();
                    }
                });
            }
            indeterminateAnimator.start();
        } else {
            if (indeterminateAnimator != null) {
                indeterminateAnimator.cancel();
            }
            invalidate();
        }
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = dp(8);
        int measuredHeight = resolveSize(height, heightMeasureSpec);
        int measuredWidth = resolveSize(dp(200), widthMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int w = getWidth();
        final int h = getHeight();
        if (w <= 0 || h <= 0) return;

        final float radius = h / 2f;
        final float gap = dp(4);
        final float dotRadius = Math.max(dp(2), h * 0.35f);

        if (indeterminate) {
            // Background track
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, radius, radius, trackPaint);

            // Flowing M3 pulse wave
            float waveWidth = w * 0.45f;
            float left = (w + waveWidth) * indetPhase - waveWidth;
            float right = left + waveWidth;

            float drawLeft = Math.max(0, left);
            float drawRight = Math.min(w, right);
            if (drawRight > drawLeft) {
                rect.set(drawLeft, 0, drawRight, h);
                canvas.drawRoundRect(rect, radius, radius, indicatorPaint);
            }
            return;
        }

        // Determinate M3 Expressive layout
        final float activeEnd = w * animatedProgress;

        if (animatedProgress <= 0.001f) {
            // Empty track with stop dot
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, radius, radius, trackPaint);

            // Stop indicator at right
            canvas.drawCircle(w - radius, h / 2f, dotRadius, stopDotPaint);
            return;
        }

        if (animatedProgress >= 0.999f) {
            // Full bar
            rect.set(0, 0, w, h);
            canvas.drawRoundRect(rect, radius, radius, indicatorPaint);
            return;
        }

        // Active indicator (left to activeEnd)
        rect.set(0, 0, activeEnd, h);
        canvas.drawRoundRect(rect, radius, radius, indicatorPaint);

        // Remaining track with MD3 gap
        float trackStart = activeEnd + gap;
        if (trackStart < w) {
            rect.set(trackStart, 0, w, h);
            canvas.drawRoundRect(rect, radius, radius, trackPaint);

            // Stop indicator dot at right edge
            canvas.drawCircle(w - radius, h / 2f, dotRadius, stopDotPaint);
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (progressAnimator != null) progressAnimator.cancel();
        if (indeterminateAnimator != null) indeterminateAnimator.cancel();
    }
}
