package icu.nullptr.twifucker.hook.activity

import android.app.Activity
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.init.InitFields.modulePath
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import com.github.kyuubiran.ezxhelper.utils.loadClass
import icu.nullptr.twifucker.hook.BaseHook
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexHelper
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexHelper
import icu.nullptr.twifucker.modulePrefs
import icu.nullptr.twifucker.ui.SettingsDialog
import java.io.File

object SettingsHook : BaseHook() {

    private val aboutActivityClass = loadClass("com.twitter.app.settings.AboutActivity")
    private val preferenceClass = loadClass("android.preference.Preference")

    private lateinit var onVersionClickListenerClassName: String
    private lateinit var onVersionClickMethodName: String

    override fun init() {
        val onVersionClickMethod = aboutActivityClass.declaredMethods.firstOrNull {
            it.parameterTypes.size == 1 && it.parameterTypes[0] == preferenceClass
        }
        if (onVersionClickMethod != null) {
            onVersionClickMethod.hookReplace { param ->
                SettingsDialog(param.thisObject as Activity)
                return@hookReplace true
            }
        } else {
            try {
                loadHookInfo()
            } catch (t: Throwable) {
                Log.e(t)
                return
            }
            val onVersionClickListenerClass = loadClass(onVersionClickListenerClassName)
            val activityField = onVersionClickListenerClass.declaredFields.firstOrNull {
                it.type == aboutActivityClass
            } ?: throw NoSuchFieldError()
            onVersionClickListenerClass.declaredMethods.first {
                it.parameterTypes.size == 1 && it.parameterTypes[0] == preferenceClass
            }.hookReplace { param ->
                SettingsDialog(activityField.get(param.thisObject) as Activity)
                return@hookReplace true
            }
        }
    }

    private fun loadCachedHookInfo() {
        onVersionClickListenerClassName =
            modulePrefs.getString("hook_on_version_click_listener_class", null)
                ?: throw Throwable("cached hook not found")
    }

    private fun saveHookInfo() {
        modulePrefs.edit().putString(
            "hook_on_version_click_listener_class", onVersionClickListenerClassName
        ).apply()
    }

    private fun searchHook() {
        val onCreateMethod = aboutActivityClass.declaredMethods.firstOrNull {
            it.name == "onCreate"
        } ?: throw NoSuchMethodError()
        val initMethodIndex = dexHelper.findMethodInvoking(
            dexHelper.encodeMethodIndex(onCreateMethod),
            dexHelper.encodeClassIndex(Void.TYPE),
            1,
            null,
            -1,
            longArrayOf(dexHelper.encodeClassIndex(aboutActivityClass)),
            null,
            null,
            true,
        ).first()
        val onPreferenceClickListenerClass =
            dexHelper.decodeMethodIndex(initMethodIndex)?.declaringClass
                ?: throw ClassNotFoundException()
        val onVersionClickMethod = onPreferenceClickListenerClass.declaredMethods.firstOrNull {
            it.parameterTypes.size == 1 && it.parameterTypes[0] == preferenceClass
        } ?: throw NoSuchMethodError()
        onVersionClickListenerClassName = onPreferenceClickListenerClass.name
        onVersionClickMethodName = onVersionClickMethod.name
    }

    private fun loadHookInfo() {
        val hookSettingsLastUpdate = modulePrefs.getLong("hook_settings_last_update", 0)

        @Suppress("DEPRECATION") val appLastUpdateTime = appContext.packageManager.getPackageInfo(
            appContext.packageName, 0
        ).lastUpdateTime
        val moduleLastUpdate = File(modulePath).lastModified()

        Log.d("hookSettingsLastUpdate: $hookSettingsLastUpdate, appLastUpdateTime: $appLastUpdateTime, moduleLastUpdate: $moduleLastUpdate")

        val timeStart = System.currentTimeMillis()

        if (hookSettingsLastUpdate > appLastUpdateTime && hookSettingsLastUpdate > moduleLastUpdate) {
            loadCachedHookInfo()
            Log.d("Settings Hook load time: ${System.currentTimeMillis() - timeStart} ms")
        } else {
            loadDexHelper()
            searchHook()
            Log.d("Settings Hook search time: ${System.currentTimeMillis() - timeStart} ms")
            saveHookInfo()
            modulePrefs.edit().putLong("hook_settings_last_update", System.currentTimeMillis())
                .apply()
        }
    }
}
