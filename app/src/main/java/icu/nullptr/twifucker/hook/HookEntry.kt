package icu.nullptr.twifucker.hook

import android.app.Activity
import android.app.Application
import android.content.Context
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.Log.logexIfThrow
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import icu.nullptr.twifucker.hook.activity.MainActivityHook
import icu.nullptr.twifucker.hook.activity.SettingsHook
import icu.nullptr.twifucker.logFile
import icu.nullptr.twifucker.logFileDir
import icu.nullptr.twifucker.modulePrefs
import io.luckypray.dexkit.DexKitBridge
import java.lang.ref.WeakReference

private const val TAG = "TwiFucker"

class HookEntry : IXposedHookZygoteInit, IXposedHookLoadPackage {

    companion object {
        lateinit var dexKit: DexKitBridge
        lateinit var currentActivity: WeakReference<Activity>
        lateinit var logcatProcess: Process

        fun loadDexKit() {
            if (this::dexKit.isInitialized) return
            val ts = System.currentTimeMillis()
            System.loadLibrary("dexkit")
            DexKitBridge.create(appContext.applicationInfo.sourceDir)?.let {
                dexKit = it
                Log.i("DexKit loaded in ${System.currentTimeMillis() - ts} ms")
            }
        }

        fun closeDexKit() {
            if (this::dexKit.isInitialized) dexKit.close()
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
        EzXHelper.initZygote(startupParam)
        EzXHelper.setLogTag(TAG)
        EzXHelper.setToastTag(TAG)
        Log.d("InitZygote")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.twitter.android") return
        EzXHelper.initHandleLoadPackage(lpparam)
        Log.d("HandleLoadedPackage")

        MethodFinder.fromClass(Application::class.java).filterByName("attach")
            .filterByParamTypes(Context::class.java).first().createHook {
                before { param ->
                    EzXHelper.initAppContext(param.args[0] as Context)

                    if (!lpparam.processName.contains(":")) {
                        startLog()
                    }

                    Log.d("AttachContext")

                    val hooks = arrayListOf(
                        MainActivityHook,
                        SettingsHook,
                        UrlHook,
                        SelectableTextHook,
                        DownloadHook,
                        ActivityHook,
                        CustomTabsHook,
                        DrawerNavbarHook,
                        FeatureSwitchHook,
                    )

                    if (modulePrefs.getBoolean("use_legacy_hook", false)) {
                        hooks.add(JsonHook)
                    } else {
                        hooks.addAll(
                            listOf(
                                JsonTimelineEntryHook,
                                JsonTimelineTweetHook,
                                JsonTimelineUserHook,
                                JsonTimelineTrendHook,
                                SensitiveMediaWarningHook,
                                JsonProfileRecommendationModuleResponseHook,
                                JsonFleetsTimelineResponseHook,
                                JsonTimelineModuleHook,
                            )
                        )
                    }
                    initHooks(hooks)
                    closeDexKit()
                }
            }
    }

    private fun initHooks(hook: List<BaseHook>) {
        hook.forEach {
            kotlin.runCatching {
                if (it.isInit) return@forEach
                val ts = System.currentTimeMillis()
                it.init()
                it.isInit = true
                Log.i("Inited ${it.name} hook in ${System.currentTimeMillis() - ts} ms")
            }.logexIfThrow("Failed init hook: ${it.name}")
        }
    }
}
