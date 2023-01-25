package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.FieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import icu.nullptr.twifucker.afterMeasure
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

        val threadsField = FieldFinder.fromClass(jsonFleetsTimelineResponseClass)
            .filterByType(ArrayList::class.java).first()

        MethodFinder.fromClass(jsonFleetsTimelineResponseMapperClass).filterByName("_parse")
            .filterByReturnType(jsonFleetsTimelineResponseClass).first().createHook {
                afterMeasure(name) { param ->
                    param.result ?: return@afterMeasure
                    threadsField.set(param.result, null)
                    Log.d("Removed threads")
                }
            }
    }
}