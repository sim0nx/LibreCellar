package org.librecellar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import org.librecellar.data.repository.WineRepository
import org.librecellar.ui.MainScreen
import org.librecellar.ui.components.LocalWineRepository
import org.librecellar.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: WineRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = WineRepository(applicationContext)

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalWineRepository provides repository) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MainScreen(repository = repository)
                    }
                }
            }
        }
    }
}
