package com.haaz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.haaz.home.HomePage
import com.haaz.ui.theme.HaazTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HaazTheme {
                App()
            }
        }
    }
}

@Composable
fun App() {
    Surface(modifier = Modifier.fillMaxSize()) {
        HomePage()
    }
}
