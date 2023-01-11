package icu.nullptr.twifucker.hook

import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import icu.nullptr.twifucker.getId

object SelectableTextHook : BaseHook() {
    override val name: String
        get() = "SelectableTextHook"

    override fun init() {
        val notSelectableText = listOf(
            "name",
            "user_name",
            "user_bio",
            "profile_header_location",
            "text",
            "secondary_text"
        ).map {
            getId(it, "id")
        }
        findMethod(TextView::class.java) {
            name == "setText" && parameterTypes.contentEquals(arrayOf(CharSequence::class.java))
        }.hookAfter { param ->
            val textView = param.thisObject as TextView
            if (textView.id in notSelectableText && !textView.isTextSelectable) {
                textView.setTextIsSelectable(true)
            }
        }
    }
}
