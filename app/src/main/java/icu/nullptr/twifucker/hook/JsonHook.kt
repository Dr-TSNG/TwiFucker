package icu.nullptr.twifucker.hook

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

fun JSONObject.entryIsWhoToFollow(): Boolean = optString("entryId").startsWith("whoToFollow-")

fun JSONObject.entryIsTopicsModule(): Boolean = optString("entryId").startsWith("TopicsModule-")

fun JSONObject.entryGetContentItems(): JSONArray? =
    optJSONObject("content")?.optJSONArray("items")
        ?: optJSONObject("content")?.optJSONObject("timelineModule")?.optJSONArray("items")

fun JSONObject.entryIsTweet(): Boolean = optString("entryId").startsWith("tweet-")
fun JSONObject.entryIsConversationThread(): Boolean =
    optString("entryId").startsWith("conversationthread-")

fun JSONObject.entryGetLegacy(): JSONObject? {
    val temp = when {
        has("content") -> {
            optJSONObject("content")
        }
        has("item") -> {
            optJSONObject("item")
        }
        else -> return null
    }
    return temp?.optJSONObject("content")?.optJSONObject("tweetResult")
        ?.optJSONObject("result")?.optJSONObject("legacy")
}

fun JSONObject.entryGetTrends(): JSONArray? =
    optJSONObject("content")?.optJSONObject("timelineModule")?.optJSONArray("items")

// trend
fun JSONObject.trendHasPromotedMetadata(): Boolean =
    optJSONObject("item")?.optJSONObject("content")?.optJSONObject("trend")
        ?.has("promotedMetadata") == true

fun JSONArray.trendRemoveAds() {
    if (!modulePrefs.getBoolean("disable_promoted_trends", true)) return
    val trendRemoveIndex = mutableListOf<Int>()
    forEachIndexed<JSONObject> { trendIndex, trend ->
        if (trend.trendHasPromotedMetadata()) {
            Log.d("Handle trends ads $trendIndex $trend")
            trendRemoveIndex.add(trendIndex)
        }
    }
    for (i in trendRemoveIndex.asReversed()) {
        remove(i)
    }
}

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
fun JSONObject.itemContainsPromotedUser(): Boolean = optJSONObject("item")?.optJSONObject("content")
    ?.has("userPromotedMetadata") == true || optJSONObject("item")?.optJSONObject("content")
    ?.optJSONObject("user")
    ?.has("userPromotedMetadata") == true || optJSONObject("item")?.optJSONObject("content")
    ?.optJSONObject("user")?.has("promotedMetadata") == true

// instruction
fun JSONObject.instructionTimelinePinEntry(): JSONObject? = optJSONObject("entry")
fun JSONObject.instructionTimelineAddEntries(): JSONArray? = optJSONArray("entries")

fun JSONObject.instructionGetAddEntries(): JSONArray? =
    optJSONObject("addEntries")?.optJSONArray("entries")

fun JSONObject.instructionCheckAndRemove() {
    instructionTimelinePinEntry()?.entryRemoveSensitiveMediaWarning()
    instructionTimelineAddEntries()?.entriesRemoveAnnoyance()
    instructionGetAddEntries()?.entriesRemoveAnnoyance()
}

// media
fun JSONObject.mediaHasSensitiveMediaWarning(): Boolean =
    has("sensitive_media_warning") || (has("ext_sensitive_media_warning") && optJSONObject("ext_sensitive_media_warning") != null)

fun JSONObject.mediaRemoveSensitiveMediaWarning() {
    remove("sensitive_media_warning")
    remove("ext_sensitive_media_warning")
}

fun JSONObject.mediaCheckAndRemove() {
    if (!modulePrefs.getBoolean("disable_sensitive_media_warning", false)) return
    if (mediaHasSensitiveMediaWarning()) {
        Log.d("Handle sensitive media warning $this")
        mediaRemoveSensitiveMediaWarning()
    }
}

