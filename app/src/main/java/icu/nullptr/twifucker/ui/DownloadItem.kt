package icu.nullptr.twifucker.ui

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageButton
import android.widget.TextView
import icu.nullptr.twifucker.R

class DownloadItem(context: Context) : CustomLayout(context) {

    private val selectableItemBackground = TypedValue().also {
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
    }

    private val itemText = TextView(context).apply {
        setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Medium)
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).also {
            it.marginStart = 24.dp
            setPadding(18.dp, 0, 18.dp, 0)
        }
        addView(this)
    }

    private val btnCopy = ImageButton(context).apply {
        setImageResource(R.drawable.baseline_copy_24)
        setBackgroundColor(Color.TRANSPARENT)
        foreground = context.getDrawable(selectableItemBackground.resourceId)
        layoutParams = LayoutParams(96.dp, 96.dp).also {
            it.marginStart = 8.dp
        }
        addView(this)
    }

    private val btnDownload = ImageButton(context).apply {
        setImageResource(R.drawable.baseline_download_24)
        setBackgroundColor(Color.TRANSPARENT)
        foreground = context.getDrawable(selectableItemBackground.resourceId)
        layoutParams = LayoutParams(96.dp, 96.dp).also {
            it.marginStart = 8.dp
            it.marginEnd = 24.dp
        }
        addView(this)
    }

    fun setTitle(title: String) {
        itemText.text = title
    }

    fun setOnCopy(onCopy: () -> Unit) {
        btnCopy.setOnClickListener { onCopy() }
    }

    fun setOnDownload(onDownload: () -> Unit) {
        btnDownload.setOnClickListener { onDownload() }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        btnCopy.autoMeasure()
        btnDownload.autoMeasure()

        val itemTextWidth =
            measuredWidth - itemText.marginLeft - itemText.paddingLeft - itemText.paddingRight - itemText.marginRight - btnCopy.measuredWidthWithMargins - btnDownload.measuredWidthWithMargins
        itemText.measure(
            itemTextWidth.toExactlyMeasureSpec(), itemText.defaultHeightMeasureSpec(this)
        )

        val maxWidth =
            (itemTextWidth + btnCopy.measuredWidthWithMargins + btnDownload.measuredWidthWithMargins).coerceAtLeast(
                measuredWidth
            )
        val maxHeight =
            (itemText.measuredHeight + itemText.marginTop + itemText.marginBottom).coerceAtLeast(
                btnCopy.measuredHeightWithMargins
            )
        setMeasuredDimension(maxWidth, maxHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        itemText.let {
            it.layout(
                x = it.marginLeft, y = (this.measuredHeight / 2) - (it.measuredHeight / 2)
            )
        }
        btnCopy.let { it.layout(x = itemText.right + it.marginLeft, y = 0) }
        btnDownload.let { it.layout(x = btnCopy.right + it.marginLeft, y = 0) }
    }
}