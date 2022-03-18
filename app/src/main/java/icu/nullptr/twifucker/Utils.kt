package icu.nullptr.twifucker

import android.content.Context
import android.content.SharedPreferences
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import icu.nullptr.twifucker.ui.SettingsDialog

@Suppress("DEPRECATION")
val modulePrefs: SharedPreferences by lazy {
    appContext.getSharedPreferences(
        SettingsDialog.PREFS_NAME, Context.MODE_MULTI_PROCESS
    )
}
