package icu.nullptr.twifucker.hook

import android.app.Application
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.Log.logexIfThrow
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import icu.nullptr.twifucker.hook.activity.MainActivityHook
import icu.nullptr.twifucker.hook.activity.SettingsHook
import me.iacn.biliroaming.utils.DexHelper

private const val TAG = "TwiFucker"

class HookEntry : IXposedHookZygoteInit, IXposedHookLoadPackage {

    companion object {
        lateinit var dexHelper: DexHelper

        fun loadDexHelper() {
            if (this::dexHelper.isInitialized) return
            dexHelper = DexHelper(appContext.classLoader)
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
        EzXHelperInit.setLogTag(TAG)
        EzXHelperInit.setToastTag(TAG)
        Log.d("InitZygote")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.twitter.android") return
        EzXHelperInit.initHandleLoadPackage(lpparam)
        Log.d("HandleLoadedPackage")

        findMethod(Application::class.java) {
            name == "attach" && parameterTypes.contentEquals(arrayOf(Context::class.java))
        }.hookAfter { param ->
            EzXHelperInit.initAppContext(param.args[0] as Context)
            EzXHelperInit.setEzClassLoader(appContext.classLoader)
            Log.d("AttachContext")

            val hooks =
                arrayListOf(MainActivityHook, SettingsHook, UrlHook, AltTextHook, DownloadHook)

            if (modulePrefs.getBoolean("use_legacy_hook", false)) {
                hooks.add(JsonHook)
            } else {
                hooks.addAll(
                    listOf(
                        TimelineEntryHook,
                        TimelineModuleHook,
                        TimelineUserHook,
                        TimelineTrendHook,
                        TimelineTweetHook,
                        SensitiveMediaWarningHook
                    )
                )
            }
            initHooks(hooks)
        }
    }

    private fun initHooks(hook: List<BaseHook>) {
        hook.forEach {
            kotlin.runCatching {
                if (it.isInit) return@forEach
                it.init()
                it.isInit = true
                Log.d("Inited hook: ${it.javaClass.simpleName}")
            }.logexIfThrow("Failed init hook: ${it.javaClass.simpleName}")
        }
    }
}
