package icu.nullptr.twifucker.hook

import android.widget.TextView
import com.github.kyuubiran.ezxhelper.init.InitFields
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter

fun altTextHook() {
    findMethod(TextView::class.java) {
        name == "setText" && parameterTypes.contentEquals(arrayOf(CharSequence::class.java))
    }.hookAfter { param ->
        val textView = param.thisObject as TextView
        if (textView.id == InitFields.appContext.resources.getIdentifier("text", "id", "com.twitter.android")) {
            textView.setTextIsSelectable(true)
        }
    }
}
