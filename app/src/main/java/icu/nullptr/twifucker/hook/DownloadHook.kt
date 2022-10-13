package icu.nullptr.twifucker.hook

import android.app.Activity
import android.content.Intent
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.init.InitFields.ezXClassLoader
import com.github.kyuubiran.ezxhelper.init.InitFields.modulePath
import com.github.kyuubiran.ezxhelper.utils.*
import icu.nullptr.twifucker.*
import icu.nullptr.twifucker.hook.HookEntry.Companion.currentActivity
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexHelper
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexHelper
import icu.nullptr.twifucker.ui.DownloadDialog
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Method


object DownloadHook : BaseHook() {
    private var downloadUrls: List<String> = listOf()

    private lateinit var carouselActionItemFactoryClassName: String
    private lateinit var genCarouselActionItemMethodName: String
    private lateinit var actionItemViewDataClassName: String

    private lateinit var actionTypeEnumClassName: String
    private lateinit var carouselViewDataClassName: String

    private lateinit var actionItemViewOnClickClassName: String
    private lateinit var actionItemViewOnClickMethodName: String
    private lateinit var viewDataFieldName: String
    private lateinit var actionTypeFieldName: String

    private lateinit var shareMenuClassName: String
    private lateinit var shareMenuMethodName: String
    private lateinit var tweetResultFieldName: String
    private lateinit var resultFieldName: String
    private lateinit var legacyFieldName: String
    private lateinit var extendedEntitiesFieldName: String
    private lateinit var mediaFieldName: String
    private lateinit var mediaTypeFieldName: String
    private lateinit var mediaUrlHttpsFieldName: String
    private lateinit var mediaInfoFieldName: String
    private lateinit var variantsFieldName: String

    override fun init() {
        if (!modulePrefs.getBoolean("enable_download_hook", false)) return

        try {
            loadHookInfo()
        } catch (t: Throwable) {
            Log.e(t)
            return
        }

        findMethod(carouselActionItemFactoryClassName) {
            name == genCarouselActionItemMethodName
        }.hookAfter { param ->
            if (downloadUrls.isEmpty()) return@hookAfter
            @Suppress("UNCHECKED_CAST") val result = param.result as MutableList<Any>
            val testEnumNone = loadClass(actionTypeEnumClassName).newInstance(
                args("None", 0), argTypes(String::class.java, Int::class.java)
            )
            // ic_vector_incoming.xml
            appContext.addModuleAssetPath()
            val item = loadClass(actionItemViewDataClassName).newInstance(
                args(
                    testEnumNone, appContext.getString(R.string.download_or_copy), getId(
                        "ic_vector_incoming", "drawable"
                    )
                ), argTypes(loadClass(actionTypeEnumClassName), String::class.java, Int::class.java)
            ) ?: return@hookAfter
            val newList = mutableListOf<Any>()
            newList.add(item)
            val newSecondRow = loadClassOrNull(carouselViewDataClassName)?.let {
                if (it.constructors[0].parameterTypes.size == 2) {
                    it.newInstance(
                        args(newList.toList(), true),
                        argTypes(List::class.java, Boolean::class.java)
                    )
                } else {
                    it.newInstance(
                        args(newList.toList()), argTypes(List::class.java)
                    )
                }
            } ?: return@hookAfter
            result.add(1, newSecondRow)
        }

        findMethod(actionItemViewOnClickClassName) {
            name == actionItemViewOnClickMethodName
        }.hookAfter { param ->
            val viewData = param.thisObject.getObjectOrNull(viewDataFieldName)
            val actionType = viewData?.getObjectOrNull(actionTypeFieldName)
            if (actionType.toString() != "None") return@hookAfter
            try {
                currentActivity.get()?.let {
                    DownloadDialog(it, downloadUrls).show()
                }
            } catch (t: Throwable) {
                Log.e(t)
            }
        }


        findMethod(shareMenuClassName) {
            name == shareMenuMethodName
        }.hookBefore { param ->
            val event = param.args[1]
            // share_menu_click
            // share_menu_cancel
            if (event == "share_menu_cancel") {
                downloadUrls = listOf()
                return@hookBefore
            }
            if (event != "share_menu_click") return@hookBefore
            val tweetResult = param.args[2]
            val media =
                tweetResult?.getObjectOrNull(tweetResultFieldName)?.getObjectOrNull(resultFieldName)
                    ?.getObjectOrNull(legacyFieldName)?.getObjectOrNull(extendedEntitiesFieldName)
                    ?.getObjectOrNull(mediaFieldName) as List<*>
            val urls = arrayListOf<String>()
            media.forEach { m ->
                when (m?.getObjectOrNull(mediaTypeFieldName).toString()) {
                    "IMAGE" -> {
                        val mediaUrlHttps = m?.getObjectOrNull(mediaUrlHttpsFieldName) as String
                        urls.add(genOrigUrl(mediaUrlHttps))
                    }
                    "VIDEO", "ANIMATED_GIF" -> {
                        val variants = m?.getObjectOrNull(mediaInfoFieldName)
                            ?.getObjectOrNull(variantsFieldName) as List<*>
                        // a - bitrate
                        // b - url
                        // c - contentType
                        variants.sortedByDescending { v ->
                            v?.getObjectOrNull("a") as Int
                        }[0]?.let {
                            val url = it.getObjectOrNull("b") as String
                            urls.add(clearUrlQueries(url))
                        }
                    }
                }
            }
            downloadUrls = urls
        }
    }

