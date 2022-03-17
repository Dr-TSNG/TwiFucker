package icu.nullptr.twifucker

import com.github.kyuubiran.ezxhelper.utils.*
import de.robv.android.xposed.XC_MethodHook
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream

// root
fun JSONObject.jsonGetTweets(): JSONObject? =
    optJSONObject("globalObjects")?.optJSONObject("tweets")

fun JSONObject.jsonGetInstructions(): JSONArray? =
    optJSONObject("timeline")?.optJSONArray("instructions")

fun JSONObject.jsonGetData(): JSONObject? = optJSONObject("data")

// data
fun JSONObject.dataGetInstructions(): JSONArray? =
    optJSONObject("user_result")?.optJSONObject("result")?.optJSONObject("timeline_response")
        ?.optJSONObject("timeline")?.optJSONArray("instructions") ?: optJSONObject(
        "timeline_response"
    )?.optJSONArray("instructions")

fun JSONObject.dataCheckAndRemove() {
    dataGetInstructions()?.forEach<JSONObject> { instruction ->
        instruction.instructionCheckAndRemove()
    }
    dataGetLegacy()?.legacyCheckAndRemove()
}

fun JSONObject.dataGetLegacy(): JSONObject? =
    optJSONObject("tweet_result")?.optJSONObject("result")?.optJSONObject("legacy")

// tweets
fun JSONObject.tweetsForEach(action: (JSONObject) -> Unit) {
    for (i in keys()) {
        optJSONObject(i)?.let { action(it) }
    }
}

// tweet
fun JSONObject.tweetGetExtendedEntitiesMedias(): JSONArray? =
    optJSONObject("extended_entities")?.optJSONArray("media")

fun JSONObject.tweetCheckAndRemove() {
    tweetGetExtendedEntitiesMedias()?.forEach<JSONObject> { media ->
        media.mediaCheckAndRemove()
    }
}

// entry
fun JSONObject.entryHasPromotedMetadata(): Boolean =
    optJSONObject("content")?.optJSONObject("item")?.optJSONObject("content")
        ?.optJSONObject("tweet")
        ?.has("promotedMetadata") == true || optJSONObject("content")?.optJSONObject("content")
        ?.has("tweetPromotedMetadata") == true

fun JSONObject.entryHasWhoToFollow(): Boolean = optString("entryId").startsWith("whoToFollow-")

fun JSONObject.entryGetWhoToFollowItems(): JSONArray? =
    optJSONObject("content")?.optJSONArray("items")
        ?: optJSONObject("content")?.optJSONObject("timelineModule")?.optJSONArray("items")

fun JSONObject.entryIsTweet(): Boolean = optString("entryId").startsWith("tweet-")

fun JSONObject.entryGetLegacy(): JSONObject? =
    optJSONObject("content")?.optJSONObject("content")?.optJSONObject("tweetResult")
        ?.optJSONObject("result")?.optJSONObject("legacy")

// legacy
fun JSONObject.legacyGetRetweetedStatusLegacy(): JSONObject? =
    optJSONObject("retweeted_status_result")?.optJSONObject("result")?.optJSONObject("legacy")

fun JSONObject.legacyGetExtendedEntitiesMedias(): JSONArray? =
    optJSONObject("extended_entities")?.optJSONArray("media")

fun JSONObject.legacyCheckAndRemove() {
    legacyGetExtendedEntitiesMedias()?.forEach<JSONObject> { media ->
        media.mediaCheckAndRemove()
    }
    legacyGetRetweetedStatusLegacy()?.legacyGetExtendedEntitiesMedias()
        ?.forEach<JSONObject> { media ->
            media.mediaCheckAndRemove()
        }
}

// item
fun JSONObject.itemContainsPromotedUser(): Boolean =
    optJSONObject("item")?.optJSONObject("content")?.has("userPromotedMetadata") == true

// timeline
fun JSONObject.timelinePinEntry(): JSONObject? = optJSONObject("entry")
fun JSONObject.timelineAddEntries(): JSONArray? = optJSONArray("entries")

// instruction
fun JSONObject.instructionIsTimelineAddEntries(): Boolean =
    optString("__typename") == "TimelineAddEntries"

