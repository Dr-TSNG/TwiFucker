package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClassOrNull
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.MemberExtensions.isStatic
import com.github.kyuubiran.ezxhelper.finders.ConstructorFinder
import com.github.kyuubiran.ezxhelper.finders.FieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.misc.Utils.getAllClassesList
import de.robv.android.xposed.XposedHelpers
import icu.nullptr.twifucker.beforeMeasure
import icu.nullptr.twifucker.data.TwitterItem
import icu.nullptr.twifucker.exceptions.CachedHookNotFound
import icu.nullptr.twifucker.hook.HookEntry.Companion.dexKit
import icu.nullptr.twifucker.hook.HookEntry.Companion.loadDexKit
import icu.nullptr.twifucker.hostAppLastUpdate
import icu.nullptr.twifucker.moduleLastModify
import icu.nullptr.twifucker.modulePrefs
import icu.nullptr.twifucker.ui.SettingsDialog.Companion.PREF_HIDDEN_BOTTOM_NAVBAR_ITEMS
import icu.nullptr.twifucker.ui.SettingsDialog.Companion.PREF_HIDDEN_DRAWER_ITEMS
import io.luckypray.dexkit.enums.FieldUsingType

object DrawerNavbarHook : BaseHook() {
    override val name: String
        get() = "DrawerNavbarHook"

    private const val HOOK_DRAWER_ITEMS_CLASS = "hook_drawer_items_class"
    private const val HOOK_BOOL_FALSE_CLASS = "hook_bool_false_class"
    private const val HOOK_BOTTOM_NAVBAR_CLASS = "hook_bottom_navbar_class"
    private const val HOOK_CUSTOM_MAP_CLASS = "hook_custom_map_class"
    private const val HOOK_CUSTOM_MAP_INIT_METHOD = "hook_custom_map_init_method"
    private const val HOOK_CUSTOM_MAP_INNER_CLASS = "hook_custom_map_inner_class"
    private const val HOOK_CUSTOM_MAP_INNER_ADD_METHOD = "hook_custom_map_inner_add_method"
    private const val HOOK_CUSTOM_MAP_INNER_GET_METHOD = "hook_custom_map_inner_get_method"

    var drawerItems = mutableListOf<TwitterItem>()
    var bottomNavbarItems = mutableListOf<TwitterItem>()

    private lateinit var drawerItemsClassName: String
    private lateinit var boolFalseClassName: String
    private lateinit var bottomNavbarClassName: String
    private lateinit var customMapClassName: String
    private lateinit var customMapInitMethodName: String
    private lateinit var customMapInnerClassName: String
    private lateinit var customMapInnerAddMethodName: String
    private lateinit var customMapInnerGetMethodName: String

    override fun init() {
        try {
            loadHookInfo()
        } catch (t: Throwable) {
            Log.e(t)
            return
        }

        ConstructorFinder.fromClass(loadClass(bottomNavbarClassName)).first().createHook {
            beforeMeasure(name) { param ->
                val hiddenItems =
                    modulePrefs.getStringSet(PREF_HIDDEN_BOTTOM_NAVBAR_ITEMS, mutableSetOf())
                val map = param.args[2] as Map<*, *>
                val newMap = XposedHelpers.callStaticMethod(
                    loadClass(customMapClassName),
                    customMapInitMethodName,
                    arrayOf(Int::class.java),
                    map.size
                )
                bottomNavbarItems.clear()
                map.forEach { item ->
                    val keyString = item.key.toString()
                    bottomNavbarItems.add(
                        TwitterItem(
                            keyString, hiddenItems?.contains(keyString) == false
                        )
                    )
                    if (hiddenItems?.contains(keyString) == false || keyString.lowercase() == "home") {
                        XposedHelpers.callMethod(
                            newMap,
                            customMapInnerAddMethodName,
                            arrayOf(Any::class.java, Any::class.java),
                            item.key,
                            item.value
                        )
                    }
                }
                param.args[2] = XposedHelpers.callMethod(newMap, customMapInnerGetMethodName)
            }
        }

        ConstructorFinder.fromClass(loadClass(drawerItemsClassName)).first().createHook {
            beforeMeasure(name) { param ->
                val hiddenItems = modulePrefs.getStringSet(PREF_HIDDEN_DRAWER_ITEMS, mutableSetOf())
                val drawerItemMap = XposedHelpers.callMethod(param.args[0], "get") as Map<*, *>
                drawerItems.clear()
                drawerItemMap.forEach { item ->
                    val keyString = item.key.toString()
                    drawerItems.add(
                        TwitterItem(
                            keyString, hiddenItems?.contains(keyString) == false
                        )
                    )
                    val boolField = item.value?.let {
                        FieldFinder.fromClass(it.javaClass).filter {
                            type.isInterface && type != List::class.java
                        }.first()
                    } ?: return@forEach
                    if (hiddenItems?.contains(keyString) == true && keyString.lowercase() != "settings") {
                        XposedHelpers.setObjectField(
                            item.value, boolField.name, loadClass(boolFalseClassName).newInstance()
                        )
                    }
                }
//            val drawerItemGroupMap = param.args[1].invokeMethod("get") as Map<*, *>
//            drawerItemGroupMap.forEach { item ->
//                val keyString = item.key.toString()
//                drawerItems.add(CleanItem(keyString, hiddenItems?.contains(keyString) == false))
//                val boolField =
//                    item.value?.javaClass?.declaredFields?.firstOrNull { it.type.isInterface && it.type != List::class.java }
//                        ?: return@forEach
//                item.value?.putObject(boolField, loadClass(falseClassName).newInstance())
//            }
            }
        }
    }

