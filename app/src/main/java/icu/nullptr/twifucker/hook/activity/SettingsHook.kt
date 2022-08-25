package icu.nullptr.twifucker.hook.activity

import android.app.Activity
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import com.github.kyuubiran.ezxhelper.utils.loadClass
import icu.nullptr.twifucker.hook.BaseHook
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexHelper
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexHelper
import icu.nullptr.twifucker.ui.SettingsDialog

object SettingsHook : BaseHook() {
    override fun init() {
        val aboutActivityClass = loadClass("com.twitter.app.settings.AboutActivity")
        val preferenceClass = loadClass("android.preference.Preference")
        val onVersionClickMethod = aboutActivityClass.declaredMethods.firstOrNull {
            it.parameterTypes.size == 1 && it.parameterTypes[0] == preferenceClass
        }
        if (onVersionClickMethod != null) {
            onVersionClickMethod.hookReplace { param ->
                SettingsDialog(param.thisObject as Activity)
                return@hookReplace true
            }
        } else {
            loadDexHelper()
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
            val activityField = onPreferenceClickListenerClass.declaredFields.firstOrNull {
                it.type == aboutActivityClass
            } ?: throw NoSuchFieldError()
            onPreferenceClickListenerClass.declaredMethods.first {
                it.parameterTypes.size == 1 && it.parameterTypes[0] == preferenceClass
            }.hookReplace { param ->
                SettingsDialog(activityField.get(param.thisObject) as Activity)
                return@hookReplace true
            }
        }
    }
}
