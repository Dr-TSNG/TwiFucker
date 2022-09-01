@file:Suppress("DEPRECATION")

package icu.nullptr.twifucker.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.addModuleAssetPath
import com.github.kyuubiran.ezxhelper.utils.restartHostApp
import icu.nullptr.twifucker.R
import icu.nullptr.twifucker.modulePrefs

class SettingsDialog(context: Context) : AlertDialog.Builder(context) {

    companion object {
        private lateinit var outDialog: AlertDialog
        private lateinit var prefs: SharedPreferences
        const val PREFS_NAME = "twifucker"
    }

    private fun deleteFromDatabase() {
        val disablePromotedContent = modulePrefs.getBoolean("disable_promoted_content", true)
        if (!disablePromotedContent) return
        val re = Regex("^\\d+-\\d+\\.db$")
        var count = 0
        context.databaseList().forEach { db ->
            if (re.matches(db)) {
                val database = SQLiteDatabase.openDatabase(
                    context.getDatabasePath(db).absolutePath, null, SQLiteDatabase.OPEN_READWRITE
                )
                count += database.delete(
                    "timeline", "entity_id LIKE ?", arrayOf("promotedTweet-%")
                )
                database.close()
            }
        }
        Log.toast(context.getString(R.string.deleted_n_promoted_tweet, count))
    }

    class PrefsFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferenceManager.sharedPreferencesName = PREFS_NAME
            addPreferencesFromResource(R.xml.settings_dialog)
            prefs = preferenceManager.sharedPreferences
            findPreference("about").setOnPreferenceClickListener {
                activity.startActivity(
                    Intent(
                        Intent.ACTION_VIEW, Uri.parse("https://github.com/Dr-TSNG/TwiFucker")
                    )
                )
                true
            }
        }

        override fun onPreferenceChange(p0: Preference?, p1: Any?): Boolean {
            return true
        }

        override fun onPreferenceClick(p0: Preference?): Boolean {
            return true
        }

    }

    init {
        context.addModuleAssetPath()

        val act = context as Activity

        outDialog = run {
            val prefsFragment = PrefsFragment()
            act.fragmentManager.beginTransaction().add(prefsFragment, "settings").commit()
            act.fragmentManager.executePendingTransactions()

            prefsFragment.onActivityCreated(null)

            setView(prefsFragment.view)

            setTitle(context.getString(R.string.twifucker_settings))
            setPositiveButton(context.getString(R.string.save_restart)) { _, _ ->
                deleteFromDatabase()
                restartHostApp(act)
            }
            setNegativeButton(context.getString(R.string.settings_dismiss), null)
            setCancelable(false)
            show()
        }
    }
}
