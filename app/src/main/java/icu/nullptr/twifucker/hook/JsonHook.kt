package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.utils.*
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.twifucker.modulePrefs
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream

object JsonHook : BaseHook() {
    override fun init() {
        try {
            val jsonClass =
                findField("com.bluelinelabs.logansquare.LoganSquare") { name == "JSON_FACTORY" }.type
            Log.d("Located json class ${jsonClass.simpleName}")
            // com.fasterxml.jackson.core.JsonFactory
            val jsonMethod = findMethod(jsonClass) {
                isFinal && parameterTypes.size >= 1 && parameterTypes[0] == InputStream::class.java
            }
            Log.d("Located json method ${jsonMethod.name}")
            jsonMethod.hookBefore { param ->
                try {
                    handleJson(param)
                } catch (t: Throwable) {
                    Log.e(t)
                }
            }
        } catch (e: NoSuchFieldException) {
            Log.d("Failed to relocate json field", e)
        } catch (e: NoSuchMethodException) {
            Log.d("Failed to relocate json method", e)
        } catch (e: Throwable) {
            Log.e("json hook failed", e)
        }
    }

    // root
    private fun JSONObject.jsonGetTweets(): JSONObject? =
        optJSONObject("globalObjects")?.optJSONObject("tweets")

    private fun JSONObject.jsonGetInstructions(): JSONArray? =
        optJSONObject("timeline")?.optJSONArray("instructions")

    private fun JSONObject.jsonGetData(): JSONObject? = optJSONObject("data")

    private fun JSONObject.jsonHasRecommendedUsers(): Boolean = has("recommended_users")

    private fun JSONObject.jsonRemoveRecommendedUsers() {
        remove("recommended_users")
    }

    private fun JSONObject.jsonCheckAndRemoveRecommendedUsers() {
        if (modulePrefs.getBoolean(
                "disable_recommended_users", false
            ) && jsonHasRecommendedUsers()
        ) {
            Log.d("Handle recommended users: $this}")
            jsonRemoveRecommendedUsers()
        }
    }

    // data
    private fun JSONObject.dataGetInstructions(): JSONArray? =
        optJSONObject("user_result")?.optJSONObject("result")?.optJSONObject("timeline_response")
            ?.optJSONObject("timeline")?.optJSONArray("instructions") ?: optJSONObject(
            "timeline_response"
        )?.optJSONArray("instructions")

    private fun JSONObject.dataCheckAndRemove() {
        dataGetInstructions()?.forEach { instruction ->
            (instruction as JSONObject).instructionCheckAndRemove()
        }
        dataGetLegacy()?.legacyCheckAndRemove()
    }

    private fun JSONObject.dataGetLegacy(): JSONObject? =
        optJSONObject("tweet_result")?.optJSONObject("result")?.optJSONObject("legacy")

    // tweets
    private fun JSONObject.tweetsForEach(action: (JSONObject) -> Unit) {
        for (i in keys()) {
            optJSONObject(i)?.let { action(it) }
        }
    }

    // tweet
    private fun JSONObject.tweetGetExtendedEntitiesMedias(): JSONArray? =
        optJSONObject("extended_entities")?.optJSONArray("media")

    private fun JSONObject.tweetCheckAndRemove() {
        tweetGetExtendedEntitiesMedias()?.forEach { media ->
            (media as JSONObject).mediaCheckAndRemove()
        }
    }

    // entry
    private fun JSONObject.entryHasPromotedMetadata(): Boolean =
        optJSONObject("content")?.optJSONObject("item")?.optJSONObject("content")
            ?.optJSONObject("tweet")
            ?.has("promotedMetadata") == true || optJSONObject("content")?.optJSONObject("content")
            ?.has("tweetPromotedMetadata") == true

    private fun JSONObject.entryIsWhoToFollow(): Boolean =
        optString("entryId").startsWith("whoToFollow-")

    private fun JSONObject.entryIsTopicsModule(): Boolean =
        optString("entryId").startsWith("TopicsModule-")

