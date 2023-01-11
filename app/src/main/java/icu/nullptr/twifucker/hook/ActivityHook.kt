package icu.nullptr.twifucker.hook

import android.app.Activity
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import java.lang.ref.WeakReference

object ActivityHook : BaseHook() {
    override val name: String
        get() = "ActivityHook"

    override fun init() {
        findMethod(Activity::class.java) {
            name == "onResume"
        }.hookAfter { param ->
            HookEntry.currentActivity = WeakReference(param.thisObject as Activity)
        }
    }
}