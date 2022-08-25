package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull

object SensitiveMediaWarningHook : BaseHook() {
    private val jsonSensitiveMediaWarningClass =
        loadClassOrNull("com.twitter.model.json.core.JsonSensitiveMediaWarning\$\$JsonObjectMapper")

    override fun init() {
        if (!modulePrefs.getBoolean("disable_sensitive_media_warning", false)) return
        jsonSensitiveMediaWarningClass?.let { c ->
            findMethod(c) {
                name == "parseField"
            }.hookReplace { param ->
                val fieldName = param.args[1] as String
                Log.d("Hooking sensitive media warning: $fieldName")
            }
        }
    }

}
