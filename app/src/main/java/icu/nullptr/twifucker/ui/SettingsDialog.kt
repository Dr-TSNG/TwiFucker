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
import android.preference.PreferenceGroup
import android.preference.SwitchPreference
import androidx.documentfile.provider.DocumentFile
import com.github.kyuubiran.ezxhelper.AndroidLogger
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
        const val REQUEST_SET_DOWNLOAD_DIRECTORY = 1003

        const val PREF_DISABLE_PROMOTED_CONTENT = "disable_promoted_content"
        const val PREF_HIDE_DRAWER_ITEMS = "hide_drawer_items"
        const val PREF_HIDE_BOTTOM_NAVBAR_ITEMS = "hide_bottom_navbar_items"
        const val PREF_ENABLE_LOG = "enable_log"
        const val PREF_EXPORT_LOG = "export_log"
        const val PREF_EXPORT_JSON_LOG = "export_json_log"
        const val PREF_CLEAR_LOG = "clear_log"
        const val PREF_DELETE_DATABASES = "delete_databases"
        const val PREF_ABOUT = "about"

        const val PREF_DOWNLOAD_DIRECTORY = "download_directory"
        const val PREF_HIDDEN_DRAWER_ITEMS = "hidden_drawer_items"
        const val PREF_HIDDEN_BOTTOM_NAVBAR_ITEMS = "hidden_bottom_navbar_items"
        const val PREF_FEATURE_SWITCH = "feature_switch"
        const val PREF_VERSION = "version"
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
            AndroidLogger.toast(context.getString(R.string.deleted_n_promoted_tweet, count))
        }
    }

    class PrefsFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {
        @Deprecated("Deprecated in Java")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.settings_dialog)

            for (i in 0 until preferenceScreen.preferenceCount) {
                val p = preferenceScreen.getPreference(i)
                if (p is SwitchPreference) {
                    if (modulePrefs.containsKey(p.key)) {
                        p.isChecked = modulePrefs.getBoolean(p.key, false)
                    }
                    p.onPreferenceChangeListener = this
                } else if (p is Preference) {
                    p.onPreferenceClickListener = this
                }
                if (p is PreferenceGroup) {
                    for (j in 0 until p.preferenceCount) {
                        val p2 = p.getPreference(j)
                        if (p2 is SwitchPreference) {
                            if (modulePrefs.containsKey(p2.key)) {
                                p2.isChecked = modulePrefs.getBoolean(p2.key, false)
                            }
                            p2.onPreferenceChangeListener = this
                        } else if (p2 is Preference) {
                            p2.onPreferenceClickListener = this
                        }
                    }
                }
            }

            refreshDownloadDirectory()

            findPreference(PREF_VERSION).summary =
                "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

            if (BuildConfig.DEBUG) {
                findPreference(PREF_DELETE_DATABASES).isEnabled = true
            }
        }

        private fun refreshDownloadDirectory() {
            val downloadDirectory = modulePrefs.getString(
                PREF_DOWNLOAD_DIRECTORY, null
            )
            if (downloadDirectory == null) {
                findPreference(PREF_DOWNLOAD_DIRECTORY).summary = ""
                return
            }
            Uri.parse(
                downloadDirectory
            )?.let { uri1 ->
                DocumentFile.fromTreeUri(
                    context, uri1
                )?.uri?.let { uri2 ->
                    findPreference(PREF_DOWNLOAD_DIRECTORY).summary = uri2.path?.split(":")?.last()
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPreferenceChange(p0: Preference?, p1: Any?): Boolean {
            if (p0 is SwitchPreference) {
                modulePrefs.putBoolean(p0.key, p1 as Boolean)
                if (p0.key == PREF_ENABLE_LOG) {
                    if (!p1) {
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

                PREF_FEATURE_SWITCH -> {
                    FeatureSwitchDialog(context)
                }

                PREF_DOWNLOAD_DIRECTORY -> {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    startActivityForResult(intent, REQUEST_SET_DOWNLOAD_DIRECTORY)
                }
            }
            return true
        }

        @Deprecated("Deprecated in Java")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            when (requestCode) {
                REQUEST_EXPORT_LOG, REQUEST_EXPORT_JSON_LOG -> {
                    if (resultCode != Activity.RESULT_OK) return
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

                REQUEST_SET_DOWNLOAD_DIRECTORY -> {
                    if (resultCode != Activity.RESULT_OK) {
                        modulePrefs.remove(PREF_DOWNLOAD_DIRECTORY)
                        refreshDownloadDirectory()
                        return
                    }
                    data?.data?.let { uri ->
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        modulePrefs.putString(PREF_DOWNLOAD_DIRECTORY, uri.toString())
                    }
                    refreshDownloadDirectory()
                }

                else -> {
                    super.onActivityResult(requestCode, resultCode, data)
                }
            }
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
                AndroidLogger.toast(context.getString(R.string.deleted_n_database, count))
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
                    modulePrefs.putStringSet(PREF_HIDDEN_DRAWER_ITEMS, hideItems)
                }
                setNeutralButton(R.string.reset) { _, _ ->
                    modulePrefs.remove(PREF_HIDDEN_DRAWER_ITEMS)
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
                    modulePrefs.putStringSet(PREF_HIDDEN_BOTTOM_NAVBAR_ITEMS, hideItems)

                }
                setNeutralButton(R.string.reset) { _, _ ->
                    modulePrefs.remove(PREF_HIDDEN_BOTTOM_NAVBAR_ITEMS)
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
