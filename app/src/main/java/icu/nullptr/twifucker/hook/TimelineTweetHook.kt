package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.*
import icu.nullptr.twifucker.modulePrefs

object TimelineTweetHook : BaseHook() {
    private val jsonTimelineTweetClass =
        loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineTweet")
    private val jsonTimelineTweetMapperClass =
        loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineTweet\$\$JsonObjectMapper")
//    private val jsonTimelineTweetEntryIdField =
//        jsonTimelineTweetClass?.declaredFields?.firstOrNull { it.type == String::class.java }
    private val jsonTimelineTweetTweetResultField =
        jsonTimelineTweetClass?.declaredFields?.firstOrNull { it.isNotStatic }

    override fun init() {
        if (!modulePrefs.getBoolean("disable_promoted_content", true)) return
        jsonTimelineTweetMapperClass?.let { c ->
            findMethod(c) {
                name == "parseField"
            }.hookAfter { param ->
                val fieldName = param.args[1] as String
                // timeline ads and replies ads
                if (fieldName == "promotedMetadata" || fieldName == "tweetPromotedMetadata") {
                    Log.d("Hooking timeline ads $fieldName")
                    jsonTimelineTweetTweetResultField?.set(param.args[0], null)
                }
                // timeline ads
//                if (fieldName == "promotedMetadata") {
//                    val entryId =
//                        jsonTimelineTweetEntryIdField?.get(param.args[0]) ?: return@hookAfter
//                    Log.d("Hooking timeline ads $fieldName $entryId")
//                    jsonTimelineTweetEntryIdField.set(param.args[0], "")
//                }
            }
        }
    }
}
