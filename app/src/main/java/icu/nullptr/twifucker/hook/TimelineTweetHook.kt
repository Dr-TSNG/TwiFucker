package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClass
import icu.nullptr.twifucker.modulePrefs

object TimelineTweetHook : BaseHook() {
    override fun init() {
        if (!modulePrefs.getBoolean("disable_promoted_content", true)) return

        val jsonTimelineTweetClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineTweet")
        val jsonTimelineTweetMapperClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineTweet\$\$JsonObjectMapper")

        val jsonTweetResultsClass =
            loadClass("com.twitter.model.json.core.JsonTweetResults").declaredFields.firstOrNull()?.type
                ?: throw NoSuchFieldError()
        val jsonTweetResultsField =
            jsonTimelineTweetClass.declaredFields.firstOrNull { it.type == jsonTweetResultsClass }
                ?: throw NoSuchFieldError()

        val jsonPromotedContentUrtClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonPromotedContentUrt")
        val jsonPromotedContentUrtField =
            jsonTimelineTweetClass.declaredFields.firstOrNull { it.type == jsonPromotedContentUrtClass }
                ?: throw NoSuchFieldError()

        val entryIdField =
            jsonTimelineTweetClass.declaredFields.firstOrNull { it.type == String::class.java }
                ?: throw NoSuchFieldError()

        findMethod(jsonTimelineTweetMapperClass) {
            name == "_parse" && returnType == jsonTimelineTweetClass
        }.hookAfter { param ->
            param.result ?: return@hookAfter
            jsonPromotedContentUrtField.get(param.result) ?: return@hookAfter
            jsonTweetResultsField.set(param.result, null)
            entryIdField.set(param.result, "")
            Log.d("Removed promoted content")
        }
    }
}
