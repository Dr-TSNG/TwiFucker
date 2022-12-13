package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClass
import icu.nullptr.twifucker.modulePrefs

object SensitiveMediaWarningHook : BaseHook() {
    override fun init() {
        if (!modulePrefs.getBoolean("disable_sensitive_media_warning", false)) return

        val jsonSensitiveMediaWarningClass =
            loadClass("com.twitter.model.json.core.JsonSensitiveMediaWarning")
        val jsonSensitiveMediaWarningMapperClass =
            loadClass("com.twitter.model.json.core.JsonSensitiveMediaWarning\$\$JsonObjectMapper")

        val warningFields =
            jsonSensitiveMediaWarningClass.declaredFields.filter { it.type == Boolean::class.java }

        findMethod(jsonSensitiveMediaWarningMapperClass) {
            name == "_parse" && returnType == jsonSensitiveMediaWarningClass
        }.hookAfter { param ->
            if (param.result == null) return@hookAfter
            val fieldsName = listOf("adult_content", "graphic_violence", "other")
            warningFields.forEachIndexed { i, field ->
                field.get(param.result).let { value ->
                    if ((value as Boolean)) {
                        field.set(param.result, false)
                        fieldsName[i].let { Log.d("Removed sensitive media warning: $it") }
                    }
                }
            }
        }
    }
}