    private fun JSONObject.entryGetContentItems(): JSONArray? =
        optJSONObject("content")?.optJSONArray("items")
            ?: optJSONObject("content")?.optJSONObject("timelineModule")?.optJSONArray("items")

    private fun JSONObject.entryIsTweet(): Boolean = optString("entryId").startsWith("tweet-")
    private fun JSONObject.entryIsConversationThread(): Boolean =
        optString("entryId").startsWith("conversationthread-")

    private fun JSONObject.entryGetLegacy(): JSONObject? {
        val temp = when {
            has("content") -> {
                optJSONObject("content")
            }
            has("item") -> {
                optJSONObject("item")
            }
            else -> return null
        }
        return temp?.optJSONObject("content")?.optJSONObject("tweetResult")?.optJSONObject("result")
            ?.optJSONObject("legacy")
    }

    private fun JSONObject.entryGetTrends(): JSONArray? =
        optJSONObject("content")?.optJSONObject("timelineModule")?.optJSONArray("items")

    // trend
    private fun JSONObject.trendHasPromotedMetadata(): Boolean =
        optJSONObject("item")?.optJSONObject("content")?.optJSONObject("trend")
            ?.has("promotedMetadata") == true

    private fun JSONArray.trendRemoveAds() {
        if (!modulePrefs.getBoolean("disable_promoted_trends", true)) return
        val trendRemoveIndex = mutableListOf<Int>()
        forEachIndexed { trendIndex, trend ->
            if ((trend as JSONObject).trendHasPromotedMetadata()) {
                Log.d("Handle trends ads $trendIndex $trend")
                trendRemoveIndex.add(trendIndex)
            }
        }
        for (i in trendRemoveIndex.asReversed()) {
            remove(i)
        }
    }

    // legacy
    private fun JSONObject.legacyGetRetweetedStatusLegacy(): JSONObject? =
        optJSONObject("retweeted_status_result")?.optJSONObject("result")?.optJSONObject("legacy")

    private fun JSONObject.legacyGetExtendedEntitiesMedias(): JSONArray? =
        optJSONObject("extended_entities")?.optJSONArray("media")

    private fun JSONObject.legacyCheckAndRemove() {
        legacyGetExtendedEntitiesMedias()?.forEach { media ->
            (media as JSONObject).mediaCheckAndRemove()
        }
        legacyGetRetweetedStatusLegacy()?.legacyGetExtendedEntitiesMedias()?.forEach { media ->
            (media as JSONObject).mediaCheckAndRemove()
        }
    }

    // item
    private fun JSONObject.itemContainsPromotedUser(): Boolean =
        optJSONObject("item")?.optJSONObject("content")
            ?.has("userPromotedMetadata") == true || optJSONObject("item")?.optJSONObject("content")
            ?.optJSONObject("user")
            ?.has("userPromotedMetadata") == true || optJSONObject("item")?.optJSONObject("content")
            ?.optJSONObject("user")?.has("promotedMetadata") == true

    // instruction
    private fun JSONObject.instructionTimelinePinEntry(): JSONObject? = optJSONObject("entry")
    private fun JSONObject.instructionTimelineAddEntries(): JSONArray? = optJSONArray("entries")

    private fun JSONObject.instructionGetAddEntries(): JSONArray? =
        optJSONObject("addEntries")?.optJSONArray("entries")

    private fun JSONObject.instructionCheckAndRemove() {
        instructionTimelinePinEntry()?.entryRemoveSensitiveMediaWarning()
        instructionTimelineAddEntries()?.entriesRemoveAnnoyance()
        instructionGetAddEntries()?.entriesRemoveAnnoyance()
    }

    // media
    private fun JSONObject.mediaHasSensitiveMediaWarning(): Boolean =
        has("sensitive_media_warning") || (has("ext_sensitive_media_warning") && optJSONObject("ext_sensitive_media_warning") != null)

    private fun JSONObject.mediaRemoveSensitiveMediaWarning() {
        remove("sensitive_media_warning")
        remove("ext_sensitive_media_warning")
    }

