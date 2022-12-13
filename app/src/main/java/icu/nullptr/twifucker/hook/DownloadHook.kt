package icu.nullptr.twifucker.hook

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.init.InitFields.ezXClassLoader
import com.github.kyuubiran.ezxhelper.utils.*
import icu.nullptr.twifucker.*
import icu.nullptr.twifucker.exceptions.CachedHookNotFound
import icu.nullptr.twifucker.hook.HookEntry.Companion.currentActivity
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexKit
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexKit
import icu.nullptr.twifucker.ui.DownloadDialog


object DownloadHook : BaseHook() {
    private var downloadUrls: List<String> = listOf()

    // tweet share download button
    private const val HOOK_TWEET_SHARE_CLASS = "hook_tweet_share_class"
    private const val HOOK_TWEET_SHARE_SHOW_METHOD = "hook_tweet_share_show_method"
    private const val HOOK_TWEET_SHARE_LIST_FIELD = "hook_tweet_share_list_field"

    private const val HOOK_ACTION_ENUM_WRAPPED_CLASS = "hook_action_enum_wrapped_class"
    private const val HOOK_ACTION_ENUM_WRAPPED_INNER_CLASS = "hook_action_enum_wrapped_inner_class"
    private const val HOOK_ACTION_ENUM_CLASS = "hook_action_enum_class"

    private const val HOOK_ACTION_SHEET_ITEM_CLASS = "hook_action_sheet_item_class"
    private const val HOOK_ACTION_SHEET_ITEM_FIELD = "hook_action_sheet_item_field"

    // tweet share onClick
    private const val HOOK_SHARE_TWEET_ON_CLICK_LISTENER_CLASS =
        "hook_share_tweet_on_click_listener_class"
    private const val HOOK_SHARE_TWEET_ITEM_ADAPTER_FIELD = "hook_share_tweet_item_adapter_field"
    private const val HOOK_SHARE_TWEET_ON_CLICK_LISTENER_2_CLASS =
        "hook_share_tweet_on_click_listener_2_class"
    private const val HOOK_SHARE_TWEET_ITEM_ADAPTER_2_FIELD =
        "hook_share_tweet_item_adapter_2_field"
    private const val HOOK_SHARE_TWEET_ON_CLICK_LISTENER_3_CLASS =
        "hook_share_tweet_on_click_listener_3_class"
    private const val HOOK_SHARE_TWEET_ITEM_ADAPTER_3_FIELD =
        "hook_share_tweet_item_adapter_3_field"
    private const val HOOK_ACTION_ITEM_VIEW_DATA_FIELD = "hook_action_item_view_data_field"

    // protected tweet share onClick
    private const val HOOK_PROTECTED_SHARE_ITEM_ADAPTER_CLASS =
        "hook_protected_share_item_adapter_class"
    private const val HOOK_PROTECTED_SHARE_TWEET_ITEM_ADAPTER_CLASS_TITLE_FIELD =
        "hook_protected_share_tweet_item_adapter_class_title_field"

    // share menu
    private const val HOOK_SHARE_MENU_CLASS = "hook_share_menu_class"
    private const val HOOK_SHARE_MENU_METHOD = "hook_share_menu_method"

    // tweet object
    private const val HOOK_TWEET_RESULT_FIELD = "hook_tweet_result_field"
    private const val HOOK_RESULT_FIELD = "hook_result_field"
    private const val HOOK_LEGACY_FIELD = "hook_legacy_field"
    private const val HOOK_EXTENDED_ENTITIES_FIELD = "hook_extended_entities_field"
    private const val HOOK_MEDIA_FIELD = "hook_media_field"
    private const val HOOK_MEDIA_TYPE_FIELD = "hook_media_type_field"
    private const val HOOK_MEDIA_URL_HTTPS_FIELD = "hook_media_url_https_field"
    private const val HOOK_MEDIA_INFO_FIELD = "hook_media_info_field"
    private const val HOOK_VARIANTS_FIELD = "hook_variants_field"

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
                appContext.addModuleAssetPath()
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
                appContext.addModuleAssetPath()
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
                appContext.addModuleAssetPath()
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
            appContext.addModuleAssetPath()
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
            appContext.addModuleAssetPath()
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
        tweetShareClassName = modulePrefs.getString(HOOK_TWEET_SHARE_CLASS, null)
            ?: throw CachedHookNotFound()
        tweetShareShowMethodName = modulePrefs.getString(HOOK_TWEET_SHARE_SHOW_METHOD, null)
            ?: throw CachedHookNotFound()
        tweetShareShareListFieldName = modulePrefs.getString(HOOK_TWEET_SHARE_LIST_FIELD, null)
            ?: throw CachedHookNotFound()

