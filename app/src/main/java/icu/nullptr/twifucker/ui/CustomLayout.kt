package icu.nullptr.twifucker.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

// Thanks Drakeet Xu

abstract class CustomLayout(context: Context) : ViewGroup(context) {
    protected fun View.autoMeasure() {
        measure(
            defaultWidthMeasureSpec(this@CustomLayout), defaultHeightMeasureSpec(this@CustomLayout)
        )
    }

    protected fun View.layout(x: Int, y: Int, fromRight: Boolean = false) {
        if (!fromRight) {
            layout(x, y, x + measuredWidth, y + measuredHeight)
        } else {
            layout(this@CustomLayout.measuredWidth - x - measuredWidth, y)
        }
    }

    protected val View.measuredWidthWithMargins get() = (measuredWidth + marginLeft + marginRight)

    protected val View.measuredHeightWithMargins get() = (measuredHeight + marginTop + marginBottom)

    protected fun View.defaultWidthMeasureSpec(parentView: ViewGroup): Int {
        return when (layoutParams.width) {
            MATCH_PARENT -> parentView.measuredWidth.toExactlyMeasureSpec()
            WRAP_CONTENT -> WRAP_CONTENT.toAtMostMeasureSpec()
            0 -> throw IllegalArgumentException("Need special treatment for $this")
            else -> layoutParams.width.toExactlyMeasureSpec()
        }
    }

    protected fun View.defaultHeightMeasureSpec(parentView: ViewGroup): Int {
        return when (layoutParams.height) {
            MATCH_PARENT -> parentView.measuredHeight.toExactlyMeasureSpec()
            WRAP_CONTENT -> WRAP_CONTENT.toAtMostMeasureSpec()
            0 -> throw IllegalArgumentException("Need special treatment for $this")
            else -> layoutParams.height.toExactlyMeasureSpec()
        }
    }

    protected fun Int.toExactlyMeasureSpec(): Int =
        MeasureSpec.makeMeasureSpec(this, MeasureSpec.EXACTLY)

    protected fun Int.toAtMostMeasureSpec(): Int =
        MeasureSpec.makeMeasureSpec(this, MeasureSpec.AT_MOST)

    protected val Int.dp: Int get() = (this * resources.displayMetrics.density * 0.5f).toInt()

    protected class LayoutParams(width: Int, height: Int) : MarginLayoutParams(width, height)

    // Taken from
    // https://android.googlesource.com/platform/frameworks/support/+/android-room-release/core/ktx/src/main/java/androidx/core/view/View.kt

    inline val View.marginLeft: Int
        get() = (layoutParams as? MarginLayoutParams)?.leftMargin ?: 0
    inline val View.marginTop: Int
        get() = (layoutParams as? MarginLayoutParams)?.topMargin ?: 0
    inline val View.marginRight: Int
        get() = (layoutParams as? MarginLayoutParams)?.rightMargin ?: 0
    inline val View.marginBottom: Int
        get() = (layoutParams as? MarginLayoutParams)?.bottomMargin ?: 0
}