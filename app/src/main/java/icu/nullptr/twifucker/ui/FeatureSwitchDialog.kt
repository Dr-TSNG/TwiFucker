package icu.nullptr.twifucker.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kyuubiran.ezxhelper.EzXHelper.addModuleAssetPath
import icu.nullptr.twifucker.R
import icu.nullptr.twifucker.modulePrefs
import org.json.JSONArray

class FeatureSwitchDialog(context: Context) : Dialog(context) {

    class FeatureSwitchAdapter(private var arr: JSONArray) :
        RecyclerView.Adapter<FeatureSwitchAdapter.ViewHolder>() {
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val linearLayout = itemView as LinearLayout
            var keyView: TextView
            var valueView: TextView

            init {
                itemView.findViewById<TextView>(R.id.feature_switch_key_view).let {
                    keyView = it
                }
                itemView.findViewById<TextView>(R.id.feature_switch_value_view).let {
                    valueView = it
                }
            }
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
            val view = LinearLayout(parent.context).let {
                it.orientation = LinearLayout.VERTICAL
                it.setPadding(16, 16, 16, 16)
                val keyView = TextView(parent.context)
                keyView.id = R.id.feature_switch_key_view
                keyView.textSize = 18f
                val valueView = TextView(parent.context)
                valueView.id = R.id.feature_switch_value_view
                valueView.textSize = 14f
                it.addView(keyView)
                it.addView(valueView)
                it
            }
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.keyView.text = arr.getJSONObject(position).optString("key")
            holder.valueView.text = arr.getJSONObject(position).optBoolean("value").toString()

            holder.linearLayout.setOnClickListener { _ ->
                val bool = !arr.getJSONObject(position).optBoolean("value")
                holder.valueView.text = bool.toString()
                arr.getJSONObject(position).put("value", bool)
                modulePrefs.edit().putString("feature_switch", arr.toString()).apply()
            }
            holder.linearLayout.setOnLongClickListener { _ ->
                arr.remove(position)
                modulePrefs.edit().putString("feature_switch", arr.toString()).apply()
                notifyItemChanged(position)
                true
            }
        }

        override fun getItemCount(): Int = arr.length()


    }

    init {
        addModuleAssetPath(context)

        val featureSwitch = modulePrefs.getString("feature_switch", "[]")
        var arr = JSONArray(featureSwitch)
        val featureSwitchAdapter = FeatureSwitchAdapter(arr)

        val titleView = TextView(context)
        titleView.setPadding(32, 32, 32, 32)
        titleView.textSize = 24f
        titleView.text = context.getString(R.string.feature_switch)

        val recyclerView = RecyclerView(context)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = featureSwitchAdapter

        val buttonsLinearLayout = LinearLayout(context)
        buttonsLinearLayout.orientation = LinearLayout.HORIZONTAL

        val buttonReset = Button(context, null, android.R.attr.buttonBarNegativeButtonStyle)
        buttonReset.text = context.getString(R.string.reset)
        buttonReset.setOnClickListener {
            addModuleAssetPath(context)
            AlertDialog.Builder(context).let {
                it.setMessage(R.string.msg_yes_no)
                it.setPositiveButton(R.string.yes) { _, _ ->
                    arr = JSONArray()
                    modulePrefs.edit().putString("feature_switch", "[]").apply()
                    featureSwitchAdapter.reset()
                }
                it.setNegativeButton(R.string.no, null)
                it.show()
            }
        }

        val spaceParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        spaceParams.weight = 1.0f
        val space = Space(context)
        space.layoutParams = spaceParams

        val buttonAdd = Button(context, null, android.R.attr.buttonBarPositiveButtonStyle)
        buttonAdd.text = context.getString(R.string.add)
        buttonAdd.setOnClickListener { _ ->
            KeyValueDialog(context) {
                featureSwitchAdapter.reloadAddedData(it)
            }
        }

        buttonsLinearLayout.addView(buttonReset)
        buttonsLinearLayout.addView(space)
        buttonsLinearLayout.addView(buttonAdd)

        setCancelable(true)

        val linearLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val linearLayout = LinearLayout(context)
        linearLayout.setPadding(16, 16, 16, 16)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.layoutParams = linearLayoutParams

        linearLayout.addView(titleView)
        linearLayout.addView(recyclerView)
        linearLayout.addView(buttonsLinearLayout)

        setContentView(linearLayout)
        show()
    }
}