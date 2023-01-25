package icu.nullptr.twifucker.hook

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import de.robv.android.xposed.XposedHelpers
import icu.nullptr.twifucker.*
import icu.nullptr.twifucker.exceptions.CachedHookNotFound
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexKit
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexKit
import java.lang.reflect.Modifier

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

        MethodFinder.fromClass(Activity::class.java).filterByName("startActivity")
            .filterByParamTypes {
                it.size == 2 && it[0] == Intent::class.java && it[1] == Bundle::class.java
            }.first().createHook {
                beforeMeasure(name) { param ->
                    val activity = param.thisObject as Activity
                    val intent = param.args[0] as Intent

                    if (intent.categories == null || (intent.action != Intent.ACTION_VIEW && intent.categories != null && !intent.categories.contains(
                            Intent.CATEGORY_BROWSABLE
                        ))
                    ) {
                        return@beforeMeasure
                    }

                    val isInAppBrowserEnabled = hostPrefs.getBoolean("in_app_browser", true)
                    val data = intent.dataString
                    val uri = Uri.parse(data)
                    val host = uri.host

                    if ((host == null) || DOMAIN_WHITELIST_SUFFIX.any { host.endsWith(it) }) {
                        return@beforeMeasure
                    }

                    val customTabsClass = loadClass(customTabsClassName)
                    val customTabsObj =
                        XposedHelpers.callStaticMethod(customTabsClass, customTabsGetMethodName)

                    // skip original method
                    param.result = null

                    if (isInAppBrowserEnabled) {
                        XposedHelpers.callMethod(
                            customTabsObj, customTabsLaunchUrlMethodName, activity, data, null
                        )
                    } else {
                        val newIntent = Intent(Intent.ACTION_VIEW, uri)
                        activity.startActivity(newIntent)
                    }
                }
            }
    }

    private fun loadCachedHookInfo() {
        customTabsClassName =
            modulePrefs.getString(HOOK_CUSTOM_TABS_CLASS, null) ?: throw CachedHookNotFound()
        customTabsGetMethodName =
            modulePrefs.getString(HOOK_CUSTOM_TABS_GET_METHOD, null) ?: throw CachedHookNotFound()
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

        val customTabsClass = dexKit.findMethodUsingString {
            usingString = "^android.support.customtabs.action.CustomTabsService$"
            methodReturnType = Void.TYPE.name
        }.firstOrNull()?.getMemberInstance(EzXHelper.classLoader)?.declaringClass
            ?: throw ClassNotFoundException()


        val customTabsGetMethod =
            MethodFinder.fromClass(customTabsClass).filterByModifiers { Modifier.isStatic(it) }
                .filterByParamCount(0).filterByReturnType(customTabsClass).first()
        val customTabsLaunchUrlMethod = MethodFinder.fromClass(customTabsClass).filterByModifiers {
            !Modifier.isStatic(it) && Modifier.isPublic(it) && Modifier.isFinal(it)
        }.filterByParamCount(3)
            .filterByParamTypes { it[0] == Activity::class.java && it[1] == String::class.java }
            .first()

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