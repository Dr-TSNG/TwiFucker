package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClassOrNull
import icu.nullptr.twifucker.isEntryNeedsRemove

object JsonTimelineModuleItemHook : BaseHook() {
    override fun init() {
        val jsonTimelineModuleItemClass =
            loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineModuleItem")
                ?: throw ClassNotFoundException()

        val jsonTimelineModuleItemEntryIdField =
            jsonTimelineModuleItemClass.declaredFields.firstOrNull { it.type == String::class.java }
                ?: throw NoSuchFieldException()

        val jsonAddToModuleInstructionClass =
            loadClassOrNull("com.twitter.model.json.timeline.urt.JsonAddToModuleInstruction")
                ?: throw ClassNotFoundException()
        val jsonAddToModuleInstructionMapperClass =
            loadClassOrNull("com.twitter.model.json.timeline.urt.JsonAddToModuleInstruction\$\$JsonObjectMapper")
                ?: throw ClassNotFoundException()

        val jsonTimelineModuleItemField1 =
            jsonAddToModuleInstructionClass.declaredFields.firstOrNull { it.type == ArrayList::class.java }
                ?: throw NoSuchFieldException()

        val jsonTimelineModuleClass =
            loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineModule")
                ?: throw ClassNotFoundException()
        val jsonTimelineModuleMapperClass =
            loadClassOrNull("com.twitter.model.json.timeline.urt.JsonTimelineModule\$\$JsonObjectMapper")
                ?: throw ClassNotFoundException()

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
