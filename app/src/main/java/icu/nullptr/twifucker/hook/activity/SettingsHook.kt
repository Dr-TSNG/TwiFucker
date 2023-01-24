package icu.nullptr.twifucker.hook.activity

import android.app.Activity
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.FieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import icu.nullptr.twifucker.exceptions.CachedHookNotFound
import icu.nullptr.twifucker.hook.BaseHook
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexKit
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexKit
import icu.nullptr.twifucker.hostAppLastUpdate
import icu.nullptr.twifucker.moduleLastModify
import icu.nullptr.twifucker.modulePrefs
import icu.nullptr.twifucker.ui.SettingsDialog
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor

object SettingsHook : BaseHook() {
    override val name: String
        get() = "SettingsHook"

    private val aboutActivityClass = loadClass("com.twitter.app.settings.AboutActivity")
    private val preferenceClass = loadClass("android.preference.Preference")

    private lateinit var onVersionClickListenerClassName: String
    private lateinit var onVersionClickMethodName: String

    override fun init() {
        val onVersionClickMethod =
            MethodFinder.fromClass(aboutActivityClass).filterByParamTypes(preferenceClass)
                .firstOrNull()

        if (onVersionClickMethod != null) {
            onVersionClickMethod.createHook {
                replace { param ->
                    SettingsDialog(param.thisObject as Activity)
                    return@replace true
                }
            }
        } else {
            try {
                loadHookInfo()
            } catch (t: Throwable) {
                Log.e(t)
                return
            }
            val onVersionClickListenerClass = loadClass(onVersionClickListenerClassName)
            val activityField =
                FieldFinder.fromClass(onVersionClickListenerClass).filterByType(aboutActivityClass)
                    .first()
            MethodFinder.fromClass(onVersionClickListenerClass).filterByParamTypes(preferenceClass)
                .first().createHook {
                    replace { param ->
                        SettingsDialog(activityField.get(param.thisObject) as Activity)
                        return@replace true
                    }
                }
        }
    }

    private fun loadCachedHookInfo() {
        onVersionClickListenerClassName =
            modulePrefs.getString("hook_on_version_click_listener_class", null)
                ?: throw CachedHookNotFound()
    }

    private fun saveHookInfo() {
        modulePrefs.edit().putString(
            "hook_on_version_click_listener_class", onVersionClickListenerClassName
        ).apply()
    }

    private fun searchHook() {
        val onCreateMethod =
            MethodFinder.fromClass(aboutActivityClass).filterByName("onCreate").first()

        val onPreferenceClickListenerClass = dexKit.findMethodInvoking {
            methodDescriptor = DexMethodDescriptor(onCreateMethod).descriptor
            beInvokedMethodName = "<init>"
            beInvokedMethodReturnType = Void.TYPE.name
            beInvokedMethodParameterTypes = arrayOf(aboutActivityClass.name)
        }.firstNotNullOfOrNull {
            it.value
        }?.firstOrNull()?.getMemberInstance(EzXHelper.classLoader)?.declaringClass
            ?: throw ClassNotFoundException()
        val onVersionClickMethod = MethodFinder.fromClass(onPreferenceClickListenerClass)
            .filterByParamTypes(preferenceClass).first()

        onVersionClickListenerClassName = onPreferenceClickListenerClass.name
        onVersionClickMethodName = onVersionClickMethod.name
    }

    private fun loadHookInfo() {
        val hookSettingsLastUpdate = modulePrefs.getLong("hook_settings_last_update", 0)

        Log.d("hookSettingsLastUpdate: $hookSettingsLastUpdate, hostAppLastUpdate: $hostAppLastUpdate, moduleLastModify: $moduleLastModify")

        val timeStart = System.currentTimeMillis()

        if (hookSettingsLastUpdate > hostAppLastUpdate && hookSettingsLastUpdate > moduleLastModify) {
            loadCachedHookInfo()
            Log.d("Settings Hook load time: ${System.currentTimeMillis() - timeStart} ms")
        } else {
            loadDexKit()
            searchHook()
            Log.d("Settings Hook search time: ${System.currentTimeMillis() - timeStart} ms")
            saveHookInfo()
            modulePrefs.edit().putLong("hook_settings_last_update", System.currentTimeMillis())
                .apply()
        }
    }
}
