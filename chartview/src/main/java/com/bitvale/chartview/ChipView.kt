package com.bitvale.chartview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.graphics.withTranslation

/**
 * Created by Alexander Kolpakov (jquickapp@gmail.com) on 11-Mar-19
 */
class ChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint: TextPaint

    private val outlinePaint: Paint
    private val outlineRect = RectF()

    private val padding: Int
    private var rounding: Float = 0f

    private val backgroundPaint: Paint

    private val clippingPaint: Paint
    private var outlineColor: Int = 0

    private lateinit var textLayout: StaticLayout
    private var text: CharSequence = ""

    private var progress = 1f
        set(value) {
            if (field != value) {
                field = value
                postInvalidateOnAnimation()
            }
        }

    var isChecked = true
        set(value) {
            field = value
            progress = if (value) 1f else 0f
        }

    private var progressAnimator: ValueAnimator? = null

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.ChipView,
            R.attr.chipViewStyle,
            R.style.ChipView
        )

        outlineColor = a.getColorOrThrow(R.styleable.ChipView_strokeColor)

        outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = a.getDimensionOrThrow(R.styleable.ChipView_outlineWidth)
            style = Paint.Style.STROKE
        }

        textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = a.getColorOrThrow(R.styleable.ChipView_android_textColor)
            textSize = a.getDimensionOrThrow(R.styleable.ChipView_android_textSize)

        }

        clippingPaint = Paint().apply {
            color = a.getColorOrThrow(R.styleable.ChipView_checked_background)
        }

        backgroundPaint = Paint().apply {
            color = a.getColor(R.styleable.ChipView_default_background, 0)
        }

        padding = a.getDimensionPixelSizeOrThrow(R.styleable.ChipView_android_padding)

        val isChecked = a.getBoolean(R.styleable.ChipView_isChecked, false)
        if (isChecked) progress = 1f

        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        createTextLayout(MeasureSpec.getSize(widthMeasureSpec))

        val h = textLayout.height + padding
        val nonTextWidth = padding * 2 + outlinePaint.strokeWidth.toInt() * 2 + h / 2 + padding
        val w = nonTextWidth + textLayout.textWidth()

        setMeasuredDimension(w, h)

        rounding = Math.min(w, h).toFloat() / 2f

        val strokeWidth = outlinePaint.strokeWidth
        val halfStroke = strokeWidth / 2f

        outlineRect.set(
            halfStroke,
            halfStroke,
            w - halfStroke,
            h - halfStroke
        )
    }

    private fun createTextLayout(textWidth: Int) {
        textLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, textWidth).build()
        } else {
            StaticLayout(text, textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, true)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        outlinePaint.color = outlineColor
        canvas?.drawRoundRect(
            outlineRect,
            rounding,
            rounding,
            outlinePaint
        )

        val radius = lerp(0f, (height - padding - outlinePaint.strokeWidth * 2f) / 2f, progress)

        canvas?.drawCircle(height - padding - outlinePaint.strokeWidth * 4f, height / 2f, radius, clippingPaint)

        outlinePaint.color = clippingPaint.color

        canvas?.drawCircle(
            height - padding - outlinePaint.strokeWidth * 4f,
            height / 2f,
            (height - padding - outlinePaint.strokeWidth * 2f) / 2f,
            outlinePaint
        )

        canvas?.withTranslation(
            x = outlinePaint.strokeWidth + padding + height / 2 + padding,
            y = (height - textLayout.height) / 2f - padding / 6
        ) {
            textLayout.draw(canvas)
        }
    }

    fun animateChecked() {
        val newProgress = if (!isChecked) 1f else 0f
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(progress, newProgress).apply {
            addUpdateListener {
                progress = it.animatedValue as Float
            }
            doOnEnd {
                progress = newProgress
            }
            duration = if (!isChecked) SELECTING_DURATION else DESELECTING_DURATION
            start()
        }
        isChecked = !isChecked
    }

    fun setText(text: String) {
        this.text = text
//        requestLayout()
    }

    fun setCheckedColor(color: Int) {
        clippingPaint.color = color
    }

    companion object {
        private const val SELECTING_DURATION = 200L
        private const val DESELECTING_DURATION = 150L
    }

}