package icu.nullptr.twifucker.hook

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.init.InitFields
import com.github.kyuubiran.ezxhelper.utils.*
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexHelper
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexHelper
import icu.nullptr.twifucker.hostPrefs
import icu.nullptr.twifucker.modulePrefs
import java.io.File

object CustomTabsHook : BaseHook() {
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
        customTabsClassName = modulePrefs.getString("hook_custom_tabs_class", null)
            ?: throw Throwable("cached hook not found")
        customTabsGetMethodName = modulePrefs.getString("hook_custom_tabs_get_method", null)
            ?: throw Throwable("cached hook not found")
        customTabsLaunchUrlMethodName =
            modulePrefs.getString("hook_custom_tabs_launch_url_method", null)
                ?: throw Throwable("cached hook not found")
    }

    private fun saveHookInfo() {
        modulePrefs.edit().let {
            it.putString("hook_custom_tabs_class", customTabsClassName)
            it.putString("hook_custom_tabs_get_method", customTabsGetMethodName)
            it.putString("hook_custom_tabs_launch_url_method", customTabsLaunchUrlMethodName)
        }.apply()
    }

    private fun searchHook() {
        val customTabsClass = dexHelper.findMethodUsingString(
            "android.support.customtabs.action.CustomTabsService",
            false,
            dexHelper.encodeClassIndex(Void.TYPE),
            0,
            null,
            -1,
            null,
            null,
            null,
            true,
        ).firstOrNull()?.let { dexHelper.decodeMethodIndex(it)?.declaringClass }
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

        @Suppress("DEPRECATION") val appLastUpdateTime =
            InitFields.appContext.packageManager.getPackageInfo(
                InitFields.appContext.packageName, 0
            ).lastUpdateTime
        val moduleLastUpdate = File(InitFields.modulePath).lastModified()

        Log.d("hookCustomTabsLastUpdate: $hookCustomTabsLastUpdate, appLastUpdateTime: $appLastUpdateTime, moduleLastUpdate: $moduleLastUpdate")

        val timeStart = System.currentTimeMillis()

        if (hookCustomTabsLastUpdate > appLastUpdateTime && hookCustomTabsLastUpdate > moduleLastUpdate) {
            loadCachedHookInfo()
            Log.d("Custom Tabs Hook load time: ${System.currentTimeMillis() - timeStart} ms")
        } else {
            loadDexHelper()
            searchHook()
            Log.d("Custom Tabs Hook search time: ${System.currentTimeMillis() - timeStart} ms")
            saveHookInfo()
            modulePrefs.edit().putLong("hook_custom_tabs_last_update", System.currentTimeMillis())
                .apply()
        }
    }
}