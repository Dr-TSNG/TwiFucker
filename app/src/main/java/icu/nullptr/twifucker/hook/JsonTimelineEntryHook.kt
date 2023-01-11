package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClass
import icu.nullptr.twifucker.isEntryNeedsRemove

object JsonTimelineEntryHook : BaseHook() {
    override val name: String
        get() = "TimelineEntryHook"

    override fun init() {
        val jsonTimelineEntryClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineEntry")
        val jsonTimelineEntryMapperClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineEntry\$\$JsonObjectMapper")

        val entryIdField =
            jsonTimelineEntryClass.declaredFields.firstOrNull { it.type == String::class.java }
                ?: throw NoSuchFieldError()
        val contentField = jsonTimelineEntryClass.declaredFields.firstOrNull { it.type.isInterface }
            ?: throw NoSuchFieldError()

        findMethod(jsonTimelineEntryMapperClass) {
            name == "_parse" && returnType == jsonTimelineEntryClass
        }.hookAfter { param ->
            param.result ?: return@hookAfter
            val entryId = entryIdField.get(param.result) as String
            if (isEntryNeedsRemove(entryId)) {
                contentField.set(param.result, null)
                Log.d("Remove timeline entry item: $entryId")
            }
        }
    }
}
