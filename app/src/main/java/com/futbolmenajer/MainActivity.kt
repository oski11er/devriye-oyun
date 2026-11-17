package com.futbolmenajer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.futbolmenajer.ui.App
import com.futbolmenajer.ui.FutbolTheme
import com.futbolmenajer.ui.GameViewModel

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FutbolTheme { App(vm) } }
    }
}
