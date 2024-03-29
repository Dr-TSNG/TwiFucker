package icu.nullptr.twifucker.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.RecyclerView
import com.github.kyuubiran.ezxhelper.EzXHelper.addModuleAssetPath
import icu.nullptr.twifucker.R
import icu.nullptr.twifucker.modulePrefs
import org.json.JSONArray

class FeatureSwitchDialog(context: Context) : Dialog(context) {

    class FeatureSwitchAdapter(private var arr: JSONArray, private val context: Context) :
        RecyclerView.Adapter<FeatureSwitchAdapter.ViewHolder>() {
        class ViewHolder(itemView: FeatureSwitchItem) : RecyclerView.ViewHolder(itemView) {
            val featureSwitchItem = itemView
            val keyView = itemView.keyTextView
            var valueView = itemView.valueTextView
        }

        fun reloadAddedData(arr: JSONArray) {
            this.arr = arr
            notifyItemInserted(arr.length() - 1)
        }

        fun reset() {
            val len = arr.length()
            arr = JSONArray()
            notifyItemRangeRemoved(0, len)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = FeatureSwitchItem(parent.context)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val isBoolean = arr.getJSONObject(position).optString("type", "boolean") == "boolean"
            val key = arr.getJSONObject(position).optString("key")

            holder.keyView.text = key
            if (isBoolean) {
                val bool = arr.getJSONObject(position).optBoolean("value")
                holder.valueView.text = bool.toString()
            } else {
                val decimal = arr.getJSONObject(position).optString("value")
                holder.valueView.text = decimal
            }

            holder.featureSwitchItem.setOnClickListener { _ ->
                if (!isBoolean) return@setOnClickListener
                val bool = !arr.getJSONObject(position).optBoolean("value")
                holder.valueView.text = bool.toString()
                arr.getJSONObject(position).put("value", bool)
                modulePrefs.putString("feature_switch", arr.toString())
            }
            holder.featureSwitchItem.setOnLongClickListener { _ ->
                addModuleAssetPath(context)
                AlertDialog.Builder(context).let {
                    it.setMessage(R.string.msg_yes_no)
                    it.setPositiveButton(R.string.yes) { _, _ ->
                        arr.remove(position)
                        modulePrefs.putString("feature_switch", arr.toString())
                        notifyItemChanged(position)
                    }
                    it.setNegativeButton(R.string.no, null)
                    it.show()
                }
                true
            }
        }

        override fun getItemCount(): Int = arr.length()
    }

    init {
        addModuleAssetPath(context)

        val featureSwitch = modulePrefs.getString("feature_switch", "[]")
        var arr = JSONArray(featureSwitch)
        val featureSwitchAdapter = FeatureSwitchAdapter(arr, context)

        val featureSwitchView = FeatureSwitchView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                MATCH_PARENT, WRAP_CONTENT
            )
        }
        featureSwitchView.setAdapter(featureSwitchAdapter)
        featureSwitchView.setOnResetClickListener {
            addModuleAssetPath(context)
            AlertDialog.Builder(context).let {
                it.setMessage(R.string.msg_yes_no)
                it.setPositiveButton(R.string.yes) { _, _ ->
                    arr = JSONArray()
                    modulePrefs.putString("feature_switch", "[]")
                    featureSwitchAdapter.reset()
                }
                it.setNegativeButton(R.string.no, null)
                it.show()
            }
        }
        featureSwitchView.setOnAddClickListener {
            KeyValueDialog(context) {
                featureSwitchAdapter.reloadAddedData(it)
            }
        }
        setContentView(featureSwitchView)
        show()
        window?.setLayout(
            MATCH_PARENT, WRAP_CONTENT
        )
    }
}
