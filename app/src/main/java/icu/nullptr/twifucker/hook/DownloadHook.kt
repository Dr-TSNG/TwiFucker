package icu.nullptr.twifucker.hook

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.addModuleAssetPath
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.MemberExtensions.isNotFinal
import com.github.kyuubiran.ezxhelper.MemberExtensions.isNotStatic
import com.github.kyuubiran.ezxhelper.MemberExtensions.isPublic
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder
import com.github.kyuubiran.ezxhelper.finders.FieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import dalvik.bytecode.Opcodes
import de.robv.android.xposed.XposedHelpers
import icu.nullptr.twifucker.R
import icu.nullptr.twifucker.beforeMeasure
import icu.nullptr.twifucker.clearUrlQueries
import icu.nullptr.twifucker.exceptions.CachedHookNotFound
import icu.nullptr.twifucker.genOrigUrl
import icu.nullptr.twifucker.getId
import icu.nullptr.twifucker.hook.HookEntry.Companion.currentActivity
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexKit
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexKit
import icu.nullptr.twifucker.hostAppLastUpdate
import icu.nullptr.twifucker.moduleLastModify
import icu.nullptr.twifucker.modulePrefs
import icu.nullptr.twifucker.ui.DownloadDialog


object DownloadHook : BaseHook() {
    override val name: String
        get() = "DownloadHook"

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

