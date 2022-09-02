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
import android.preference.SwitchPreference
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.addModuleAssetPath
import com.github.kyuubiran.ezxhelper.utils.restartHostApp
import icu.nullptr.twifucker.*
import icu.nullptr.twifucker.hook.HookEntry.Companion.isLogcatProcessInitialized
import icu.nullptr.twifucker.hook.HookEntry.Companion.logcatProcess

class SettingsDialog(context: Context) : AlertDialog.Builder(context) {

    companion object {
        private lateinit var outDialog: AlertDialog
        private lateinit var prefs: SharedPreferences
        const val PREFS_NAME = "twifucker"
        const val EXPORT_LOG = 1001
        const val EXPORT_JSON_LOG = 1002
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
        @Deprecated("Deprecated in Java")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferenceManager.sharedPreferencesName = PREFS_NAME
            addPreferencesFromResource(R.xml.settings_dialog)
            prefs = preferenceManager.sharedPreferences
            findPreference("enable_log").onPreferenceChangeListener = this
            findPreference("export_log").onPreferenceClickListener = this
            findPreference("export_json_log").onPreferenceClickListener = this
            findPreference("clear_log").onPreferenceClickListener = this
            findPreference("about").setOnPreferenceClickListener {
                activity.startActivity(
                    Intent(
                        Intent.ACTION_VIEW, Uri.parse("https://github.com/Dr-TSNG/TwiFucker")
                    )
                )
                true
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPreferenceChange(p0: Preference?, p1: Any?): Boolean {
            if (p0 is SwitchPreference) {
                if (p0.key == "enable_log") {
                    if (!(p1 as Boolean)) {
                        clearLog()
                    }
                }
            }
            return true
        }

        @Deprecated("Deprecated in Java")
        override fun onPreferenceClick(p0: Preference?): Boolean {
            when (p0?.key) {
                "export_log" -> {
                    exportLog(EXPORT_LOG, logFile.name)
                }
                "export_json_log" -> {
                    exportLog(EXPORT_JSON_LOG, logJsonFile.name)
                }
                "clear_log" -> {
                    clearLog()
                }
            }
            return true
        }

        @Deprecated("Deprecated in Java")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (resultCode == Activity.RESULT_OK) {
                data?.data?.let { uri ->
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        when (requestCode) {
                            EXPORT_LOG -> {
                                logFile.inputStream().use { input ->
                                    input.copyTo(out)
                                }
                            }
                            EXPORT_JSON_LOG -> {
                                logJsonFile.inputStream().use { input ->
                                    input.copyTo(out)
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
            super.onActivityResult(requestCode, resultCode, data)
        }

        private fun exportLog(logType: Int, fileName: String) {
            when (logType) {
                EXPORT_LOG -> {
                    if (!logFile.exists()) return
                }
                EXPORT_JSON_LOG -> {
                    if (!logJsonFile.exists()) return
                }
                else -> {
                    return
                }
            }
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "text/plain"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            try {
                startActivityForResult(intent, logType)
            } catch (t: Throwable) {
                Log.e(t)
            }
        }

        private fun clearLog() {
            try {
                if (isLogcatProcessInitialized()) {
                    logcatProcess.destroy()
                }
                logFileDir.deleteRecursively()
                logFileDir.delete()
            } catch (t: Throwable) {
                Log.e(t)
            }
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
