package com.example.notacara.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notacara.data.model.AnalysisResult
import com.example.notacara.data.ml.SemanticAnalyzer
import com.example.notacara.data.ml.AccuracyTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProcessingScreen(
    imageUri: Uri,
    onProcessingFinished: (AnalysisResult) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.Default) {
            val analyzer = SemanticAnalyzer(context)
            val result = analyzer.analyzeImage(imageUri)
            
            // Evaluasi Akurasi (Manual Scan mode)
            AccuracyTest.evaluate(context, result, result.rawText)
            
            withContext(Dispatchers.Main) {
                onProcessingFinished(result)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(70.dp).testTag("loading_indicator"),
                color = Color(0xFFBB86FC),
                strokeWidth = 5.dp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Sedang Memproses Nota Anda",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
