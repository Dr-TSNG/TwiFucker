package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.init.InitFields
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.forEach
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import icu.nullptr.twifucker.exceptions.CachedHookNotFound
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexKit
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexKit
import icu.nullptr.twifucker.hostAppLastUpdate
import icu.nullptr.twifucker.moduleLastModify
import icu.nullptr.twifucker.modulePrefs
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object FeatureSwitchHook : BaseHook() {
    override val name: String
        get() = "FeatureSwitchHook"

    private const val HOOK_FEATURE_SWITCH_GET_BOOL_CLASS = "hook_feature_switch_get_bool_class"
    private const val HOOK_FEATURE_SWITCH_GET_BOOL_METHOD = "hook_feature_switch_get_bool_method"

    private lateinit var featureSwitchGetBoolClassName: String
    private lateinit var featureSwitchGetBoolMethodName: String

    override fun init() {
        val featureSwitch = modulePrefs.getString("feature_switch", "[]")
        val arr = try {
            JSONArray(featureSwitch)
        } catch (_: JSONException) {
            JSONArray("[]")
        }
        if (arr.length() <= 0) return

        try {
            loadHookInfo()
        } catch (t: Throwable) {
            Log.e(t)
            return
        }

        findMethod(featureSwitchGetBoolClassName) {
            name == featureSwitchGetBoolMethodName
        }.hookAfter { param ->
            val paramKey = param.args[0] as String
            arr.forEach { obj ->
                val replaceKey = (obj as JSONObject).getString("key")
                val replaceValue = obj.getBoolean("value")
                if (paramKey == replaceKey) {
//                    Log.d("replaced feature switch: $paramKey $replaceValue")
                    param.result = replaceValue
                }
            }
        }
    }

    private fun loadCachedHookInfo() {
        featureSwitchGetBoolClassName =
            modulePrefs.getString(HOOK_FEATURE_SWITCH_GET_BOOL_CLASS, null)
                ?: throw CachedHookNotFound()
        featureSwitchGetBoolMethodName =
            modulePrefs.getString(HOOK_FEATURE_SWITCH_GET_BOOL_METHOD, null)
                ?: throw CachedHookNotFound()
    }

    private fun saveHookInfo() {
        modulePrefs.edit().let {
            it.putString(HOOK_FEATURE_SWITCH_GET_BOOL_CLASS, featureSwitchGetBoolClassName)
            it.putString(HOOK_FEATURE_SWITCH_GET_BOOL_METHOD, featureSwitchGetBoolMethodName)
        }.apply()
    }

    private fun searchHook() {
        val featureSwitchGetBoolClass = dexKit.findMethodUsingString {
            usingString = "^feature_switches_configs_crashlytics_enabled$"
        }.firstOrNull()?.getMethodInstance(InitFields.ezXClassLoader)?.declaringClass
            ?: throw ClassNotFoundException()
        val featureSwitchGetBoolMethod = featureSwitchGetBoolClass.declaredMethods.firstOrNull {
            it.parameterTypes.size == 2 && it.parameterTypes[0] == String::class.java && it.parameterTypes[1] == Boolean::class.java && it.returnType == Boolean::class.java
        } ?: throw NoSuchMethodException()

        featureSwitchGetBoolClassName = featureSwitchGetBoolClass.name
        featureSwitchGetBoolMethodName = featureSwitchGetBoolMethod.name
    }

    private fun loadHookInfo() {
        val hookFeatureSwitchLastUpdate = modulePrefs.getLong("hook_feature_switch_last_update", 0)

        Log.d("hookFeatureSwitchLastUpdate: $hookFeatureSwitchLastUpdate, hostAppLastUpdate: $hostAppLastUpdate, moduleLastModify: $moduleLastModify")

        val timeStart = System.currentTimeMillis()

        if (hookFeatureSwitchLastUpdate > hostAppLastUpdate && hookFeatureSwitchLastUpdate > moduleLastModify) {
            loadCachedHookInfo()
            Log.d("Feature Switch Hook load time: ${System.currentTimeMillis() - timeStart} ms")
        } else {
            loadDexKit()
            searchHook()
            Log.d("Feature Switch Hook search time: ${System.currentTimeMillis() - timeStart} ms")
            saveHookInfo()
            modulePrefs.edit()
                .putLong("hook_feature_switch_last_update", System.currentTimeMillis())
                .apply()
        }
    }
}