package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.*
import icu.nullptr.twifucker.modulePrefs

object JsonFleetsTimelineResponseHook : BaseHook() {
    override val name: String
        get() = "JsonFleetsTimelineResponseHook"

    override fun init() {
        if (!modulePrefs.getBoolean("disable_threads", false)) return

        val jsonFleetsTimelineResponseClass =
            loadClass("com.twitter.fleets.api.json.JsonFleetsTimelineResponse")
        val jsonFleetsTimelineResponseMapperClass =
            loadClass("com.twitter.fleets.api.json.JsonFleetsTimelineResponse\$\$JsonObjectMapper")

        val threadsField =
            jsonFleetsTimelineResponseClass.declaredFields.firstOrNull { it.type == ArrayList::class.java }
                ?: throw NoSuchFieldException()

        findMethod(jsonFleetsTimelineResponseMapperClass) {
            name == "_parse" && returnType == jsonFleetsTimelineResponseClass
        }.hookAfter {
            it.result ?: return@hookAfter
            threadsField.set(it.result, null)
            Log.d("Removed threads")
        }
    }
}