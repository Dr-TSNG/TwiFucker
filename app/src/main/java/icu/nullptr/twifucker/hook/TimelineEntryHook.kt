package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull

val jsonTimelineEntryClass =
    loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineEntry")
val jsonTimelineEntryMapperClass =
    loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineEntry\$\$JsonObjectMapper")
val jsonTimelineEntryEntryIdField =
    jsonTimelineEntryClass?.declaredFields?.firstOrNull { it.type == String::class.java }

fun isEntryNeedsRemove(entryId: String): Boolean {
    // promoted tweet
    if (entryId.startsWith("promotedTweet-") && modulePrefs.getBoolean(
            "disable_promoted_content",
            true
        )
    ) {
        return true
    }
    // who to follow module
    if (entryId.startsWith("whoToFollow-") && modulePrefs.getBoolean(
            "disable_who_to_follow",
            false
        )
    ) {
        return true
    }
    // topics to follow module
    if (entryId.startsWith("TopicsModule-") && modulePrefs.getBoolean(
            "disable_topics_to_follow",
            false
        )
    ) {
        return true
    }
    return false
}

fun timelineEntryHook() {
    jsonTimelineEntryMapperClass?.let { c ->
        findMethod(c) {
            name == "parseField"
        }.hookAfter { param ->
            val fieldName = param.args[1] as String
            if (fieldName != "entryId") return@hookAfter
            val entryId = jsonTimelineEntryEntryIdField?.get(param.args[0])?.let { it as String }
                ?: return@hookAfter
            if (isEntryNeedsRemove(entryId)) {
                Log.d(
                    "Hooking timeline entry item $fieldName $entryId"
                )
                jsonTimelineEntryEntryIdField.set(param.args[0], "")
            }
        }
    }
}