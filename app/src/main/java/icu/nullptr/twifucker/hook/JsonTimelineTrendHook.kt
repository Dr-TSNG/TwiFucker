package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.FieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import icu.nullptr.twifucker.modulePrefs
import icu.nullptr.twifucker.reGenericClass

object JsonTimelineTrendHook : BaseHook() {
    override val name: String
        get() = "TimelineTrendHook"

    override fun init() {
        if (!modulePrefs.getBoolean("disable_promoted_trends", true)) return

        val jsonTimelineTrendClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineTrend")
        val jsonTimelineTrendMapperClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineTrend\$\$JsonObjectMapper")

        val jsonPromotedTrendMetadataClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonPromotedTrendMetadata").genericSuperclass?.toString()
                ?.let {
                    reGenericClass.matchEntire(it)?.groupValues?.get(2)
                        ?.let { genericClass -> loadClass(genericClass) }
                } ?: throw ClassNotFoundException()
        val jsonPromotedTrendMetadataField = FieldFinder.fromClass(jsonTimelineTrendClass)
            .filterByType(jsonPromotedTrendMetadataClass).first()

        MethodFinder.fromClass(jsonTimelineTrendMapperClass).filterByName("_parse")
            .filterByReturnType(jsonTimelineTrendClass).first().createHook {
                after { param ->
                    param.result ?: return@after
                    jsonPromotedTrendMetadataField.get(param.result) ?: return@after
                    param.result = null
                    Log.d("Remove promoted trend item")
                }
            }
    }
}