            MethodFinder.fromClass(loadClass(className)).filterByName("onClick").first()
                .createHook {
                    beforeMeasure(name) { param ->
                        if (downloadUrls.isEmpty()) return@beforeMeasure
                        val actionItemViewData = XposedHelpers.getObjectField(
                            XposedHelpers.getObjectField(
                                param.thisObject, shareTweetItemAdapterFieldName
                            ), actionItemViewDataFieldName
                        )
                        // a - actionType
                        // b - title
                        // c - iconRes
                        addModuleAssetPath(appContext)
                        if (XposedHelpers.getObjectField(
                                actionItemViewData, "b"
                            ) != appContext.getString(R.string.download_or_copy)
                        ) return@beforeMeasure

                        try {
                            currentActivity.get()?.let { act ->
                                DownloadDialog(act, downloadUrls) {
                                    downloadUrls = listOf()
                                }
                            }
                        } catch (t: Throwable) {
                            Log.e(t)
                        }
                    }
                }
        }

        // protected tweet
        MethodFinder.fromClass(loadClass(protectedShareTweetItemAdapterClassName))
            .filterByName("onClick").first().createHook {
                beforeMeasure(name) { param ->
                    if (downloadUrls.isEmpty()) return@beforeMeasure

                    val protectedShareTweetItemAdapterTitleTextView = XposedHelpers.getObjectField(
                        param.thisObject, protectedShareTweetItemAdapterClassTitleFieldName
                    ) as TextView
                    addModuleAssetPath(appContext)
                    if (protectedShareTweetItemAdapterTitleTextView.text != appContext.getString(R.string.download_or_copy)) return@beforeMeasure

                    try {
                        currentActivity.get()?.let { act ->
                            DownloadDialog(act, downloadUrls) {
                                downloadUrls = listOf()
                            }
                        }
                    } catch (t: Throwable) {
                        Log.e(t)
                    }
                }
            }

        MethodFinder.fromClass(loadClass(tweetShareClassName))
            .filterByName(tweetShareShowMethodName).first().createHook {
                beforeMeasure(name) { param ->
                    val shareList = XposedHelpers.getObjectField(
                        param.thisObject, tweetShareShareListFieldName
                    ) as List<*>

                    val mutList = shareList.toMutableList()

                    val actionEnumWrappedClass = loadClass(actionEnumWrappedInnerClassName)
                    val actionEnumClass = loadClass(actionEnumClassName)
                    val actionSheetItemClass = loadClass(actionSheetItemClassName)

                    val actionEnumWrapped = XposedHelpers.newInstance(
                        actionEnumWrappedClass,
                        XposedHelpers.callStaticMethod(actionEnumClass, "valueOf", "None"),
                        ""
                    )
                    // drawableRes, actionId, title
                    addModuleAssetPath(appContext)
                    val buttonConstructor =
                        ConstructorFinder.fromClass(actionSheetItemClass).filterByParamCount(3)
                            .filterByParamTypes {
                                // IILjava/lang/String; or Ljava/lang/String;II
                                (it[0] == Int::class.java && it[1] == Int::class.java && it[2] == String::class.java) || (it[0] == String::class.java && it[1] == Int::class.java && it[2] == Int::class.java)
                            }.first()
                    val dlButton = if (buttonConstructor.parameterTypes[0] == Int::class.java) {
                        buttonConstructor.newInstance(
                            getId("ic_vector_incoming", "drawable"),
                            0,
                            appContext.getString(R.string.download_or_copy)
                        )
                    } else {
                        buttonConstructor.newInstance(
                            appContext.getString(R.string.download_or_copy),
                            getId("ic_vector_incoming", "drawable"),
                            0
                        )
                    }

                    XposedHelpers.setObjectField(
                        actionEnumWrapped, actionSheetItemFieldName,
                        dlButton,
                    )

                    mutList.add(
                        XposedHelpers.newInstance(
                            loadClass(actionEnumWrappedClassName), actionEnumWrapped
                        )
                    )

                    XposedHelpers.setObjectField(
                        param.thisObject, tweetShareShareListFieldName, mutList.toList()
                    )
                }
            }

        // share menu
        MethodFinder.fromClass(loadClass(shareMenuClassName)).filterByName(shareMenuMethodName)
            .first().createHook {
                beforeMeasure(name) { param ->
                    val event = param.args[1]
                    // share_menu_click
                    // share_menu_cancel
                    if (event == "share_menu_cancel") {
                        downloadUrls = listOf()
                        return@beforeMeasure
                    }
                    if (event != "share_menu_click") return@beforeMeasure
                    val tweetResults = param.args[2]

                    val tweetResult =
                        XposedHelpers.getObjectField(tweetResults, tweetResultFieldName)
                    val result = XposedHelpers.getObjectField(tweetResult, resultFieldName)
                    val legacy = XposedHelpers.getObjectField(result, legacyFieldName)
                    val extendedEntities =
                        XposedHelpers.getObjectField(legacy, extendedEntitiesFieldName)
                    val media =
                        XposedHelpers.getObjectField(extendedEntities, mediaFieldName) as List<*>

                    val urls = arrayListOf<String>()
                    media.forEach { m ->
                        val mediaType = XposedHelpers.getObjectField(m, mediaTypeFieldName)
                        when (mediaType.toString()) {
                            "IMAGE" -> {
                                val mediaUrlHttps = XposedHelpers.getObjectField(
                                    m, mediaUrlHttpsFieldName
                                ) as String
                                urls.add(genOrigUrl(mediaUrlHttps))
                            }

                            "VIDEO", "ANIMATED_GIF" -> {
                                val mediaInfo = XposedHelpers.getObjectField(m, mediaInfoFieldName)
                                val variants = XposedHelpers.getObjectField(
                                    mediaInfo, variantsFieldName
                                ) as List<*>
                                // a - bitrate
                                // b - url
                                // c - contentType
                                variants.sortedByDescending { v ->
                                    XposedHelpers.getObjectField(v, "a") as Int
                                }[0]?.let {
                                    val url = XposedHelpers.getObjectField(it, "b") as String
                                    urls.add(clearUrlQueries(url))
                                }
                            }
                        }
                    }
                    downloadUrls = urls
                }
            }
    }

    private fun loadCachedHookInfo() {
        // tweet share download button
        tweetShareClassName =
            modulePrefs.getString(HOOK_TWEET_SHARE_CLASS, null) ?: throw CachedHookNotFound()
        tweetShareShowMethodName =
            modulePrefs.getString(HOOK_TWEET_SHARE_SHOW_METHOD, null) ?: throw CachedHookNotFound()
        tweetShareShareListFieldName =
            modulePrefs.getString(HOOK_TWEET_SHARE_LIST_FIELD, null) ?: throw CachedHookNotFound()

        actionEnumWrappedClassName = modulePrefs.getString(HOOK_ACTION_ENUM_WRAPPED_CLASS, null)
            ?: throw CachedHookNotFound()
        actionEnumWrappedInnerClassName =
            modulePrefs.getString(HOOK_ACTION_ENUM_WRAPPED_INNER_CLASS, null)
                ?: throw CachedHookNotFound()
        actionEnumClassName =
            modulePrefs.getString(HOOK_ACTION_ENUM_CLASS, null) ?: throw CachedHookNotFound()

        actionSheetItemClassName =
            modulePrefs.getString(HOOK_ACTION_SHEET_ITEM_CLASS, null) ?: throw CachedHookNotFound()
        actionSheetItemFieldName =
            modulePrefs.getString(HOOK_ACTION_SHEET_ITEM_FIELD, null) ?: throw CachedHookNotFound()

        // tweet share onClick
        shareTweetOnClickListenerClassName =
            modulePrefs.getString(HOOK_SHARE_TWEET_ON_CLICK_LISTENER_CLASS, null)
                ?: throw CachedHookNotFound()
        shareTweetItemAdapterFieldName =
            modulePrefs.getString(HOOK_SHARE_TWEET_ITEM_ADAPTER_FIELD, null)
                ?: throw CachedHookNotFound()
        actionItemViewDataFieldName = modulePrefs.getString(HOOK_ACTION_ITEM_VIEW_DATA_FIELD, null)
            ?: throw CachedHookNotFound()

        // protected tweet share onClick
        protectedShareTweetItemAdapterClassName =
            modulePrefs.getString(HOOK_PROTECTED_SHARE_ITEM_ADAPTER_CLASS, null)
                ?: throw CachedHookNotFound()
        protectedShareTweetItemAdapterClassTitleFieldName =
            modulePrefs.getString(HOOK_PROTECTED_SHARE_TWEET_ITEM_ADAPTER_CLASS_TITLE_FIELD, null)
                ?: throw CachedHookNotFound()

        // share menu
        shareMenuClassName =
            modulePrefs.getString(HOOK_SHARE_MENU_CLASS, null) ?: throw CachedHookNotFound()
        shareMenuMethodName =
            modulePrefs.getString(HOOK_SHARE_MENU_METHOD, null) ?: throw CachedHookNotFound()

        // tweet object
        tweetResultFieldName =
            modulePrefs.getString(HOOK_TWEET_RESULT_FIELD, null) ?: throw CachedHookNotFound()
        resultFieldName =
            modulePrefs.getString(HOOK_RESULT_FIELD, null) ?: throw CachedHookNotFound()
        legacyFieldName =
            modulePrefs.getString(HOOK_LEGACY_FIELD, null) ?: throw CachedHookNotFound()
        extendedEntitiesFieldName =
            modulePrefs.getString(HOOK_EXTENDED_ENTITIES_FIELD, null) ?: throw CachedHookNotFound()
        mediaFieldName = modulePrefs.getString(HOOK_MEDIA_FIELD, null) ?: throw CachedHookNotFound()
        mediaTypeFieldName =
            modulePrefs.getString(HOOK_MEDIA_TYPE_FIELD, null) ?: throw CachedHookNotFound()
        mediaUrlHttpsFieldName =
            modulePrefs.getString(HOOK_MEDIA_URL_HTTPS_FIELD, null) ?: throw CachedHookNotFound()
        mediaInfoFieldName =
            modulePrefs.getString(HOOK_MEDIA_INFO_FIELD, null) ?: throw CachedHookNotFound()
        variantsFieldName =
            modulePrefs.getString(HOOK_VARIANTS_FIELD, null) ?: throw CachedHookNotFound()
    }

    private fun saveHookInfo() {
        modulePrefs.let {
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
        }
    }


    private fun searchHook() {
        // tweet share download button
        val tweetShareClass = dexKit.findMethodUsingString {
            usingString = "^timeline_selected_caret_position$"
        }.map {
            it.getMethodInstance(EzXHelper.classLoader)
        }
            .firstOrNull { it.parameterTypes.size == 2 && it.parameterTypes[1] == Bundle::class.java }?.declaringClass
            ?: throw ClassNotFoundException()


        val tweetShareShowMethod =
            MethodFinder.fromClass(tweetShareClass).filterPublic().filterFinal()
                .filterByParamCount(1).filterByReturnType(Void.TYPE).first()
        val tweetShareShareListField =
            FieldFinder.fromClass(tweetShareClass).filterPublic().filterFinal()
                .filterByType(List::class.java).first()
        val actionEnumWrappedClassRefMethod =
            MethodFinder.fromClass(tweetShareClass).filterPublic().filterFinal()
                .filterByParamTypes { it.size >= 3 && it[1] == String::class.java && it[2] == Boolean::class.java }
                .first()
        val actionEnumWrappedClass = actionEnumWrappedClassRefMethod.returnType
        val actionEnumWrappedInnerClass = actionEnumWrappedClass.constructors[0].parameterTypes[0]
        val actionEnumClass = actionEnumWrappedClassRefMethod.parameterTypes[0]

        val actionSheetItemClass = dexKit.findMethodUsingString {
            usingString = "^ActionSheetItem(drawableRes=$"
            methodName = "toString"
            methodReturnType = String::class.java.name
        }.firstOrNull()?.getMethodInstance(EzXHelper.classLoader)?.declaringClass
            ?: throw ClassNotFoundException()
        val actionSheetItemField =
            FieldFinder.fromClass(actionEnumWrappedInnerClass).filterByType(actionSheetItemClass)
                .first()

        tweetShareClassName = tweetShareClass.name
        tweetShareShowMethodName = tweetShareShowMethod.name
        tweetShareShareListFieldName = tweetShareShareListField.name

        actionEnumWrappedClassName = actionEnumWrappedClass.name
        actionEnumWrappedInnerClassName = actionEnumWrappedInnerClass.name
        actionEnumClassName = actionEnumClass.name

        actionSheetItemClassName = actionSheetItemClass.name
        actionSheetItemFieldName = actionSheetItemField.name

        // tweet share onClick
        val shareTweetOnClickListenerRefMethodsDesc = dexKit.findMethodUsingString {
            usingString = "itemView.findViewById(R.id.action_sheet_item_icon)"
        }
        val shareTweetOnClickListenerConstructorDesc =
            shareTweetOnClickListenerRefMethodsDesc.firstNotNullOfOrNull { methodDesc ->
                val result = dexKit.findMethodInvoking {
                    methodDescriptor = methodDesc.descriptor
                    beInvokedMethodName = "<init>"
                    beInvokedMethodReturnType = Void.TYPE.name
                }
                result.values.firstNotNullOfOrNull { l ->
                    l.firstOrNull { desc ->
                        desc.getConstructorInstance(EzXHelper.classLoader).parameterTypes.size == 3
                    }
                }
            } ?: throw ClassNotFoundException()

        val shareTweetOnClickListenerClass =
            shareTweetOnClickListenerConstructorDesc.getConstructorInstance(EzXHelper.classLoader).declaringClass
                ?: throw ClassNotFoundException()
        val shareTweetItemAdapterField =
            FieldFinder.fromClass(shareTweetOnClickListenerClass).last()

        val shareTweetItemAdapterClass = dexKit.findMethodUsingString {
            usingString = "^itemView.findViewById(R.id.action_sheet_item_icon)$"
            methodName = "<init>"
            methodReturnType = Void.TYPE.name
        }.map {
            it.getConstructorInstance(EzXHelper.classLoader).declaringClass
        }.firstOrNull {
            it?.declaredFields?.any { f ->
                f.isPublic && f.isNotStatic && f.isNotFinal
            } == true
        } ?: throw ClassNotFoundException()
        val actionItemViewDataField =
            FieldFinder.fromClass(shareTweetItemAdapterClass).filterPublic().filterNonStatic()
                .filterNonFinal().first()

        shareTweetOnClickListenerClassName = shareTweetOnClickListenerClass.name
        shareTweetItemAdapterFieldName = shareTweetItemAdapterField.name
        actionItemViewDataFieldName = actionItemViewDataField.name

        // protected tweet share onClick
        val protectedShareTweetItemAdapterClass = dexKit.findMethodUsingOpPrefixSeq {
            opSeq = intArrayOf(
                Opcodes.OP_IGET_OBJECT,
                Opcodes.OP_IF_EQZ,
                Opcodes.OP_INVOKE_VIRTUAL,
                Opcodes.OP_MOVE_RESULT,
                Opcodes.OP_INVOKE_INTERFACE,
                Opcodes.OP_RETURN_VOID
            )
            methodName = "onClick"
            methodParamTypes = arrayOf(View::class.java.name)
            methodReturnType = Void.TYPE.name
        }.first().getMethodInstance(EzXHelper.classLoader).declaringClass
        val protectedShareTweetItemAdapterClassTitleField =
            FieldFinder.fromClass(protectedShareTweetItemAdapterClass)
                .filterByType(TextView::class.java).first()

        // protected tweet share onClick
        protectedShareTweetItemAdapterClassName = protectedShareTweetItemAdapterClass.name
        protectedShareTweetItemAdapterClassTitleFieldName =
            protectedShareTweetItemAdapterClassTitleField.name

        // share menu
        val shareMenuClass = dexKit.findMethodUsingString {
            usingString = "^sandbox://tweetview?id=$"
            methodReturnType = Void.TYPE.name
        }.first().declaringClassName.let {
            loadClass(it)
        }
        val shareMenuMethod = MethodFinder.fromClass(shareMenuClass).filterByReturnType(Void.TYPE)
            .filterByParamTypes { it.size == 4 && it[0] == String::class.java && it[1] == String::class.java }
            .first()
        val tweetResultClass = dexKit.findMethodUsingOpPrefixSeq {
            methodName = "<init>"
            methodReturnType = Void.TYPE.name
            opSeq = intArrayOf(
                Opcodes.OP_INVOKE_DIRECT,
                Opcodes.OP_IGET_WIDE,
                Opcodes.OP_IPUT_WIDE,
                Opcodes.OP_IGET_OBJECT,
                Opcodes.OP_IPUT_OBJECT,
                Opcodes.OP_IGET_WIDE,
                Opcodes.OP_IPUT_WIDE,
                Opcodes.OP_IGET_WIDE,
                Opcodes.OP_IPUT_WIDE,
                Opcodes.OP_IGET_WIDE,
                Opcodes.OP_IPUT_WIDE,
                Opcodes.OP_IGET_OBJECT,
                Opcodes.OP_IPUT_OBJECT,
            )
        }.first().getConstructorInstance(EzXHelper.classLoader).declaringClass
        val tweetResultField =
            FieldFinder.fromClass(shareMenuMethod.parameterTypes[2]).filterByType(tweetResultClass)
                .first()
        val resultField = tweetResultField.type.declaredFields.groupBy { it.type }
            .filter { it.value.size == 2 && it.key.declaredFields.size == 3 }.map { it.value[1] }[0]
            ?: throw NoSuchFieldError()
        val legacyField =
            resultField.type.declaredFields.filter { it.isNotStatic }.maxByOrNull { it.name }
                ?: throw NoSuchFieldError()
        val extendedEntitiesField =
            legacyField.type.declaredFields.filter { it.isNotStatic }.maxByOrNull { it.name }
                ?: throw NoSuchFieldError()
        val mediaField = FieldFinder.fromClass(extendedEntitiesField.type.superclass)
            .filterByType(List::class.java).first()

        val mediaTypeEnumClass = dexKit.findMethodUsingString {
            usingString = "^MODEL3D$"
            methodName = "<clinit>"
            methodReturnType = Void.TYPE.name
        }.firstOrNull()?.let { loadClass(it.declaringClassName) } ?: throw ClassNotFoundException()
        val perMediaClass = loadClass(mediaTypeEnumClass.name.split("$")[0])
        val mediaTypeField =
            perMediaClass.declaredFields.firstOrNull { it.type == mediaTypeEnumClass }
                ?: throw NoSuchFieldError()
        val mediaUrlHttpsField =
            FieldFinder.fromClass(perMediaClass).filterNonStatic().filterByType(String::class.java)
                .first()
        val mediaInfoField = FieldFinder.fromClass(perMediaClass).filter {
            type.declaredFields.size == 4 && type.declaredFields.filter { f2 -> f2.type == Float::class.java }.size == 2 && type.declaredFields.filter { f2 -> f2.type == List::class.java }.size == 1
        }.first()
        val variantsField =
            FieldFinder.fromClass(mediaInfoField.type).filterByType(List::class.java).first()

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
            modulePrefs.putLong("hook_download_last_update", System.currentTimeMillis())

        }
    }
}
