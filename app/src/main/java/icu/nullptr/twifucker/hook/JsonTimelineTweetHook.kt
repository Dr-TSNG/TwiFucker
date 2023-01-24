package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.FieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import icu.nullptr.twifucker.modulePrefs

object JsonTimelineTweetHook : BaseHook() {
    override val name: String
        get() = "JsonTimelineTweetHook"

    override fun init() {
        if (!modulePrefs.getBoolean("disable_promoted_content", true)) return

        val jsonTimelineTweetClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineTweet")
        val jsonTimelineTweetMapperClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineTweet\$\$JsonObjectMapper")

        val jsonTweetResultsClass =
            FieldFinder.fromClass(loadClass("com.twitter.model.json.core.JsonTweetResults"))
                .first().type
        val jsonTweetResultsField =
            FieldFinder.fromClass(jsonTimelineTweetClass).filterByType(jsonTweetResultsClass)
                .first()
        val jsonTweetIdField =
            FieldFinder.fromClass(jsonTimelineTweetClass).filterByType(String::class.java).first()

        val jsonPromotedContentUrtClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonPromotedContentUrt")
        val jsonPromotedContentUrtField =
            FieldFinder.fromClass(jsonTimelineTweetClass).filterByType(jsonPromotedContentUrtClass)
                .first()

        MethodFinder.fromClass(jsonTimelineTweetMapperClass).filterByName("_parse")
            .filterByReturnType(jsonTimelineTweetClass).first().createHook {
                after { param ->
                    param.result ?: return@after
                    jsonPromotedContentUrtField.get(param.result) ?: return@after
                    jsonTweetResultsField.set(param.result, null)
                    jsonTweetIdField.set(param.result, null) // saved search timeline
                    Log.d("Removed promoted timeline tweet")
                }
            }
    }
}