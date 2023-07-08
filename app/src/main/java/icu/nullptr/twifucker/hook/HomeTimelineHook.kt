package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.FieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import dalvik.bytecode.Opcodes
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.twifucker.beforeMeasure
import icu.nullptr.twifucker.exceptions.CachedHookNotFound
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexKit
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexKit
import icu.nullptr.twifucker.hostAppLastUpdate
import icu.nullptr.twifucker.moduleLastModify
import icu.nullptr.twifucker.modulePrefs
import icu.nullptr.twifucker.replaceMeasure

object HomeTimelineHook : BaseHook() {

    override val name: String
        get() = "HomeTimelineHook"

    private lateinit var unhook: XC_MethodHook.Unhook

    private const val HOOK_HOME_TIMELINE_TAB_CLASS = "hook_home_timeline_tab_class"
    private const val HOOK_HOME_TIMELINE_LIST_CLASS = "hook_home_timeline_list_class"
    private const val HOOK_HOME_TIMELINE_LIST_ADD_METHOD = "hook_home_timeline_list_add_method"

    private lateinit var homeTimelineTabClassName: String
    private lateinit var homeTimelineListClassName: String
    private lateinit var homeTimelineListAddMethodName: String

    override fun init() {
        if (!modulePrefs.getBoolean("hide_for_you_tab", false)) return

        try {
            loadHookInfo()
        } catch (t: Throwable) {
            Log.e(t)
            return
        }

        MethodFinder.fromClass(homeTimelineTabClassName).filterByName("apply").first().createHook {
            beforeMeasure(name) { param ->
                if (param.args[0] !is List<*>) return@beforeMeasure
		val c =
                    FieldFinder.fromClass(homeTimelineTabClassName).filterByType(Int::class.java)
                        .first().getInt(param.thisObject)
                unhook = MethodFinder.fromClass(homeTimelineListClassName).filterByName(
                    homeTimelineListAddMethodName
                ).first().createHook {
                    replaceMeasure(name) {
                        // assume 1st tab is "For You"
                        unhook.unhook()
                    }
                }
            }
        }
    }

    private fun loadCachedHookInfo() {
        homeTimelineTabClassName =
            modulePrefs.getString(HOOK_HOME_TIMELINE_TAB_CLASS, null) ?: throw CachedHookNotFound()
        homeTimelineListClassName =
            modulePrefs.getString(HOOK_HOME_TIMELINE_LIST_CLASS, null) ?: throw CachedHookNotFound()
        homeTimelineListAddMethodName =
            modulePrefs.getString(HOOK_HOME_TIMELINE_LIST_ADD_METHOD, null)
                ?: throw CachedHookNotFound()
    }

    private fun saveHookInfo() {
        modulePrefs.let {
            it.putString(HOOK_HOME_TIMELINE_TAB_CLASS, homeTimelineTabClassName)
            it.putString(HOOK_HOME_TIMELINE_LIST_CLASS, homeTimelineListClassName)
            it.putString(HOOK_HOME_TIMELINE_LIST_ADD_METHOD, homeTimelineListAddMethodName)
        }
    }

    private fun searchHook() {
        homeTimelineTabClassName = dexKit.findMethodUsingString {
            usingString = "^super_follow_subscriptions_home_timeline_tab_enabled$"
            methodName = "apply"
            methodReturnType = Object::class.java.name
            methodParamTypes = arrayOf(Object::class.java.name, Object::class.java.name)
            unique = true
        }.first().declaringClassName

        val listAddMethod = dexKit.findMethodUsingOpPrefixSeq {
            opSeq = intArrayOf(
                Opcodes.OP_IF_EQZ,
                Opcodes.OP_IGET_OBJECT,
                Opcodes.OP_IF_NEZ,
                Opcodes.OP_INVOKE_VIRTUAL,
                Opcodes.OP_MOVE_RESULT,
                Opcodes.OP_IF_EQZ,
                Opcodes.OP_INVOKE_VIRTUAL,
                Opcodes.OP_GOTO,
                Opcodes.OP_IGET_OBJECT,
                Opcodes.OP_IF_EQZ,
                Opcodes.OP_CONST_4,
                Opcodes.OP_INVOKE_VIRTUAL,
            )
            methodReturnType = Void.TYPE.name
            methodParamTypes = arrayOf(Object::class.java.name)
        }.first()
        homeTimelineListClassName = listAddMethod.declaringClassName
        homeTimelineListAddMethodName = listAddMethod.name
    }

    private fun loadHookInfo() {
        val hookHomeTimelineLastUpdate = modulePrefs.getLong("hook_home_timeline_last_update", 0)

        Log.d("hookHomeTimelineLastUpdate: $hookHomeTimelineLastUpdate, hostAppLastUpdate: $hostAppLastUpdate, moduleLastModify: $moduleLastModify")

        val timeStart = System.currentTimeMillis()

        if (hookHomeTimelineLastUpdate > hostAppLastUpdate && hookHomeTimelineLastUpdate > moduleLastModify) {
            loadCachedHookInfo()
            Log.d("Home Timeline Hook load time: ${System.currentTimeMillis() - timeStart} ms")
        } else {
            loadDexKit()
            searchHook()
            Log.d("Home Timeline Hook search time: ${System.currentTimeMillis() - timeStart} ms")
            saveHookInfo()
            modulePrefs.putLong("hook_home_timeline_last_update", System.currentTimeMillis())
        }
    }
}
