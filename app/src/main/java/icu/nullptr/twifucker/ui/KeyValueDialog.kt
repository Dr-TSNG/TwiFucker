package icu.nullptr.twifucker.ui

import android.app.AlertDialog
import android.content.Context
import com.github.kyuubiran.ezxhelper.EzXHelper.addModuleAssetPath
import icu.nullptr.twifucker.R
import icu.nullptr.twifucker.modulePrefs
import org.json.JSONArray
import org.json.JSONObject

class KeyValueDialog(context: Context, onChange: (JSONArray) -> Unit) :
    AlertDialog.Builder(context) {
    init {
        addModuleAssetPath(context)

        setTitle(R.string.feature_switch_value_boolean)
        val keyValueView = KeyValueView(context)
        setView(keyValueView)
        setNeutralButton(R.string.settings_dismiss, null)
        setPositiveButton(R.string.save) { _, _ ->
            val featureSwitch = modulePrefs.getString("feature_switch", "[]")
            val arr = JSONArray(featureSwitch)
            arr.put(
                JSONObject().put("key", keyValueView.editText.text)
                    .put("value", keyValueView.switch.isChecked)
            )
            modulePrefs.putString("feature_switch", arr.toString())
            onChange(arr)
        }
        keyValueView.focus()
        show()
    }
}