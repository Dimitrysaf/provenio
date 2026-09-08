package io.github.dimitrysaf.provenio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.dimitrysaf.provenio.data.initDatabaseContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars; individual M3 components apply their own insets.
        enableEdgeToEdge()
        initDatabaseContext(this)
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
