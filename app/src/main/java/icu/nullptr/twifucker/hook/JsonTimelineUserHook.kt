package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClass
import icu.nullptr.twifucker.modulePrefs

object JsonTimelineUserHook : BaseHook() {
    override fun init() {
        if (!modulePrefs.getBoolean("disable_promoted_user", true)) return

        val jsonTimelineUserClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineUser")
        val jsonTimelineUserMapperClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineUser\$\$JsonObjectMapper")

        val jsonUserResultsClass =
            loadClass("com.twitter.model.json.core.JsonUserResults").declaredFields.firstOrNull()?.type
                ?: throw NoSuchFieldError()
        val jsonUserResultsField =
            jsonTimelineUserClass.declaredFields.firstOrNull { it.type == jsonUserResultsClass }
                ?: throw NoSuchFieldError()

        val jsonPromotedContentUrtClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonPromotedContentUrt")
        val jsonPromotedContentUrtField =
            jsonTimelineUserClass.declaredFields.firstOrNull { it.type == jsonPromotedContentUrtClass }
                ?: throw NoSuchFieldError()

        findMethod(jsonTimelineUserMapperClass) {
            name == "_parse" && returnType == jsonTimelineUserClass
        }.hookAfter { param ->
            param.result ?: return@hookAfter
            jsonPromotedContentUrtField.get(param.result) ?: return@hookAfter
            Log.d("Removed promoted user")
            jsonUserResultsField.set(param.result, null)
        }
    }
}
