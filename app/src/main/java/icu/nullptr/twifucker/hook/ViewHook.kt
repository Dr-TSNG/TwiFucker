package icu.nullptr.twifucker.hook

import android.view.View
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import icu.nullptr.twifucker.getId
import icu.nullptr.twifucker.modulePrefs

object ViewHook : BaseHook() {
    override val name: String
        get() = "ViewHook"

    override fun init() {
        if (!modulePrefs.getBoolean("disable_banner_view", false)) return
        MethodFinder.fromClass(View::class.java).filterByName("setVisibility").first().createHook {
            before { param ->
                val id = (param.thisObject as View).id
                val visibility = param.args[0] as Int
                if (id == getId("banner", "id") && visibility == View.VISIBLE) {
                    Log.d("Prevented banner view")
                    param.args[0] = View.GONE
                }
            }
        }
    }
}