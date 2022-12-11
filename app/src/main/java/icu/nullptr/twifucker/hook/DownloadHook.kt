package icu.nullptr.twifucker.hook

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.init.InitFields.ezXClassLoader
import com.github.kyuubiran.ezxhelper.utils.*
import icu.nullptr.twifucker.*
import icu.nullptr.twifucker.hook.HookEntry.Companion.currentActivity
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexHelper
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexHelper
import icu.nullptr.twifucker.ui.DownloadDialog


object DownloadHook : BaseHook() {
    private var downloadUrls: List<String> = listOf()

    // tweet share download button
    private lateinit var tweetShareClassName: String
    private lateinit var tweetShareShowMethodName: String
    private lateinit var tweetShareShareListFieldName: String

    private lateinit var actionEnumWrappedClassName: String
    private lateinit var actionEnumWrappedInnerClassName: String
    private lateinit var actionEnumClassName: String

    private lateinit var actionSheetItemClassName: String
    private lateinit var actionSheetItemFieldName: String

    // tweet share onClick
    private lateinit var shareTweetOnClickListenerClassName: String
    private lateinit var shareTweetItemAdapterFieldName: String
    private lateinit var shareTweetOnClickListener2ClassName: String
    private lateinit var shareTweetItemAdapter2FieldName: String
    private lateinit var shareTweetOnClickListener3ClassName: String
    private lateinit var shareTweetItemAdapter3FieldName: String
    private lateinit var actionItemViewDataFieldName: String

    // protected tweet share onClick
    private lateinit var protectedShareTweetItemAdapterClassName: String
    private lateinit var protectedShareTweetItemAdapterClassTitleFieldName: String

    // share menu
    private lateinit var shareMenuClassName: String
    private lateinit var shareMenuMethodName: String

    // tweet object
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

        appContext.addModuleAssetPath()

        // normal tweet
        shareTweetOnClickListenerClassName.let { className ->
            if (className.isEmpty()) return@let
            findMethod(className) { name == "onClick" }.hookBefore {
                if (downloadUrls.isEmpty()) return@hookBefore
                val actionItemViewData =
                    it.thisObject.getObjectOrNull(shareTweetItemAdapterFieldName)
                        ?.getObjectOrNull(actionItemViewDataFieldName)
                // a - actionType
                // b - title
                // c - iconRes
                if (actionItemViewData?.getObjectOrNull("b") != appContext.getString(R.string.download_or_copy)) return@hookBefore

                try {
                    currentActivity.get()?.let { act ->
                        DownloadDialog(act, downloadUrls) {
                            downloadUrls = listOf()
                        }.show()
                    }
                } catch (t: Throwable) {
                    Log.e(t)
                }
            }
        }
        shareTweetOnClickListener2ClassName.let { className ->
            if (className.isEmpty()) return@let
            findMethod(className) { name == "onClick" }.hookBefore {
                if (downloadUrls.isEmpty()) return@hookBefore
                val actionItemViewData =
                    it.thisObject.getObjectOrNull(shareTweetItemAdapter2FieldName)
                        ?.getObjectOrNull(actionItemViewDataFieldName)
                // a - actionType
                // b - title
                // c - iconRes
                if (actionItemViewData?.getObjectOrNull("b") != appContext.getString(R.string.download_or_copy)) return@hookBefore

                try {
                    currentActivity.get()?.let { act ->
                        DownloadDialog(act, downloadUrls) {
                            downloadUrls = listOf()
                        }.show()
                    }
                } catch (t: Throwable) {
                    Log.e(t)
                }
            }
        }
        shareTweetOnClickListener3ClassName.let { className ->
            if (className.isEmpty()) return@let
            findMethod(className) { name == "onClick" }.hookBefore {
                if (downloadUrls.isEmpty()) return@hookBefore
                val actionItemViewData =
                    it.thisObject.getObjectOrNull(shareTweetItemAdapter3FieldName)
                        ?.getObjectOrNull(actionItemViewDataFieldName)
                // a - actionType
                // b - title
                // c - iconRes
                if (actionItemViewData?.getObjectOrNull("b") != appContext.getString(R.string.download_or_copy)) return@hookBefore

                try {
                    currentActivity.get()?.let { act ->
                        DownloadDialog(act, downloadUrls) {
                            downloadUrls = listOf()
                        }.show()
                    }
                } catch (t: Throwable) {
                    Log.e(t)
                }
            }
        }

