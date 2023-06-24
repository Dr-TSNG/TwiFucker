package icu.nullptr.twifucker.hook

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.FieldFinder
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.twifucker.beforeMeasure
import icu.nullptr.twifucker.forEach
import icu.nullptr.twifucker.forEachIndexed
import icu.nullptr.twifucker.modulePrefs
import icu.nullptr.twifucker.writeJsonLog
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream

object JsonHook : BaseHook() {
    override val name: String
        get() = "JsonHook"

    override fun init() {
        try {
            FieldFinder.fromClass(loadClass("com.bluelinelabs.logansquare.LoganSquare"))
                .filterByName("JSON_FACTORY").first().type.apply {
                    // com.fasterxml.jackson.core.JsonFactory
                    Log.d("Located json class $name")
                }.methodFinder().filterFinal()
                .filterByParamTypes { it.isNotEmpty() && it[0] == InputStream::class.java }.first()
                .apply {
                    Log.d("Located json method $name")
                }.createHook {
                    beforeMeasure(name) { param ->
                        try {
                            handleJson(param)
                        } catch (t: Throwable) {
                            Log.e(t)
                        }
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
            Log.d("Handle recommended users: $this")
            jsonRemoveRecommendedUsers()
        }
    }

    private fun JSONObject.jsonHasThreads(): Boolean = has("threads")

    private fun JSONObject.jsonRemoveThreads() {
        remove("threads")
    }

    private fun JSONObject.jsonCheckAndRemoveThreads() {
        if (modulePrefs.getBoolean("disable_threads", false) && jsonHasThreads()) {
            Log.d("Handle threads: $this")
            jsonRemoveThreads()
        }
    }

    // data
    private fun JSONObject.dataGetInstructions(): JSONArray? {
        val timeline = optJSONObject("user_result")?.optJSONObject("result")
            ?.optJSONObject("timeline_response")?.optJSONObject("timeline")
            ?: optJSONObject("timeline_response")?.optJSONObject("timeline")
            ?: optJSONObject("timeline_response")
            ?: optJSONObject("search")?.optJSONObject("timeline_response")
            ?.optJSONObject("timeline")
        return timeline?.optJSONArray("instructions")
    }

    private fun JSONObject.dataCheckAndRemove() {
        dataGetInstructions()?.forEach { instruction ->
            instruction.instructionCheckAndRemove()
        }
        dataGetLegacy()?.legacyCheckAndRemove()
    }

    private fun JSONObject.dataGetLegacy(): JSONObject? =
        optJSONObject("tweet_result")?.optJSONObject("result")?.let {
            if (it.has("tweet")) {
                it.optJSONObject("tweet")
            } else {
                it
            }
        }?.optJSONObject("legacy")

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
            media.mediaCheckAndRemove()
        }
    }

    // entry
    private fun JSONObject.entryHasPromotedMetadata(): Boolean =
        optJSONObject("content")?.optJSONObject("item")?.optJSONObject("content")
            ?.optJSONObject("tweet")
            ?.has("promotedMetadata") == true || optJSONObject("content")?.optJSONObject("content")
            ?.has("tweetPromotedMetadata") == true || optJSONObject("item")?.optJSONObject("content")
            ?.has("tweetPromotedMetadata") == true

    private fun JSONObject.entryIsWhoToFollow(): Boolean = optString("entryId").let {
        it.startsWith("whoToFollow-") || it.startsWith("who-to-follow-") || it.startsWith("connect-module-")
    }

    private fun JSONObject.entryIsWhoToSubscribe(): Boolean =
        optString("entryId").startsWith("who-to-subscribe-")

    private fun JSONObject.entryIsTopicsModule(): Boolean =
        optString("entryId").startsWith("TopicsModule-")

    private fun JSONObject.entryGetContentItems(): JSONArray? =
        optJSONObject("content")?.optJSONArray("items")
            ?: optJSONObject("content")?.optJSONObject("timelineModule")?.optJSONArray("items")

    private fun JSONObject.entryIsTweet(): Boolean = optString("entryId").startsWith("tweet-")
    private fun JSONObject.entryIsConversationThread(): Boolean =
        optString("entryId").startsWith("conversationthread-")

    private fun JSONObject.entryIsTweetDetailRelatedTweets(): Boolean =
        optString("entryId").startsWith("tweetdetailrelatedtweets-")

    private fun JSONObject.entryIsVideoCarousel(): Boolean =
        optJSONObject("content")?.optJSONObject("timelineModule")?.optJSONObject("clientEventInfo")
            ?.optString("component") == "video_carousel"

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
            ?.let {
                if (it.has("tweet")) {
                    it.optJSONObject("tweet")
                } else {
                    it
                }
            }?.optJSONObject("legacy")
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
    private fun JSONObject.legacyGetRetweetedStatusLegacy(): JSONObject? =
        optJSONObject("retweeted_status_result")?.optJSONObject("result")?.optJSONObject("legacy")

    private fun JSONObject.legacyGetExtendedEntitiesMedias(): JSONArray? =
        optJSONObject("extended_entities")?.optJSONArray("media")