// entries
fun JSONArray.entriesRemoveTimelineAds() {
    val removeIndex = mutableListOf<Int>()
    forEachIndexed<JSONObject> { entryIndex, entry ->
        entry.entryGetTrends()?.trendRemoveAds()

        if (!modulePrefs.getBoolean("disable_promoted_content", true)) return@forEachIndexed
        if (entry.entryHasPromotedMetadata()) {
            Log.d("Handle timeline ads $entryIndex $entry")
            removeIndex.add(entryIndex)
        }
    }
    for (i in removeIndex.reversed()) {
        remove(i)
    }
}

fun JSONArray.entriesRemoveWhoToFollow() {
    val entryRemoveIndex = mutableListOf<Int>()
    forEachIndexed<JSONObject> { entryIndex, entry ->
        if (!entry.entryIsWhoToFollow()) return@forEachIndexed

        if (modulePrefs.getBoolean("disable_who_to_follow", false)) {
            Log.d("Handle whoToFollow $entryIndex $entry")
            entryRemoveIndex.add(entryIndex)
            return@forEachIndexed
        }

        if (!modulePrefs.getBoolean("disable_promoted_user", true)) return@forEachIndexed

        val items = entry.entryGetContentItems()
        val userRemoveIndex = mutableListOf<Int>()
        items?.forEachIndexed<JSONObject> { index, item ->
            item.itemContainsPromotedUser().let {
                if (it) {
                    Log.d("Handle whoToFollow promoted user $index $item")
                    userRemoveIndex.add(index)
                }
            }
        }
        for (i in userRemoveIndex.reversed()) {
            items?.remove(i)
        }
    }
    for (i in entryRemoveIndex.reversed()) {
        remove(i)
    }
}

fun JSONArray.entriesRemoveTopicsToFollow() {
    val entryRemoveIndex = mutableListOf<Int>()
    forEachIndexed<JSONObject> { entryIndex, entry ->
        if (!entry.entryIsTopicsModule()) return@forEachIndexed

        if (modulePrefs.getBoolean("disable_topics_to_follow", false)) {
            Log.d("Handle TopicsModule $entryIndex $entry")
            entryRemoveIndex.add(entryIndex)
            return@forEachIndexed
        }
    }
    for (i in entryRemoveIndex.reversed()) {
        remove(i)
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
    } else if (entryIsConversationThread()) {
        entryGetContentItems()?.forEach<JSONObject> { item ->
            item.entryGetLegacy()?.let { legacy ->
                legacy.legacyGetExtendedEntitiesMedias()?.forEach<JSONObject> { media ->
                    media.mediaCheckAndRemove()
                }
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
    entriesRemoveWhoToFollow()
    entriesRemoveTopicsToFollow()
    entriesRemoveSensitiveMediaWarning()
}


fun handleJson(param: XC_MethodHook.MethodHookParam) {
    val inputStream = param.result as InputStream
    val reader = BufferedReader(inputStream.reader())
    var content: String
    try {
        reader.use { r ->
            content = r.readText()
        }
    } catch (_: java.io.IOException) {
        param.result = object : InputStream() {
            override fun read(): Int {
                return -1
            }
        }
        return
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
        Log.e("json hook failed to parse JSONObject", e)
        Log.d(content)
    }

    try {
        val json = JSONArray(content)
        json.forEach<Any> {
            if (it is JSONObject) {
                it.tweetCheckAndRemove()
            }
        }
        content = json.toString()
    } catch (_: JSONException) {
    } catch (e: Throwable) {
        Log.e("json hook failed to parse JSONArray", e)
        Log.d(content)
    }

    param.result = content.byteInputStream()
}

fun jsonHook() {
    try {
        val jsonClass =
            findField("com.bluelinelabs.logansquare.LoganSquare") { name == "JSON_FACTORY" }.type
        Log.d("Located json class ${jsonClass.simpleName}")
        val jsonMethod = findMethod(jsonClass) {
            isFinal && parameterTypes.size == 2 && parameterTypes[0] == InputStream::class.java && returnType == InputStream::class.java
        }
        Log.d("Located json method ${jsonMethod.name}")
        jsonMethod.hookAfter { param ->
            handleJson(param)
        }
    } catch (e: NoSuchFieldException) {
        Log.d("Failed to relocate json field", e)
    } catch (e: NoSuchMethodException) {
        Log.d("Failed to relocate json method", e)
    } catch (e: Throwable) {
        Log.e("json hook failed", e)
    }
}