        // protected tweet
        findMethod(protectedShareTweetItemAdapterClassName) { name == "onClick" }.hookBefore {
            if (downloadUrls.isEmpty()) return@hookBefore
            val protectedShareTweetItemAdapterTitleTextView =
                it.thisObject.getObjectOrNull(protectedShareTweetItemAdapterClassTitleFieldName) as TextView
            if (protectedShareTweetItemAdapterTitleTextView.text != appContext.getString(R.string.download_or_copy)) return@hookBefore

            try {
                currentActivity.get()?.let { act ->
                    DownloadDialog(act, downloadUrls) {
                        downloadUrls = listOf()
                    }.show()
                }
            } catch (t: Throwable) {
                Log.e(t)
            }
        }

        findMethod(tweetShareClassName) { name == tweetShareShowMethodName }.hookBefore {
            val shareList = it.thisObject.getObjectAs<List<*>>(tweetShareShareListFieldName)

            val mutList = shareList.toMutableList()

            val actionEnumWrappedClass = loadClass(actionEnumWrappedInnerClassName)
            val actionEnumClass = loadClass(actionEnumClassName)
            val actionSheetItemClass = loadClass(actionSheetItemClassName)
            val actionEnumWrapped = actionEnumWrappedClass.newInstance(
                args(
                    actionEnumClass.invokeStaticMethod(
                        "valueOf", args("None"), argTypes(String::class.java)
                    ), ""
                ), argTypes(actionEnumClass, String::class.java)
            )
            // drawableRes, actionId, title
            actionEnumWrapped?.putObject(
                actionSheetItemFieldName, actionSheetItemClass.newInstance(
                    args(
                        getId("ic_vector_incoming", "drawable"),
                        0,
                        appContext.getString(R.string.download_or_copy)
                    ), argTypes(Int::class.java, Int::class.java, String::class.java)
                )
            )

            mutList.add(
                loadClass(actionEnumWrappedClassName).newInstance(
                    args(actionEnumWrapped), argTypes(actionEnumWrappedClass)
                )
            )

            it.thisObject.putObject(tweetShareShareListFieldName, mutList.toList())
        }

        // share menu
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
        // tweet share download button
        tweetShareClassName = modulePrefs.getString("hook_tweet_share_class", null)
            ?: throw Throwable("cached hook not found")
        tweetShareShowMethodName = modulePrefs.getString("hook_tweet_share_show_method", null)
            ?: throw Throwable("cached hook not found")
        tweetShareShareListFieldName = modulePrefs.getString("hook_tweet_share_list_field", null)
            ?: throw Throwable("cached hook not found")

        actionEnumWrappedClassName = modulePrefs.getString("hook_action_enum_wrapped_class", null)
            ?: throw Throwable("cached hook not found")
        actionEnumWrappedInnerClassName =
            modulePrefs.getString("hook_action_enum_wrapped_inner_class", null)
                ?: throw Throwable("cached hook not found")
        actionEnumClassName = modulePrefs.getString("hook_action_enum_class", null)
            ?: throw Throwable("cached hook not found")

        actionSheetItemClassName = modulePrefs.getString("hook_action_sheet_item_class", null)
            ?: throw Throwable("cached hook not found")
        actionSheetItemFieldName = modulePrefs.getString("hook_action_sheet_item_field", null)
            ?: throw Throwable("cached hook not found")