    private fun loadCachedHookInfo() {
        drawerItemsClassName =
            modulePrefs.getString(HOOK_DRAWER_ITEMS_CLASS, null) ?: throw CachedHookNotFound()
        boolFalseClassName =
            modulePrefs.getString(HOOK_BOOL_FALSE_CLASS, null) ?: throw CachedHookNotFound()
        bottomNavbarClassName =
            modulePrefs.getString(HOOK_BOTTOM_NAVBAR_CLASS, null) ?: throw CachedHookNotFound()
        customMapClassName =
            modulePrefs.getString(HOOK_CUSTOM_MAP_CLASS, null) ?: throw CachedHookNotFound()
        customMapInitMethodName =
            modulePrefs.getString(HOOK_CUSTOM_MAP_INIT_METHOD, null) ?: throw CachedHookNotFound()
        customMapInnerClassName =
            modulePrefs.getString(HOOK_CUSTOM_MAP_INNER_CLASS, null) ?: throw CachedHookNotFound()
        customMapInnerAddMethodName = modulePrefs.getString(HOOK_CUSTOM_MAP_INNER_ADD_METHOD, null)
            ?: throw CachedHookNotFound()
        customMapInnerGetMethodName = modulePrefs.getString(HOOK_CUSTOM_MAP_INNER_GET_METHOD, null)
            ?: throw CachedHookNotFound()
    }

    private fun saveHookInfo() {
        modulePrefs.let {
            it.putString(HOOK_DRAWER_ITEMS_CLASS, drawerItemsClassName)
            it.putString(HOOK_BOOL_FALSE_CLASS, boolFalseClassName)
            it.putString(HOOK_BOTTOM_NAVBAR_CLASS, bottomNavbarClassName)
            it.putString(HOOK_CUSTOM_MAP_CLASS, customMapClassName)
            it.putString(HOOK_CUSTOM_MAP_INIT_METHOD, customMapInitMethodName)
            it.putString(HOOK_CUSTOM_MAP_INNER_CLASS, customMapInnerClassName)
            it.putString(
                HOOK_CUSTOM_MAP_INNER_ADD_METHOD, customMapInnerAddMethodName
            )
            it.putString(
                HOOK_CUSTOM_MAP_INNER_GET_METHOD, customMapInnerGetMethodName
            )
        }
    }

