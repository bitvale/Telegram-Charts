package com.bitvale.chartview.widget.chart

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getDimensionPixelOffsetOrThrow
import androidx.core.graphics.withTranslation
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.bitvale.chartview.*
import com.bitvale.chartview.model.Chart
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import android.R.attr.end
import android.R.attr.start




/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 09-Mar-19
 */
class ChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ChartSpinnerListener {

    companion object {
        const val OFFSET_COEFFICIENT = 2.5f
        const val TITLES_COUNT = 6
        const val ANIMATION_DURATION = 450L
        const val ANIMATION_DELAY = 100L
        const val TRANSPARENT = 0f
        const val OPAQUE = 255f
    }

    private var listener: ChartViewListener? = null

    private var xAxis = ArrayList<Long>()
    private val yAxis = ArrayList<Chart.Column>()

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val valueFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chartPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private var linesHeight = 0f
    private var linesOffset = 0f

    private val xAxisCoordinates = LinkedHashMap<Long, Float>()
    private var firstXCoordinate = 0f
    private var lastXCoordinate = 0f
    private var firstXValue = 0L
    private var lastXValue = 0L

    private var yNewStep = 0f
    private var yOldStep = 0f
    private var yMultiplier = 0f
    private var yCurrentMultiplier = 0f

    private val chartPath = Path()

    private var daysBeforeFrame: Int = 0
    private var daysAfterFrame: Int = 0
    private var daysInFrame: Int = 0

    @ColorInt
    private var axisColor = 0

    private var startIndex = 0
    private var endIndex = 0
    private var valuesAxisXCoordinate = 0f

    private var yMaxValue = 0
    private var yAxisOldAnimatedOffset = 0f

    private var yAxisNewAnimatedOffset = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var chartAlpha = OPAQUE
    private var chartTranslationOffset = 0f

    private var minHeight = 0

    private var dx = 0f

    private var yAxisOldAlpha = OPAQUE
    private var yAxisNewAlpha = TRANSPARENT

    private var isChartDrawing = false

    private var animatorSet: AnimatorSet = AnimatorSet()
    private var inOutAnimatorSet: AnimatorSet = AnimatorSet()
    private var yMultiplierAnimator: ValueAnimator? = null

    private val dayFormat = SimpleDateFormat("MMM d", Locale.US)

    init {
        attrs?.let { retrieveAttributes(attrs) }
    }

    private fun retrieveAttributes(attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.ChartView,
            R.attr.chartViewStyle,
            R.style.ChartView
        )

        axisColor = a.getColorOrThrow(R.styleable.ChartView_line_color)
        axisPaint.color = axisColor

        textPaint.apply {
            color = a.getColorOrThrow(R.styleable.ChartView_axis_text_color)
            textSize = a.getDimensionOrThrow(R.styleable.ChartView_android_textSize)
        }

        linesHeight = resources.displayMetrics.density * 2

        chartPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = a.getDimensionOrThrow(R.styleable.ChartView_chart_stroke_width)
        }
        valuePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = chartPaint.strokeWidth
        }

        valueFillPaint.color = a.getColorOrThrow(R.styleable.ChartView_value_fill_color)

        minHeight = a.getDimensionPixelOffsetOrThrow(R.styleable.ChartView_chart_min_height)
        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        var h = heightSize - heightSize / 3
        if (h < minHeight) h = minHeight
        setMeasuredDimension(widthSize, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        linesOffset = (h - (textPaint.textSize * (OFFSET_COEFFICIENT * 2f)) - linesHeight * TITLES_COUNT) / (TITLES_COUNT - 1)
    }

    override fun onDraw(canvas: Canvas?) {
        if (yAxis.isEmpty()) return
        isChartDrawing = false
        yAxis.forEach {
            if (it.enabled || it.animation != Chart.ChartAnimation.NONE) {
                isChartDrawing = true
                return@forEach
            }
        }

        calculateHorizontalStep()
        drawXHorizontalLines(canvas)
        calculateXAxisCoordinates()
        drawXAxisTitles(canvas)

        yAxis.forEach {
            if (it.enabled || it.animation != Chart.ChartAnimation.NONE) drawChart(canvas, it)
        }
        if (isChartDrawing) {
            drawYAxisTitles(canvas)
        }
        drawSelectedValues(canvas)
    }

    private fun drawXHorizontalLines(canvas: Canvas?) {
        drawOldGridHorizontalLines(canvas)
        drawNewGridHorizontalLines(canvas)
    }

    private fun drawOldGridHorizontalLines(canvas: Canvas?) {
        for (i in 0 until TITLES_COUNT) {
            var yOffset = i * linesOffset + textPaint.textSize * OFFSET_COEFFICIENT + (i * linesHeight)
            axisPaint.alpha = yAxisOldAlpha.toInt()
            if (i < TITLES_COUNT - 1) yOffset += yAxisOldAnimatedOffset
            else axisPaint.alpha = OPAQUE.toInt()
            canvas?.withTranslation(
                y = yOffset
            ) {
                drawLine(0f, linesHeight, width.toFloat(), linesHeight, axisPaint)
            }
        }
    }

    private fun drawNewGridHorizontalLines(canvas: Canvas?) {
        for (i in 0 until TITLES_COUNT) {
            var yOffset = i * linesOffset + textPaint.textSize * OFFSET_COEFFICIENT + (i * linesHeight)
            val m = if (yAxisNewAnimatedOffset < 0) -1 else 1
            axisPaint.alpha = yAxisNewAlpha.toInt()
            if (i < TITLES_COUNT - 1) yOffset = yOffset - (linesOffset * m) + yAxisNewAnimatedOffset
            else axisPaint.alpha = OPAQUE.toInt()
            canvas?.withTranslation(
                y = yOffset
            ) {
                drawLine(0f, linesHeight, width.toFloat(), linesHeight, axisPaint)
            }
        }
    }

    private fun drawYAxisTitles(canvas: Canvas?) {
        drawOldYAxisTitle(canvas)
        drawNewYAxisTitle(canvas)
    }

    private fun drawOldYAxisTitle(canvas: Canvas?) {
        for (i in 0 until TITLES_COUNT) {
            var yOffset = i * linesOffset + textPaint.textSize * OFFSET_COEFFICIENT + (i * linesHeight)
            val title = (yOldStep * ((TITLES_COUNT - 1) - i)).toInt().toString()
            textPaint.alpha = yAxisOldAlpha.toInt()
            if (i < TITLES_COUNT - 1) yOffset += yAxisOldAnimatedOffset
            else axisPaint.alpha = OPAQUE.toInt()
            canvas?.withTranslation(
                y = yOffset
            ) {
                drawText(title, 0f, 0f - linesHeight * 2f, textPaint)
            }
        }
    }

    private fun drawNewYAxisTitle(canvas: Canvas?) {
        for (i in 0 until TITLES_COUNT) {
            var yOffset = i * linesOffset + textPaint.textSize * OFFSET_COEFFICIENT + (i * linesHeight)
            val title = (yNewStep * ((TITLES_COUNT - 1) - i)).toInt().toString()
            val m = if (yAxisNewAnimatedOffset < 0) -1 else 1
            textPaint.alpha = yAxisNewAlpha.toInt()
            if (i < TITLES_COUNT - 1) yOffset = yOffset - (linesOffset * m) + yAxisNewAnimatedOffset
            else textPaint.alpha = OPAQUE.toInt()
            canvas?.withTranslation(
                y = yOffset
            ) {
                drawText(title, 0f, 0f - linesHeight * 2f, textPaint)
            }
        }
    }

    private fun calculateXAxisCoordinates() {
        xAxisCoordinates.clear()
        val step = width / (daysInFrame - 0f)
        for ((j, i) in (startIndex..endIndex).withIndex()) {
            val title = dayFormat.format(xAxis[i])
            val textWidth = textPaint.measureText(title)
            val textStartX = (step - textWidth) / 2f
            val s = if (daysBeforeFrame == 0) j else j - 1
            val coordinate = textStartX + textWidth / 2f + s * step
            if (i == startIndex) {
                firstXCoordinate = coordinate
                firstXValue = xAxis[i]
            }
            if (i == endIndex) {
                lastXCoordinate = coordinate
                lastXValue = xAxis[i]
            }
            xAxisCoordinates[xAxis[i]] = coordinate
        }
    }

    private fun drawXAxisTitles(canvas: Canvas?) {
        val textStep = width / (TITLES_COUNT - 0f)

        val yOffset = height - (textPaint.textSize * (OFFSET_COEFFICIENT / 2f))

        val step = xAxisCoordinates.size / TITLES_COUNT

        var start =
            if (daysBeforeFrame == 0) startIndex else if (daysAfterFrame == 0) startIndex + 1 else startIndex + 1
        if (start < 0) start = 0

        var end = if (daysAfterFrame == 0) endIndex else if (daysBeforeFrame == 0) endIndex - 1 else endIndex
        if (end > endIndex) end = endIndex

        if(dx != 0f) {
            if (end < step) end += step
            if (start > step) start -= step
        }

        for (i in -1..(TITLES_COUNT + 1)) {
            var index = start + i * step
            if (index < 0) index = start
            if (index > end) index = end

            var title = dayFormat.format(xAxis[index])
            val xOffset = i * textStep
            var textWidth = textPaint.measureText(title)
            var value = xAxisCoordinates.entries.find { it.value >= (xOffset + textWidth / 2f) }?.key
            if (value == null) value = lastXValue

            title = dayFormat.format(value)

//            if (j == TITLES_COUNT - 1 && daysAfterFrame == 0) title = dayFormat.format(xAxis[end])
//            if (j == 0 && daysBeforeFrame == 0) title = dayFormat.format(xAxis[start])

            textWidth = textPaint.measureText(title)
            val textStartX = (textStep - textWidth) / 2f

            textPaint.alpha = OPAQUE.toInt()

            var canvasDx = xOffset
            if (daysBeforeFrame != 0 && daysAfterFrame != 0) canvasDx = xOffset - (dx % textStep)

            canvas?.withTranslation(
                x = canvasDx,
                y = yOffset
            ) {
                drawText(title, textStartX, linesHeight * 2f, textPaint)
            }
        }
    }

    private fun calculateHorizontalStep() {
        var max = 0
        yAxis.forEach {
            if (it.enabled || it.animation == Chart.ChartAnimation.DOWN) {
                for (i in startIndex..endIndex) {
                    val newMax = it.values[i]
                    if (newMax > max) max = newMax.toInt()
                }
            }
        }

        // for drawing some chart values under the top horizontal (y) axis
        while (max % 5 != 0) {
            if (max > 10) max--
            else max++
        }
        
        val tmp = max / (TITLES_COUNT - 1f)
        if (tmp != yNewStep || yOldStep == 0f) {
            if (!animatorSet.isRunning) yOldStep = yNewStep
            yNewStep = tmp
            if (yOldStep == 0f) yOldStep = yNewStep
            if (yNewStep > 0) yMultiplier = linesOffset / yNewStep
            if (yCurrentMultiplier == 0f) yCurrentMultiplier = yMultiplier
        }

        if (yMaxValue != 0 && isChartDrawing) {
            if (yMaxValue > max) startYAxisAnimation(true)
            else if (yMaxValue < max) startYAxisAnimation(false)
        }
        if (max != 0) yMaxValue = max
    }

    private fun startYAxisAnimation(animateUp: Boolean) {
        yMultiplierAnimator?.cancel()
        yMultiplierAnimator = ValueAnimator.ofFloat(yCurrentMultiplier, yMultiplier).apply {
            addUpdateListener {
                yCurrentMultiplier = it.animatedValue as Float
                invalidate()
            }
            duration = ANIMATION_DURATION
        }

        yMultiplierAnimator?.start()

        if (animatorSet.isRunning) return

        val from = 0f
        val to = if (animateUp) -1f else 1f

        val oldAnimator = ValueAnimator.ofFloat(from, to).apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                yAxisOldAnimatedOffset = lerp(0f, linesOffset, value)
                yAxisOldAlpha = lerp(
                    OPAQUE,
                    TRANSPARENT,
                    Math.abs(value)
                )
            }
            interpolator = FastOutSlowInInterpolator()
            duration = ANIMATION_DURATION / 2
        }

        val newAnimator = ValueAnimator.ofFloat(from, to).apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                yAxisNewAnimatedOffset = lerp(0f, linesOffset, value)
            }
            interpolator = FastOutSlowInInterpolator()
            duration = ANIMATION_DURATION
            startDelay = ANIMATION_DELAY
        }

        val newAlphaAnimator = ValueAnimator.ofFloat(from, to).apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                yAxisNewAlpha = lerp(
                    TRANSPARENT,
                    OPAQUE,
                    Math.abs(value)
                )
            }
            doOnEnd {
                yAxisOldAlpha = OPAQUE
                yAxisNewAlpha = TRANSPARENT
                yAxisOldAnimatedOffset = 0f
                yAxisNewAnimatedOffset = 0f
                yOldStep = yNewStep
                invalidate()
            }
            duration = ANIMATION_DURATION
            startDelay = ANIMATION_DELAY
        }

        animatorSet.apply {
            playTogether(oldAnimator, newAnimator, newAlphaAnimator)
            start()
        }
    }

    fun animateInOut(out: Boolean) {
        inOutAnimatorSet.cancel()

        val from = 0f
        val to = if (out) -1f else 1f

        val translateAnimator = ValueAnimator.ofFloat(from, to).apply {
            addUpdateListener {
                val value = it.animatedValue as Float
                if (out) {
                    chartTranslationOffset = lerp(0f, linesOffset * 4, value)
                    chartAlpha = lerp(
                        OPAQUE,
                        TRANSPARENT,
                        Math.abs(value)
                    )
                } else {
                    chartTranslationOffset = lerp(linesOffset * -4, 0f, value)
                    chartAlpha = lerp(
                        TRANSPARENT,
                        OPAQUE,
                        Math.abs(value)
                    )
                }
                invalidate()
            }
            doOnEnd {
                yAxis.forEach {
                    it.animation = Chart.ChartAnimation.NONE
                }
            }
            interpolator = FastOutSlowInInterpolator()
            duration = ANIMATION_DURATION
        }

        inOutAnimatorSet.apply {
            playTogether(translateAnimator)
            start()
        }
    }

    private fun drawChart(canvas: Canvas?, column: Chart.Column) {
        chartPaint.color = Color.parseColor(column.color)
        chartPath.reset()
        for (i in startIndex..endIndex) {
            val x = xAxisCoordinates[xAxis[i]]!!
            var y = getYCoordinate(column.values[i].toFloat())
            chartPaint.alpha = OPAQUE.toInt()
            if (column.animation != Chart.ChartAnimation.NONE) {
                y += chartTranslationOffset
                chartPaint.alpha = chartAlpha.toInt()
            }
            if (i == startIndex) chartPath.moveTo(x, y)
            else chartPath.lineTo(x, y)
        }
        canvas?.drawPath(chartPath, chartPaint)
    }

    private fun drawSelectedValues(canvas: Canvas?) {
        if (valuesAxisXCoordinate == 0f) return
        canvas?.drawLine(
            valuesAxisXCoordinate,
            0f,
            valuesAxisXCoordinate,
            (TITLES_COUNT - 1) * linesOffset + textPaint.textSize * OFFSET_COEFFICIENT + (TITLES_COUNT * linesHeight),
            axisPaint
        )
        drawValues(canvas)
    }

    private fun drawValues(canvas: Canvas?) {
        var value = xAxisCoordinates.entries.find { it.value >= valuesAxisXCoordinate }?.key
        if (value == null) value = lastXValue

        val valuePosition = xAxis.indexOf(value)

        val dataPos = if (valuesAxisXCoordinate == xAxisCoordinates[value]) valuePosition
        else valuePosition - 1
        val data = Chart.ChartSelectedData(xAxis[dataPos], ArrayList())

        var i = 0
        yAxis.forEach {
            if (it.enabled) {
                valuePaint.color = Color.parseColor(it.color)
                val interpolatedY = if (valuePosition == 0) {
                    it.values[valuePosition].toFloat()
                } else {
                    val x1 = xAxisCoordinates[xAxis[valuePosition - 1]]!!
                    val y1 = it.values[valuePosition - 1].toFloat()
                    val x2 = xAxisCoordinates[xAxis[valuePosition]]!!
                    val y2 = it.values[valuePosition].toFloat()
                    getInterpolatedY(x1, y1, x2, y2, valuesAxisXCoordinate)
                }
                val cy = getYCoordinate(interpolatedY)
                data.values.add(Chart.Data(interpolatedY.toLong(), it.color))
                canvas?.drawCircle(valuesAxisXCoordinate, cy, valuePaint.strokeWidth * 2f, valueFillPaint)
                canvas?.drawCircle(valuesAxisXCoordinate, cy, valuePaint.strokeWidth * 2f, valuePaint)
                i++
            }
        }

        listener?.onDataSelected(data)
    }

    private fun getInterpolatedY(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float): Float {
        return ((y2 - y1) / (x2 - x1)) * (x3 - x1) + y1
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action and MotionEvent.ACTION_MASK
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                valuesAxisXCoordinate = event.x
                if (valuesAxisXCoordinate > lastXCoordinate) {
                    valuesAxisXCoordinate = lastXCoordinate
                }
                if (valuesAxisXCoordinate < firstXCoordinate) {
                    valuesAxisXCoordinate = firstXCoordinate
                }
                invalidate()
            }
        }
        return true
    }

    override fun onRangeChanged(daysBeforeFrame: Int, daysAfterFrame: Int, daysInFrame: Int, dx: Float) {
        this.daysBeforeFrame = daysBeforeFrame
        this.daysAfterFrame = daysAfterFrame
        this.daysInFrame = daysInFrame
        startIndex = if (daysBeforeFrame == 0) 0 else daysBeforeFrame - 1
        endIndex = if (daysAfterFrame == 0) daysBeforeFrame + daysInFrame - 1 else daysBeforeFrame + daysInFrame
        valuesAxisXCoordinate = 0f
        listener?.onDataCleared()
        this.dx = dx
        invalidate()
    }

    private fun getYCoordinate(value: Float): Float {
        return height - (textPaint.textSize * OFFSET_COEFFICIENT) - linesHeight * (TITLES_COUNT - 1) - value * yCurrentMultiplier
    }

    fun setChartViewListener(listener: ChartViewListener) {
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
