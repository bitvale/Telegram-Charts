package com.bitvale.chartview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 16-Mar-19
 */
public class ChartSpinner extends View {

    private ChartSpinnerListener listener;

    private ArrayList<Long> xAxis;
    private ArrayList<Chart.Column> yAxis = new ArrayList<Chart.Column>();

    private Paint chartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @ColorInt
    private int foregroundColor = 0;
    @ColorInt
    private int frameColor = 0;

    private int spinnerHeight = 0;

    private Path chartPath = new Path();

    private float xMultiplier = 0f;
    private float yMultiplier = 0f;

    private float smallPadding = 0f;
    private int minFrameWidth = 0;
    private int frameSideSize = 0;
    private Rect frameOuterRect = new Rect();
    private Rect frameInnerRect = new Rect();
    private Region frameRegion = new Region();

    private int currentFrameWidth = 0;

    private float dX = 0f;
    private int currentOuterLeft = 0;
    private int currentOuterRight = 0;

    private boolean moveLeftBorder = false;
    private boolean moveRightBorder = false;

    public ChartSpinner(Context context) {
        super(context);
        init(context, null, 0);
    }

    public ChartSpinner(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public ChartSpinner(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        setId(generateViewId());
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.ChartSpinner,
                R.attr.chartSpinnerStyle,
                R.style.ChartSpinner
        );

        foregroundColor = a.getColor(R.styleable.ChartSpinner_foreground_color, 0);
        frameColor = a.getColor(R.styleable.ChartSpinner_frame_color, 0);

        spinnerHeight = a.getDimensionPixelOffset(R.styleable.ChartSpinner_spinner_height, 0);

        chartPaint.setStyle(Paint.Style.STROKE);

        smallPadding = context.getResources().getDisplayMetrics().density * 4;
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(widthSize, spinnerHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (yAxis.isEmpty()) return;
        calculateMultipliers();
        calculateFrameSize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (yAxis.isEmpty()) return;

        for (int i = 0; i < yAxis.size(); i++) {
            if (yAxis.get(i).enabled) drawChart(canvas, yAxis.get(i));
        }
        drawFrame(canvas);
        drawForeground(canvas);
    }

    private void drawChart(Canvas canvas, Chart.Column column) {
        chartPaint.setColor(Color.parseColor(column.color));
        chartPaint.setStrokeWidth(4f);
        chartPath.reset();

        for (int i = 0; i < column.values.size(); i++) {
            float x = i * xMultiplier;
            float y = getHeight() - column.values.get(i) * yMultiplier;
            if (y >= getHeight() / 2) y -= smallPadding;
            else y += smallPadding;
            if (i == 0) {
                chartPath.moveTo(x, y);
            } else {
                chartPath.lineTo(x, y);
            }
        }
        canvas.drawPath(chartPath, chartPaint);
    }

    private void drawFrame(Canvas canvas) {
        paint.setColor(frameColor);
        frameRegion.set(frameOuterRect);
        frameRegion.op(frameInnerRect, Region.Op.XOR);
        canvas.drawPath(frameRegion.getBoundaryPath(), paint);
    }

    private void calculateFrameSize() {
        minFrameWidth = 6 * (getWidth() / xAxis.size());
        frameSideSize = minFrameWidth / 4;
        if (currentFrameWidth == 0) currentFrameWidth = minFrameWidth;
        updateFrameSize();
    }

    private void updateFrameSize() {
        frameOuterRect.set(getWidth() - currentFrameWidth, 0, getWidth(), getHeight());
        frameInnerRect.set(
                (int) (frameOuterRect.left + (frameSideSize * 1.5)),
                (frameOuterRect.top + frameSideSize / 2),
                (int) (frameOuterRect.right - (frameSideSize * 1.5)),
                (frameOuterRect.bottom - frameSideSize / 2)
        );
        calculateChartData(moveRightBorder, moveLeftBorder);
    }

    private void calculateChartData(boolean moveRightBorder, boolean moveLeftBorder) {
        int frameStartPosition = frameOuterRect.left;
        int frameEndPosition = frameOuterRect.right;
        int frameWidth = frameEndPosition - frameStartPosition;
        float oneDayWidth = getWidth() / ((float) xAxis.size());
        int daysInFrame = (int) (Math.ceil(frameWidth / oneDayWidth));
        int daysAfterFrame = (int) (Math.floor((getWidth() - frameEndPosition) / oneDayWidth));
        int daysBeforeFrame = (int) (Math.floor((frameStartPosition / oneDayWidth)));

        if (moveLeftBorder) {
            if (daysBeforeFrame < xAxis.size() - daysAfterFrame - daysInFrame) daysBeforeFrame =
                    xAxis.size() - daysAfterFrame - daysInFrame;
        }
        if (moveRightBorder) {
            if (daysAfterFrame > xAxis.size() - daysBeforeFrame - daysInFrame) daysAfterFrame =
                    xAxis.size() - daysAfterFrame - daysInFrame;
        }
        listener.onRangeChanged(daysBeforeFrame, daysAfterFrame, daysInFrame);
    }

    private void drawForeground(Canvas canvas) {
        paint.setColor(foregroundColor);
        canvas.drawRect(0f, 0f, frameOuterRect.left, spinnerHeight, paint);
        canvas.drawRect(frameOuterRect.right, 0f, getWidth(), spinnerHeight, paint);
    }

    void setupData(Chart chart) {
        xAxis = chart.columns.get(0).values;
        for (int i = 1; i < chart.columns.size(); i++) {
            yAxis.add(chart.columns.get(i));
        }
        invalidate();
    }

    private void calculateMultipliers() {
        int max = 0;
        for (int i = 0; i < yAxis.size(); i++) {
            long newMax = Collections.max(yAxis.get(i).values);
            if (newMax > max) max = (int) newMax;
        }

        xMultiplier = ((float) getWidth()) / (xAxis.size() - 1);
        yMultiplier = ((float) spinnerHeight) / max;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        int action = event.getAction() & MotionEvent.ACTION_MASK;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                dX = x;
                currentOuterLeft = frameOuterRect.left;
                currentOuterRight = frameOuterRect.right;

                moveLeftBorder = false;
                moveRightBorder = false;

                if (x < frameInnerRect.left + 20) {
                    moveLeftBorder = true;
                }
                if (x > frameInnerRect.right - 20) {
                    moveRightBorder = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int l = currentOuterLeft + (int) (x - dX);
                int r = currentOuterRight + (int) (x - dX);
                if (moveLeftBorder) {
                    frameOuterRect.left = l;
                    if (frameOuterRect.width() < minFrameWidth) {
                        frameOuterRect.left = frameOuterRect.right - minFrameWidth;
                    }
                    if (frameOuterRect.left < 0) {
                        frameOuterRect.left = 0;
                    }
                    frameInnerRect.left = frameOuterRect.left + (int) (frameSideSize * 1.5);
                } else {
                    if (moveRightBorder) {
                        frameOuterRect.right = r;
                        if (frameOuterRect.width() < minFrameWidth) {
                            frameOuterRect.right = frameOuterRect.left + minFrameWidth;
                        }
                        if (frameOuterRect.right > getWidth()) {
                            frameOuterRect.right = getWidth();
                        }
                        frameInnerRect.right = frameOuterRect.right - (int) (frameSideSize * 1.5);
                    } else {
                        if (l < 0) {
                            r = frameOuterRect.right;
                            l = 0;
                        }
                        if (r > getWidth()) {
                            r = getWidth();
                            l = frameOuterRect.left;
                        }
                        frameOuterRect.left = l;
                        frameOuterRect.right = r;
                        frameInnerRect.left = l + ((int) (frameSideSize * 1.5));
                        frameInnerRect.right = r - ((int) (frameSideSize * 1.5));
                    }
                }
                break;
        }
        calculateChartData(moveRightBorder, moveLeftBorder);
        invalidate();
        return true;
    }


    public void setChartListener(ChartSpinnerListener listener) {
        this.listener = listener;
    }
}
