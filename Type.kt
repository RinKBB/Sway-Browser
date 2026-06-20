package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BrowserScreen
import com.example.ui.LocalAppStrings
import com.example.ui.RuStrings
import com.example.ui.EnStrings
import com.example.ui.KkStrings
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          val viewModel: BrowserViewModel = viewModel()
          val appLang by viewModel.appLanguage.collectAsState()
          val strings = when (appLang) {
            "en" -> EnStrings
            "kk" -> KkStrings
            else -> RuStrings
          }
          CompositionLocalProvider(LocalAppStrings provides strings) {
            BrowserScreen(viewModel)
          }
        }
      }
    }
  }
}