    private fun JSONObject.mediaCheckAndRemove() {
        if (!modulePrefs.getBoolean("disable_sensitive_media_warning", false)) return
        if (mediaHasSensitiveMediaWarning()) {
            Log.d("Handle sensitive media warning $this")
            mediaRemoveSensitiveMediaWarning()
        }
    }

    // entries
    private fun JSONArray.entriesRemoveTimelineAds() {
        val removeIndex = mutableListOf<Int>()
        forEachIndexed { entryIndex, entry ->
            (entry as JSONObject).entryGetTrends()?.trendRemoveAds()

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

    private fun JSONArray.entriesRemoveWhoToFollow() {
        val entryRemoveIndex = mutableListOf<Int>()
        forEachIndexed { entryIndex, entry ->
            if (!(entry as JSONObject).entryIsWhoToFollow()) return@forEachIndexed

            if (modulePrefs.getBoolean("disable_who_to_follow", false)) {
                Log.d("Handle whoToFollow $entryIndex $entry")
                entryRemoveIndex.add(entryIndex)
                return@forEachIndexed
            }

            if (!modulePrefs.getBoolean("disable_promoted_user", true)) return@forEachIndexed

            val items = entry.entryGetContentItems()
            val userRemoveIndex = mutableListOf<Int>()
            items?.forEachIndexed { index, item ->
                (item as JSONObject).itemContainsPromotedUser().let {
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

    private fun JSONArray.entriesRemoveTopicsToFollow() {
        val entryRemoveIndex = mutableListOf<Int>()
        forEachIndexed { entryIndex, entry ->
            if (!(entry as JSONObject).entryIsTopicsModule()) return@forEachIndexed

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

    private fun JSONObject.entryRemoveSensitiveMediaWarning() {
        if (entryIsTweet()) {
            entryGetLegacy()?.let {
                it.legacyGetExtendedEntitiesMedias()?.forEach { media ->
                    (media as JSONObject).mediaCheckAndRemove()
                }
                it.legacyGetRetweetedStatusLegacy()?.legacyGetExtendedEntitiesMedias()
                    ?.forEach { media ->
                        (media as JSONObject).mediaCheckAndRemove()
                    }
            }
        } else if (entryIsConversationThread()) {
            entryGetContentItems()?.forEach { item ->
                (item as JSONObject).entryGetLegacy()?.let { legacy ->
                    legacy.legacyGetExtendedEntitiesMedias()?.forEach { media ->
                        (media as JSONObject).mediaCheckAndRemove()
                    }
                }
            }
        }
    }

    private fun JSONArray.entriesRemoveSensitiveMediaWarning() {
        forEach { entry ->
            (entry as JSONObject).entryRemoveSensitiveMediaWarning()
        }
    }

    private fun JSONArray.entriesRemoveAnnoyance() {
        entriesRemoveTimelineAds()
        entriesRemoveWhoToFollow()
        entriesRemoveTopicsToFollow()
        entriesRemoveSensitiveMediaWarning()
    }


    private fun handleJson(param: XC_MethodHook.MethodHookParam) {
        val inputStream = param.args[0] as InputStream
        val reader = BufferedReader(inputStream.reader())
        var content: String
        try {
            reader.use { r ->
                content = r.readText()
            }
        } catch (_: java.io.IOException) {
            param.args[0] = object : InputStream() {
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
            json.jsonGetInstructions()?.forEach { instruction ->
                (instruction as JSONObject).instructionCheckAndRemove()
            }
            json.jsonGetData()?.dataCheckAndRemove()

            json.jsonCheckAndRemoveRecommendedUsers()

            content = json.toString()
        } catch (_: JSONException) {
        } catch (e: Throwable) {
            Log.e("json hook failed to parse JSONObject", e)
            Log.d(content)
        }

        try {
            val json = JSONArray(content)
            json.forEach {
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

        param.args[0] = content.byteInputStream()
    }

}
