package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.*
import icu.nullptr.twifucker.modulePrefs

object TimelineUserHook : BaseHook() {
    private val jsonTimelineUserClass =
        loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineUser")
    private val jsonTimelineUserMapperClass =
        loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineUser\$\$JsonObjectMapper")
    private val jsonTimelineUserStringFields =
        jsonTimelineUserClass?.declaredFields?.firstOrNull { it.isPublic && it.type != String::class.java && it.type != Boolean::class.java }

    override fun init() {
        if (!modulePrefs.getBoolean("disable_promoted_user", true)) return
        jsonTimelineUserMapperClass?.let { c ->
            findMethod(c) {
                name == "parseField"
            }.hookAfter { param ->
                val fieldName = param.args[1] as String
                // ? and promoted user in who to follow
                if (fieldName == "promotedMetadata" || fieldName == "userPromotedMetadata") {
                    Log.d("Hooking timeline user $fieldName")
                    jsonTimelineUserStringFields?.let { f ->
                        val value = f.get(param.args[0]) ?: null
                        Log.d("${f.name}: $value")
                        f.set(param.args[0], null)
                    }
                }
            }
        }
    }
}
