package com.yasin.muzikcalar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.yasin.muzikcalar.ui.AppTheme
import com.yasin.muzikcalar.ui.MainScreen
import com.yasin.muzikcalar.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
