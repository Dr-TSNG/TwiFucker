package icu.nullptr.twifucker.hook

import android.app.Activity
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import icu.nullptr.twifucker.afterMeasure
import java.lang.ref.WeakReference

object ActivityHook : BaseHook() {
    override val name: String
        get() = "ActivityHook"

    override fun init() {
        MethodFinder.fromClass(Activity::class.java).filterByName("onResume").first().createHook {
            afterMeasure(name) { param ->
                HookEntry.currentActivity = WeakReference(param.thisObject as Activity)
            }
        }
    }
}