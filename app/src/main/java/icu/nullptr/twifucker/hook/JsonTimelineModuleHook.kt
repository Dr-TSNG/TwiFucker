package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.FieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import icu.nullptr.twifucker.modulePrefs

object JsonTimelineModuleHook : BaseHook() {
    override val name: String
        get() = "JsonTimelineModuleHook"

    override fun init() {
        if (!modulePrefs.getBoolean("disable_video_carousel", false)) return
        val jsonTimelineModuleMapperClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineModule\$\$JsonObjectMapper")
        val jsonTimelineModuleClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineModule")
        val jsonClientEventInfoClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonClientEventInfo")
        val jsonClientEventInfoField =
            FieldFinder.fromClass(jsonTimelineModuleClass).filterByType(jsonClientEventInfoClass)
                .first()
        val moduleField =
            FieldFinder.fromClass(jsonClientEventInfoClass).filterByType(String::class.java).first()

        MethodFinder.fromClass(jsonTimelineModuleMapperClass).filterByName("_parse")
            .filterByReturnType(jsonTimelineModuleClass).first().createHook {
                after { param ->
                    param.result ?: return@after
                    val module = jsonClientEventInfoField.get(param.result) ?: return@after
                    if (moduleField.get(module) == "video_carousel") {
                        param.result = null
                        Log.d("Removed video carousel")
                    }
                }
            }
    }
}