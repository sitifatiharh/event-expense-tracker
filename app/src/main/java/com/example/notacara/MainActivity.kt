package com.example.notacara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.notacara.ui.navigation.NavGraph
import com.example.notacara.ui.theme.NotacaraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotacaraTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
