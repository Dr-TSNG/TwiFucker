package icu.nullptr.twifucker.ui

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.utils.addModuleAssetPath
import icu.nullptr.twifucker.R
import icu.nullptr.twifucker.modulePrefs
import org.json.JSONArray

class FeatureSwitchDialog(context: Context) : AlertDialog.Builder(context) {
    class FeatureSwitchAdapter(val context: Context, val arr: JSONArray) : BaseAdapter() {
        override fun getCount(): Int {
            return arr.length()
        }

        override fun getItem(position: Int): Any {
            return arr.get(position)
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LinearLayout(context).let {
                it.orientation = LinearLayout.VERTICAL
                it.setPadding(32, 16, 32, 16)
                val keyView = TextView(context)
                keyView.textSize = 18f
                keyView.text = arr.getJSONObject(position).optString("key")
                val valueView = TextView(context)
                valueView.textSize = 14f
                valueView.text = arr.getJSONObject(position).optBoolean("value").toString()
                it.addView(keyView)
                it.addView(valueView)

                it.setOnClickListener { _ ->
                    val bool = !arr.getJSONObject(position).optBoolean("value")
                    valueView.text = bool.toString()
                    arr.getJSONObject(position).put("value", bool)
                    modulePrefs.edit().putString("feature_switch", arr.toString()).apply()
                    notifyDataSetChanged()
                }
                it.setOnLongClickListener { _ ->
                    arr.remove(position)
                    modulePrefs.edit().putString("feature_switch", arr.toString()).apply()
                    notifyDataSetChanged()
                    true
                }

                it
            }
            return view
        }

    }

    init {
        context.addModuleAssetPath()

        val featureSwitch = modulePrefs.getString("feature_switch", "[]")
        var arr = JSONArray(featureSwitch)
        val featureSwitchAdapter = FeatureSwitchAdapter(context, arr)
        setAdapter(featureSwitchAdapter, null)

        setTitle(R.string.feature_switch)
        setNeutralButton(R.string.add) { _, _ ->
            KeyValueDialog(context)
        }
        setNegativeButton(R.string.reset) { _, _ ->
            arr = JSONArray()
            modulePrefs.edit().putString("feature_switch", "[]").apply()
        }
        setPositiveButton(R.string.settings_dismiss, null)

        setOnDismissListener(null)
        show()
    }
}