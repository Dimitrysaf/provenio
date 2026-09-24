package io.github.dimitrysaf.provenio.android

import android.app.job.JobScheduler
import android.net.Uri
import android.os.Build
import io.github.dimitrysaf.provenio.MainActivity
import io.github.dimitrysaf.provenio.core.downloads.DownloadEnqueueResult
import io.github.dimitrysaf.provenio.core.downloads.DownloadStatus
import io.github.dimitrysaf.provenio.core.downloads.DownloadsRepository
import io.github.dimitrysaf.provenio.core.streams.StreamItem
import java.io.File

/** Debug-only launcher: instrumentation force-stops its app when it ends or is killed. */
class BackgroundDownloadTestActivity : MainActivity() {
    private var started = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || started) return
        started = true
        val url = requireNotNull(intent.getStringExtra("downloadUrl"))
        require(Uri.parse(url).host == "10.0.2.2") { "Only the emulator's local fixture server is supported" }
        val id = "provenio-background-download-fixture"
        val result = DownloadsRepository.enqueueFromStream(
            contentType = "movie", videoId = id, parentMetaId = id, parentMetaType = "movie",
            title = "Background download test", logo = null, poster = null, background = null,
            seasonNumber = null, episodeNumber = null, episodeTitle = null, episodeThumbnail = null,
            stream = StreamItem(name = "Test fixture", url = url, addonName = "Local test", addonId = "test"),
        )
        check(result == DownloadEnqueueResult.Started || result == DownloadEnqueueResult.Replaced)
        if (Build.VERSION.SDK_INT >= 34) {
            val jobs = getSystemService(JobScheduler::class.java).forNamespace("provenio-downloads").allPendingJobs
            check(jobs.any { it.isUserInitiated && it.isPersisted })
        }
        // Exercise UI reload against the real scheduler; Robolectric does not share
        // pending jobs between separate namespaced JobScheduler instances.
        DownloadsRepository.onProfileChanged()
        check(DownloadsRepository.uiState.value.items.single { it.videoId == id }.status == DownloadStatus.Downloading)
        File(cacheDir, "download-test-ready").writeText("ready")
    }
}