        actionEnumWrappedClassName = modulePrefs.getString(HOOK_ACTION_ENUM_WRAPPED_CLASS, null)
            ?: throw CachedHookNotFound()
        actionEnumWrappedInnerClassName =
            modulePrefs.getString(HOOK_ACTION_ENUM_WRAPPED_INNER_CLASS, null)
                ?: throw CachedHookNotFound()
        actionEnumClassName = modulePrefs.getString(HOOK_ACTION_ENUM_CLASS, null)
            ?: throw CachedHookNotFound()

        actionSheetItemClassName = modulePrefs.getString(HOOK_ACTION_SHEET_ITEM_CLASS, null)
            ?: throw CachedHookNotFound()
        actionSheetItemFieldName = modulePrefs.getString(HOOK_ACTION_SHEET_ITEM_FIELD, null)
            ?: throw CachedHookNotFound()

        // tweet share onClick
        shareTweetOnClickListenerClassName =
            modulePrefs.getString(HOOK_SHARE_TWEET_ON_CLICK_LISTENER_CLASS, null)
                ?: throw CachedHookNotFound()
        shareTweetItemAdapterFieldName =
            modulePrefs.getString(HOOK_SHARE_TWEET_ITEM_ADAPTER_FIELD, null)
                ?: throw CachedHookNotFound()
        shareTweetOnClickListener2ClassName =
            modulePrefs.getString(HOOK_SHARE_TWEET_ON_CLICK_LISTENER_2_CLASS, null)
                ?: throw CachedHookNotFound()
        shareTweetItemAdapter2FieldName =
            modulePrefs.getString(HOOK_SHARE_TWEET_ITEM_ADAPTER_2_FIELD, null)
                ?: throw CachedHookNotFound()
        shareTweetOnClickListener3ClassName =
            modulePrefs.getString(HOOK_SHARE_TWEET_ON_CLICK_LISTENER_3_CLASS, null)
                ?: throw CachedHookNotFound()
        shareTweetItemAdapter3FieldName =
            modulePrefs.getString(HOOK_SHARE_TWEET_ITEM_ADAPTER_3_FIELD, null)
                ?: throw CachedHookNotFound()
        actionItemViewDataFieldName =
            modulePrefs.getString(HOOK_ACTION_ITEM_VIEW_DATA_FIELD, null)
                ?: throw CachedHookNotFound()

        // protected tweet share onClick
        protectedShareTweetItemAdapterClassName =
            modulePrefs.getString(HOOK_PROTECTED_SHARE_ITEM_ADAPTER_CLASS, null)
                ?: throw CachedHookNotFound()
        protectedShareTweetItemAdapterClassTitleFieldName =
            modulePrefs.getString(HOOK_PROTECTED_SHARE_TWEET_ITEM_ADAPTER_CLASS_TITLE_FIELD, null)
                ?: throw CachedHookNotFound()

        // share menu
        shareMenuClassName = modulePrefs.getString(HOOK_SHARE_MENU_CLASS, null)
            ?: throw CachedHookNotFound()
        shareMenuMethodName = modulePrefs.getString(HOOK_SHARE_MENU_METHOD, null)
            ?: throw CachedHookNotFound()

