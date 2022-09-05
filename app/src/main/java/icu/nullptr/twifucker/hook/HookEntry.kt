package icu.nullptr.twifucker.hook

import android.app.Activity
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
import icu.nullptr.twifucker.logFile
import icu.nullptr.twifucker.logFileDir
import icu.nullptr.twifucker.modulePrefs
import me.iacn.biliroaming.utils.DexHelper
import java.lang.ref.WeakReference

private const val TAG = "TwiFucker"

class HookEntry : IXposedHookZygoteInit, IXposedHookLoadPackage {

    companion object {
        lateinit var dexHelper: DexHelper
        lateinit var currentActivity: WeakReference<Activity>
        lateinit var logcatProcess: Process

        fun loadDexHelper() {
            if (this::dexHelper.isInitialized) return
            val ts = System.currentTimeMillis()
            dexHelper = DexHelper(appContext.classLoader)
            Log.i("DexHelper load in ${System.currentTimeMillis() - ts} ms")
        }

        fun closeDexHelper() {
            if (this::dexHelper.isInitialized) dexHelper.close()
        }

        fun isLogcatProcessInitialized(): Boolean {
            return this::logcatProcess.isInitialized
        }

        fun startLog() {
            if (!modulePrefs.getBoolean("enable_log", false)) return
            if (!logFileDir.exists()) {
                logFileDir.mkdirs()
            }
            try {
                logcatProcess = Runtime.getRuntime().exec(
                    arrayOf(
                        "logcat", "-T", "100", "-f", logFile.absolutePath
                    )
                )
            } catch (t: Throwable) {
                Log.e(t)
            }
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

            if (!lpparam.processName.contains(":")) {
                startLog()
            }

            Log.d("AttachContext")

            val hooks = arrayListOf(
                MainActivityHook,
                SettingsHook,
                UrlHook,
                AltTextHook,
                DownloadHook,
                ActivityHook,
                CustomTabsHook,
                DrawerNavbarHook,
            )

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
                        SensitiveMediaWarningHook,
                        ProfileRecommendationModuleHook,
                    )
                )
            }
            initHooks(hooks)
            closeDexHelper()
        }
    }

    private fun initHooks(hook: List<BaseHook>) {
        hook.forEach {
            kotlin.runCatching {
                if (it.isInit) return@forEach
                val ts = System.currentTimeMillis()
                it.init()
                it.isInit = true
                Log.i("Inited ${it.javaClass.simpleName} hook in ${System.currentTimeMillis() - ts} ms")
            }.logexIfThrow("Failed init hook: ${it.javaClass.simpleName}")
        }
    }
}