    private fun searchHook() {
        val boolSuperClassName = dexKit.findMethodUsingString {
            usingString = "^renderLambdaToString(this)$"
            methodReturnType = String::class.java.name
        }.firstOrNull {
            loadClass(it.declaringClassName).superclass == Object::class.java
        }?.declaringClassName ?: throw ClassNotFoundException()

        val booleanClass = loadClass("java.lang.Boolean")
//        val trueField = booleanClass.getField("TRUE")
        val falseField = booleanClass.getField("FALSE")
//        val trueFieldIndexes = dexHelper.findMethodGettingField(
//            dexHelper.encodeFieldIndex(trueField),
//            dexHelper.encodeClassIndex(Object::class.java),
//            0,
//            null,
//            -1,
//            null,
//            null,
//            null,
//            false,
//        )
//        trueFieldIndexes.firstOrNull {
//            val method = dexHelper.decodeMethodIndex(it)
//            val declaringClass = method?.declaringClass
//            val declaredMethodsSize = declaringClass?.declaredMethods?.size
//            val declaredFieldsSize = declaringClass?.declaredFields?.size
//            method?.name == "invoke" && declaringClass?.superclass == superClass && declaredMethodsSize == 1 && declaredFieldsSize == 1 && declaringClass.declaredFields[0].isStatic && !declaringClass.name.contains(
//                "$"
//            )
//        }?.let { it ->
//            dexHelper.decodeMethodIndex(it)
//        }

        val falseFieldMap = dexKit.findMethodUsingField {
            fieldDescriptor = ""
            fieldDeclareClass = booleanClass.name
            fieldName = falseField.name
            fieldType = falseField.type.name
            usingType = FieldUsingType.GET
            callerMethodName = "invoke"
            callerMethodReturnType = Object::class.java.name
            callerMethodParamTypes = emptyArray()
        }
        val boolFalseClass = falseFieldMap.keys.firstOrNull {
            val declaringClass = loadClass(it.declaringClassName)
            val declaredMethodsSize = declaringClass.declaredMethods.size
            val declaredFieldsSize = declaringClass.declaredFields.size
            declaringClass.superclass?.name == boolSuperClassName && declaredMethodsSize == 1 && declaredFieldsSize == 1 && declaringClass.declaredFields[0].isStatic && !declaringClass.name.contains(
                "$"
            )
        }?.declaringClassName ?: throw ClassNotFoundException()

        val drawerItemsClass = dexKit.findMethodUsingString {
            usingString = "^drawerItemGroupMap$"
            methodReturnType = Void.TYPE.name
        }.firstNotNullOfOrNull { it.declaringClassName } ?: throw ClassNotFoundException()

        val customMapInitMethodDescriptor = dexKit.findMethodUsingString {
            usingString = "^expectedSize$"
            methodParamTypes = arrayOf(Int::class.java.name)
        }.firstOrNull { loadClass(it.declaringClassName).interfaces.contains(Map::class.java) }
            ?: throw ClassNotFoundException()

        val customMapInitMethod =
            customMapInitMethodDescriptor.getMethodInstance(EzXHelper.classLoader)

        val customMapClass = customMapInitMethod.declaringClass
        val customMapInnerClass = customMapInitMethod.returnType
        val customMapInnerGetMethod = MethodFinder.fromClass(customMapInnerClass)
            .filter { returnType.name == customMapInitMethodDescriptor.declaringClassName }.first()
        val customMapInnerAddMethod = MethodFinder.fromClass(customMapInnerClass)
            .filter { returnType.name == customMapInitMethodDescriptor.getMethodInstance(EzXHelper.classLoader).returnType.name }
            .first()

        val bottomTabClass = EzXHelper.classLoader.getAllClassesList().firstOrNull {
            try {
                val clazz = loadClassOrNull(it) ?: return@firstOrNull false
                clazz.constructors.size == 1 && clazz.constructors[0].parameterTypes.size == 3 && clazz.constructors[0].parameterTypes[2] == Map::class.java && clazz.declaredFields.size == 3 && clazz.declaredFields.filter { f -> f.type == Map::class.java }.size == 1 && clazz.declaredMethods.size == 1 && clazz.interfaces.isEmpty()
            } catch (_: Throwable) {
                false
            }
        }?.let { loadClass(it) } ?: throw ClassNotFoundException()

        drawerItemsClassName = drawerItemsClass
        boolFalseClassName = boolFalseClass

        customMapClassName = customMapClass.name
        customMapInitMethodName = customMapInitMethod.name
        customMapInnerClassName = customMapInnerClass.name
        customMapInnerGetMethodName = customMapInnerGetMethod.name
        customMapInnerAddMethodName = customMapInnerAddMethod.name
        bottomNavbarClassName = bottomTabClass.name
    }

    private fun loadHookInfo() {
        val hookDrawerLastUpdate = modulePrefs.getLong("hook_drawer_last_update", 0)

        Log.d("hookDrawerLastUpdate: $hookDrawerLastUpdate, hostAppLastUpdate: $hostAppLastUpdate, moduleLastModify: $moduleLastModify")

        val timeStart = System.currentTimeMillis()

        if (hookDrawerLastUpdate > hostAppLastUpdate && hookDrawerLastUpdate > moduleLastModify) {
            loadCachedHookInfo()
            Log.d("Drawer Hook load time: ${System.currentTimeMillis() - timeStart} ms")
        } else {
            loadDexKit()
            searchHook()
            Log.d("Drawer Hook search time: ${System.currentTimeMillis() - timeStart} ms")
            saveHookInfo()
            modulePrefs.putLong("hook_drawer_last_update", System.currentTimeMillis())

        }
    }
}