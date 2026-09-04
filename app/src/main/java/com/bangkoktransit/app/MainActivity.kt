package com.bangkoktransit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bangkoktransit.app.ui.TransitGoApp
import com.bangkoktransit.app.ui.theme.BangkoktransitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BangkoktransitTheme {
                TransitGoApp()
            }
        }
    }
}
