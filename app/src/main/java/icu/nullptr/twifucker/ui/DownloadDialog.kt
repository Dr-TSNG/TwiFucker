@file:Suppress("DEPRECATION")

package icu.nullptr.twifucker.ui

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Bundle
import android.os.Environment
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.init.InitFields.appContext
import com.github.kyuubiran.ezxhelper.utils.Log
import com.github.kyuubiran.ezxhelper.utils.addModuleAssetPath
import icu.nullptr.twifucker.R
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadDialog(
    context: Context, private val downloadUrls: List<String>
) : AlertDialog(context) {
    companion object {
        private fun contentTypeToExt(contentType: String): String {
            return when {
                contentType.contains("image/jpeg") -> ".jpg"
                contentType.contains("image/png") -> ".png"
                contentType.contains("video/mp4") -> ".mp4"
                contentType.contains("video/webm") -> ".webm"
                contentType.contains("application/x-mpegURL") -> ".m3u8"
                else -> ""
            }
        }

        private fun copyFile(fileName: String) {
            val downloadPath = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "TwiFucker"
            )
            if (!downloadPath.exists()) {
                downloadPath.mkdirs()
            }
            val outputFile = File(downloadPath, fileName)
            val inputStream = File(appContext.cacheDir, fileName).inputStream()
            val outputStream = outputFile.outputStream()
            inputStream.copyTo(outputStream)
        }
    }

    private fun download(url: String) {
        val progressDialog = ProgressDialog(context)
        progressDialog.setTitle(R.string.downloading)
        progressDialog.setCancelable(false)
        progressDialog.show()

        Thread {
            try {
                val downloadUrl = URL(url)
                val httpConnection = downloadUrl.openConnection() as HttpURLConnection
                httpConnection.connect()
                val inputStream = httpConnection.inputStream
                val buffer = ByteArray(1024)
                var len = inputStream.read(buffer)

                val file = File(
                    appContext.cacheDir,
                    "" + System.currentTimeMillis() + contentTypeToExt(httpConnection.contentType)
                )

                val outputStream = FileOutputStream(file)
                while (len != -1) {
                    outputStream.write(buffer, 0, len)
                    len = inputStream.read(buffer)
                }

                outputStream.close()
                inputStream.close()
                httpConnection.disconnect()

                copyFile(file.name)
                file.delete()
            } catch (t: Throwable) {
                Log.e(t)
                Log.toast(context.getString(R.string.download_failed))
            }
            progressDialog.cancel()
        }.start()
    }

    private fun toClipboard(text: String) {
        val clipboardManager = appContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", text)
        clipboardManager.setPrimaryClip(clip)
        Log.toast(context.getString(R.string.download_link_copied))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        context.addModuleAssetPath()
        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.setPadding(32, 32, 32, 32)
        linearLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )

        downloadUrls.forEachIndexed { i, url ->
            val textView = TextView(context)
            textView.setOnClickListener {
                download(url)
            }
            textView.setOnLongClickListener {
                toClipboard(url)
                true
            }
            textView.setPadding(0, 16, 0, 16)
            textView.textSize = 18f
            textView.text = context.getString(R.string.download_media, i)
            linearLayout.addView(textView)
        }

        setView(linearLayout)
        setTitle(R.string.download_or_copy)
        super.onCreate(savedInstanceState)
    }
}