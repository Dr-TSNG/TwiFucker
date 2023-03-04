package icu.nullptr.twifucker.ui

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import icu.nullptr.twifucker.R

class FeatureSwitchView(context: Context) : CustomLayout(context) {

    private val selectableItemBackground = TypedValue().also {
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
    }

    private val titleView = TextView(context).apply {
        setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Large)
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            topMargin = 32.dp
            marginStart = 32.dp
        }
        text = context.getString(R.string.feature_switch)
        addView(this)
    }

    private var isRecyclerViewAdded = false
    private val recyclerView = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context)
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            marginStart = 32.dp
            marginEnd = 32.dp
        }
        // TODO check if this is necessary
        // addView(this)
    }

    private val buttonReset = Button(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        foreground = context.getDrawable(selectableItemBackground.resourceId)
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            marginStart = 32.dp
        }
        text = context.getString(R.string.reset)
        addView(this)
    }

    private val buttonAdd = Button(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        foreground = context.getDrawable(selectableItemBackground.resourceId)
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            marginEnd = 32.dp
        }
        text = context.getString(R.string.add)
        addView(this)
    }

    fun setAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter
    }

    fun setOnResetClickListener(listener: OnClickListener) {
        buttonReset.setOnClickListener(listener)
    }

    fun setOnAddClickListener(listener: OnClickListener) {
        buttonAdd.setOnClickListener(listener)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        titleView.autoMeasure()
        recyclerView.autoMeasure()
        buttonReset.autoMeasure()
        buttonAdd.autoMeasure()
        val maxHeight =
            titleView.measuredHeightWithMargins + recyclerView.measuredHeightWithMargins + buttonReset.measuredHeightWithMargins
        setMeasuredDimension(recyclerView.measuredWidthWithMargins, maxHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        titleView.let {
            it.layout(it.marginLeft, it.marginTop)
        }
        recyclerView.let {
            it.layout(it.marginLeft, titleView.bottom)
            // TODO check if this is necessary
            if (!isRecyclerViewAdded) {
                addView(it)
                isRecyclerViewAdded = true
            }
        }
        buttonReset.let {
            it.layout(it.marginLeft, recyclerView.bottom)
        }
        buttonAdd.let {
            it.layout(it.marginRight, recyclerView.bottom, fromRight = true)
        }
    }
}