        // tweet share onClick
        shareTweetOnClickListenerClassName =
            modulePrefs.getString("hook_share_tweet_on_click_listener_class", null)
                ?: throw Throwable("cached hook not found")
        shareTweetItemAdapterFieldName =
            modulePrefs.getString("hook_share_tweet_item_adapter_field", null)
                ?: throw Throwable("cached hook not found")
        shareTweetOnClickListener2ClassName =
            modulePrefs.getString("hook_share_tweet_on_click_listener_2_class", null)
                ?: throw Throwable("cached hook not found")
        shareTweetItemAdapter2FieldName =
            modulePrefs.getString("hook_share_tweet_item_adapter_2_field", null)
                ?: throw Throwable("cached hook not found")
        shareTweetOnClickListener3ClassName =
            modulePrefs.getString("hook_share_tweet_on_click_listener_3_class", null)
                ?: throw Throwable("cached hook not found")
        shareTweetItemAdapter3FieldName =
            modulePrefs.getString("hook_share_tweet_item_adapter_3_field", null)
                ?: throw Throwable("cached hook not found")
        actionItemViewDataFieldName =
            modulePrefs.getString("hook_action_item_view_data_field", null)
                ?: throw Throwable("cached hook not found")

        // protected tweet share onClick
        protectedShareTweetItemAdapterClassName =
            modulePrefs.getString("hook_protected_share_item_adapter_class", null)
                ?: throw Throwable("cached hook not found")
        protectedShareTweetItemAdapterClassTitleFieldName =
            modulePrefs.getString("hook_protected_share_tweet_item_adapter_class_title_field", null)
                ?: throw Throwable("cached hook not found")

        // share menu
        shareMenuClassName = modulePrefs.getString("hook_share_menu_class", null)
            ?: throw Throwable("cached hook not found")
        shareMenuMethodName = modulePrefs.getString("hook_share_menu_method", null)
            ?: throw Throwable("cached hook not found")

        // tweet object
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
            // tweet share download button
            it.putString("hook_tweet_share_class", tweetShareClassName)
            it.putString("hook_tweet_share_show_method", tweetShareShowMethodName)
            it.putString("hook_tweet_share_list_field", tweetShareShareListFieldName)

            it.putString("hook_action_enum_wrapped_class", actionEnumWrappedClassName)
            it.putString("hook_action_enum_wrapped_inner_class", actionEnumWrappedInnerClassName)
            it.putString("hook_action_enum_class", actionEnumClassName)

            it.putString("hook_action_sheet_item_class", actionSheetItemClassName)
            it.putString("hook_action_sheet_item_field", actionSheetItemFieldName)

            // tweet share onClick
            it.putString(
                "hook_share_tweet_on_click_listener_class", shareTweetOnClickListenerClassName
            )
            it.putString("hook_share_tweet_item_adapter_field", shareTweetItemAdapterFieldName)
            it.putString(
                "hook_share_tweet_on_click_listener_2_class", shareTweetOnClickListener2ClassName
            )
            it.putString("hook_share_tweet_item_adapter_2_field", shareTweetItemAdapter2FieldName)
            it.putString(
                "hook_share_tweet_on_click_listener_3_class", shareTweetOnClickListener3ClassName
            )
            it.putString("hook_share_tweet_item_adapter_3_field", shareTweetItemAdapter3FieldName)
            it.putString("hook_action_item_view_data_field", actionItemViewDataFieldName)

            // protected tweet share onClick
            it.putString(
                "hook_protected_share_item_adapter_class", protectedShareTweetItemAdapterClassName
            )
            it.putString(
                "hook_protected_share_tweet_item_adapter_class_title_field",
                protectedShareTweetItemAdapterClassTitleFieldName
            )

            // share menu
            it.putString("hook_share_menu_class", shareMenuClassName)
            it.putString("hook_share_menu_method", shareMenuMethodName)

            // tweet object
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
        // tweet share download button
        val tweetShareClass = dexHelper.findMethodUsingString(
            "timeline_selected_caret_position",
            false,
            -1,
            2,
            null,
            -1,
            null,
            null,
            null,
            true,
        ).firstOrNull()?.let { dexHelper.decodeMethodIndex(it)?.declaringClass }
            ?: throw NoSuchMethodError()
        val tweetShareShowMethod =
            tweetShareClass.declaredMethods.firstOrNull { m -> m.isPublic && m.isFinal && m.parameterTypes.size == 1 && m.returnType == Void.TYPE }
                ?: throw NoSuchMethodError()
        val tweetShareShareListField =
            tweetShareClass.declaredFields.firstOrNull { f -> f.isPublic && f.isFinal && f.type == List::class.java }
                ?: throw NoSuchFieldError()

