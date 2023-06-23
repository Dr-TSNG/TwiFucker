package icu.nullptr.twifucker

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.Build
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.modulePath
import com.github.kyuubiran.ezxhelper.HookFactory
import com.github.kyuubiran.ezxhelper.Log
import com.tencent.mmkv.MMKV
import de.robv.android.xposed.XC_MethodHook
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

val reGenericClass by lazy { Regex("""^(\w+)<(\w+)>$""") }

val logFileDir by lazy { File(appContext.externalCacheDir?.absolutePath + "/twifucker_log/") }

val logFile by lazy { File(logFileDir, "log.txt") }

val logJsonFile by lazy { File(logFileDir, "log_json.txt") }

@Suppress("DEPRECATION")
val packageInfo: PackageInfo by lazy {
    appContext.packageManager.getPackageInfo(
        appContext.packageName, 0
    )
}
val hostVersionName: String by lazy {
    packageInfo.versionName
}
@Suppress("DEPRECATION")
val hostVersionCode by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode
    }
}
val hostAppLastUpdate by lazy {
    packageInfo.lastUpdateTime
}
val moduleLastModify by lazy {
    File(modulePath).lastModified()
}

val modulePrefs: MMKV by lazy {
    nativeLoadPrefs()
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

external fun nativeLoadPrefs(): MMKV

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
    // who to subscribe (a.k.a. Creators for you) module
    if (entryId.startsWith("who-to-subscribe-") && modulePrefs.getBoolean("disable_who_to_subscribe", false)) {
        return true
    }
    // topics to follow module
    if (entryId.startsWith("TopicsModule-") && modulePrefs.getBoolean(
            "disable_topics_to_follow", false
        )
    ) {
        return true
    }
    // tweet detail related tweets
    if (entryId.startsWith("tweetdetailrelatedtweets-") && modulePrefs.getBoolean(
            "disable_tweet_detail_related_tweets", false
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

inline fun JSONArray.forEach(action: (JSONObject) -> Unit) {
    (0 until this.length()).forEach { i ->
        if (this[i] is JSONObject) {
            action(this[i] as JSONObject)
        }
    }
}

inline fun JSONArray.forEachIndexed(action: (index: Int, JSONObject) -> Unit) {
    (0 until this.length()).forEach { i ->
        if (this[i] is JSONObject) {
            action(i, this[i] as JSONObject)
        }
    }
}

inline fun HookFactory.replaceMeasure(
    name: String,
    crossinline block: (XC_MethodHook.MethodHookParam) -> Any?
) {
    replace {
        val start = System.currentTimeMillis()
        val ret = block(it)
        val end = System.currentTimeMillis()
        val elapsed = end - start
        if (elapsed > 10) {
            Log.d("$name elapsed: ${System.currentTimeMillis() - start}ms")
        }
        return@replace ret
    }
}

inline fun HookFactory.beforeMeasure(
    name: String,
    crossinline block: (XC_MethodHook.MethodHookParam) -> Unit
) {
    before {
        val start = System.currentTimeMillis()
        block(it)
        val end = System.currentTimeMillis()
        val elapsed = end - start
        if (elapsed > 10) {
            Log.d("$name elapsed: ${System.currentTimeMillis() - start}ms")
        }
    }
}

inline fun HookFactory.afterMeasure(
    name: String,
    crossinline block: (XC_MethodHook.MethodHookParam) -> Unit
) {
    after {
        val start = System.currentTimeMillis()
        block(it)
        val end = System.currentTimeMillis()
        val elapsed = end - start
        if (elapsed > 10) {
            Log.d("$name elapsed: ${System.currentTimeMillis() - start}ms")
        }
    }
}
