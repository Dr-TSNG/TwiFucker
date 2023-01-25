package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.FieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import icu.nullptr.twifucker.afterMeasure
import icu.nullptr.twifucker.modulePrefs

object SensitiveMediaWarningHook : BaseHook() {
    override val name: String
        get() = "SensitiveMediaWarningHook"

    override fun init() {
        if (!modulePrefs.getBoolean("disable_sensitive_media_warning", false)) return

        val jsonSensitiveMediaWarningClass =
            loadClass("com.twitter.model.json.core.JsonSensitiveMediaWarning")
        val jsonSensitiveMediaWarningMapperClass =
            loadClass("com.twitter.model.json.core.JsonSensitiveMediaWarning\$\$JsonObjectMapper")

        val warningFields =
            FieldFinder.fromClass(jsonSensitiveMediaWarningClass).filterByType(Boolean::class.java)

        MethodFinder.fromClass(jsonSensitiveMediaWarningMapperClass).filterByName("_parse")
            .filterByReturnType(jsonSensitiveMediaWarningClass).first().createHook {
                afterMeasure(name) { param ->
                    param.result ?: return@afterMeasure
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
}
