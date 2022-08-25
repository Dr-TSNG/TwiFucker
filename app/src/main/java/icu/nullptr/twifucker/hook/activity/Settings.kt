package icu.nullptr.twifucker.hook.activity

import android.app.Activity
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import com.github.kyuubiran.ezxhelper.utils.loadClass
import icu.nullptr.twifucker.ui.SettingsDialog

fun settingsActivityHook() {
    try {
        findMethod("com.twitter.app.settings.AboutActivity") {
            parameterTypes.size == 1 && parameterTypes[0] == loadClass("android.preference.Preference")
        }.hookReplace { param ->
            SettingsDialog(param.thisObject as Activity)
            return@hookReplace true
        }
    } catch (t: Throwable) {
        Log.e(t)
    }
}
