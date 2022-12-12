package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull
import icu.nullptr.twifucker.modulePrefs

object SensitiveMediaWarningHook : BaseHook() {
    override fun init() {
        if (!modulePrefs.getBoolean("disable_sensitive_media_warning", false)) return

        val jsonSensitiveMediaWarningClass =
            loadClassOrNull("com.twitter.model.json.core.JsonSensitiveMediaWarning")
                ?: throw ClassNotFoundException()
        val jsonSensitiveMediaWarningMapperClass =
            loadClassOrNull("com.twitter.model.json.core.JsonSensitiveMediaWarning\$\$JsonObjectMapper")
                ?: throw ClassNotFoundException()

        jsonSensitiveMediaWarningMapperClass.let { c ->
            findMethod(c) {
                name == "parse" && returnType == jsonSensitiveMediaWarningClass
            }.hookReplace { param ->
                val fieldsName = listOf("adult_content", "graphic_violence", "other")
                val warningFields =
                    jsonSensitiveMediaWarningClass.declaredFields.filter { it.type == Boolean::class.java }
                warningFields.forEachIndexed { i, field ->
                    field.get(param.result).let { value ->
                        if (value == true) {
                            field.set(param.result, false)
                            fieldsName[i].let { Log.d("Removed sensitive media warning: $it") }
                        }
                    }
                }
            }
        }
    }

}
