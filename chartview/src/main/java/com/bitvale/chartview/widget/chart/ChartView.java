package com.bitvale.chartview.widget.chart;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import com.bitvale.chartview.ChartSpinnerListener;
import com.bitvale.chartview.ChartViewListener;
import com.bitvale.chartview.R;
import com.bitvale.chartview.Utils;
import com.bitvale.chartview.model.Chart;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 17-Mar-19
 */
public class ChartView extends View implements ChartSpinnerListener {

    private static final float OFFSET_COEFFICIENT = 2.5f;
    private static final int TITLES_COUNT = 6;
    private static final long ANIMATION_DURATION = 450L;
    private static final long ANIMATION_DELAY = 100L;
    private static final float TRANSPARENT = 0f;
    private static final float OPAQUE = 255f;

    private ChartViewListener listener;

    private ArrayList<Long> xAxis = new ArrayList<>();
    private ArrayList<Chart.Column> yAxis = new ArrayList<>();

    private Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint valueFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint chartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private float linesHeight = 0f;
    private float linesOffset = 0f;

    private HashMap<Long, Float> xAxisCoordinates = new LinkedHashMap<>();
    private float firstXCoordinate = 0f;
    private float lastXCoordinate = 0f;

    private float yNewStep = 0f;
    private float yOldStep = 0f;
    private float yMultiplier = 0f;
    private float yCurrentMultiplier = 0f;

    private Path chartPath = new Path();

    private int daysBeforeFrame = 0;
    private int daysAfterFrame = 0;
    private int daysInFrame = 0;

    @ColorInt
    private int axisColor = 0;

    private int startIndex = 0;
    private int endIndex = 0;
    private float valuesAxisXCoordinate = 0f;

    private int yMaxValue = 0;
    private float yAxisOldAnimatedOffset = 0f;

    private float yAxisNewAnimatedOffset = 0f;

    private void setYAxisNewAnimatedOffset(float value) {
        yAxisNewAnimatedOffset = value;
        invalidate();
    }

    private float chartAlpha = OPAQUE;
    private float chartTranslationOffset = 0f;
    private int minHeight = 0;

    private float yAxisOldAlpha = OPAQUE;
    private float yAxisNewAlpha = TRANSPARENT;

    private boolean isChartDrawing = false;

    private AnimatorSet animatorSet = new AnimatorSet();
    private AnimatorSet inOutAnimatorSet = new AnimatorSet();
    private ValueAnimator yMultiplierAnimator;

    private SimpleDateFormat dayFormat = new SimpleDateFormat("MMM d", Locale.US);

