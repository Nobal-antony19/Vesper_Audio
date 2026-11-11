package com.vesper.audio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vesper.audio.ui.SonoraMainContent
import com.vesper.audio.ui.SonoraViewModel
import com.vesper.audio.ui.theme.SonoraTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SonoraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SonoraTheme {
                SonoraMainContent(viewModel = viewModel)
            }
        }
    }
}
