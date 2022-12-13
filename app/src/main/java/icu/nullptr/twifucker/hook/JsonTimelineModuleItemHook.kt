package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClass
import icu.nullptr.twifucker.isEntryNeedsRemove
import icu.nullptr.twifucker.modulePrefs

object JsonTimelineModuleItemHook : BaseHook() {
    override fun init() {
        if (!modulePrefs.getBoolean("disable_who_to_follow", false)) return

        val jsonTimelineModuleItemClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineModuleItem")

        val jsonTimelineModuleItemEntryIdField =
            jsonTimelineModuleItemClass.declaredFields.firstOrNull { it.type == String::class.java }
                ?: throw NoSuchFieldException()

        val jsonAddToModuleInstructionClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonAddToModuleInstruction")
        val jsonAddToModuleInstructionMapperClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonAddToModuleInstruction\$\$JsonObjectMapper")

        val jsonTimelineModuleItemField1 =
            jsonAddToModuleInstructionClass.declaredFields.firstOrNull { it.type == ArrayList::class.java }
                ?: throw NoSuchFieldException()

        val jsonTimelineModuleClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineModule")
        val jsonTimelineModuleMapperClass =
            loadClass("com.twitter.model.json.timeline.urt.JsonTimelineModule\$\$JsonObjectMapper")

        val jsonTimelineModuleItemField2 =
            jsonTimelineModuleClass.declaredFields.firstOrNull { it.type == ArrayList::class.java }
                ?: throw NoSuchFieldException()


        findMethod(jsonAddToModuleInstructionMapperClass) {
            name == "_parse" && returnType == jsonAddToModuleInstructionClass
        }.hookAfter { param ->
            if (param.result == null) return@hookAfter
            jsonTimelineModuleItemField1.get(param.result).let { itemsRaw ->
                val items = itemsRaw as ArrayList<*>
                val itemsNeedRemove = mutableListOf<Any>()
                items.forEach { item ->
                    jsonTimelineModuleItemEntryIdField.get(item).let { entryIdRaw ->
                        val entryId = entryIdRaw as String
                        if (isEntryNeedsRemove(entryId)) {
                            itemsNeedRemove.add(item)
                        }
                    }
                }
                items.removeAll(itemsNeedRemove.toSet())
                if (items.removeAll(itemsNeedRemove.toSet())) {
                    Log.d("Remove ${itemsNeedRemove.size} timeline module item(s)")
                }
            }
        }

        findMethod(jsonTimelineModuleMapperClass) {
            name == "_parse" && returnType == jsonTimelineModuleClass
        }.hookAfter { param ->
            if (param.result == null) return@hookAfter
            jsonTimelineModuleItemField2.get(param.result).let { itemsRaw ->
                val items = itemsRaw as ArrayList<*>
                val itemsNeedRemove = mutableListOf<Any>()
                items.forEach { item ->
                    jsonTimelineModuleItemEntryIdField.get(item).let { entryIdRaw ->
                        val entryId = entryIdRaw as String
                        if (isEntryNeedsRemove(entryId)) {
                            itemsNeedRemove.add(item)
                        }
                    }
                }
                if (items.removeAll(itemsNeedRemove.toSet())) {
                    Log.d("Remove ${itemsNeedRemove.size} timeline module item(s)")
                }
            }
        }
    }
}
