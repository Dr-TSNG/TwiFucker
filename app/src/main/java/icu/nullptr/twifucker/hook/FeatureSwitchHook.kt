package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import icu.nullptr.twifucker.afterMeasure
import icu.nullptr.twifucker.exceptions.CachedHookNotFound
import icu.nullptr.twifucker.forEach
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexKit
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexKit
import icu.nullptr.twifucker.hostAppLastUpdate
import icu.nullptr.twifucker.moduleLastModify
import icu.nullptr.twifucker.modulePrefs
import org.json.JSONArray
import org.json.JSONException

object FeatureSwitchHook : BaseHook() {
    override val name: String
        get() = "FeatureSwitchHook"

    private const val HOOK_FEATURE_SWITCH_CLASS = "hook_feature_switch_class"
    private const val HOOK_FEATURE_SWITCH_GET_BOOL_METHOD = "hook_feature_switch_get_bool_method"
    private const val HOOK_FEATURE_SWITCH_GET_DOUBLE_METHOD =
        "hook_feature_switch_get_double_method"
    private const val HOOK_FEATURE_SWITCH_GET_FLOAT_METHOD = "hook_feature_switch_get_float_method"
    private const val HOOK_FEATURE_SWITCH_GET_LONG_METHOD = "hook_feature_switch_get_long_method"
    private const val HOOK_FEATURE_SWITCH_GET_INT_METHOD = "hook_feature_switch_get_int_method"

    private lateinit var featureSwitchClassName: String
    private lateinit var featureSwitchGetBoolMethodName: String
    private lateinit var featureSwitchGetDoubleMethodName: String
    private lateinit var featureSwitchGetFloatMethodName: String
    private lateinit var featureSwitchGetLongMethodName: String
    private lateinit var featureSwitchGetIntMethodName: String

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

