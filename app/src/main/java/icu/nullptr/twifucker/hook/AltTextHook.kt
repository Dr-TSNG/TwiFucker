package icu.nullptr.twifucker.hook

import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import icu.nullptr.twifucker.getId

object AltTextHook : BaseHook() {
    override fun init() {
        findMethod(TextView::class.java) {
            name == "setText" && parameterTypes.contentEquals(arrayOf(CharSequence::class.java))
        }.hookAfter { param ->
            val textView = param.thisObject as TextView
            if (textView.id == getId(
                    "text", "id"
                )
            ) {
                textView.setTextIsSelectable(true)
            }
        }
    }

}
