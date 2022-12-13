package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClass
import icu.nullptr.twifucker.isEntryNeedsRemove

object JsonTimelineModuleItemHook : BaseHook() {
    override fun init() {
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
            jsonTimelineModuleItemField1.get(param.result).let { itemsRaw ->
                val items = itemsRaw as ArrayList<*>
                val itemsNeedRemove = mutableListOf<Any>()
                items.forEach { item ->
                    jsonTimelineModuleItemEntryIdField.get(item).let { entryIdRaw ->
                        val entryId = entryIdRaw as String
                        if (isEntryNeedsRemove(entryId)) {
                            itemsNeedRemove.add(item)
                            Log.d("Remove timeline module item $entryId")
                        }
                    }
                }
                items.removeAll(itemsNeedRemove.toSet())
            }
        }

        findMethod(jsonTimelineModuleMapperClass) {
            name == "_parse" && returnType == jsonTimelineModuleClass
        }.hookAfter { param ->
            jsonTimelineModuleItemField2.get(param.result).let { itemsRaw ->
                val items = itemsRaw as ArrayList<*>
                val itemsNeedRemove = mutableListOf<Any>()
                items.forEach { item ->
                    jsonTimelineModuleItemEntryIdField.get(item).let { entryIdRaw ->
                        val entryId = entryIdRaw as String
                        if (isEntryNeedsRemove(entryId)) {
                            itemsNeedRemove.add(item)
                            Log.d("Remove timeline module item $entryId")
                        }
                    }
                }
                items.removeAll(itemsNeedRemove.toSet())
            }
        }
    }
}
