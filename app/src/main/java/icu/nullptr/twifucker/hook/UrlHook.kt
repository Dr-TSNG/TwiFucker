package icu.nullptr.twifucker.hook

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder

object UrlHook : BaseHook() {
    override val name: String
        get() = "UrlHook"

    override fun init() {
        MethodFinder.fromClass(Intent::class.java).filterByName("replaceExtras")
            .filterByParamTypes(Bundle::class.java).first().createHook {
                before { param ->
                    val bundle = param.args[0] as Bundle
                    val extraText = bundle.getString(Intent.EXTRA_TEXT) ?: return@before
                    if (extraText.isTwitterUrl()) {
                        val newExtraText = clearExtraParams(extraText)
                        bundle.putString(Intent.EXTRA_TEXT, newExtraText)
                    }
                }
            }

        MethodFinder.fromClass(Intent::class.java).filterByName("createChooser")
            .filterByParamCount(2..3).filterByParamTypes {
                it[0] == Intent::class.java && it[1] == CharSequence::class.java
            }.forEach {
                it.createHook {
                    before { param ->
                        val intent = param.args[0] as Intent
                        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return@before
                        if (extraText.isTwitterUrl()) {
                            intent.putExtra(Intent.EXTRA_TEXT, clearExtraParams(extraText))
                        }
                    }
                }
            }


        MethodFinder.fromClass(ClipData::class.java).filterByName("newPlainText")
            .filterByParamTypes(CharSequence::class.java, CharSequence::class.java).first()
            .createHook {
                before { param ->
                    val text = (param.args[1] as CharSequence).toString()
                    if (text.isTwitterUrl()) {
                        param.args[1] = clearExtraParams(text)
                    }
                }
            }

        MethodFinder.fromClass(loadClass("com.twitter.deeplink.implementation.UrlInterpreterActivity"))
            .filterByName("onCreate").first().createHook {
                before { param ->
                    val intent = (param.thisObject as Activity).intent
                    val url = intent.data.toString()
                    if (url.isTwitterUrl()) {
                        val newUrl = clearExtraParams(url)
                        intent.data = Uri.parse(newUrl)
                    }
                }
            }
    }

    private fun String.isTwitterUrl(): Boolean {
        val uri = Uri.parse(this)
        return (uri.scheme == "https" || uri.scheme == "http") && (uri.host == "twitter.com" || uri.host == "www.twitter.com" || uri.host == "mobile.twitter.com")
    }

    private fun String.hasExtraParam(): Boolean {
        return this == "t" || this == "s"
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
}

