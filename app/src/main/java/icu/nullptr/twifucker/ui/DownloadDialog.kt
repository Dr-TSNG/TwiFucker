@file:Suppress("DEPRECATION")

package icu.nullptr.twifucker.ui

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.media.MediaScannerConnection
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
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
    context: Context, downloadUrls: List<String>, onDismiss: () -> Unit,
) : AlertDialog.Builder(context) {
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

        private fun copyFile(fileName: String): String {
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

            return outputFile.absolutePath
        }

        private fun download(
            context: Context, url: String, onDownloadCompleted: (() -> Unit)? = null
        ) {
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

                    val outputPath = copyFile(file.name)
                    file.delete()

                    MediaScannerConnection.scanFile(
                        context, arrayOf(outputPath), null, null
                    )

                    onDownloadCompleted?.invoke()
                } catch (t: Throwable) {
                    Log.e(t)
                    Log.toast(appContext.getString(R.string.download_failed))
                }
                progressDialog.cancel()
            }.start()
        }

        private fun toClipboard(text: String) {
            val clipboardManager =
                appContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", text)
            clipboardManager.setPrimaryClip(clip)
            Log.toast(appContext.getString(R.string.download_link_copied))
        }
    }

    private class DownloadMediaAdapter(val context: Context, val urls: List<String>) :
        BaseAdapter() {

        override fun getCount(): Int {
            return urls.size
        }

        override fun getItem(position: Int): Any {
            return urls[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.download_item, parent, false).apply {
                    findViewById<TextView>(R.id.download_item_text).text =
                        context.getString(R.string.download_media, position + 1)
                    findViewById<ImageButton>(R.id.download_item_copy).setOnClickListener {
                        toClipboard(urls[position])
                    }
                    findViewById<ImageButton>(R.id.download_item_download).setOnClickListener {
                        download(context, urls[position]) {
                            Log.toast(context.getString(R.string.download_completed))
                        }
                    }
                }
            return view
        }

    }

    init {
        context.addModuleAssetPath()

        val adapter = DownloadMediaAdapter(context, downloadUrls)
        setAdapter(adapter, null)

        setNeutralButton(R.string.download_all) { _, _ ->
            downloadUrls.forEachIndexed { i, j ->
                download(context, j) {
                    if (i == downloadUrls.size - 1) {
                        Log.toast(context.getString(R.string.download_completed))
                    }
                }
            }
        }
        setNegativeButton(R.string.settings_dismiss) { _, _ ->
            onDismiss()
        }
        setOnDismissListener { onDismiss() }

        setTitle(R.string.download_or_copy)
        show()
    }
}