        val actionEnumWrappedClassRefMethod =
            tweetShareClass.declaredMethods.firstOrNull { m -> m.isPublic && m.isFinal && m.parameterTypes.size == 4 && m.parameterTypes[1] == String::class.java && m.parameterTypes[2] == Boolean::class.java && m.parameterTypes[3] == String::class.java }
                ?: throw NoSuchMethodError()
        val actionEnumWrappedClass = actionEnumWrappedClassRefMethod.returnType
        val actionEnumWrappedInnerClass = actionEnumWrappedClass.constructors[0].parameterTypes[0]
        val actionEnumClass = actionEnumWrappedClassRefMethod.parameterTypes[0]

        val actionSheetItemClass = dexHelper.findMethodUsingString(
            "ActionSheetItem(drawableRes=",
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
        val actionSheetItemField =
            actionEnumWrappedInnerClass.declaredFields.firstOrNull { f -> f.type == actionSheetItemClass }
                ?: throw NoSuchFieldError()

        tweetShareClassName = tweetShareClass.name
        tweetShareShowMethodName = tweetShareShowMethod.name
        tweetShareShareListFieldName = tweetShareShareListField.name

        actionEnumWrappedClassName = actionEnumWrappedClass.name
        actionEnumWrappedInnerClassName = actionEnumWrappedInnerClass.name
        actionEnumClassName = actionEnumClass.name

        actionSheetItemClassName = actionSheetItemClass.name
        actionSheetItemFieldName = actionSheetItemField.name

        // tweet share onClick
        val shareTweetOnClickListenerClass = dexHelper.findMethodUsingString(
            "profile_modal",
            false,
            dexHelper.encodeClassIndex(Void.TYPE),
            1,
            null,
            -1,
            longArrayOf(dexHelper.encodeClassIndex(View::class.java)),
            null,
            null,
            true,
        ).firstOrNull()?.let { dexHelper.decodeMethodIndex(it)?.declaringClass }
        val shareTweetItemAdapterField =
            shareTweetOnClickListenerClass?.declaredFields?.lastOrNull()
        // twitter alpha 9.69 alpha 4
        val shareTweetOnClickListener2Class = dexHelper.findMethodUsingString(
            "fabContainerView.findViewById(R.id.tweet_label)",
            false,
            dexHelper.encodeClassIndex(Void.TYPE),
            1,
            null,
            -1,
            longArrayOf(dexHelper.encodeClassIndex(View::class.java)),
            null,
            null,
            true,
        ).firstOrNull()?.let {
            dexHelper.decodeMethodIndex(it)?.declaringClass
        }
        val shareTweetItemAdapter2Field =
            shareTweetOnClickListener2Class?.declaredFields?.lastOrNull()
        // twitter alpha 9.69 alpha 9
        val shareTweetOnClickListener3Class = dexHelper.findMethodUsingString(
            "\$onSwitchToggled",
            false,
            dexHelper.encodeClassIndex(Void.TYPE),
            1,
            null,
            -1,
            longArrayOf(dexHelper.encodeClassIndex(View::class.java)),
            null,
            null,
            true,
        ).firstOrNull()?.let {
            dexHelper.decodeMethodIndex(it)?.declaringClass
        }
        val shareTweetItemAdapter3Field =
            shareTweetOnClickListener2Class?.declaredFields?.lastOrNull()

        val shareTweetItemAdapterClass = dexHelper.findMethodUsingString(
            "itemView.findViewById(R.id.action_sheet_item_icon)",
            false,
            dexHelper.encodeClassIndex(Void.TYPE),
            2,
            null,
            -1,
            null,
            longArrayOf(dexHelper.encodeClassIndex(ViewGroup::class.java)),
            null,
            false,
        ).map { dexHelper.decodeMethodIndex(it)?.declaringClass }.firstOrNull {
            it?.declaredFields?.any { f ->
                f.isPublic && f.isNotStatic && f.isNotFinal
            } == true
        } ?: throw ClassNotFoundException()
        val actionItemViewDataField =
            shareTweetItemAdapterClass.declaredFields.firstOrNull { f -> f.isPublic && f.isNotStatic && f.isNotFinal }
                ?: throw NoSuchFieldError()

        if (shareTweetOnClickListenerClass == null && shareTweetOnClickListener2Class == null && shareTweetOnClickListener3Class == null) {
            throw ClassNotFoundException()
        }
        if (shareTweetItemAdapterField == null && shareTweetItemAdapter2Field == null && shareTweetItemAdapter3Field == null) {
            throw NoSuchFieldError()
        }

        shareTweetOnClickListenerClassName = shareTweetOnClickListenerClass?.name ?: ""
        shareTweetItemAdapterFieldName = shareTweetItemAdapterField?.name ?: ""
        shareTweetOnClickListener2ClassName = shareTweetOnClickListener2Class?.name ?: ""
        shareTweetItemAdapter2FieldName = shareTweetItemAdapter2Field?.name ?: ""
        shareTweetOnClickListener3ClassName = shareTweetOnClickListener3Class?.name ?: ""
        shareTweetItemAdapter3FieldName = shareTweetItemAdapter3Field?.name ?: ""
        actionItemViewDataFieldName = actionItemViewDataField.name

        // protected tweet share onClick
        val refMethodIndex = dexHelper.findMethodUsingString(
            "bceHierarchyContext",
            false,
            dexHelper.encodeClassIndex(Void.TYPE),
            2,
            null,
            -1,
            null,
            null,
            null,
            false,
        ).firstOrNull { index ->
            val clazz = dexHelper.decodeMethodIndex(index)?.declaringClass
            clazz?.declaredFields?.any { f -> f.type == View::class.java } ?: false
        } ?: throw NoSuchMethodError()

        val refClass = dexHelper.findMethodInvoking(
            refMethodIndex,
            -1,
            3,
            null,
            -1,
            null,
            longArrayOf(dexHelper.encodeClassIndex(List::class.java)),
            null,
            true,
        ).firstOrNull()?.let { dexHelper.decodeMethodIndex(it)?.declaringClass }
            ?: throw ClassNotFoundException()
        val protectedShareTweetItemAdapterClass =
            refClass.declaredMethods.firstOrNull { m -> m.isPublic && m.parameterTypes.size == 2 && m.parameterTypes[0] == ViewGroup::class.java && m.parameterTypes[1] == Int::class.java }?.returnType
                ?: throw ClassNotFoundException()
        val protectedShareTweetItemAdapterClassTitleField =
            protectedShareTweetItemAdapterClass.declaredFields.firstOrNull { f -> f.type == TextView::class.java }
                ?: throw NoSuchFieldError()

        // protected tweet share onClick
        protectedShareTweetItemAdapterClassName = protectedShareTweetItemAdapterClass.name
        protectedShareTweetItemAdapterClassTitleFieldName =
            protectedShareTweetItemAdapterClassTitleField.name

        // share menu
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
            resultField.type.declaredFields.filter { it.isNotStatic }.maxByOrNull { it.name }
                ?: throw NoSuchFieldError()
        val extendedEntitiesField =
            legacyField.type.declaredFields.filter { it.isNotStatic }.maxByOrNull { it.name }
                ?: throw NoSuchFieldError()
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

        Log.d("hookDownloadLastUpdate: $hookDownloadLastUpdate, hostAppLastUpdate: $hostAppLastUpdate, moduleLastModify: $moduleLastModify")

        val timeStart = System.currentTimeMillis()

        if (hookDownloadLastUpdate > hostAppLastUpdate && hookDownloadLastUpdate > moduleLastModify) {
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
