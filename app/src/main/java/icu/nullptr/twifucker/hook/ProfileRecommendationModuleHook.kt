package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClass
import icu.nullptr.twifucker.modulePrefs

object ProfileRecommendationModuleHook : BaseHook() {
    override fun init() {
        if (!modulePrefs.getBoolean("disable_recommended_users", false)) return

        val jsonProfileRecommendationModuleResponseClass =
            loadClass("com.twitter.model.json.people.JsonProfileRecommendationModuleResponse")
        val jsonProfileRecommendationModuleResponseMapperClass =
            loadClass("com.twitter.model.json.people.JsonProfileRecommendationModuleResponse\$\$JsonObjectMapper")

        val recommendedUsersField =
            jsonProfileRecommendationModuleResponseClass.declaredFields.firstOrNull { it.type == ArrayList::class.java }
                ?: throw NoSuchFieldException()

        findMethod(jsonProfileRecommendationModuleResponseMapperClass) {
            name == "_parse" && returnType == jsonProfileRecommendationModuleResponseClass
        }.hookAfter { param ->
            param.result ?: return@hookAfter
            recommendedUsersField.get(param.result).let { users ->
                if (users is ArrayList<*> && users.isNotEmpty()) {
                    recommendedUsersField.set(param.result, null)
                    Log.d("Removed recommended users")
                }
            }
        }
    }
}