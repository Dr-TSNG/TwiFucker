package icu.nullptr.twifucker

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.utils.Log
import icu.nullptr.twifucker.ui.SettingsDialog
import java.io.File


val logFileDir by lazy { File(appContext.externalCacheDir?.absolutePath + "/twifucker_log/") }

val logFile by lazy { File(logFileDir, "log.txt") }

val logJsonFile by lazy { File(logFileDir, "log_json.txt") }

@Suppress("DEPRECATION")
val modulePrefs: SharedPreferences by lazy {
    appContext.getSharedPreferences(
        SettingsDialog.PREFS_NAME, Context.MODE_MULTI_PROCESS
    )
}

@Suppress("DEPRECATION")
val hostPrefs: SharedPreferences by lazy {
    appContext.getSharedPreferences(
        appContext.packageName + "_preferences", Context.MODE_MULTI_PROCESS
    )
}

@SuppressLint("DiscouragedApi")
fun getId(name: String, defType: String): Int {
    return appContext.resources.getIdentifier(
        name, defType, appContext.packageName
    )
}

fun writeJsonLog(content: String) {
    try {
        if (!logFileDir.exists()) logFileDir.mkdirs()
        if (!logJsonFile.exists()) logJsonFile.createNewFile()
        logJsonFile.appendText(content + "\n")
    } catch (t: Throwable) {
        Log.e(t)
    }
}

fun isEntryNeedsRemove(entryId: String): Boolean {
    // promoted tweet
    if (entryId.startsWith("promotedTweet-") && modulePrefs.getBoolean(
            "disable_promoted_content", true
        )
    ) {
        return true
    }
    // who to follow module
    if ((entryId.startsWith("whoToFollow-") || entryId.startsWith("who-to-follow-") || entryId.startsWith(
            "connect-module-"
        )) && modulePrefs.getBoolean(
            "disable_who_to_follow", false
        )
    ) {
        return true
    }
    // topics to follow module
    if (entryId.startsWith("TopicsModule-") && modulePrefs.getBoolean(
            "disable_topics_to_follow", false
        )
    ) {
        return true
    }
    return false
}

fun clearUrlQueries(url: String): String {
    return url.split("?")[0]
}

fun getUrlExtension(url: String): String {
    val urlWithoutQueries = clearUrlQueries(url)
    return urlWithoutQueries.substring(urlWithoutQueries.lastIndexOf(".") + 1)
}

fun genOrigUrl(url: String): String {
    val urlWithoutQueries = clearUrlQueries(url)
    val urlWithoutExt = urlWithoutQueries.substring(0, urlWithoutQueries.lastIndexOf("."))
    val ext = getUrlExtension(urlWithoutQueries)
    return "$urlWithoutExt?format=$ext&name=orig"
}
