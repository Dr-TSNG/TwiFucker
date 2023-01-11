package icu.nullptr.twifucker.hook

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.init.InitFields.ezXClassLoader
import com.github.kyuubiran.ezxhelper.utils.*
import icu.nullptr.twifucker.exceptions.CachedHookNotFound
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexKit
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexKit
import icu.nullptr.twifucker.hostAppLastUpdate
import icu.nullptr.twifucker.hostPrefs
import icu.nullptr.twifucker.moduleLastModify
import icu.nullptr.twifucker.modulePrefs
import io.luckypray.dexkit.builder.MethodUsingStringArgs

object CustomTabsHook : BaseHook() {
    override val name: String
        get() = "CustomTabsHook"

    private const val HOOK_CUSTOM_TABS_CLASS = "hook_custom_tabs_class"
    private const val HOOK_CUSTOM_TABS_GET_METHOD = "hook_custom_tabs_get_method"
    private const val HOOK_CUSTOM_TABS_LAUNCH_URL_METHOD = "hook_custom_tabs_launch_url_method"

    private val DOMAIN_WHITELIST_SUFFIX = listOf("pscp.tv", "periscope.tv", "twitter.com", "t.co")
    private lateinit var customTabsClassName: String
    private lateinit var customTabsGetMethodName: String
    private lateinit var customTabsLaunchUrlMethodName: String

    override fun init() {
        if (!modulePrefs.getBoolean("disable_url_redirect", false)) return

        try {
            loadHookInfo()
        } catch (t: Throwable) {
            Log.e(t)
            return
        }

        findMethod(Activity::class.java) {
            name == "startActivity" && parameterTypes.size == 2 && parameterTypes[0] == Intent::class.java && parameterTypes[1] == Bundle::class.java
        }.hookBefore { param ->
            val activity = param.thisObject as Activity
            val intent = param.args[0] as Intent

            if (intent.categories == null || (intent.action != Intent.ACTION_VIEW && intent.categories != null && !intent.categories.contains(
                    Intent.CATEGORY_BROWSABLE
                ))
            ) {
                return@hookBefore
            }

            val isInAppBrowserEnabled = hostPrefs.getBoolean("in_app_browser", true)
            val data = intent.dataString
            val uri = Uri.parse(data)
            val host = uri.host

            if ((host == null) || DOMAIN_WHITELIST_SUFFIX.any { host.endsWith(it) }) {
                return@hookBefore
            }

            val customTabsClass = loadClass(customTabsClassName)
            val customTabsObj = customTabsClass.invokeStaticMethod(customTabsGetMethodName)

            // skip original method
            param.result = null

            if (isInAppBrowserEnabled) {
                customTabsObj?.invokeMethodAuto(
                    customTabsLaunchUrlMethodName, activity, data, null
                )
            } else {
                val newIntent = Intent(Intent.ACTION_VIEW, uri)
                activity.startActivity(newIntent)
            }
        }
    }

    private fun loadCachedHookInfo() {
        customTabsClassName = modulePrefs.getString(HOOK_CUSTOM_TABS_CLASS, null)
            ?: throw CachedHookNotFound()
        customTabsGetMethodName = modulePrefs.getString(HOOK_CUSTOM_TABS_GET_METHOD, null)
            ?: throw CachedHookNotFound()
        customTabsLaunchUrlMethodName =
            modulePrefs.getString(HOOK_CUSTOM_TABS_LAUNCH_URL_METHOD, null)
                ?: throw CachedHookNotFound()
    }

    private fun saveHookInfo() {
        modulePrefs.edit().let {
            it.putString(HOOK_CUSTOM_TABS_CLASS, customTabsClassName)
            it.putString(HOOK_CUSTOM_TABS_GET_METHOD, customTabsGetMethodName)
            it.putString(HOOK_CUSTOM_TABS_LAUNCH_URL_METHOD, customTabsLaunchUrlMethodName)
        }.apply()
    }

    private fun searchHook() {

        val customTabsClass = dexKit.findMethodUsingString(
            MethodUsingStringArgs.build {
                usingString = "^android.support.customtabs.action.CustomTabsService$"
                methodReturnType = Void.TYPE.name
            }
        ).firstOrNull()?.getMemberInstance(ezXClassLoader)?.declaringClass
            ?: throw ClassNotFoundException()

        val customTabsGetMethod = customTabsClass.declaredMethods.firstOrNull {
            it.isStatic && it.parameterTypes.isEmpty() && it.returnType == customTabsClass
        } ?: throw NoSuchMethodException()
        val customTabsLaunchUrlMethod = customTabsClass.declaredMethods.firstOrNull {
            it.isNotStatic && it.isPublic && it.isFinal && it.parameterTypes.size == 3 && it.parameterTypes[0] == Activity::class.java && it.parameterTypes[1] == String::class.java
        } ?: throw NoSuchMethodException()

        customTabsClassName = customTabsClass.name
        customTabsGetMethodName = customTabsGetMethod.name
        customTabsLaunchUrlMethodName = customTabsLaunchUrlMethod.name
    }

    private fun loadHookInfo() {
        val hookCustomTabsLastUpdate = modulePrefs.getLong("hook_custom_tabs_last_update", 0)

        Log.d("hookCustomTabsLastUpdate: $hookCustomTabsLastUpdate, hostAppLastUpdate: $hostAppLastUpdate, moduleLastModify: $moduleLastModify")

        val timeStart = System.currentTimeMillis()

        if (hookCustomTabsLastUpdate > hostAppLastUpdate && hookCustomTabsLastUpdate > moduleLastModify) {
            loadCachedHookInfo()
            Log.d("Custom Tabs Hook load time: ${System.currentTimeMillis() - timeStart} ms")
        } else {
            loadDexKit()
            searchHook()
            Log.d("Custom Tabs Hook search time: ${System.currentTimeMillis() - timeStart} ms")
            saveHookInfo()
            modulePrefs.edit().putLong("hook_custom_tabs_last_update", System.currentTimeMillis())
                .apply()
        }
    }
}