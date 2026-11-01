package com.example.notacara.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.*

@Composable
fun TotalExpenditureBottomBar(
    label: String = "Total Pengeluaran",
    total: String
) {
    val localeID = Locale("in", "ID")
    val numberFormat = NumberFormat.getInstance(localeID)
    
    val formattedTotal = remember(total) {
        val cleanValue = total.replace("Rp", "").replace(".", "").replace(",", "").trim()
        val numericValue = cleanValue.toLongOrNull()
        if (numericValue != null) {
            "Rp${numberFormat.format(numericValue)}"
        } else {
            total
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.White,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label, 
                fontWeight = FontWeight.Bold, 
                fontSize = 18.sp
            )
            Text(
                text = formattedTotal, 
                fontWeight = FontWeight.Bold, 
                fontSize = 18.sp, 
                color = Color.Black
            )
        }
    }
}
