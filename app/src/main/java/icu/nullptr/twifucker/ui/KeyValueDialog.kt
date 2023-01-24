package icu.nullptr.twifucker.ui

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.*
import com.github.kyuubiran.ezxhelper.EzXHelper.addModuleAssetPath
import icu.nullptr.twifucker.R
import icu.nullptr.twifucker.modulePrefs
import org.json.JSONArray
import org.json.JSONObject

class KeyValueDialog(context: Context) : AlertDialog.Builder(context) {
    lateinit var keyEditText: EditText
    lateinit var valueSwitch: Switch

    private fun buildView(context: Context): View {
        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(32, 32, 32, 32)

        keyEditText = EditText(context)
        keyEditText.hint = "key"
        keyEditText.textSize = 18f

        val valueLinearLayout = RelativeLayout(context)
        valueLinearLayout.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT
        )
        val valueText = TextView(context)
        val valueTextLp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        valueTextLp.addRule(RelativeLayout.ALIGN_PARENT_START)
        valueText.layoutParams = valueTextLp
        valueText.textSize = 14f
        valueText.text = "Boolean"
        valueSwitch = Switch(context)
        val valueSwitchLp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        valueSwitchLp.addRule(RelativeLayout.ALIGN_PARENT_END)
        valueSwitch.layoutParams = valueSwitchLp
        valueLinearLayout.addView(valueText)
        valueLinearLayout.addView(valueSwitch)

        linearLayout.addView(keyEditText)
        linearLayout.addView(valueLinearLayout)

        return linearLayout
    }

    init {
        addModuleAssetPath(context)

        setView(buildView(context))
        setNeutralButton(R.string.settings_dismiss, null)
        setPositiveButton(R.string.save) { _, _ ->
            val featureSwitch = modulePrefs.getString("feature_switch", "[]")
            val arr = JSONArray(featureSwitch)
            arr.put(JSONObject().put("key", keyEditText.text).put("value", valueSwitch.isChecked))
            modulePrefs.edit().putString("feature_switch", arr.toString()).apply()
        }

        show()
    }
}