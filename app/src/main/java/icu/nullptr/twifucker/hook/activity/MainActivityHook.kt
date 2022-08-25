package icu.nullptr.twifucker.hook.activity

import android.app.Activity
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import de.robv.android.xposed.XC_MethodHook.Unhook
import icu.nullptr.twifucker.BuildConfig
import icu.nullptr.twifucker.hook.BaseHook
import icu.nullptr.twifucker.hook.modulePrefs
import icu.nullptr.twifucker.ui.SettingsDialog

object MainActivityHook : BaseHook() {
    private lateinit var theHook: Unhook
    override fun init() {
        theHook = findMethod("com.twitter.app.main.MainActivity", findSuper = true) {
            name == "onResume"
        }.hookAfter { param ->
            Log.d("MainActivity onResume")
            if (BuildConfig.DEBUG || modulePrefs.getBoolean("first_run", true)) {
                SettingsDialog(param.thisObject as Activity)
                modulePrefs.edit().putBoolean("first_run", false).apply()
            }
            if (modulePrefs.getBoolean("show_toast", true)) {
                Log.toast("TwiFucker version ${BuildConfig.VERSION_NAME}")
            }
            theHook.unhook()
        }
    }
}