    private fun loadCachedHookInfo() {
        carouselActionItemFactoryClassName =
            modulePrefs.getString("hook_carousel_action_item_factory_class", null)
                ?: throw Throwable("cached hook not found")
        genCarouselActionItemMethodName =
            modulePrefs.getString("hook_gen_carousel_action_item_method", null)
                ?: throw Throwable("cached hook not found")
        actionItemViewDataClassName =
            modulePrefs.getString("hook_action_item_view_data_class", null)
                ?: throw Throwable("cached hook not found")

        actionTypeEnumClassName = modulePrefs.getString("hook_action_type_enum_class", null)
            ?: throw Throwable("cached hook not found")
        carouselViewDataClassName = modulePrefs.getString("hook_carousel_view_data_class", null)
            ?: throw Throwable("cached hook not found")

        actionItemViewOnClickClassName =
            modulePrefs.getString("hook_action_item_view_on_click_class", null)
                ?: throw Throwable("cached hook not found")
        actionItemViewOnClickMethodName =
            modulePrefs.getString("hook_action_item_view_on_click_method", null) ?: throw Throwable(
                "cached hook not found"
            )
        viewDataFieldName = modulePrefs.getString("hook_view_data_field", null)
            ?: throw Throwable("cached hook not found")
        actionTypeFieldName = modulePrefs.getString("hook_action_type_field", null)
            ?: throw Throwable("cached hook not found")

        shareMenuClassName = modulePrefs.getString("hook_share_menu_class", null)
            ?: throw Throwable("cached hook not found")
        shareMenuMethodName = modulePrefs.getString("hook_share_menu_method", null)
            ?: throw Throwable("cached hook not found")
        tweetResultFieldName = modulePrefs.getString("hook_tweet_result_field", null)
            ?: throw Throwable("cached hook not found")
        resultFieldName = modulePrefs.getString("hook_result_field", null)
            ?: throw Throwable("cached hook not found")
        legacyFieldName = modulePrefs.getString("hook_legacy_field", null)
            ?: throw Throwable("cached hook not found")
        extendedEntitiesFieldName = modulePrefs.getString("hook_extended_entities_field", null)
            ?: throw Throwable("cached hook not found")
        mediaFieldName = modulePrefs.getString("hook_media_field", null)
            ?: throw Throwable("cached hook not found")
        mediaTypeFieldName = modulePrefs.getString("hook_media_type_field", null)
            ?: throw Throwable("cached hook not found")
        mediaUrlHttpsFieldName = modulePrefs.getString("hook_media_url_https_field", null)
            ?: throw Throwable("cached hook not found")
        mediaInfoFieldName = modulePrefs.getString("hook_media_info_field", null)
            ?: throw Throwable("cached hook not found")
        variantsFieldName = modulePrefs.getString("hook_variants_field", null)
            ?: throw Throwable("cached hook not found")
    }

