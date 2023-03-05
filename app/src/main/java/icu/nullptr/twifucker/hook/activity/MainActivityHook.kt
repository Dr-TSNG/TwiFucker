package icu.nullptr.twifucker.hook.activity

import android.app.Activity
import com.github.kyuubiran.ezxhelper.AndroidLogger
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.XC_MethodHook.Unhook
import icu.nullptr.twifucker.BuildConfig
import icu.nullptr.twifucker.afterMeasure
import icu.nullptr.twifucker.hook.BaseHook
import icu.nullptr.twifucker.modulePrefs
import icu.nullptr.twifucker.ui.SettingsDialog

object MainActivityHook : BaseHook() {
    override val name: String
        get() = "MainActivityHook"

    private lateinit var theHook: Unhook

    override fun init() {
        theHook = MethodFinder.fromClass(loadClass("com.twitter.app.main.MainActivity"))
            .findSuper { false }.filterByName("onResume").first().createHook {
                afterMeasure(name) { param ->
                    Log.d("MainActivity onResume")
                    if (BuildConfig.DEBUG || modulePrefs.getBoolean("first_run", true)) {
                        SettingsDialog(param.thisObject as Activity)
                        modulePrefs.putBoolean("first_run", false)
                    }
                    if (modulePrefs.getBoolean("show_toast", true)) {
                        AndroidLogger.toast("TwiFucker version ${BuildConfig.VERSION_NAME}")
                    }
                    theHook.unhook()
                }
            }
    }
}
