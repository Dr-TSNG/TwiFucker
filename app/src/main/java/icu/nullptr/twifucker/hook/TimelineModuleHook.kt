package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull
import icu.nullptr.twifucker.isEntryNeedsRemove

object TimelineModuleHook : BaseHook() {
    private val jsonTimelineModuleItemClass =
        loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineModuleItem")
    private val jsonTimelineModuleItemMapperClass =
        loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineModuleItem\$\$JsonObjectMapper")
    private val jsonTimelineModuleItemEntryIdField =
        jsonTimelineModuleItemClass?.declaredFields?.firstOrNull { it.type == String::class.java }

    override fun init() {
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
}
