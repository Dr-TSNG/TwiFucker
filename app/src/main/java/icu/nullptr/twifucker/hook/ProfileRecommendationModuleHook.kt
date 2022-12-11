package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull
import icu.nullptr.twifucker.modulePrefs

object ProfileRecommendationModuleHook : BaseHook() {
    override fun init() {
        if (!modulePrefs.getBoolean("disable_recommended_users", false)) return

        val jsonProfileRecommendationModuleResponseClass =
            loadClassOrNull("com.twitter.model.json.people.JsonProfileRecommendationModuleResponse")
                ?: throw ClassNotFoundException()
        val jsonProfileRecommendationModuleResponseMapperClass =
            loadClassOrNull("com.twitter.model.json.people.JsonProfileRecommendationModuleResponse\$\$JsonObjectMapper")
                ?: throw ClassNotFoundException()

        val recommendedUsersField =
            jsonProfileRecommendationModuleResponseClass.declaredFields.firstOrNull { it.type == ArrayList::class.java }
                ?: throw NoSuchFieldException()

        findMethod(jsonProfileRecommendationModuleResponseMapperClass) {
            name == "parse" && returnType == jsonProfileRecommendationModuleResponseClass
        }.hookAfter {
            recommendedUsersField.set(it.result, null)
            Log.d("Removed recommended users")
        }
    }
}