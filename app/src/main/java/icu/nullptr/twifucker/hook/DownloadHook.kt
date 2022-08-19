package icu.nullptr.twifucker.hook

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.init.InitFields.modulePath
import com.github.kyuubiran.ezxhelper.utils.*
import icu.nullptr.twifucker.R
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexHelper
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexHelper
import icu.nullptr.twifucker.ui.DownloadDialog
import java.io.File
import java.io.FileOutputStream
import java.lang.reflect.Method

data class VideoVariant(val bitrate: Int, val url: String, val contentType: String)

private var urlPhotos: List<String> = listOf()
private var urlVideos: List<VideoVariant> = listOf()

@SuppressLint("StaticFieldLeak")
lateinit var currentActivity: Activity


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


private fun loadCachedHookInfo() {
    carouselActionItemFactoryClassName =
        modulePrefs.getString("hook_carousel_action_item_factory_class", null)
            ?: throw Throwable("cached hook not found")
    genCarouselActionItemMethodName =
        modulePrefs.getString("hook_gen_carousel_action_item_method", null)
            ?: throw Throwable("cached hook not found")
    actionItemViewDataClassName = modulePrefs.getString("hook_action_item_view_data_class", null)
        ?: throw Throwable("cached hook not found")

    actionTypeEnumClassName = modulePrefs.getString("hook_action_type_enum_class", null)
        ?: throw Throwable("cached hook not found")
    carouselViewDataClassName = modulePrefs.getString("hook_carousel_view_data_class", null)
        ?: throw Throwable("cached hook not found")

    actionItemViewOnClickClassName =
        modulePrefs.getString("hook_action_item_view_on_click_class", null)
            ?: throw Throwable("cached hook not found")
    actionItemViewOnClickMethodName =
        modulePrefs.getString("hook_action_item_view_on_click_method", null)
            ?: throw Throwable("cached hook not found")
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
    resultFieldName =
        modulePrefs.getString("hook_result_field", null) ?: throw Throwable("cached hook not found")
    legacyFieldName =
        modulePrefs.getString("hook_legacy_field", null) ?: throw Throwable("cached hook not found")
    extendedEntitiesFieldName = modulePrefs.getString("hook_extended_entities_field", null)
        ?: throw Throwable("cached hook not found")
    mediaFieldName =
        modulePrefs.getString("hook_media_field", null) ?: throw Throwable("cached hook not found")
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
        it.putString("hook_carousel_action_item_factory_class", carouselActionItemFactoryClassName)
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
    val carouselActionItemFactoryClass = dexHelper.findMethodUsingString(
        "carouselActionItemFactory",
        false,
        dexHelper.encodeClassIndex(Void.TYPE),
        5,
        null,
        -1,
        null,
        null,
        null,
        true,
    ).firstOrNull()?.let { dexHelper.decodeMethodIndex(it)?.declaringClass }
        ?: throw NoSuchMethodError()
    val genCarouselActionItemMethod =
        carouselActionItemFactoryClass.declaredMethods.firstOrNull { m ->
            m.isPrivate && m.isFinal && m.parameterTypes.isEmpty() && m.returnType == List::class.java
        } ?: throw NoSuchMethodError()

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

    val viewDataClass = loadClassOrNull(actionItemViewDataClass.name.split("$")[0]) ?: return

    // carouselActionItemFactoryClass
    carouselActionItemFactoryClassName = carouselActionItemFactoryClass.name
    genCarouselActionItemMethodName = genCarouselActionItemMethod.name
    actionItemViewDataClassName = actionItemViewDataClass.name

    // actionTypeEnumClass
    // carouselViewDataClass
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
        longArrayOf(dexHelper.encodeClassIndex(Int::class.java)),
        null,
        true,
    ).firstOrNull() ?: throw NoSuchMethodError()
    val actionItemViewMethod = dexHelper.decodeMethodIndex(actionItemViewMethodIndex) as Method
    val actionItemViewOnClickConstructor = dexHelper.findMethodInvoking(
        actionItemViewMethodIndex,
        dexHelper.encodeClassIndex(Void.TYPE),
        3,
        null,
        -1,
        longArrayOf(
            dexHelper.encodeClassIndex(actionItemViewMethod.parameterTypes[1]),
            dexHelper.encodeClassIndex(actionItemViewMethod.parameterTypes[0]),
            dexHelper.encodeClassIndex(Int::class.java)
        ),
        null,
        null,
        true,
    ).firstOrNull()?.let { dexHelper.decodeMethodIndex(it) } ?: throw NoSuchMethodError()
    val actionItemViewOnClickMethod =
        actionItemViewOnClickConstructor.declaringClass.declaredMethods.firstOrNull { it.name == "onClick" } as Method
    val viewDataField =
        actionItemViewOnClickConstructor.declaringClass.declaredFields.firstOrNull { it.type == viewDataClass }
            ?: throw NoSuchFieldError()
    val actionTypeField =
        actionItemViewDataClass.declaredFields.firstOrNull { it.type == actionTypeEnumClass }
            ?: throw NoSuchFieldError()

    // actionItemViewOnClickMethod
    // viewDataField
    // actionTypeField
    actionItemViewOnClickClassName = actionItemViewOnClickConstructor.declaringClass.name
    actionItemViewOnClickMethodName = actionItemViewOnClickMethod.name
    viewDataFieldName = viewDataField.name
    actionTypeFieldName = actionTypeField.name


    val shareMenuRefMethodIndex = dexHelper.findMethodUsingString(
        "Unhandled QuoteView Long Click Choice:",
        false,
        dexHelper.encodeClassIndex(Void.TYPE),
        9,
        null,
        -1,
        null,
        null,
        null,
        true,
    ).firstOrNull() ?: return
    val shareMenuClass =
        dexHelper.decodeMethodIndex(shareMenuRefMethodIndex)?.declaringClass ?: return
    val shareMenuMethod = shareMenuClass.declaredMethods.firstOrNull { m ->
        m.returnType == Void.TYPE && m.isPrivate && m.parameterTypes.size == 4 && m.parameterTypes[0] == String::class.java && m.parameterTypes[1] == String::class.java
    } ?: return

    val tweetResultField = shareMenuMethod.parameterTypes[2].declaredFields.firstOrNull { f ->
        f.isPublic && f.isFinal && f.type.declaredFields.any { it.type == loadClassOrNull("com.twitter.model.vibe.Vibe") }
    } ?: return
    val resultField = tweetResultField.type.declaredFields.lastOrNull { f ->
        f.isPublic && f.isFinal && f.type.declaredFields.size == 3 && f.type.declaredFields.filter { it.isFinal }.size == 3 && f.type.declaredFields.filter { it.isStatic && it.isFinal && it.isPublic }.size == 2
    } ?: return
    val legacyField =
        resultField.type.declaredFields.firstOrNull { it.isPrivate && it.isFinal } ?: return
    val extendedEntitiesField =
        legacyField.type.declaredFields.firstOrNull { it.isPrivate && it.isFinal } ?: return
    val mediaField =
        extendedEntitiesField.type.superclass.declaredFields.firstOrNull { it.isPrivate && it.isFinal && it.type == List::class.java }
            ?: return

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
    ).firstOrNull()?.let { dexHelper.decodeMethodIndex(it)?.declaringClass } ?: return
    val perMediaClass = loadClassOrNull(mediaTypeEnumClass.name.split("$")[0])
    val mediaTypeField =
        perMediaClass?.declaredFields?.firstOrNull { it.type == mediaTypeEnumClass } ?: return
    val mediaUrlHttpsField =
        perMediaClass.declaredFields.lastOrNull { it.type == String::class.java } ?: return
    val mediaInfoField = perMediaClass.declaredFields.firstOrNull { f ->
        f.type.declaredFields.size == 4 && f.type.declaredFields.filter { f2 -> f2.type == Float::class.java }.size == 2 && f.type.declaredFields.filter { f2 -> f2.type == List::class.java }.size == 1
    } ?: return
    val variantsField =
        mediaInfoField.type?.declaredFields?.firstOrNull { it.type == List::class.java } ?: return


    // shareMenuMethod
    // tweetResultField
    // resultField
    // legacyField
    // extendedEntitiesField
    // mediaField
    // mediaInfoField
    // variantsField
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
    val hookLastUpdate = modulePrefs.getLong("hook_last_update", 0)

    @Suppress("DEPRECATION") val appLastUpdateTime =
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).lastUpdateTime
    val moduleLastUpdate = File(modulePath).lastModified()

    Log.d("hookLastUpdate: $hookLastUpdate, appLastUpdateTime: $appLastUpdateTime, moduleLastUpdate: $moduleLastUpdate")

    val timeStart = System.currentTimeMillis()

    if (hookLastUpdate > appLastUpdateTime && hookLastUpdate > moduleLastUpdate) {
        loadCachedHookInfo()
        Log.d("Hook load time: ${System.currentTimeMillis() - timeStart} ms")
    } else {
        loadDexHelper()
        searchHook()
        Log.d("Hook search time: ${System.currentTimeMillis() - timeStart} ms")
        saveHookInfo()
        modulePrefs.edit().putLong("hook_last_update", System.currentTimeMillis()).apply()
    }

}

