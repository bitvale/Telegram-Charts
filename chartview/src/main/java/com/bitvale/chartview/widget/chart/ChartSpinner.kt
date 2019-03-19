package com.bitvale.chartview.widget.chart

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionPixelOffsetOrThrow
import com.bitvale.chartview.model.Chart
import com.bitvale.chartview.ChartSpinnerListener
import com.bitvale.chartview.R


/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 10-Mar-19
 */
class ChartSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var listener: ChartSpinnerListener? = null

    private var xAxis = ArrayList<Long>()
    private val yAxis = ArrayList<Chart.Column>()

    private val chartPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    @ColorInt
    private var foregroundColor: Int = 0
    @ColorInt
    private var frameColor: Int = 0

    private var spinnerHeight: Int = 0

    private val chartPath = Path()

    private var xMultiplier = 0f
    private var yMultiplier = 0f

    private var smallPadding = 0f
    private var minFrameWidth = 0
    private var frameSideSize = 0
    private val frameOuterRect = Rect()
    private val frameInnerRect = Rect()
    private val frameRegion = Region()

    private var currentFrameWidth = 0

    private var dX: Float = 0f
    private var deltaX: Float = 0f

    var currentOuterLeft = 0
    var currentOuterRight = 0

    var moveLeftBorder = false
    var moveRightBorder = false
    init {
        id = generateViewId()
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        attrs?.let { retrieveAttributes(attrs) }
    }

    private fun retrieveAttributes(attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.ChartSpinner,
            R.attr.chartSpinnerStyle,
            R.style.ChartSpinner
        )

        foregroundColor = a.getColorOrThrow(R.styleable.ChartSpinner_foreground_color)
        frameColor = a.getColorOrThrow(R.styleable.ChartSpinner_frame_color)

        spinnerHeight = a.getDimensionPixelOffsetOrThrow(R.styleable.ChartSpinner_spinner_height)

        chartPaint.apply {
            style = Paint.Style.STROKE
        }

        smallPadding = resources.displayMetrics.density * 4
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(widthSize, spinnerHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (yAxis.isEmpty()) return
        calculateMultipliers()
        calculateFrameSize()
    }

    override fun onDraw(canvas: Canvas?) {
        if (yAxis.isEmpty()) return
        yAxis.forEach {
            if (it.enabled) drawChart(canvas, it)
        }
        drawFrame(canvas)
        drawForeground(canvas)
    }

    private fun drawChart(canvas: Canvas?, column: Chart.Column) {
        chartPaint.color = Color.parseColor(column.color)
        chartPaint.strokeWidth = 4f
        chartPath.reset()
        var i = 0
        column.values.forEach {
            val x = i * xMultiplier
            var y = height - it * yMultiplier
            if (y >= height / 2) y -= smallPadding
            else y += smallPadding
            if (i == 0) {
                chartPath.moveTo(x, y)
            } else {
                chartPath.lineTo(x, y)
            }
            i++
        }

        canvas?.drawPath(chartPath, chartPaint)
    }

    private fun drawFrame(canvas: Canvas?) {
        paint.color = frameColor
        frameRegion.set(frameOuterRect)
        frameRegion.op(frameInnerRect, Region.Op.XOR)
        canvas?.drawPath(frameRegion.boundaryPath, paint)
    }

    private fun calculateFrameSize() {
        minFrameWidth = 6 * (width / xAxis.size)
        frameSideSize = minFrameWidth / 4
        if (currentFrameWidth == 0) currentFrameWidth = minFrameWidth
        updateFrameSize()
    }

    private fun updateFrameSize() {
        frameOuterRect.set(width - currentFrameWidth, 0, width, height)
        frameInnerRect.set(
            frameOuterRect.left + (frameSideSize * 1.5).toInt(),
            frameOuterRect.top + frameSideSize / 2,
            frameOuterRect.right - (frameSideSize * 1.5).toInt(),
            frameOuterRect.bottom - frameSideSize / 2
        )
        calculateChartData(moveRightBorder, moveLeftBorder)
    }

    private fun calculateChartData(moveRightBorder: Boolean, moveLeftBorder: Boolean) {
        val frameStartPosition = frameOuterRect.left
        val frameEndPosition = frameOuterRect.right
        val frameWidth = frameEndPosition - frameStartPosition
        val oneDayWidth = width / xAxis.size.toFloat()
        val daysInFrame = Math.ceil((frameWidth / oneDayWidth).toDouble()).toInt()
        var daysAfterFrame = Math.floor(((width - frameEndPosition) / oneDayWidth).toDouble()).toInt()
        var daysBeforeFrame = Math.floor((frameStartPosition / oneDayWidth).toDouble()).toInt()

        if (moveLeftBorder) {
            if (daysBeforeFrame < xAxis.size - daysAfterFrame - daysInFrame) daysBeforeFrame =
                xAxis.size - daysAfterFrame - daysInFrame
        }
        if (moveRightBorder) {
            if (daysAfterFrame > xAxis.size - daysBeforeFrame - daysInFrame) daysAfterFrame =
                xAxis.size - daysAfterFrame - daysInFrame
        }
        listener?.onRangeChanged(daysBeforeFrame, daysAfterFrame, daysInFrame, deltaX)
    }

    private fun drawForeground(canvas: Canvas?) {
        paint.color = foregroundColor
        canvas?.drawRect(0f, 0f, frameOuterRect.left.toFloat(), spinnerHeight.toFloat(), paint)
        canvas?.drawRect(frameOuterRect.right.toFloat(), 0f, width.toFloat(), spinnerHeight.toFloat(), paint)
    }

    private fun calculateMultipliers() {
        var max = 0
        yAxis.forEach {
            val newMax = it.values.max() ?: 0
            if (newMax > max) max = newMax.toInt()
        }

        xMultiplier = width.toFloat() / (xAxis.size - 1)
        yMultiplier = spinnerHeight.toFloat() / max
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val action = event.action and MotionEvent.ACTION_MASK

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                dX = x
                currentOuterLeft = frameOuterRect.left
                currentOuterRight = frameOuterRect.right

                moveLeftBorder = false
                moveRightBorder = false

                if (x < frameInnerRect.left + 20) {
                    moveLeftBorder = true
                }
                if (x > frameInnerRect.right - 20) {
                    moveRightBorder = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                deltaX = 0f
                var l = currentOuterLeft + (x - dX).toInt()
                var r = currentOuterRight + (x - dX).toInt()
                if (moveLeftBorder) {
                    frameOuterRect.left = l
                    if (frameOuterRect.width() < minFrameWidth) {
                        frameOuterRect.left = frameOuterRect.right - minFrameWidth
                    }
                    if (frameOuterRect.left < 0) {
                        frameOuterRect.left = 0
                    }
                    frameInnerRect.left = frameOuterRect.left + (frameSideSize * 1.5).toInt()
                } else {
                    if (moveRightBorder) {
                        frameOuterRect.right = r
                        if (frameOuterRect.width() < minFrameWidth) {
                            frameOuterRect.right = frameOuterRect.left + minFrameWidth
                        }
                        if (frameOuterRect.right > width) {
                            frameOuterRect.right = width
                        }
                        frameInnerRect.right = frameOuterRect.right - (frameSideSize * 1.5).toInt()
                    } else {
                        deltaX = x - dX
                        if (l < 0) {
                            r = frameOuterRect.right
                            l = 0
                        }
                        if (r > width) {
                            r = width
                            l = frameOuterRect.left
                        }
                        frameOuterRect.left = l
                        frameOuterRect.right = r
                        frameInnerRect.left = l + (frameSideSize * 1.5).toInt()
                        frameInnerRect.right = r - (frameSideSize * 1.5).toInt()
                    }
                }
            }
        }
        calculateChartData(moveRightBorder, moveLeftBorder)
        invalidate()
        return true
    }

    fun setChartListener(listener: ChartSpinnerListener) {
        this.listener = listener
    }

    fun setupData(xAxis: ArrayList<Long>, yAxis: ArrayList<Chart.Column>) {
        this.xAxis.clear()
        this.xAxis.addAll(xAxis)
        this.yAxis.clear()
        this.yAxis.addAll(yAxis)
        invalidate()
    }
}