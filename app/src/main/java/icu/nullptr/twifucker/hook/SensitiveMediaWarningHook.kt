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
            param.result ?: return@hookAfter
            var count = 0
            warningFields.forEach { field ->
                field.get(param.result).let { value ->
                    if ((value as Boolean)) {
                        field.set(param.result, false)
                        count++
                    }
                }
            }
            if (count > 0) {
                Log.d("Set $count sensitive media warning field(s) to false")
            }
        }
    }
}
