package app.paralino.android.app_update

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import app.paralino.core.app_update.AppUpdateStatus
import app.paralino.core.app_update.IAppUpdateManager
import app.paralino.core.logg
import app.paralino.core.models.base.BackendResourceIDs
import app.paralino.core.models.base.ConfigData
import app.paralino.core.provider.ICoroutineScopeProvider
import app.paralino.core.utils.extensions.openUri
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.Locale

class AppUpdateManager(
    private val appClass: Application,
    private val scopeProvider: ICoroutineScopeProvider
) : IAppUpdateManager() {

    override fun startUpdateProcess(data: ConfigData) {
        val url = data.androidAppUrl?.takeIf { it.isNotBlank() }
        if (url == null || !isAllowedDownloadUrl(url)) {
            logg.w("app_update", "Rejected download url: $url")
            appClass.openUri(BackendResourceIDs.downloadLandingPageUrl.toUri())
        } else {
            downloadAndInstall(url)
        }
    }

    private fun isAllowedDownloadUrl(url: String): Boolean {
        // keep the last "/" in url comparison so we don't have to do actual host matching
        // this way it is full base url that is compared
        return url.startsWith("https://static.paralino.com/") ||
            url.startsWith("https://paralino.com/") ||
            url.startsWith("https://paralino.app/") ||
            url.startsWith("https://github.com/paralino/")
    }

    private fun downloadAndInstall(url: String) {
        updateJob?.cancel()
        updateJob = flow<Unit> {
            triggerRemoteOnly {
                updateStatus.update { AppUpdateStatus.Downloading(progress = null, mb = null) }

                val destinationFolder = File(appClass.cacheDir, "ParalinoUpdate")
                if (destinationFolder.exists()) {
                    destinationFolder.deleteRecursively()
                }

                destinationFolder.mkdirs()

                val destination =
                    File.createTempFile("paralino-app-update", ".apk", destinationFolder)

                logg.v("app_update", "Download url: $url")
                logg.i("app_update", "Download path: $destination")

                val connection = URL(url).openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("Accept-Encoding", "identity")
                connection.connect()

                val length = connection.contentLengthLong
                logg.v("app_update", "Download size: $length")

                connection.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesRead = input.read(buffer)
                        var bytesCopied = 0L
                        while (bytesRead >= 0) {
                            output.write(buffer, 0, bytesRead)
                            bytesCopied += bytesRead
                            val downloadedMb = bytesCopied.toDouble() / (1024 * 1024)
                            val formattedMb =
                                String.format(Locale.US, "%.2f MB", downloadedMb)

                            if (length > 0) {
                                updateStatus.update {
                                    val progress = bytesCopied.toFloat() / length
                                    logg.v("app_update", "Download progress: $progress")
                                    AppUpdateStatus.Downloading(
                                        progress = progress,
                                        mb = formattedMb
                                    )
                                }
                            } else {
                                updateStatus.update {
                                    AppUpdateStatus.Downloading(
                                        progress = null,
                                        mb = formattedMb
                                    )
                                }
                            }
                            bytesRead = input.read(buffer)
                        }
                    }
                }

                updateStatus.update { AppUpdateStatus.Installing }
                install(context = appClass, apkUri = Uri.fromFile(destination))
            }
        }
            .catch { updateStatus.update { AppUpdateStatus.Idle } }
            .launchIn(scopeProvider.defaultIOScope)
    }

    private fun install(context: Context, apkUri: Uri) {
        logg.v("app_update", "Install new version from $apkUri")
        context.contentResolver.openInputStream(apkUri)?.use { apkStream ->
            val length =
                DocumentFile.fromSingleUri(context, apkUri)?.length() ?: -1
            val params =
                PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = context.packageManager.packageInstaller.createSession(params)
            val session = context.packageManager.packageInstaller.openSession(sessionId)

            logg.v("app_update", "Open install session stream")
            session.openWrite("Paralino", 0, length).use { sessionStream ->
                apkStream.copyTo(sessionStream)
                session.fsync(sessionStream)
            }

            val intent = Intent(context, InstallRequestReceiver::class.java)
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            val pi = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                flags
            )

            logg.i("app_update", "Request install")
            session.commit(pi.intentSender)
            session.close()
        }
    }
}