    private fun JSONObject.legacyCheckAndRemove() {
        legacyGetExtendedEntitiesMedias()?.forEach { media ->
            media.mediaCheckAndRemove()
        }
        legacyGetRetweetedStatusLegacy()?.legacyGetExtendedEntitiesMedias()?.forEach { media ->
            media.mediaCheckAndRemove()
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
            entry.entryGetTrends()?.trendRemoveAds()

            if (!modulePrefs.getBoolean("disable_promoted_content", true)) return@forEachIndexed
            if (entry.entryHasPromotedMetadata()) {
                Log.d("Handle timeline ads $entryIndex $entry")
                removeIndex.add(entryIndex)
            }

            val innerRemoveIndex = mutableListOf<Int>()
            val contentItems = entry.entryGetContentItems()
            contentItems?.forEachIndexed inner@{ itemIndex, item ->
                if (item.entryHasPromotedMetadata()) {
                    Log.d("Handle timeline replies ads $entryIndex $entry")
                    if (contentItems.length() == 1) {
                        removeIndex.add(entryIndex)
                    } else {
                        innerRemoveIndex.add(itemIndex)
                    }
                    return@inner
                }
            }
            for (i in innerRemoveIndex.asReversed()) {
                contentItems?.remove(i)
            }
        }
        for (i in removeIndex.reversed()) {
            remove(i)
        }
    }

    private fun JSONArray.entriesRemoveWhoToFollow() {
        val entryRemoveIndex = mutableListOf<Int>()
        forEachIndexed { entryIndex, entry ->
            if (!entry.entryIsWhoToFollow()) return@forEachIndexed

            if (modulePrefs.getBoolean("disable_who_to_follow", false)) {
                Log.d("Handle whoToFollow $entryIndex $entry")
                entryRemoveIndex.add(entryIndex)
                return@forEachIndexed
            }

            if (!modulePrefs.getBoolean("disable_promoted_user", true)) return@forEachIndexed

            val items = entry.entryGetContentItems()
            val userRemoveIndex = mutableListOf<Int>()
            items?.forEachIndexed { index, item ->
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

    private fun JSONArray.entriesRemoveWhoToSubscribe() {
        val entryRemoveIndex = mutableListOf<Int>()
        forEachIndexed { entryIndex, entry ->
            if (!entry.entryIsWhoToSubscribe()) return@forEachIndexed

            if (modulePrefs.getBoolean("disable_who_to_subscribe", false)) {
                Log.d("Handle whoToSubscribe $entryIndex $entry")
                entryRemoveIndex.add(entryIndex)
                return@forEachIndexed
            }
        }
        for (i in entryRemoveIndex.reversed()) {
            remove(i)
        }
    }

    private fun JSONArray.entriesRemoveTopicsToFollow() {
        val entryRemoveIndex = mutableListOf<Int>()
        forEachIndexed { entryIndex, entry ->
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

    private fun JSONObject.entryRemoveSensitiveMediaWarning() {
        if (entryIsTweet()) {
            entryGetLegacy()?.let {
                it.legacyGetExtendedEntitiesMedias()?.forEach { media ->
                    media.mediaCheckAndRemove()
                }
                it.legacyGetRetweetedStatusLegacy()?.legacyGetExtendedEntitiesMedias()
                    ?.forEach { media ->
                        media.mediaCheckAndRemove()
                    }
            }
        } else if (entryIsConversationThread()) {
            entryGetContentItems()?.forEach { item ->
                item.entryGetLegacy()?.let { legacy ->
                    legacy.legacyGetExtendedEntitiesMedias()?.forEach { media ->
                        media.mediaCheckAndRemove()
                    }
                }
            }
        }
    }

    private fun JSONArray.entriesRemoveSensitiveMediaWarning() {
        forEach { entry ->
            entry.entryRemoveSensitiveMediaWarning()
        }
    }

    private fun JSONArray.entriesRemoveTweetDetailRelatedTweets() {
        val removeIndex = mutableListOf<Int>()
        forEachIndexed { entryIndex, entry ->
            if (!modulePrefs.getBoolean(
                    "disable_tweet_detail_related_tweets", false
                )
            ) return@forEachIndexed
            if (entry.entryIsTweetDetailRelatedTweets()) {
                Log.d("Handle tweet detail related tweets $entryIndex $entry")
                removeIndex.add(entryIndex)
            }
        }
        for (i in removeIndex.reversed()) {
            remove(i)
        }
    }

    private fun JSONArray.entriesRemoveVideoCarousel() {
        val removeIndex = mutableListOf<Int>()
        forEachIndexed { entryIndex, entry ->
            if (!modulePrefs.getBoolean("disable_video_carousel", false)) return@forEachIndexed
            if (entry.entryIsVideoCarousel()) {
                Log.d("Handle explore video carousel $entryIndex $entry")
                removeIndex.add(entryIndex)
            }
        }
        for (i in removeIndex.reversed()) {
            remove(i)
        }
    }

    private fun JSONArray.entriesRemoveAnnoyance() {
        entriesRemoveTimelineAds()
        entriesRemoveWhoToFollow()
        entriesRemoveWhoToSubscribe()
        entriesRemoveTopicsToFollow()
        entriesRemoveSensitiveMediaWarning()
        entriesRemoveTweetDetailRelatedTweets()
        entriesRemoveVideoCarousel()
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

        if (modulePrefs.getBoolean("enable_log", false)) {
            val isRefreshSessionResponse = try {
                JSONObject(content).let {
                    return@let it.has("session_token")
                }
            } catch (t: Throwable) {
                false
            }
            if (!isRefreshSessionResponse) {
                writeJsonLog(content)
            }
        }

        try {
            val json = JSONObject(content)

            json.jsonGetTweets()?.tweetsForEach { tweet ->
                tweet.tweetCheckAndRemove()
            }
            json.jsonGetInstructions()?.forEach { instruction ->
                instruction.instructionCheckAndRemove()
            }
            json.jsonGetData()?.dataCheckAndRemove()

            json.jsonCheckAndRemoveRecommendedUsers()

            json.jsonCheckAndRemoveThreads()

            content = json.toString()
        } catch (_: JSONException) {
        } catch (e: Throwable) {
            Log.e("json hook failed to parse JSONObject", e)
            Log.d(content)
        }

        try {
            val json = JSONArray(content)
            json.forEach {
                it.tweetCheckAndRemove()
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
