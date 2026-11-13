package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.Idea2ImpactApp
import com.example.util.StartupSoundPlayer
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check before UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        if (savedInstanceState == null) {
            StartupSoundPlayer.playAppOpenChime(this)
        } else {
            StartupSoundPlayer.warmup(this)
        }

        enableEdgeToEdge()
        setContent {
            Idea2ImpactApp()
        }
    }
}
