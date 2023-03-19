package icu.nullptr.twifucker.ui

import android.content.Context
import android.text.InputType
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Switch
import icu.nullptr.twifucker.R

class KeyValueView(context: Context) : CustomLayout(context) {
    var isBoolean: Boolean = true

    val typeSwitch = Switch(context).apply {
        setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium)
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            marginStart = 32.dp
            marginEnd = 32.dp
            setPadding(8.dp, 0, 0, 0)
        }
        text = context.getString(R.string.feature_switch_value_boolean)
        setOnClickListener {
            isBoolean = !isBoolean
            if (isBoolean) {
                text = context.getString(R.string.feature_switch_value_boolean)
                inputDecimal.visibility = GONE
                switch.visibility = VISIBLE
            } else {
                text = context.getString(R.string.feature_switch_value_decimal)
                inputDecimal.visibility = VISIBLE
                switch.visibility = GONE
            }
        }
        addView(this)
    }

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
            height = 64.dp
            marginStart = 32.dp
            marginEnd = 32.dp
            setPadding(8.dp, 0, 0, 0)
        }
        text = context.getString(R.string.feature_switch_bool_label)
        addView(this)
    }

    val inputDecimal = EditText(context).apply {
        setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium)
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            height = 64.dp
            marginStart = 32.dp
            marginEnd = 32.dp
            setPadding(8.dp, 0, 0, 0)
        }
        hint = context.getString(R.string.feature_switch_value_decimal_hint)
        inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
        visibility = GONE
        addView(this)
    }

    fun focus() {
        editText.requestFocus()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
            editText, InputMethodManager.SHOW_IMPLICIT
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val maxWidth = measuredWidth - editText.marginStart - editText.marginEnd
        typeSwitch.measure(
            (maxWidth - typeSwitch.paddingLeft).toExactlyMeasureSpec(),
            typeSwitch.defaultHeightMeasureSpec(this)
        )
        editText.measure(
            maxWidth.toExactlyMeasureSpec(), editText.defaultHeightMeasureSpec(this)
        )
        switch.measure(
            (maxWidth - switch.paddingLeft).toExactlyMeasureSpec(),
            switch.defaultHeightMeasureSpec(this)
        )
        inputDecimal.measure(
            (maxWidth - switch.paddingLeft).toExactlyMeasureSpec(),
            inputDecimal.defaultHeightMeasureSpec(this)
        )
        setMeasuredDimension(
            maxWidth.coerceAtLeast(measuredWidth),
            typeSwitch.measuredHeightWithMargins + editText.measuredHeightWithMargins + switch.measuredHeightWithMargins
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (!isRTL) {
            typeSwitch.let {
                it.layout(x = it.marginStart, y = 0)
            }
            editText.let { it.layout(x = it.marginStart, y = typeSwitch.bottom + it.marginTop) }
            switch.let { it.layout(x = it.marginStart, y = editText.bottom + it.marginTop) }
            inputDecimal.let { it.layout(x = it.marginStart, y = editText.bottom + it.marginTop) }
        } else {
            typeSwitch.let {
                it.layout(x = it.marginEnd, y = 0, fromRight = true)
            }
            editText.let {
                it.layout(
                    x = it.marginEnd, y = typeSwitch.bottom + it.marginTop, fromRight = true
                )
            }
            switch.let {
                it.layout(
                    x = it.marginEnd, y = editText.bottom + it.marginTop, fromRight = true
                )
            }
            inputDecimal.let {
                it.layout(
                    x = it.marginEnd, y = editText.bottom + it.marginTop, fromRight = true
                )
            }
        }
    }
}