    private fun saveHookInfo() {
        modulePrefs.edit().let {
            it.putString(
                "hook_carousel_action_item_factory_class", carouselActionItemFactoryClassName
            )
            it.putString("hook_gen_carousel_action_item_method", genCarouselActionItemMethodName)
            it.putString("hook_action_item_view_data_class", actionItemViewDataClassName)

            it.putString("hook_action_type_enum_class", actionTypeEnumClassName)
            it.putString("hook_carousel_view_data_class", carouselViewDataClassName)

            it.putString("hook_action_item_view_on_click_class", actionItemViewOnClickClassName)
            it.putString("hook_action_item_view_on_click_method", actionItemViewOnClickMethodName)
            it.putString("hook_view_data_field", viewDataFieldName)
            it.putString("hook_action_type_field", actionTypeFieldName)

            it.putString("hook_share_menu_class", shareMenuClassName)
            it.putString("hook_share_menu_method", shareMenuMethodName)
            it.putString("hook_tweet_result_field", tweetResultFieldName)
            it.putString("hook_result_field", resultFieldName)
            it.putString("hook_legacy_field", legacyFieldName)
            it.putString("hook_extended_entities_field", extendedEntitiesFieldName)
            it.putString("hook_media_field", mediaFieldName)
            it.putString("hook_media_type_field", mediaTypeFieldName)
            it.putString("hook_media_url_https_field", mediaUrlHttpsFieldName)
            it.putString("hook_media_info_field", mediaInfoFieldName)
            it.putString("hook_variants_field", variantsFieldName)
        }.apply()
    }


