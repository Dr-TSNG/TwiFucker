package icu.nullptr.twifucker

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.init.InitFields.hostPackageName
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.loadClass

private val TweetViewClass = loadClass("com.twitter.tweetview.core.TweetView")

private val AdTextId = appContext.resources
    .getIdentifier("tweet_promoted_badge_bottom", "id", hostPackageName)

private val OnLayoutChangeListener = View.OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
    handleView(v as ViewGroup, false)
}

private fun handleView(viewGroup: ViewGroup, delayed: Boolean) {
    val isAd = viewGroup.findViewById<View>(AdTextId)?.visibility == View.VISIBLE
    if (isAd) Log.i("Handle Ad")
    if (delayed) viewGroup.visibility = if (isAd) View.GONE else View.VISIBLE
    else {
        Handler(Looper.getMainLooper()).post {
            viewGroup.visibility = if (isAd) View.GONE else View.VISIBLE
        }
    }
}

fun adHook() {
    findAllMethods(ViewGroup::class.java) { name == "addView" }.hookAfter { param ->
        val vg = param.args[0] as? ViewGroup ?: return@hookAfter

        if (vg.javaClass == TweetViewClass) {
            vg.removeOnLayoutChangeListener(OnLayoutChangeListener)
            vg.addOnLayoutChangeListener(OnLayoutChangeListener)

            Handler(Looper.getMainLooper()).post {
                handleView(vg, true)
            }
        }
    }
}
