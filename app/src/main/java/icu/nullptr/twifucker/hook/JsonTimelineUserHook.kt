package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.FieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import icu.nullptr.twifucker.modulePrefs

object JsonTimelineUserHook : BaseHook() {
    override val name: String
        get() = "JsonTimelineUserHook"

    override fun init() {
        if (!modulePrefs.getBoolean("disable_promoted_user", true)) return

        val jsonTimelineUserClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineUser")
        val jsonTimelineUserMapperClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineUser\$\$JsonObjectMapper")

        val jsonUserResultsClass =
            FieldFinder.fromClass(loadClass("com.twitter.model.json.core.JsonUserResults"))
                .first().type
        val jsonUserResultsField =
            FieldFinder.fromClass(jsonTimelineUserClass).filterByType(jsonUserResultsClass).first()

        val jsonPromotedContentUrtClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonPromotedContentUrt")
        val jsonPromotedContentUrtField =
            FieldFinder.fromClass(jsonTimelineUserClass).filterByType(jsonPromotedContentUrtClass)
                .first()

        MethodFinder.fromClass(jsonTimelineUserMapperClass).filterByName("_parse")
            .filterByReturnType(jsonTimelineUserClass).first().createHook {
                after { param ->
                    param.result ?: return@after
                    jsonPromotedContentUrtField.get(param.result) ?: return@after
                    jsonUserResultsField.set(param.result, null)
                    Log.d("Removed promoted user")
                }
            }
    }
}