        MethodFinder.fromClass(loadClass(featureSwitchClassName)).filterByName(
            featureSwitchGetBoolMethodName
        ).first().createHook {
            afterMeasure(name) { param ->
                val paramKey = param.args[0] as String
                arr.forEach { obj ->
                    if (obj.optString("type", "boolean") != "boolean") return@forEach
                    val replaceKey = obj.optString("key")
                    val replaceValue = obj.optBoolean("value")
                    if (paramKey == replaceKey) {
                        param.result = replaceValue
                        return@forEach
                    }
                }
            }
        }
        MethodFinder.fromClass(loadClass(featureSwitchClassName)).filterByName(
            featureSwitchGetDoubleMethodName
        ).first().createHook {
            afterMeasure(name) { param ->
                val paramKey = param.args[0] as String
                arr.forEach { obj ->
                    if (obj.optString("type", "") != "decimal") return@forEach
                    val replaceKey = obj.optString("key")
                    val replaceValue = obj.optString("value")
                    if (paramKey == replaceKey) {
                        param.result = replaceValue.toDouble()
                        return@forEach
                    }
                }
            }
        }
        MethodFinder.fromClass(loadClass(featureSwitchClassName)).filterByName(
            featureSwitchGetFloatMethodName
        ).first().createHook {
            afterMeasure(name) { param ->
                val paramKey = param.args[0] as String
                arr.forEach { obj ->
                    if (obj.optString("type", "") != "decimal") return@forEach
                    val replaceKey = obj.optString("key")
                    val replaceValue = obj.optString("value")
                    if (paramKey == replaceKey) {
                        param.result = replaceValue.toFloat()
                        return@forEach
                    }
                }
            }
        }
        MethodFinder.fromClass(loadClass(featureSwitchClassName)).filterByName(
            featureSwitchGetLongMethodName
        ).first().createHook {
            afterMeasure(name) { param ->
                val paramKey = param.args[0] as String
                arr.forEach { obj ->
                    if (obj.optString("type", "") != "decimal") return@forEach
                    val replaceKey = obj.optString("key")
                    val replaceValue = obj.optString("value")
                    if (paramKey == replaceKey) {
                        param.result = replaceValue.toLong()
                        return@forEach
                    }
                }
            }
        }
        MethodFinder.fromClass(loadClass(featureSwitchClassName)).filterByName(
            featureSwitchGetIntMethodName
        ).first().createHook {
            afterMeasure(name) { param ->
                val paramKey = param.args[0] as String
                arr.forEach { obj ->
                    if (obj.optString("type", "") != "decimal") return@forEach
                    val replaceKey = obj.optString("key")
                    val replaceValue = obj.optString("value")
                    if (paramKey == replaceKey) {
                        param.result = replaceValue.toInt()
                        return@forEach
                    }
                }
            }
        }
    }

    private fun loadCachedHookInfo() {
        featureSwitchClassName =
            modulePrefs.getString(HOOK_FEATURE_SWITCH_CLASS, null) ?: throw CachedHookNotFound()
        featureSwitchGetBoolMethodName =
            modulePrefs.getString(HOOK_FEATURE_SWITCH_GET_BOOL_METHOD, null)
                ?: throw CachedHookNotFound()
        featureSwitchGetDoubleMethodName =
            modulePrefs.getString(HOOK_FEATURE_SWITCH_GET_DOUBLE_METHOD, null)
                ?: throw CachedHookNotFound()
        featureSwitchGetFloatMethodName =
            modulePrefs.getString(HOOK_FEATURE_SWITCH_GET_FLOAT_METHOD, null)
                ?: throw CachedHookNotFound()
        featureSwitchGetLongMethodName =
            modulePrefs.getString(HOOK_FEATURE_SWITCH_GET_LONG_METHOD, null)
                ?: throw CachedHookNotFound()
        featureSwitchGetIntMethodName =
            modulePrefs.getString(HOOK_FEATURE_SWITCH_GET_INT_METHOD, null)
                ?: throw CachedHookNotFound()
    }

    private fun saveHookInfo() {
        modulePrefs.let {
            it.putString(HOOK_FEATURE_SWITCH_CLASS, featureSwitchClassName)
            it.putString(HOOK_FEATURE_SWITCH_GET_BOOL_METHOD, featureSwitchGetBoolMethodName)
            it.putString(HOOK_FEATURE_SWITCH_GET_DOUBLE_METHOD, featureSwitchGetDoubleMethodName)
            it.putString(HOOK_FEATURE_SWITCH_GET_FLOAT_METHOD, featureSwitchGetFloatMethodName)
            it.putString(HOOK_FEATURE_SWITCH_GET_LONG_METHOD, featureSwitchGetLongMethodName)
            it.putString(HOOK_FEATURE_SWITCH_GET_INT_METHOD, featureSwitchGetIntMethodName)
        }
    }

    private fun searchHook() {
        val featureSwitchClass = dexKit.findMethodUsingString {
            usingString = "^feature_switches_configs_crashlytics_enabled$"
        }.firstOrNull()?.getMethodInstance(EzXHelper.classLoader)?.declaringClass
            ?: throw ClassNotFoundException()
        val featureSwitchGetBoolMethod = MethodFinder.fromClass(featureSwitchClass)
            .filterByParamTypes(String::class.java, Boolean::class.java)
            .filterByReturnType(Boolean::class.java).first()
        val featureSwitchGetDoubleMethod = MethodFinder.fromClass(featureSwitchClass)
            .filterByParamTypes(String::class.java, Double::class.java)
            .filterByReturnType(Double::class.java).first()
        val featureSwitchGetFloatMethod = MethodFinder.fromClass(featureSwitchClass)
            .filterByParamTypes(String::class.java, Float::class.java)
            .filterByReturnType(Float::class.java).first()
        val featureSwitchGetLongMethod = MethodFinder.fromClass(featureSwitchClass)
            .filterByParamTypes(String::class.java, Long::class.java)
            .filterByReturnType(Long::class.java).first()
        val featureSwitchGetIntMethod = MethodFinder.fromClass(featureSwitchClass)
            .filterByParamTypes(String::class.java, Int::class.java)
            .filterByReturnType(Int::class.java).first()

        featureSwitchClassName = featureSwitchClass.name
        featureSwitchGetBoolMethodName = featureSwitchGetBoolMethod.name
        featureSwitchGetDoubleMethodName = featureSwitchGetDoubleMethod.name
        featureSwitchGetFloatMethodName = featureSwitchGetFloatMethod.name
        featureSwitchGetLongMethodName = featureSwitchGetLongMethod.name
        featureSwitchGetIntMethodName = featureSwitchGetIntMethod.name
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
            modulePrefs.putLong("hook_feature_switch_last_update", System.currentTimeMillis())
        }
    }
}