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
import com.github.kyuubiran.ezxhelper.EzXHelper.addModuleAssetPath
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.misc.Utils.restartHostApp
import icu.nullptr.twifucker.*
import icu.nullptr.twifucker.hook.DrawerNavbarHook.bottomNavbarItems
import icu.nullptr.twifucker.hook.DrawerNavbarHook.drawerItems
import icu.nullptr.twifucker.hook.HookEntry.Companion.isLogcatProcessInitialized
import icu.nullptr.twifucker.hook.HookEntry.Companion.logcatProcess

class SettingsDialog(context: Context) : AlertDialog.Builder(context) {

    companion object {
        private lateinit var outDialog: AlertDialog
        private lateinit var prefs: SharedPreferences

        const val PREFS_NAME = "twifucker"

        const val REQUEST_EXPORT_LOG = 1001
        const val REQUEST_EXPORT_JSON_LOG = 1002

        const val PREF_DISABLE_PROMOTED_CONTENT = "disable_promoted_content"
        const val PREF_HIDE_DRAWER_ITEMS = "hide_drawer_items"
        const val PREF_HIDE_BOTTOM_NAVBAR_ITEMS = "hide_bottom_navbar_items"
        const val PREF_ENABLE_LOG = "enable_log"
        const val PREF_EXPORT_LOG = "export_log"
        const val PREF_EXPORT_JSON_LOG = "export_json_log"
        const val PREF_CLEAR_LOG = "clear_log"
        const val PREF_DELETE_DATABASES = "delete_databases"
        const val PREF_ABOUT = "about"

        const val PREF_HIDDEN_DRAWER_ITEMS = "hidden_drawer_items"
        const val PREF_HIDDEN_BOTTOM_NAVBAR_ITEMS = "hidden_bottom_navbar_items"
    }

    private fun deleteFromDatabase() {
        val disablePromotedContent = modulePrefs.getBoolean(PREF_DISABLE_PROMOTED_CONTENT, true)
        if (!disablePromotedContent) return
        val re = Regex("""^\d+-\d+(-versioncode-\d+)?.db$""")
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
        if (count > 0) {
            Log.toast(context.getString(R.string.deleted_n_promoted_tweet, count))
        }
    }

    class PrefsFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {
        @Deprecated("Deprecated in Java")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferenceManager.sharedPreferencesName = PREFS_NAME
            addPreferencesFromResource(R.xml.settings_dialog)
            prefs = preferenceManager.sharedPreferences
            findPreference(PREF_HIDE_DRAWER_ITEMS).onPreferenceClickListener = this
            findPreference(PREF_HIDE_BOTTOM_NAVBAR_ITEMS).onPreferenceClickListener = this
            findPreference(PREF_ENABLE_LOG).onPreferenceChangeListener = this
            findPreference(PREF_EXPORT_LOG).onPreferenceClickListener = this
            findPreference(PREF_EXPORT_JSON_LOG).onPreferenceClickListener = this
            findPreference(PREF_CLEAR_LOG).onPreferenceClickListener = this
            findPreference(PREF_DELETE_DATABASES).onPreferenceClickListener = this
            findPreference(PREF_ABOUT).onPreferenceClickListener = this

            findPreference("feature_switch").setOnPreferenceClickListener {
                FeatureSwitchDialog(context)
                true
            }

            if (BuildConfig.DEBUG) {
                findPreference(PREF_DELETE_DATABASES).isEnabled = true
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPreferenceChange(p0: Preference?, p1: Any?): Boolean {
            if (p0 is SwitchPreference) {
                if (p0.key == PREF_ENABLE_LOG) {
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
                PREF_HIDE_DRAWER_ITEMS -> {
                    onCustomizeHiddenDrawerItems()
                }
                PREF_HIDE_BOTTOM_NAVBAR_ITEMS -> {
                    onCustomizeHiddenBottomNavbarItems()
                }
                PREF_EXPORT_LOG -> {
                    exportLog(REQUEST_EXPORT_LOG, logFile.name)
                }
                PREF_EXPORT_JSON_LOG -> {
                    exportLog(REQUEST_EXPORT_JSON_LOG, logJsonFile.name)
                }
                PREF_CLEAR_LOG -> {
                    clearLog()
                }
                PREF_DELETE_DATABASES -> {
                    deleteDatabases()
                }
                PREF_ABOUT -> {
                    activity.startActivity(
                        Intent(
                            Intent.ACTION_VIEW, Uri.parse("https://github.com/Dr-TSNG/TwiFucker")
                        )
                    )
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
                            REQUEST_EXPORT_LOG -> {
                                logFile.inputStream().use { input ->
                                    input.copyTo(out)
                                }
                            }
                            REQUEST_EXPORT_JSON_LOG -> {
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

        private fun deleteDatabases() {
            val re = Regex("""^\d+-\d+(-versioncode-\d+)?.db$""")
            var count = 0
            context.databaseList().forEach { db ->
                if (re.matches(db)) {
                    context.deleteDatabase(db)
                    count++
                }
            }
            if (count > 0) {
                Log.toast(context.getString(R.string.deleted_n_database, count))
            }
        }

        private fun exportLog(logType: Int, fileName: String) {
            when (logType) {
                REQUEST_EXPORT_LOG -> {
                    if (!logFile.exists()) return
                }
                REQUEST_EXPORT_JSON_LOG -> {
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

        private fun onCustomizeHiddenDrawerItems() {
            AlertDialog.Builder(activity).apply {
                val items = drawerItems
                val ids = items.map { it.key }.toTypedArray()
                setTitle(R.string.hide_drawer_items)
                setPositiveButton(R.string.save) { _, _ ->
                    val hideItems = mutableSetOf<String>()
                    items.forEach {
                        if (it.showing.not()) {
                            hideItems.add(it.key)
                        }
                    }
                    modulePrefs.edit().putStringSet(PREF_HIDDEN_DRAWER_ITEMS, hideItems).apply()
                }
                setNeutralButton(R.string.reset) { _, _ ->
                    modulePrefs.edit().remove(PREF_HIDDEN_DRAWER_ITEMS).apply()
                }
                setNegativeButton(R.string.settings_dismiss, null)
                val showings = BooleanArray(items.size) { i ->
                    !items[i].showing
                }
                setMultiChoiceItems(ids, showings) { _, which, isChecked ->
                    items[which].showing = !isChecked
                }
            }.show()
        }

        private fun onCustomizeHiddenBottomNavbarItems() {
            AlertDialog.Builder(activity).apply {
                val items = bottomNavbarItems
                val ids = items.map { it.key }.toTypedArray()
                setTitle(R.string.hide_bottom_navbar_items)
                setPositiveButton(R.string.save) { _, _ ->
                    val hideItems = mutableSetOf<String>()
                    items.forEach {
                        if (it.showing.not()) {
                            hideItems.add(it.key)
                        }
                    }
                    modulePrefs.edit().putStringSet(PREF_HIDDEN_BOTTOM_NAVBAR_ITEMS, hideItems)
                        .apply()
                }
                setNeutralButton(R.string.reset) { _, _ ->
                    modulePrefs.edit().remove(PREF_HIDDEN_BOTTOM_NAVBAR_ITEMS).apply()
                }
                setNegativeButton(R.string.settings_dismiss, null)
                val showings = BooleanArray(items.size) { i ->
                    !items[i].showing
                }
                setMultiChoiceItems(ids, showings) { _, which, isChecked ->
                    items[which].showing = !isChecked
                }
            }.show()
        }
    }

    init {
        addModuleAssetPath(context)

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
