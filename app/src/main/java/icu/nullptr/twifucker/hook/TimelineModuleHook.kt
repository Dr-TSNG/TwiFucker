package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull

val jsonTimelineModuleItemClass =
    loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineModuleItem")
val jsonTimelineModuleItemMapperClass =
    loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineModuleItem\$\$JsonObjectMapper")
val jsonTimelineModuleItemEntryIdField =
    jsonTimelineModuleItemClass?.declaredFields?.firstOrNull { it.type == String::class.java }

fun timelineModuleItemHook() {
    jsonTimelineModuleItemMapperClass?.let { c ->
        findMethod(c) {
            name == "parseField"
        }.hookAfter { param ->
            val fieldName = param.args[1] as String
            if (fieldName != "entryId") return@hookAfter
            val entryId =
                jsonTimelineModuleItemEntryIdField?.get(param.args[0])?.let { it as String }
                    ?: return@hookAfter
            if (isEntryNeedsRemove(entryId)) {
                Log.d(
                    "Hooking timeline module item $fieldName $entryId"
                )
                jsonTimelineModuleItemEntryIdField.set(param.args[0], "")
            }
        }
    }
}