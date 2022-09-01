package icu.nullptr.twifucker

import android.annotation.SuppressLint
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

fun isEntryNeedsRemove(entryId: String): Boolean {
    // promoted tweet
    if (entryId.startsWith("promotedTweet-") && modulePrefs.getBoolean(
            "disable_promoted_content", true
        )
    ) {
        return true
    }
    // who to follow module
    if (entryId.startsWith("whoToFollow-") && modulePrefs.getBoolean(
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
