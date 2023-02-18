package icu.nullptr.twifucker.ui

import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView

class FeatureSwitchItem(context: Context) : CustomLayout(context) {
    val keyTextView = TextView(context).apply {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            marginStart = 46.dp
            marginEnd = 46.dp
        }
        setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium)
        addView(this)
    }

    val valueTextView = TextView(context).apply {
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            marginStart = 46.dp
            marginEnd = 46.dp
        }
        setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Small)
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val maxWidth = measuredWidth - marginLeft - marginRight
        keyTextView.measure(
            maxWidth.toExactlyMeasureSpec(), keyTextView.defaultHeightMeasureSpec(this)
        )
        valueTextView.measure(
            maxWidth.toExactlyMeasureSpec(), valueTextView.defaultHeightMeasureSpec(this)
        )
        setMeasuredDimension(
            measuredWidth,
            keyTextView.measuredHeightWithMargins + valueTextView.measuredHeightWithMargins
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        keyTextView.let { it.layout(x = it.marginLeft, y = 0) }
        valueTextView.let { it.layout(x = it.marginLeft, y = keyTextView.bottom) }
    }
}