package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.FieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import icu.nullptr.twifucker.afterMeasure
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
            FieldFinder.fromClass(jsonTimelineEntryClass).filterByType(String::class.java).first()
        val contentField =
            FieldFinder.fromClass(jsonTimelineEntryClass).filter { type.isInterface }.first()

        MethodFinder.fromClass(jsonTimelineEntryMapperClass).filterByName("_parse")
            .filterByReturnType(jsonTimelineEntryClass).first().createHook {
                afterMeasure(name) { param ->
                    param.result ?: return@afterMeasure
                    val entryId = entryIdField.get(param.result) as String
                    if (isEntryNeedsRemove(entryId)) {
                        contentField.set(param.result, null)
                        Log.d("Remove timeline entry item: $entryId")
                    }
                }
            }
    }
}
