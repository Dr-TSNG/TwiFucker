package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull

val jsonTimelineTweetClass =
    loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineTweet")
val jsonTimelineTweetMapperClass =
    loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineTweet\$\$JsonObjectMapper")
val jsonTimelineTweetEntryIdField =
    jsonTimelineTweetClass?.declaredFields?.firstOrNull { it.type == String::class.java }

fun timelineTweetHook() {
    if (!modulePrefs.getBoolean("disable_promoted_content", true)) return
    jsonTimelineTweetMapperClass?.let { c ->
        findMethod(c) {
            name == "parseField"
        }.hookAfter { param ->
            val fieldName = param.args[1] as String
            // promoted tweet in search
            if (fieldName == "promotedMetadata") {
                val entryId = jsonTimelineTweetEntryIdField?.get(param.args[0]) ?: return@hookAfter
                Log.d("Hooking timeline search $fieldName $entryId")
                jsonTimelineTweetEntryIdField.set(param.args[0], "")
            }
        }
    }

}