    private fun searchHook() {
        val genCarouselActionItemMethod = dexHelper.findMethodUsingString(
            "viewOptions.actionItems",
            false,
            -1,
            0,
            null,
            -1,
            null,
            null,
            null,
            true,
        ).firstOrNull()?.let { dexHelper.decodeMethodIndex(it) } ?: throw NoSuchMethodError()
        val carouselActionItemFactoryClass = genCarouselActionItemMethod.declaringClass

        val actionItemViewDataClass = dexHelper.findMethodUsingString(
            "ActionItemViewData(actionType=",
            false,
            dexHelper.encodeClassIndex(String::class.java),
            0,
            null,
            -1,
            null,
            null,
            null,
            true,
        ).firstOrNull()?.let { dexHelper.decodeMethodIndex(it)?.declaringClass }
            ?: throw ClassNotFoundException()
        val carouselViewDataClass = dexHelper.findMethodUsingString(
            "CarouselViewData(items=",
            false,
            dexHelper.encodeClassIndex(String::class.java),
            0,
            null,
            -1,
            null,
            null,
            null,
            true,
        ).firstOrNull()?.let { dexHelper.decodeMethodIndex(it)?.declaringClass }
            ?: throw ClassNotFoundException()
        val actionTypeEnumClass = dexHelper.findMethodUsingString(
            "AddRemoveFromList",
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

        carouselActionItemFactoryClassName = carouselActionItemFactoryClass.name
        genCarouselActionItemMethodName = genCarouselActionItemMethod.name
        actionItemViewDataClassName = actionItemViewDataClass.name

        actionTypeEnumClassName = actionTypeEnumClass.name
        carouselViewDataClassName = carouselViewDataClass.name


        val actionItemViewMethodIndex = dexHelper.findMethodUsingString(
            "Type not supported in share carousel: ",
            false,
            dexHelper.encodeClassIndex(Void.TYPE),
            3,
            null,
            -1,
            null,
            null,
            null,
            true,
        ).firstOrNull() ?: throw NoSuchMethodError()
        val actionItemViewOnClickConstructor = dexHelper.findMethodInvoking(
            actionItemViewMethodIndex,
            dexHelper.encodeClassIndex(Void.TYPE),
            -1,
            null,
            -1,
            null,
            longArrayOf(
                dexHelper.encodeClassIndex(Int::class.java)
            ),
            null,
            false,
        ).firstOrNull {
            dexHelper.decodeMethodIndex(it) is Constructor<*>
        }?.let { dexHelper.decodeMethodIndex(it) } ?: throw NoSuchMethodError()
        val actionItemViewOnClickMethod =
            (actionItemViewOnClickConstructor as Constructor<*>).declaringClass.declaredMethods.firstOrNull { it.name == "onClick" } as Method
        val viewDataField =
            actionItemViewOnClickConstructor.declaringClass.declaredFields.filter { it.type != Int::class.java }[1]
                ?: throw NoSuchFieldError()
        val actionTypeField =
            actionItemViewDataClass.declaredFields.lastOrNull { it.type == actionTypeEnumClass }
                ?: throw NoSuchFieldError()

        actionItemViewOnClickClassName = actionItemViewOnClickConstructor.declaringClass.name
        actionItemViewOnClickMethodName = actionItemViewOnClickMethod.name
        viewDataFieldName = viewDataField.name
        actionTypeFieldName = actionTypeField.name

        val shareMenuClass = ezXClassLoader.getAllClassesList().filter {
            val clazz = loadClassOrNull(it) ?: return@filter false
            try {
                return@filter (clazz.constructors.any { c ->
                    c.parameterTypes.size >= 15 && c.parameterTypes[1] == loadClass("androidx.fragment.app.Fragment") && c.parameterTypes[14] == loadClass(
                        "com.twitter.util.user.UserIdentifier"
                    )
                })
            } catch (_: Throwable) {
                return@filter false
            }
        }.firstOrNull()?.let { loadClassOrNull(it) } ?: throw ClassNotFoundException()
        val shareMenuMethod = shareMenuClass.declaredMethods.firstOrNull { m ->
            m.returnType == Void.TYPE && m.parameterTypes.size == 4 && m.parameterTypes[0] == String::class.java && m.parameterTypes[1] == String::class.java
        } ?: throw NoSuchMethodError()
        val tweetResultField = shareMenuMethod.parameterTypes[2].declaredFields.firstOrNull { f ->
            f.isPublic && f.isFinal && f.type.declaredFields.any { it.type == loadClassOrNull("com.twitter.model.vibe.Vibe") }
        } ?: throw NoSuchFieldError()
        val resultField = tweetResultField.type.declaredFields.groupBy { it.type }
            .filter { it.value.size == 2 && it.key.declaredFields.size == 3 }.map { it.value[1] }[0]
            ?: throw NoSuchFieldError()
        val legacyField =
            resultField.type.declaredFields.filter { it.isNotStatic }.sortedBy { it.name }
                .lastOrNull() ?: throw NoSuchFieldError()
        val extendedEntitiesField =
            legacyField.type.declaredFields.filter { it.isNotStatic }.sortedBy { it.name }
                .lastOrNull() ?: throw NoSuchFieldError()
        val mediaField =
            extendedEntitiesField.type.superclass.declaredFields.firstOrNull { it.type == List::class.java }
                ?: throw NoSuchFieldError()

        val mediaTypeEnumClass = dexHelper.findMethodUsingString(
            "MODEL3D",
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
        val perMediaClass = loadClassOrNull(mediaTypeEnumClass.name.split("$")[0])
        val mediaTypeField =
            perMediaClass?.declaredFields?.firstOrNull { it.type == mediaTypeEnumClass }
                ?: throw NoSuchFieldError()
        val mediaUrlHttpsField =
            perMediaClass.declaredFields.firstOrNull { it.isNotStatic && it.type == String::class.java }
                ?: throw NoSuchFieldError()
        val mediaInfoField = perMediaClass.declaredFields.firstOrNull { f ->
            f.type.declaredFields.size == 4 && f.type.declaredFields.filter { f2 -> f2.type == Float::class.java }.size == 2 && f.type.declaredFields.filter { f2 -> f2.type == List::class.java }.size == 1
        } ?: throw NoSuchFieldError()
        val variantsField =
            mediaInfoField.type?.declaredFields?.firstOrNull { it.type == List::class.java }
                ?: throw NoSuchFieldError()

        shareMenuClassName = shareMenuClass.name
        shareMenuMethodName = shareMenuMethod.name
        tweetResultFieldName = tweetResultField.name
        resultFieldName = resultField.name
        legacyFieldName = legacyField.name
        extendedEntitiesFieldName = extendedEntitiesField.name
        mediaFieldName = mediaField.name
        mediaTypeFieldName = mediaTypeField.name
        mediaUrlHttpsFieldName = mediaUrlHttpsField.name
        mediaInfoFieldName = mediaInfoField.name
        variantsFieldName = variantsField.name
    }

    private fun loadHookInfo() {
        val hookDownloadLastUpdate = modulePrefs.getLong("hook_download_last_update", 0)

        @Suppress("DEPRECATION") val appLastUpdateTime =
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).lastUpdateTime
        val moduleLastUpdate = File(modulePath).lastModified()

        Log.d("hookDownloadLastUpdate: $hookDownloadLastUpdate, appLastUpdateTime: $appLastUpdateTime, moduleLastUpdate: $moduleLastUpdate")

        val timeStart = System.currentTimeMillis()

        if (hookDownloadLastUpdate > appLastUpdateTime && hookDownloadLastUpdate > moduleLastUpdate) {
            loadCachedHookInfo()
            Log.d("Download Hook load time: ${System.currentTimeMillis() - timeStart} ms")
        } else {
            loadDexHelper()
            searchHook()
            Log.d("Download Hook search time: ${System.currentTimeMillis() - timeStart} ms")
            saveHookInfo()
            modulePrefs.edit().putLong("hook_download_last_update", System.currentTimeMillis())
                .apply()
        }
    }
}
