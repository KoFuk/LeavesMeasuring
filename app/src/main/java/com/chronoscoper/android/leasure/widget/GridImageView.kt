package com.chronoscoper.android.leasure.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.preference.PreferenceManager
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet

class GridImageView(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
    : AppCompatImageView(context, attrs, defStyleAttr) {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            .apply {
                strokeWidth = 3f
                color = Color.BLACK
            }

    val preference by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    var gridScale = -1
        set(value) {
            preference.edit().putInt("grid_scale", value).apply()
            field = value
            invalidate()
        }
        get() {
            if (field < 0) {
                field = preference.getInt("grid_scale", 100)
            }
            return field
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        var gridPosition = 0f
        while (gridPosition <= width) {
            canvas?.drawLine(gridPosition, 0f, gridPosition, height.toFloat(), paint)
            gridPosition += gridScale
        }
        gridPosition = 0f
        while (gridPosition <= height) {
            canvas?.drawLine(0f, gridPosition, width.toFloat(), gridPosition, paint)
            gridPosition += gridScale
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }
}
