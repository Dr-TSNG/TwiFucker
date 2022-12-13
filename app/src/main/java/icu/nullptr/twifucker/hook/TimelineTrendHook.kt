package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClass
import icu.nullptr.twifucker.modulePrefs
import icu.nullptr.twifucker.reGenericClass

object TimelineTrendHook : BaseHook() {
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
        val jsonPromotedTrendMetadataField =
            jsonTimelineTrendClass.declaredFields.firstOrNull { it.type == jsonPromotedTrendMetadataClass }
                ?: throw NoSuchFieldError()

        findMethod(jsonTimelineTrendMapperClass) {
            name == "_parse" && returnType == jsonTimelineTrendClass
        }.hookAfter { param ->
            param.result ?: return@hookAfter
            jsonPromotedTrendMetadataField.get(param.result) ?: return@hookAfter
            param.result = null
            Log.d("Remove promoted trend item")
        }
    }
}
