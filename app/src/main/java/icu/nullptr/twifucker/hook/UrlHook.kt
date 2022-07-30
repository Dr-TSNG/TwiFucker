package icu.nullptr.twifucker.hook

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.findAllMethods
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore

private fun String.isTwitterUrl(): Boolean {
    val uri = Uri.parse(this)
    return (uri.scheme == "https" || uri.scheme == "http") && (uri.host == "twitter.com" || uri.host == "www.twitter.com" || uri.host == "mobile.twitter.com")
}

private fun String.hasExtraParam(): Boolean {
    return this.startsWith("t") || this.startsWith("s")
}

private fun clearExtraParams(url: String): String {
    Log.d("Handle Url before: $url")
    val oldUri = Uri.parse(url)
    val newUri = oldUri.buildUpon().clearQuery()
    oldUri.queryParameterNames.forEach {
        if (it.hasExtraParam()) {
            return@forEach
        }
        newUri.appendQueryParameter(it, oldUri.getQueryParameter(it))
    }
    val newUrl = newUri.build().toString()
    Log.d("Handle Url after: $newUrl")
    return newUrl
}

fun urlHook() {
    findAllMethods(Intent::class.java) { name == "replaceExtras" }.hookBefore { param ->
        val bundle = param.args[0] as Bundle
        val extraText = bundle.getString(Intent.EXTRA_TEXT)
        if (extraText != null && extraText.isTwitterUrl()) {
            val newExtraText = clearExtraParams(extraText)
            bundle.putString(Intent.EXTRA_TEXT, newExtraText)
        }
    }
    findAllMethods(Intent::class.java) { name == "createChooser" }.hookBefore { param ->
        val intent = param.args[0] as Intent
        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return@hookBefore
        if (!extraText.isTwitterUrl()) {
            return@hookBefore
        }
        intent.putExtra(Intent.EXTRA_TEXT, clearExtraParams(extraText))
    }
    findAllMethods(ClipData::class.java) { name == "newPlainText" }.hookBefore { param ->
        val text = (param.args[1] as CharSequence).toString()
        if (!text.isTwitterUrl()) {
            return@hookBefore
        }
        param.args[1] = clearExtraParams(text)
    }
    findMethod("com.twitter.deeplink.implementation.UrlInterpreterActivity") {
        name == "onCreate"
    }.hookBefore { param ->
        val intent = (param.thisObject as Activity).intent
        val url = intent.data.toString()
        if (!url.isTwitterUrl()) {
            return@hookBefore
        }
        val newUrl = clearExtraParams(url)
        intent.data = Uri.parse(newUrl)
    }
}
