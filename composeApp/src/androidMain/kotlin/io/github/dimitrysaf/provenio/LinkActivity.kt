package io.github.dimitrysaf.provenio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.github.dimitrysaf.provenio.core.settings.AppIconPlatform

/**
 * Receives app links, sign-in callbacks included, and hands them to the app already running, through
 * the launcher component in use, so a link never opens a second copy of the app.
 */
class LinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.dataString?.let { link ->
            startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    component = AppIconPlatform.currentLauncherComponent(this@LinkActivity)
                    putExtra(EXTRA_LINK, link)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
            )
        }
        finish()
    }

    companion object {
        const val EXTRA_LINK = "link"
    }
}