        // tweet object
        tweetResultFieldName = modulePrefs.getString(HOOK_TWEET_RESULT_FIELD, null)
            ?: throw CachedHookNotFound()
        resultFieldName = modulePrefs.getString(HOOK_RESULT_FIELD, null)
            ?: throw CachedHookNotFound()
        legacyFieldName = modulePrefs.getString(HOOK_LEGACY_FIELD, null)
            ?: throw CachedHookNotFound()
        extendedEntitiesFieldName = modulePrefs.getString(HOOK_EXTENDED_ENTITIES_FIELD, null)
            ?: throw CachedHookNotFound()
        mediaFieldName = modulePrefs.getString(HOOK_MEDIA_FIELD, null)
            ?: throw CachedHookNotFound()
        mediaTypeFieldName = modulePrefs.getString(HOOK_MEDIA_TYPE_FIELD, null)
            ?: throw CachedHookNotFound()
        mediaUrlHttpsFieldName = modulePrefs.getString(HOOK_MEDIA_URL_HTTPS_FIELD, null)
            ?: throw CachedHookNotFound()
        mediaInfoFieldName = modulePrefs.getString(HOOK_MEDIA_INFO_FIELD, null)
            ?: throw CachedHookNotFound()
        variantsFieldName = modulePrefs.getString(HOOK_VARIANTS_FIELD, null)
            ?: throw CachedHookNotFound()
    }

    private fun saveHookInfo() {
        modulePrefs.edit().let {
            // tweet share download button
            it.putString(HOOK_TWEET_SHARE_CLASS, tweetShareClassName)
            it.putString(HOOK_TWEET_SHARE_SHOW_METHOD, tweetShareShowMethodName)
            it.putString(HOOK_TWEET_SHARE_LIST_FIELD, tweetShareShareListFieldName)

            it.putString(HOOK_ACTION_ENUM_WRAPPED_CLASS, actionEnumWrappedClassName)
            it.putString(HOOK_ACTION_ENUM_WRAPPED_INNER_CLASS, actionEnumWrappedInnerClassName)
            it.putString(HOOK_ACTION_ENUM_CLASS, actionEnumClassName)

            it.putString(HOOK_ACTION_SHEET_ITEM_CLASS, actionSheetItemClassName)
            it.putString(HOOK_ACTION_SHEET_ITEM_FIELD, actionSheetItemFieldName)

            // tweet share onClick
            it.putString(
                HOOK_SHARE_TWEET_ON_CLICK_LISTENER_CLASS, shareTweetOnClickListenerClassName
            )
            it.putString(HOOK_SHARE_TWEET_ITEM_ADAPTER_FIELD, shareTweetItemAdapterFieldName)
            it.putString(
                HOOK_SHARE_TWEET_ON_CLICK_LISTENER_2_CLASS, shareTweetOnClickListener2ClassName
            )
            it.putString(HOOK_SHARE_TWEET_ITEM_ADAPTER_2_FIELD, shareTweetItemAdapter2FieldName)
            it.putString(
                HOOK_SHARE_TWEET_ON_CLICK_LISTENER_3_CLASS, shareTweetOnClickListener3ClassName
            )
            it.putString(HOOK_SHARE_TWEET_ITEM_ADAPTER_3_FIELD, shareTweetItemAdapter3FieldName)
            it.putString(HOOK_ACTION_ITEM_VIEW_DATA_FIELD, actionItemViewDataFieldName)

            // protected tweet share onClick
            it.putString(
                HOOK_PROTECTED_SHARE_ITEM_ADAPTER_CLASS, protectedShareTweetItemAdapterClassName
            )
            it.putString(
                HOOK_PROTECTED_SHARE_TWEET_ITEM_ADAPTER_CLASS_TITLE_FIELD,
                protectedShareTweetItemAdapterClassTitleFieldName
            )

            // share menu
            it.putString(HOOK_SHARE_MENU_CLASS, shareMenuClassName)
            it.putString(HOOK_SHARE_MENU_METHOD, shareMenuMethodName)

            // tweet object
            it.putString(HOOK_TWEET_RESULT_FIELD, tweetResultFieldName)
            it.putString(HOOK_RESULT_FIELD, resultFieldName)
            it.putString(HOOK_LEGACY_FIELD, legacyFieldName)
            it.putString(HOOK_EXTENDED_ENTITIES_FIELD, extendedEntitiesFieldName)
            it.putString(HOOK_MEDIA_FIELD, mediaFieldName)
            it.putString(HOOK_MEDIA_TYPE_FIELD, mediaTypeFieldName)
            it.putString(HOOK_MEDIA_URL_HTTPS_FIELD, mediaUrlHttpsFieldName)
            it.putString(HOOK_MEDIA_INFO_FIELD, mediaInfoFieldName)
            it.putString(HOOK_VARIANTS_FIELD, variantsFieldName)
        }.apply()
    }


    private fun searchHook() {
        // tweet share download button
        val tweetShareClass = dexKit.findMethodUsingString(
            usingString = "^timeline_selected_caret_position$",
        ).map {
            it.getMethodInstance(ezXClassLoader)
        }
            .firstOrNull { it.parameterTypes.size == 2 && it.parameterTypes[1] == Bundle::class.java }?.declaringClass
            ?: throw ClassNotFoundException()

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

        val actionSheetItemClass = dexKit.findMethodUsingString(
            usingString = "^ActionSheetItem(drawableRes=$",
            methodName = "toString",
            methodReturnType = String::class.java.name,
        ).firstOrNull()?.getMethodInstance(ezXClassLoader)?.declaringClass
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
        val shareTweetOnClickListenerClass = dexKit.findMethodUsingString(
            usingString = "^profile_modal$",
            methodName = "onClick",
            methodReturnType = Void.TYPE.name,
            methodParamTypes = arrayOf(View::class.java.name),
        ).firstOrNull()?.getMethodInstance(ezXClassLoader)?.declaringClass
        val shareTweetItemAdapterField =
            shareTweetOnClickListenerClass?.declaredFields?.lastOrNull()
        // twitter alpha 9.69 alpha 4
        val shareTweetOnClickListener2Class = dexKit.findMethodUsingString(
            usingString = "^fabContainerView.findViewById(R.id.tweet_label)$",
            methodName = "onClick",
            methodReturnType = Void.TYPE.name,
            methodParamTypes = arrayOf(View::class.java.name),
        ).firstOrNull()?.getMethodInstance(ezXClassLoader)?.declaringClass
        val shareTweetItemAdapter2Field =
            shareTweetOnClickListener2Class?.declaredFields?.lastOrNull()
        // twitter alpha 9.69 alpha 9
        val shareTweetOnClickListener3Class = dexKit.findMethodUsingString(
            usingString = "^\$onSwitchToggled$",
            methodName = "onClick",
            methodReturnType = Void.TYPE.name,
            methodParamTypes = arrayOf(View::class.java.name),
        ).firstOrNull()?.getMethodInstance(ezXClassLoader)?.declaringClass
        val shareTweetItemAdapter3Field =
            shareTweetOnClickListener3Class?.declaredFields?.lastOrNull()

        val shareTweetItemAdapterClass = dexKit.findMethodUsingString(
            usingString = "^itemView.findViewById(R.id.action_sheet_item_icon)$",
            methodName = "<init>",
            methodReturnType = Void.TYPE.name,
        ).map {
            it.getConstructorInstance(ezXClassLoader).declaringClass
        }.firstOrNull {
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
        val refMethodDescriptor = dexKit.findMethodUsingString(
            usingString = "^bceHierarchyContext$",
            methodReturnType = Void.TYPE.name,
        ).firstOrNull {
            val clazz = it.getMethodInstance(ezXClassLoader).declaringClass
            clazz?.declaredFields?.any { f -> f.type == View::class.java } ?: false
        } ?: throw NoSuchMethodError()
        val refClass = dexKit.findMethodInvoking(
            methodDescriptor = refMethodDescriptor.descriptor,
            beCalledMethodName = "<init>",
        ).firstNotNullOfOrNull {
            it.value.firstOrNull()?.getConstructorInstance(ezXClassLoader)?.declaringClass
        } ?: throw ClassNotFoundException()
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
        }.firstOrNull()?.let { loadClass(it) } ?: throw ClassNotFoundException()
        val shareMenuMethod = shareMenuClass.declaredMethods.firstOrNull { m ->
            m.returnType == Void.TYPE && m.parameterTypes.size == 4 && m.parameterTypes[0] == String::class.java && m.parameterTypes[1] == String::class.java
        } ?: throw NoSuchMethodError()
        val tweetResultField = shareMenuMethod.parameterTypes[2].declaredFields.firstOrNull { f ->
            f.isPublic && f.isFinal && f.type.declaredFields.any { it.type == loadClass("com.twitter.model.vibe.Vibe") }
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

        val mediaTypeEnumClass = dexKit.findMethodUsingString(
            usingString = "^MODEL3D$",
            methodName = "<clinit>",
            methodReturnType = Void.TYPE.name,
        ).firstOrNull()?.let { loadClass(it.declaringClassName) }
            ?: throw ClassNotFoundException()
        val perMediaClass = loadClass(mediaTypeEnumClass.name.split("$")[0])
        val mediaTypeField =
            perMediaClass.declaredFields.firstOrNull { it.type == mediaTypeEnumClass }
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
            loadDexKit()
            searchHook()
            Log.d("Download Hook search time: ${System.currentTimeMillis() - timeStart} ms")
            saveHookInfo()
            modulePrefs.edit().putLong("hook_download_last_update", System.currentTimeMillis())
                .apply()
        }
    }
}
