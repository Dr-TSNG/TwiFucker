package icu.nullptr.twifucker.hook

import android.widget.TextView
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import icu.nullptr.twifucker.afterMeasure
import icu.nullptr.twifucker.getId

object SelectableTextHook : BaseHook() {
    override val name: String
        get() = "SelectableTextHook"

    override fun init() {
        val notSelectableText = listOf(
//            "name", // breaks followers and followings text
            "user_name",
            "user_bio",
            "profile_header_location",
            "text",
            "secondary_text"
        ).map {
            getId(it, "id")
        }

        MethodFinder.fromClass(TextView::class.java).filterByName("setText").filterByParamCount(4)
            .first().createHook {
                afterMeasure(name) { param ->
                    val textView = param.thisObject as TextView
                    if (textView.id in notSelectableText && !textView.isTextSelectable) {
                        textView.setTextIsSelectable(true)
                    }
                }
            }
    }
}
