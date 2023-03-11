package icu.nullptr.twifucker.ui

import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.Switch
import icu.nullptr.twifucker.R

class KeyValueView(context: Context) : CustomLayout(context) {

    val editText = EditText(context).apply {
        setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium)
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            marginStart = 32.dp
            marginEnd = 32.dp
        }
        hint = context.getString(R.string.feature_switch_key_hint)
        addView(this)
    }

    val switch = Switch(context).apply {
        setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium)
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            marginStart = 32.dp
            marginEnd = 32.dp
            setPadding(8.dp, 0, 0, 0)
        }
        text = context.getString(R.string.feature_switch_value_boolean)
        addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val maxWidth = measuredWidth - editText.marginStart - editText.marginEnd
        editText.measure(
            maxWidth.toExactlyMeasureSpec(), editText.defaultHeightMeasureSpec(this)
        )
        switch.measure(
            (maxWidth - switch.paddingLeft).toExactlyMeasureSpec(),
            switch.defaultHeightMeasureSpec(this)
        )
        setMeasuredDimension(
            maxWidth.coerceAtLeast(measuredWidth),
            editText.measuredHeightWithMargins + switch.measuredHeightWithMargins
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (!isRTL) {
            editText.let { it.layout(x = it.marginStart, y = 0) }
            switch.let { it.layout(x = it.marginStart, y = editText.measuredHeightWithMargins) }
        } else {
            editText.let { it.layout(x = it.marginEnd, y = 0, fromRight = true) }
            switch.let {
                it.layout(
                    x = it.marginEnd, y = editText.measuredHeightWithMargins, fromRight = true
                )
            }
        }
    }
}