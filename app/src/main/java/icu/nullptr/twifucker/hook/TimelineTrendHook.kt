package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookReplace
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull
import de.robv.android.xposed.XposedBridge

val jsonTimelineTrendMapperClass =
    loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineTrend\$\$JsonObjectMapper")

fun timelineTrendHook() {
    if (!modulePrefs.getBoolean("disable_promoted_trends", true)) return
    jsonTimelineTrendMapperClass?.let { c ->
        findMethod(c) {
            name == "parseField"
        }.hookReplace { param ->
            val fieldName = param.args[1] as String
            // promoted trend
            if (fieldName == "promotedMetadata") {
                Log.d("Hooking timeline trend $fieldName")
            } else {
                XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args)
            }
        }
    }

}