    public ChartView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public ChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public ChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.ChartView,
                R.attr.chartViewStyle,
                R.style.ChartView
        );

        axisColor = a.getColor(R.styleable.ChartView_line_color, 0);
        axisPaint.setColor(axisColor);

        int textPaintColor = a.getColor(R.styleable.ChartView_axis_text_color, 0);
        float textPaintTextSize = a.getDimension(R.styleable.ChartView_android_textSize, 0f);
        textPaint.setColor(textPaintColor);
        textPaint.setTextSize(textPaintTextSize);


        linesHeight = context.getResources().getDisplayMetrics().density * 2;

        float strokeWidth = a.getDimension(R.styleable.ChartView_chart_stroke_width, 0f);
        chartPaint.setStyle(Paint.Style.STROKE);
        chartPaint.setStrokeWidth(strokeWidth);

        valuePaint.setStyle(Paint.Style.STROKE);
        valuePaint.setStrokeWidth(strokeWidth);

        int fillColor = a.getColor(R.styleable.ChartView_value_fill_color, 0);
        valueFillPaint.setColor(fillColor);

        minHeight = a.getDimensionPixelSize(R.styleable.ChartView_chart_min_height, 0);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int h = heightSize - heightSize / 3;
        if (h < minHeight) h = minHeight;
        setMeasuredDimension(widthSize, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        linesOffset = (h - (textPaint.getTextSize() * (OFFSET_COEFFICIENT * 2f)) - linesHeight * TITLES_COUNT) / (TITLES_COUNT - 1);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (yAxis.isEmpty()) return;
        isChartDrawing = false;
        for (int i = 0; i < yAxis.size(); i++) {
            Chart.Column column = yAxis.get(i);
            if (column.enabled || column.animation != Chart.ChartAnimation.NONE) {
                isChartDrawing = true;
                break;
            }
        }

        calculateHorizontalStep();
        drawXHorizontalLines(canvas);
        calculateXAxisCoordinates();
        drawXAxisTitles(canvas);

        for (int i = 0; i < yAxis.size(); i++) {
            Chart.Column column = yAxis.get(i);
            if (column.enabled || column.animation != Chart.ChartAnimation.NONE) drawChart(canvas, column);
        }

        if (isChartDrawing) drawYAxisTitles(canvas);

        drawSelectedValues(canvas);
    }


    private void drawXHorizontalLines(Canvas canvas) {
        drawOldGridHorizontalLines(canvas);
        drawNewGridHorizontalLines(canvas);
    }

    private void drawOldGridHorizontalLines(Canvas canvas) {
        for (int i = 0; i < TITLES_COUNT; i++) {
            float yOffset = i * linesOffset + textPaint.getTextSize() * OFFSET_COEFFICIENT + (i * linesHeight);
            axisPaint.setAlpha((int) yAxisOldAlpha);
            if (i < TITLES_COUNT - 1) yOffset += yAxisOldAnimatedOffset;
            else axisPaint.setAlpha((int) OPAQUE);

            int checkpoint = canvas.save();
            canvas.translate(0f, yOffset);
            canvas.drawLine(0f, linesHeight, getWidth(), linesHeight, axisPaint);
            canvas.restoreToCount(checkpoint);
        }
    }

    private void drawNewGridHorizontalLines(Canvas canvas) {
        for (int i = 0; i < TITLES_COUNT; i++) {
            float yOffset = i * linesOffset + textPaint.getTextSize() * OFFSET_COEFFICIENT + (i * linesHeight);
            int m = 1;
            if (yAxisNewAnimatedOffset < 0) m = -1;

            axisPaint.setAlpha((int) yAxisNewAlpha);
            if (i < TITLES_COUNT - 1) yOffset = yOffset - (linesOffset * m) + yAxisNewAnimatedOffset;
            else axisPaint.setAlpha((int) OPAQUE);

            int checkpoint = canvas.save();
            canvas.translate(0f, yOffset);
            canvas.drawLine(0f, linesHeight, getWidth(), linesHeight, axisPaint);
            canvas.restoreToCount(checkpoint);
        }
    }

    private void drawYAxisTitles(Canvas canvas) {
        drawOldYAxisTitle(canvas);
        drawNewYAxisTitle(canvas);
    }

    private void drawOldYAxisTitle(Canvas canvas) {
        for (int i = 0; i < TITLES_COUNT; i++) {
            float yOffset = i * linesOffset + textPaint.getTextSize() * OFFSET_COEFFICIENT + (i * linesHeight);
            String title = String.valueOf(((int) (yOldStep * ((TITLES_COUNT - 1) - i))));
            textPaint.setAlpha((int) yAxisOldAlpha);
            if (i < TITLES_COUNT - 1) yOffset += yAxisOldAnimatedOffset;
            else axisPaint.setAlpha((int) OPAQUE);

            int checkpoint = canvas.save();
            canvas.translate(0f, yOffset);
            canvas.drawText(title, 0f, 0f - linesHeight * 2f, textPaint);
            canvas.restoreToCount(checkpoint);
        }
    }

    private void drawNewYAxisTitle(Canvas canvas) {
        for (int i = 0; i < TITLES_COUNT; i++) {
            float yOffset = i * linesOffset + textPaint.getTextSize() * OFFSET_COEFFICIENT + (i * linesHeight);
            String title = String.valueOf(((int) (yNewStep * ((TITLES_COUNT - 1) - i))));
            int m = 1;
            if (yAxisNewAnimatedOffset < 0) m = -1;

            textPaint.setAlpha((int) yAxisNewAlpha);
            if (i < TITLES_COUNT - 1) yOffset = yOffset - (linesOffset * m) + yAxisNewAnimatedOffset;
            else axisPaint.setAlpha((int) OPAQUE);

            int checkpoint = canvas.save();
            canvas.translate(0f, yOffset);
            canvas.drawText(title, 0f, 0f - linesHeight * 2f, textPaint);
            canvas.restoreToCount(checkpoint);
        }
    }

    private void calculateXAxisCoordinates() {
        xAxisCoordinates.clear();
        float step = getWidth() / (daysInFrame - 0f);
        int j = 0;
        for (int i = startIndex; i <= endIndex; i++) {
            String title = dayFormat.format(xAxis.get(i));
            float textWidth = textPaint.measureText(title);
            float textStartX = (step - textWidth) / 2f;
            int s = j - 1;
            if (daysBeforeFrame == 0) s = j;
            float coordinate = textStartX + textWidth / 2f + s * step;
            if (i == startIndex) firstXCoordinate = coordinate;
            if (i == endIndex) lastXCoordinate = coordinate;
            xAxisCoordinates.put(xAxis.get(i), coordinate);
            j++;
        }
    }

    private void drawXAxisTitles(Canvas canvas) {
        float textStep = getWidth() / (TITLES_COUNT - 0f);

        float yOffset = getHeight() - (textPaint.getTextSize() * (OFFSET_COEFFICIENT / 2f));

        int step = xAxisCoordinates.size() / TITLES_COUNT;

        int start;
        if (daysBeforeFrame == 0) start = startIndex;
        else if (daysAfterFrame == 0) start = startIndex + 1;
        else start = startIndex + 1;
        if (start < 0) start = 0;

        int end;
        if (daysAfterFrame == 0) end = endIndex;
        else if (daysBeforeFrame == 0) end = endIndex - 1;
        else end = endIndex;
        if (end > endIndex) end = endIndex;

        int j = 0;
        for (int i = start; i < end + 1; i += step) {
            String title = dayFormat.format(xAxis.get(i));
            float xOffset = j * textStep;
            float textWidth = textPaint.measureText(title);

            Long value = null;

            for (Map.Entry<Long, Float> current : xAxisCoordinates.entrySet()) {
                if (current.getValue() >= (xOffset + textWidth / 2f)) {
                    value = current.getKey();
                    break;
                }
            }

            if (value == null) {
                for (Long v : xAxisCoordinates.keySet()) {
                    value = v;
                }
            }

            title = dayFormat.format(value);

            if (j == TITLES_COUNT - 1 && daysAfterFrame == 0) title = dayFormat.format(xAxis.get(end));
            if (j == 0 && daysBeforeFrame == 0) title = dayFormat.format(xAxis.get(start));

            textWidth = textPaint.measureText(title);
            float textStartX = (textStep - textWidth) / 2f;

            textPaint.setAlpha((int) OPAQUE);

            int checkpoint = canvas.save();
            canvas.translate(xOffset, yOffset);
            canvas.drawText(title, textStartX, linesHeight * 2f, textPaint);
            canvas.restoreToCount(checkpoint);

            j++;
        }
    }

    private void calculateHorizontalStep() {
        int max = 0;
        for (int i = 0; i < yAxis.size(); i++) {
            Chart.Column column = yAxis.get(i);
            if (column.enabled || column.animation == Chart.ChartAnimation.DOWN) {
                for (int j = startIndex; j < endIndex; j++) {
                    int newMax = column.values.get(j).intValue();
                    if (newMax > max) max = newMax;
                }
            }
        }

        // for drawing some chart values under the top horizontal (y) axis
        while (max % 5 != 0) {
            if (max > 10) max--;
            else max++;
        }

        float tmp = max / (TITLES_COUNT - 1f);
        if (tmp != yNewStep || yOldStep == 0f) {
            if (!animatorSet.isRunning()) yOldStep = yNewStep;
            yNewStep = tmp;
            if (yOldStep == 0f) yOldStep = yNewStep;
            if (yNewStep > 0) yMultiplier = linesOffset / yNewStep;
            if (yCurrentMultiplier == 0f) yCurrentMultiplier = yMultiplier;
        }

        if (yMaxValue != 0 && isChartDrawing) {
            if (yMaxValue > max) startYAxisAnimation(true);
            else if (yMaxValue < max) startYAxisAnimation(false);
        }
        if (max != 0) yMaxValue = max;
    }

    private void startYAxisAnimation(boolean animateUp) {
        if (yMultiplierAnimator != null) yMultiplierAnimator.cancel();

        yMultiplierAnimator = ValueAnimator.ofFloat(yCurrentMultiplier, yMultiplier);

        yMultiplierAnimator.addUpdateListener(animation -> {
            yCurrentMultiplier = (float) animation.getAnimatedValue();
            invalidate();
        });

        yMultiplierAnimator.setDuration(ANIMATION_DURATION);
        yMultiplierAnimator.start();

        if (animatorSet.isRunning()) return;

        float from = 0f;
        float to = 1f;
        if (animateUp) to = -1f;

        ValueAnimator oldAnimator = ValueAnimator.ofFloat(from, to);
        oldAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            yAxisOldAnimatedOffset = Utils.lerp(0f, linesOffset, value);
            yAxisOldAlpha = Utils.lerp(OPAQUE, TRANSPARENT, Math.abs(value));
        });
        oldAnimator.setDuration(ANIMATION_DURATION / 2);
        oldAnimator.setInterpolator(new FastOutSlowInInterpolator());

        ValueAnimator newAnimator = ValueAnimator.ofFloat(from, to);
        newAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            setYAxisNewAnimatedOffset(Utils.lerp(0f, linesOffset, value));
        });
        newAnimator.setDuration(ANIMATION_DURATION);
        newAnimator.setStartDelay(ANIMATION_DELAY);
        newAnimator.setInterpolator(new FastOutSlowInInterpolator());


        ValueAnimator newAlphaAnimator = ValueAnimator.ofFloat(from, to);
        newAlphaAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            yAxisNewAlpha = Utils.lerp(TRANSPARENT, OPAQUE, Math.abs(value));
        });
        newAlphaAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                yAxisOldAlpha = OPAQUE;
                yAxisNewAlpha = TRANSPARENT;
                yAxisOldAnimatedOffset = 0f;
                yAxisNewAnimatedOffset = 0f;
                yOldStep = yNewStep;
                invalidate();
            }
        });
        newAlphaAnimator.setDuration(ANIMATION_DURATION);
        newAlphaAnimator.setStartDelay(ANIMATION_DELAY);

        animatorSet.playTogether(oldAnimator, newAnimator, newAlphaAnimator);
        animatorSet.start();
    }

    public void animateInOut(boolean out) {
        inOutAnimatorSet.cancel();

        float from = 0f;
        float to = 1f;
        if (out) to = -1f;

        ValueAnimator translateAnimator = ValueAnimator.ofFloat(from, to);

        translateAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            if (out) {
                chartTranslationOffset = Utils.lerp(0f, linesOffset * 4, value);
                chartAlpha = Utils.lerp(OPAQUE, TRANSPARENT, Math.abs(value));
            } else {
                chartTranslationOffset = Utils.lerp(linesOffset * -4, 0f, value);
                chartAlpha = Utils.lerp(TRANSPARENT, OPAQUE, Math.abs(value));
            }
            invalidate();
        });
        translateAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                for (int i = 0; i < yAxis.size(); i++) {
                    yAxis.get(i).animation = Chart.ChartAnimation.NONE;
                }
            }
        });

        translateAnimator.setInterpolator(new FastOutSlowInInterpolator());
        translateAnimator.setDuration(ANIMATION_DURATION);

        inOutAnimatorSet.playTogether(translateAnimator);
        inOutAnimatorSet.start();

    }

    private void drawChart(Canvas canvas, Chart.Column column) {
        chartPaint.setColor(Color.parseColor(column.color));
        chartPath.reset();

        for (int i = startIndex; i <= endIndex; i++) {
            Float x = xAxisCoordinates.get(xAxis.get(i));
            float y = getYCoordinate(column.values.get(i));
            chartPaint.setAlpha((int) OPAQUE);
            if (column.animation != Chart.ChartAnimation.NONE) {
                y += chartTranslationOffset;
                chartPaint.setAlpha((int) chartAlpha);
            }
            if (i == startIndex) chartPath.moveTo(x, y);
            else chartPath.lineTo(x, y);
        }

        canvas.drawPath(chartPath, chartPaint);
    }

    private void drawSelectedValues(Canvas canvas) {
        if (valuesAxisXCoordinate == 0f) return;
        canvas.drawLine(
                valuesAxisXCoordinate,
                0f,
                valuesAxisXCoordinate,
                (TITLES_COUNT - 1) * linesOffset + textPaint.getTextSize() * OFFSET_COEFFICIENT + (TITLES_COUNT * linesHeight),
                axisPaint
        );
        drawValues(canvas);
    }

    private void drawValues(Canvas canvas) {
        Long value = null;

        for (Map.Entry<Long, Float> current : xAxisCoordinates.entrySet()) {
            if (current.getValue() >= valuesAxisXCoordinate) {
                value = current.getKey();
                break;
            }
        }

        if (value == null) {
            for (Long v : xAxisCoordinates.keySet()) {
                value = v;
            }
        }

        int valuePosition = xAxis.indexOf(value);

        int dataPos = valuePosition - 1;
        if (valuesAxisXCoordinate == xAxisCoordinates.get(value)) dataPos = valuePosition;

        Chart.ChartSelectedData data = new Chart.ChartSelectedData(xAxis.get(dataPos), new ArrayList<>());

        for (int i = 0; i < yAxis.size(); i++) {
            Chart.Column column = yAxis.get(i);
            if (column.enabled) {
                valuePaint.setColor(Color.parseColor(column.color));
                float interpolatedY;
                if (valuePosition == 0) {
                    interpolatedY = column.values.get(valuePosition);
                } else {
                    float x1 = xAxisCoordinates.get(xAxis.get(valuePosition - 1));
                    float y1 = column.values.get(valuePosition - 1);
                    float x2 = xAxisCoordinates.get(xAxis.get(valuePosition));
                    float y2 = column.values.get(valuePosition);
                    interpolatedY = getInterpolatedY(x1, y1, x2, y2, valuesAxisXCoordinate);
                }

                float cy = getYCoordinate(interpolatedY);
                data.values.add(new Chart.Data((long) interpolatedY, column.color));
                canvas.drawCircle(valuesAxisXCoordinate, cy, valuePaint.getStrokeWidth() * 2f, valueFillPaint);
                canvas.drawCircle(valuesAxisXCoordinate, cy, valuePaint.getStrokeWidth() * 2f, valuePaint);
            }
        }

        listener.onDataSelected(data);
    }

    private float getInterpolatedY(Float x1, Float y1, Float x2, Float y2, Float x3) {
        return ((y2 - y1) / (x2 - x1)) * (x3 - x1) + y1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                valuesAxisXCoordinate = event.getX();
                if (valuesAxisXCoordinate > lastXCoordinate) {
                    valuesAxisXCoordinate = lastXCoordinate;
                }
                if (valuesAxisXCoordinate < firstXCoordinate) {
                    valuesAxisXCoordinate = firstXCoordinate;
                }
                invalidate();
                break;
        }

        return true;
    }

    @Override
    public void onRangeChanged(int daysBeforeFrame, int daysAfterFrame, int daysInFrame) {
        this.daysBeforeFrame = daysBeforeFrame;
        this.daysAfterFrame = daysAfterFrame;
        this.daysInFrame = daysInFrame;
        startIndex = daysBeforeFrame - 1;
        if (daysBeforeFrame == 0) startIndex = 0;

        endIndex = daysBeforeFrame + daysInFrame;
        if (daysAfterFrame == 0) endIndex = daysBeforeFrame + daysInFrame - 1;

        valuesAxisXCoordinate = 0f;
        listener.onDataCleared();
        invalidate();
    }

    private float getYCoordinate(float value) {
        return getHeight() - (textPaint.getTextSize() * OFFSET_COEFFICIENT) - linesHeight * (TITLES_COUNT - 1) - value * yCurrentMultiplier;
    }

    public void setChartViewListener(ChartViewListener listener) {
        this.listener = listener;
    }

    public void setupData(ArrayList<Long> xAxis,  ArrayList<Chart.Column> yAxis) {
        this.xAxis.clear();
        this.yAxis.clear();
        this.xAxis.addAll(xAxis);
        this.yAxis.addAll(yAxis);
        invalidate();
    }
}
