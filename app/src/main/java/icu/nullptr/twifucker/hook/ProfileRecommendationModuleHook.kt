package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull
import de.robv.android.xposed.XposedBridge

object ProfileRecommendationModuleHook : BaseHook() {
    private val jsonProfileRecommendationModuleResponseClass =
        loadClassOrNull("com.twitter.model.json.people.JsonProfileRecommendationModuleResponse\$\$JsonObjectMapper")

    override fun init() {
        if (!modulePrefs.getBoolean("disable_recommended_users", false)) return
        jsonProfileRecommendationModuleResponseClass?.let {
            findMethod(it) {
                name == "parseField"
            }.hookReplace { param ->
                val fieldName = param.args[1] as String
                if (fieldName == "recommended_users") {
                    Log.d("Hooking recommended users: $fieldName")
                } else {
                    XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
                }
            }
        }
    }
}