private fun startHook() {
    try {
        loadHookInfo()
    } catch (t: Throwable) {
        Log.e(t)
        return
    }

    findMethod(carouselActionItemFactoryClassName) {
        name == genCarouselActionItemMethodName
    }.hookAfter { param ->
        if (urlPhotos.isEmpty() && urlVideos.isEmpty()) return@hookAfter
        val result = param.result as MutableList<Any>
        val testEnumNone = loadClass(actionTypeEnumClassName).newInstance(
            args("None", 0), argTypes(String::class.java, Int::class.java)
        )
        // ic_vector_incoming.xml
        appContext.addModuleAssetPath()
        val item = loadClass(actionItemViewDataClassName).newInstance(
            args(
                testEnumNone,
                appContext.getString(R.string.download_or_copy),
                appContext.resources.getIdentifier(
                    "ic_vector_incoming", "drawable", appContext.packageName
                )
            ), argTypes(loadClass(actionTypeEnumClassName), String::class.java, Int::class.java)
        ) ?: return@hookAfter
        val newList = mutableListOf<Any>()
        newList.add(item)
        val newSecondRow = loadClass(carouselViewDataClassName).newInstance(
            args(newList.toList(), true), argTypes(List::class.java, Boolean::class.java)
        ) ?: return@hookAfter
        result.add(1, newSecondRow)
    }

    findMethod(actionItemViewOnClickClassName) {
        name == actionItemViewOnClickMethodName
    }.hookAfter { param ->
        val viewData = param.thisObject.getObjectOrNull(viewDataFieldName)
        val actionType = viewData?.getObjectOrNull(actionTypeFieldName)
        if (actionType.toString() != "None") return@hookAfter
        try {
            DownloadDialog(currentActivity, urlPhotos, urlVideos).show()
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
            urlPhotos = listOf()
            urlVideos = listOf()
            return@hookBefore
        }
        if (event != "share_menu_click") return@hookBefore
        val tweetResult = param.args[2]
        val media =
            tweetResult?.getObjectOrNull(tweetResultFieldName)?.getObjectOrNull(resultFieldName)
                ?.getObjectOrNull(legacyFieldName)?.getObjectOrNull(extendedEntitiesFieldName)
                ?.getObjectOrNull(mediaFieldName) as List<*>
        val photoList = arrayListOf<String>()
        val videoList = arrayListOf<VideoVariant>()
        media.forEach { m ->
            when (m?.getObjectOrNull(mediaTypeFieldName).toString()) {
                "IMAGE" -> {
                    photoList.add(m?.getObjectOrNull(mediaUrlHttpsFieldName) as String)
                }
                "VIDEO" -> {
                    val variants = m?.getObjectOrNull(mediaInfoFieldName)
                        ?.getObjectOrNull(variantsFieldName) as List<*>
                    variants.sortedByDescending { v ->
                        v?.getObjectOrNull("a") as Int
                    }.forEach { v ->
                        videoList.add(
                            VideoVariant(
                                v?.getObjectOrNull("a") as Int,
                                v.getObjectOrNull("b") as String,
                                v.getObjectOrNull("c") as String,
                            )
                        )
                    }
                }
            }
        }
        urlPhotos = photoList
        urlVideos = videoList
    }
}

fun downloadHook() {
    if (!modulePrefs.getBoolean("enable_download_hook", false)) return

    findMethod(Activity::class.java) {
        name == "onResume"
    }.hookAfter { param ->
        currentActivity = param.thisObject as Activity
    }
    findMethod(Activity::class.java) {
        name == "onActivityResult"
    }.hookAfter { param ->
        val requestCode = param.args[0] as Int
        val resultCode = param.args[1] as Int
        val resultData = param.args[2] as Intent?
        if (requestCode != DownloadDialog.CREATE_FILE && resultCode != Activity.RESULT_OK) return@hookAfter
        resultData?.data?.also { uri ->
            val contentResolver = (param.thisObject as Activity).contentResolver
            try {
                contentResolver.openFileDescriptor(uri, "w")?.use {
                    val inputStream =
                        File(appContext.cacheDir, DownloadDialog.lastSelectedFile).inputStream()
                    val outputStream = contentResolver.openOutputStream(uri) ?: return@use
                    inputStream.copyTo(outputStream)
                }
            } catch (t: Throwable) {
                Log.e(t)
            }
        }
    }
    startHook()
}