fun JSONObject.instructionIsTimelinePinEntry(): Boolean =
    optString("__typename") == "TimelinePinEntry"

fun JSONObject.instructionGetAddEntries(): JSONArray? =
    optJSONObject("addEntries")?.optJSONArray("entries")

fun JSONObject.instructionCheckAndRemove() {
    if (instructionIsTimelineAddEntries()) {
        timelineAddEntries()?.entriesRemoveAnnoyance()
    }
    if (instructionIsTimelinePinEntry()) {
        timelinePinEntry()?.entryRemoveSensitiveMediaWarning()
    }
    instructionGetAddEntries()?.entriesRemoveAnnoyance()
}

// media
fun JSONObject.mediaHasSensitiveMediaWarning(): Boolean =
    has("sensitive_media_warning") || has("ext_sensitive_media_warning")

fun JSONObject.mediaRemoveSensitiveMediaWarning() {
    remove("sensitive_media_warning")
    remove("ext_sensitive_media_warning")
}

fun JSONObject.mediaCheckAndRemove() {
    if (mediaHasSensitiveMediaWarning()) {
        mediaRemoveSensitiveMediaWarning()
        Log.d("Handle sensitive media warning")
    }
}

// entries
fun JSONArray.entriesRemoveTimelineAds() {
    val removeIndex = mutableListOf<Int>()
    forEachIndexed<JSONObject> { index, entry ->
        if (entry.entryHasPromotedMetadata()) {
            removeIndex.add(index)
            Log.d("Handle timeline ads $index")
        }
    }
    for (i in removeIndex.reversed()) {
        remove(i)
    }
}

fun JSONArray.entriesRemovePromotedWhoToFollow() {
    forEach<JSONObject> { entry ->
        if (entry.entryHasWhoToFollow()) {
            val items = entry.entryGetWhoToFollowItems()
            val removeIndex = mutableListOf<Int>()
            items?.forEachIndexed<JSONObject> { index, item ->
                item.itemContainsPromotedUser().let {
                    if (it) {
                        removeIndex.add(index)
                        Log.d("Handle whoToFollow promoted user $index")
                    }
                }
            }
            for (i in removeIndex.reversed()) {
                items?.remove(i)
            }
        }
    }
}

fun JSONObject.entryRemoveSensitiveMediaWarning() {
    if (entryIsTweet()) {
        entryGetLegacy()?.let {
            it.legacyGetExtendedEntitiesMedias()?.forEach<JSONObject> { media ->
                media.mediaCheckAndRemove()
            }
            it.legacyGetRetweetedStatusLegacy()?.legacyGetExtendedEntitiesMedias()
                ?.forEach<JSONObject> { media ->
                    media.mediaCheckAndRemove()
                }
        }
    }
}

fun JSONArray.entriesRemoveSensitiveMediaWarning() {
    forEach<JSONObject> { entry ->
        entry.entryRemoveSensitiveMediaWarning()
    }
}

fun JSONArray.entriesRemoveAnnoyance() {
    entriesRemoveTimelineAds()
    entriesRemovePromotedWhoToFollow()
    entriesRemoveSensitiveMediaWarning()
}


fun handleJson(param: XC_MethodHook.MethodHookParam) {
    val inputStream = param.result as InputStream
    val reader = BufferedReader(inputStream.reader())
    var content: String
    reader.use { r ->
        content = r.readText()
    }

    try {
        val json = JSONObject(content)

        json.jsonGetTweets()?.tweetsForEach { tweet ->
            tweet.tweetCheckAndRemove()
        }
        json.jsonGetInstructions()?.forEach<JSONObject> { instruction ->
            instruction.instructionCheckAndRemove()
        }
        json.jsonGetData()?.dataCheckAndRemove()

        content = json.toString()
    } catch (_: JSONException) {
    } catch (e: Throwable) {
        Log.e(e)
    }
    param.result = content.byteInputStream()
}

fun jsonHook() {
    try {
        findMethod("com.fasterxml.jackson.core.b") { name == "i" }.hookAfter { param ->
            handleJson(param)
        }
    } catch (e: Throwable) {
        Log.e(e)
    }
}
