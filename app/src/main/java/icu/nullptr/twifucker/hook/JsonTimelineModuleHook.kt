package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClass
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
            jsonTimelineModuleClass.declaredFields.firstOrNull { it.type == jsonClientEventInfoClass }
                ?: throw NoSuchFieldError()
        val moduleField =
            jsonClientEventInfoClass.declaredFields.firstOrNull { it.type == String::class.java }
                ?: throw NoSuchFieldError()

        findMethod(jsonTimelineModuleMapperClass) {
            name == "_parse" && returnType == jsonTimelineModuleClass
        }.hookAfter { param ->
            param.result ?: return@hookAfter
            val module = jsonClientEventInfoField.get(param.result) ?: return@hookAfter
            if (moduleField.get(module) == "video_carousel") {
                param.result = null
                Log.d("Removed video carousel")
            }
        }
    }
}