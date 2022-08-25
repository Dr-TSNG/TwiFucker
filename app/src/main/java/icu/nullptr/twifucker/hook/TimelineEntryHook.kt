package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull

object TimelineEntryHook : BaseHook() {
    private val jsonTimelineEntryClass =
        loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineEntry")
    private val jsonTimelineEntryMapperClass =
        loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineEntry\$\$JsonObjectMapper")
    private val jsonTimelineEntryEntryIdField =
        jsonTimelineEntryClass?.declaredFields?.firstOrNull { it.type == String::class.java }

    override fun init() {
        jsonTimelineEntryMapperClass?.let { c ->
            findMethod(c) {
                name == "parseField"
            }.hookAfter { param ->
                val fieldName = param.args[1] as String
                if (fieldName != "entryId") return@hookAfter
                val entryId =
                    jsonTimelineEntryEntryIdField?.get(param.args[0])?.let { it as String }
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
}
