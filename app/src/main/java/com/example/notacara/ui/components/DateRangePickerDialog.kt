package com.example.notacara.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    var showFromPicker by remember { mutableStateOf(false) }
    var showUntilPicker by remember { mutableStateOf(false) }
    var fromMillis by remember { mutableStateOf<Long?>(null) }
    var untilMillis by remember { mutableStateOf<Long?>(null) }
    
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    if (showFromPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    fromMillis = datePickerState.selectedDateMillis
                    showFromPicker = false
                }) { Text("OK") }
            }
        ) { 
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            ) 
        }
    }

    if (showUntilPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showUntilPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    untilMillis = datePickerState.selectedDateMillis
                    showUntilPicker = false
                }) { Text("OK") }
            }
        ) { 
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            ) 
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Rentang Waktu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fromMillis?.let { sdf.format(Date(it)) } ?: "dd/mm/yyyy",
                    onValueChange = {},
                    label = { Text("Dari") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { showFromPicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (fromMillis == null) Color.Gray else Color.Black,
                        disabledBorderColor = Color.Gray,
                        disabledLabelColor = Color.Gray
                    )
                )
                OutlinedTextField(
                    value = untilMillis?.let { sdf.format(Date(it)) } ?: "dd/mm/yyyy",
                    onValueChange = {},
                    label = { Text("Sampai") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { showUntilPicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (untilMillis == null) Color.Gray else Color.Black,
                        disabledBorderColor = Color.Gray,
                        disabledLabelColor = Color.Gray
                    )
                )
                
                if (fromMillis != null && untilMillis != null && fromMillis!! > untilMillis!!) {
                    Text(
                        "Tanggal 'Sampai' tidak boleh sebelum 'Dari'",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(fromMillis, untilMillis) },
                enabled = fromMillis != null && untilMillis != null && fromMillis!! <= untilMillis!!
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
