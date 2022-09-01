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
import icu.nullptr.twifucker.R
import icu.nullptr.twifucker.hook.HookEntry.Companion.isLogcatProcessInitialized
import icu.nullptr.twifucker.hook.HookEntry.Companion.logcatProcess
import icu.nullptr.twifucker.hook.HookEntry.Companion.startLog
import icu.nullptr.twifucker.logFile
import icu.nullptr.twifucker.logFileDir
import icu.nullptr.twifucker.modulePrefs

class SettingsDialog(context: Context) : AlertDialog.Builder(context) {

    companion object {
        private lateinit var outDialog: AlertDialog
        private lateinit var prefs: SharedPreferences
        const val PREFS_NAME = "twifucker"
        const val EXPORT_LOG = 1919810
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
            findPreference("enable_log").onPreferenceChangeListener = this
            findPreference("export_log").onPreferenceClickListener = this
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

        override fun onPreferenceChange(p0: Preference?, p1: Any?): Boolean {
            if (p0 is SwitchPreference) {
                if (p0.key == "enable_log") {
                    if (p1 as Boolean) {
                        startLog()
                    } else {
                        clearLog()
                    }
                }
            }
            return true
        }

        override fun onPreferenceClick(p0: Preference?): Boolean {
            when (p0?.key) {
                "export_log" -> {
                    exportLog()
                }
                "clear_log" -> {
                    clearLog()
                }
            }
            return true
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == EXPORT_LOG && resultCode == Activity.RESULT_OK) {
                data?.data?.let { uri ->
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        logFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                }
            }
            super.onActivityResult(requestCode, resultCode, data)
        }

        private fun exportLog() {
            if (!logFile.exists()) return
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "text/plain"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, "log.txt")
            }
            try {
                startActivityForResult(intent, EXPORT_LOG)
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
                startLog()
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
