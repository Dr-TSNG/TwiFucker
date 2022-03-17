package icu.nullptr.twifucker

import android.app.Application
import android.content.Context
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage

private const val TAG = "TwiFucker"

class HookEntry : IXposedHookZygoteInit, IXposedHookLoadPackage {

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
            if (BuildConfig.DEBUG) Log.toast("TwiFucker version ${BuildConfig.VERSION_NAME}")

            jsonHook()
            urlHook()
        }
    }
}
