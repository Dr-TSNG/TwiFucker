package icu.nullptr.twifucker

import com.github.kyuubiran.ezxhelper.utils.*
import de.robv.android.xposed.XC_MethodHook
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream

fun removeTimelineAds(entries: JSONArray) {
    val removeIndex = mutableListOf<Int>()
    entries.forEachIndexed<JSONObject> { index, entry ->
        if (entry.optString("entryId").startsWith("promotedTweet-")) {
            removeIndex.add(index)
            Log.d("Handle timeline ads")
        }
    }
    for (i in removeIndex.reversed()) {
        entries.remove(i)
    }
}

fun removePromotedWhoToFollow(entries: JSONArray) {
    entries.forEach<JSONObject> { entry ->
        if (entry.optString("entryId").startsWith("whoToFollow-")) {
            val items = entry.optJSONObject("content")?.optJSONArray("items")
            val removeIndex = mutableListOf<Int>()
            items?.forEachIndexed<JSONObject> { index, item ->
                item.optJSONObject("item")?.optJSONObject("content")?.has("userPromotedMetadata")
                    ?.let {
                        if (it) {
                            removeIndex.add(index)
                            Log.d("Handle whoToFollow promoted user")
                        }
                    }
            }
            for (i in removeIndex.reversed()) {
                items?.remove(i)
            }
        }
    }
}

fun removeSensitiveMediaWarning(entries: JSONArray) {
    entries.forEach<JSONObject> { entry ->
        if (entry.optString("entryId").startsWith("tweet-")) {
            entry.optJSONObject("content")?.optJSONObject("content")?.optJSONObject("tweetResult")
                ?.optJSONObject("result")?.optJSONObject("legacy")
                ?.optJSONObject("extended_entities")?.optJSONArray("media")
                ?.forEach<JSONObject> { media ->
                    if (media.has("sensitive_media_warning")) {
                        media.remove("sensitive_media_warning")
                        Log.d("Handle tweet sensitive media warning")
                    }
                }
        }
    }
}

fun doHandleJson(param: XC_MethodHook.MethodHookParam) {
    val inputStream = param.result as InputStream
    val reader = BufferedReader(inputStream.reader())
    var content: String
    reader.use { r ->
        content = r.readText()
    }

    try {
        val json = JSONObject(content)

        // home timeline
        json.optJSONObject("globalObjects")?.optJSONObject("tweets")?.let { tweets ->
            tweets.keys().forEach { tweet ->
                tweets.optJSONObject(tweet)?.optJSONObject("extended_entities")
                    ?.optJSONArray("media")?.forEach<JSONObject> { media ->
                        if (media.has("sensitive_media_warning")) {
                            media.remove("sensitive_media_warning")
                            Log.d("Handle timeline sensitive media warning")
                        }
                    }
            }
        }
        json.optJSONObject("timeline")?.optJSONArray("instructions")
            ?.forEach<JSONObject> { instruction ->
                instruction.optJSONObject("addEntries")?.optJSONArray("entries")?.let {
                    removeTimelineAds(it)
                }
            }

        json.optJSONObject("data")?.let { data ->
            // user profile timeline query
            data.optJSONObject("user_result")?.optJSONObject("result")
                ?.optJSONObject("timeline_response")?.optJSONObject("timeline")
                ?.optJSONArray("instructions")?.forEach<JSONObject> { instruction ->
                    if (instruction.optString("__typename") == "TimelineAddEntries") {
                        instruction.optJSONArray("entries")?.let {
                            removeSensitiveMediaWarning(it)
                            removePromotedWhoToFollow(it)
                            removeTimelineAds(it)
                        }
                    }
                }
            // tweet details
            data.optJSONObject("timeline_response")?.optJSONArray("instructions")
                ?.forEach<JSONObject> { instruction ->
                    instruction.optJSONArray("entries")?.let {
                        removeSensitiveMediaWarning(it)
                    }
                }
        }
        content = json.toString()
    } catch (_: JSONException) {
    }
    param.result = content.byteInputStream()
}

fun jsonHook() {
    try {
        findMethod("com.fasterxml.jackson.core.b") { name == "i" }.hookAfter { param ->
            doHandleJson(param)
        }
    } catch (e: Throwable) {
        Log.e(e)
    }
}