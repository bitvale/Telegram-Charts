package com.bitvale.chartview.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.bitvale.chartview.R;
import com.bitvale.chartview.Utils;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 17-Mar-19
 */
public class ChipView extends View {

    private static final long SELECTING_DURATION = 200L;
    private static final long DESELECTING_DURATION = 150L;

    private TextPaint textPaint;

    private Paint outlinePaint;
    private RectF outlineRect = new RectF();

    private int padding;
    private float rounding = 0f;

    private Paint clippingPaint;
    private int outlineColor = 0;

    private StaticLayout textLayout;
    private CharSequence text = "";

    private float progress = 1f;

    private void setProgress(float progress) {
        if (this.progress != progress) {
            this.progress = progress;
            postInvalidateOnAnimation();
        }
    }

    private boolean isChecked = true;

    public void setChecked(boolean checked) {
        this.isChecked = checked;
        postInvalidateOnAnimation();
        if (checked) progress = 1f;
        else progress = 0f;
    }

    public boolean isChecked() {
        return isChecked;
    }

    private ValueAnimator progressAnimator;

    public ChipView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public ChipView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public ChipView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.ChipView,
                R.attr.chipViewStyle,
                R.style.ChipView
        );

        outlineColor = a.getColor(R.styleable.ChipView_strokeColor, 0);

        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStrokeWidth(a.getDimension(R.styleable.ChipView_outlineWidth, 0));
        outlinePaint.setStyle(Paint.Style.STROKE);

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(a.getColor(R.styleable.ChipView_android_textColor, 0));
        textPaint.setTextSize(a.getDimension(R.styleable.ChipView_android_textSize, 0));

        clippingPaint = new Paint();
        clippingPaint.setColor(a.getColor(R.styleable.ChipView_checked_background, 0));

        padding = a.getDimensionPixelSize(R.styleable.ChipView_android_padding, 0);

        boolean isChecked = a.getBoolean(R.styleable.ChipView_isChecked, false);
        if (isChecked) progress = 1f;

        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        createTextLayout(MeasureSpec.getSize(widthMeasureSpec));

        int h = textLayout.getHeight() + padding;
        int nonTextWidth = padding * 2 + (int) outlinePaint.getStrokeWidth() * 2 + h / 2 + padding;
        int w = nonTextWidth + getTextWidyh(textLayout);

        setMeasuredDimension(w, h);

        rounding = Math.min(w, h) / 2f;

        float strokeWidth = outlinePaint.getStrokeWidth();
        float halfStroke = strokeWidth / 2f;

        outlineRect.set(
                halfStroke,
                halfStroke,
                w - halfStroke,
                h - halfStroke
        );
    }

    private int getTextWidyh(StaticLayout textLayout) {
        float width = 0f;
        for (int i = 0; i < textLayout.getLineCount(); i++) {
            width = coerceAtLeast(width, textLayout.getLineWidth(i));
        }
        return (int) width;
    }

    public float coerceAtLeast(float value, float minimumValue) {
        if (value < minimumValue) return minimumValue;
        else return value;
    }

    private void createTextLayout(int textWidth) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textLayout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, textWidth).build();
        } else {
            textLayout = new StaticLayout(text, textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, true);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        outlinePaint.setColor(outlineColor);
        canvas.drawRoundRect(
                outlineRect,
                rounding,
                rounding,
                outlinePaint
        );

        float radius = Utils.lerp(0f, (getHeight() - padding - outlinePaint.getStrokeWidth() * 2f) / 2f, progress);

        canvas.drawCircle(
                getHeight() - padding - outlinePaint.getStrokeWidth() * 4f,
                getHeight() / 2f,
                radius,
                clippingPaint
        );

        outlinePaint.setColor(clippingPaint.getColor());

        canvas.drawCircle(
                getHeight() - padding - outlinePaint.getStrokeWidth() * 4f,
                getHeight() / 2f,
                (getHeight() - padding - outlinePaint.getStrokeWidth() * 2f) / 2f,
                outlinePaint
        );

        float xOffset = outlinePaint.getStrokeWidth() + padding + getHeight() / 2f + padding;
        float yOffset = (getHeight() - textLayout.getHeight()) / 2f - padding / 6f;
        int checkpoint = canvas.save();
        canvas.translate(xOffset, yOffset);
        textLayout.draw(canvas);
        canvas.restoreToCount(checkpoint);
    }

    public void animateChecked() {
        float newProgress = 0f;
        if (!isChecked) newProgress = 1f;

        if (progressAnimator != null) progressAnimator.cancel();

        progressAnimator = ValueAnimator.ofFloat(progress, newProgress);

        progressAnimator.addUpdateListener(animation -> setProgress((float) animation.getAnimatedValue()));
        float finalNewProgress = newProgress;
        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setProgress(finalNewProgress);
            }
        });

        long duration = DESELECTING_DURATION;
        if (!isChecked) duration = SELECTING_DURATION;

        progressAnimator.setDuration(duration);
        progressAnimator.start();

        isChecked = !isChecked;
    }

    public void setText(String text) {
        this.text = text;
//        requestLayout();
    }

    public void setCheckedColor(int color) {
        clippingPaint.setColor(color);
    }
}
