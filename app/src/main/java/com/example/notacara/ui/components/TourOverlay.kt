package com.example.notacara.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

@Composable
fun TourOverlay(
    targetRect: Rect?,
    title: String,
    description: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    currentStep: Int,
    totalSteps: Int,
    showNextButton: Boolean = true
) {
    if (targetRect == null) return

    val density = LocalDensity.current
    var containerPosition by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    val localRect = targetRect.translate(-containerPosition.x, -containerPosition.y)
    
    val holePadding = with(density) { 6.dp.toPx() }
    val margin = with(density) { 6.dp.toPx() }
    val inflatedRect = localRect.inflate(holePadding)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                containerPosition = it.positionInRoot()
                containerSize = it.size
            }
    ) {
        if (containerSize != IntSize.Zero) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }
                val holePath = Path().apply {
                    addRoundRect(
                        RoundRect(rect = inflatedRect, cornerRadius = CornerRadius(with(density) { 12.dp.toPx() }))
                    )
                }
                drawPath(Path.combine(PathOperation.Difference, path, holePath), color = Color.Black.copy(alpha = 0.7f))
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = inflatedRect.topLeft,
                    size = inflatedRect.size,
                    cornerRadius = CornerRadius(with(density) { 12.dp.toPx() }),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 1.5.dp.toPx() })
                )
            }

            if (showNextButton) {
                Box(modifier = Modifier.fillMaxSize().blockClicks())
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(with(density) { inflatedRect.top.toDp() }.coerceAtLeast(0.dp)).blockClicks())
                Box(modifier = Modifier.fillMaxWidth().height(with(density) { (containerSize.height - inflatedRect.bottom).toDp() }.coerceAtLeast(0.dp)).align(Alignment.BottomStart).blockClicks())
                Box(modifier = Modifier.offset(y = with(density) { inflatedRect.top.toDp() }).width(with(density) { inflatedRect.left.toDp() }.coerceAtLeast(0.dp)).height(with(density) { inflatedRect.height.toDp() }.coerceAtLeast(0.dp)).blockClicks())
                Box(modifier = Modifier.align(Alignment.TopEnd).offset(y = with(density) { inflatedRect.top.toDp() }).width(with(density) { (containerSize.width - inflatedRect.right).toDp() }.coerceAtLeast(0.dp)).height(with(density) { inflatedRect.height.toDp() }.coerceAtLeast(0.dp)).blockClicks())
            }

            val isTargetInBottomHalf = inflatedRect.center.y > containerSize.height * 0.5f
            val isTargetInRightHalf = inflatedRect.center.x > containerSize.width * 0.5f

            Card(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .onGloballyPositioned { cardSize = it.size }
                    .offset {
                        if (cardSize == IntSize.Zero) return@offset IntOffset(0, 0)
                        val y = if (isTargetInBottomHalf) inflatedRect.top - cardSize.height - margin else inflatedRect.bottom + margin
                        var x = inflatedRect.center.x - (cardSize.width / 2)
                        if (isTargetInRightHalf && inflatedRect.right > containerSize.width - with(density) { 60.dp.toPx() }) {
                            x = inflatedRect.right - cardSize.width
                        } else if (!isTargetInRightHalf && inflatedRect.left < with(density) { 60.dp.toPx() }) {
                            x = inflatedRect.left
                        }
                        val finalX = x.coerceIn(margin, containerSize.width - cardSize.width - margin)
                        val finalY = y.coerceIn(margin, containerSize.height - cardSize.height - margin)
                        IntOffset(finalX.roundToInt(), finalY.roundToInt())
                    },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = description, fontSize = 12.sp, color = Color(0xFF424242), lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "$currentStep/$totalSteps", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentStep < totalSteps) {
                                Text(
                                    text = "LEWATI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray,
                                    modifier = Modifier.clickable { onSkip() }.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (showNextButton) {
                                Button(
                                    onClick = onNext,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B6E4E)),
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (currentStep == totalSteps) "SELESAI" else "LANJUT", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.blockClicks() = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
        }
    }
}

@Composable
fun SimpleTourPopUp(
    title: String,
    description: String,
    buttonText: String = "Mengerti",
    currentStep: Int = 0,
    totalSteps: Int = 0,
    alignment: Alignment = Alignment.Center,
    onDismiss: () -> Unit
) {
    Popup(
        alignment = alignment,
        properties = PopupProperties(
            focusable = true,
            excludeFromSystemGesture = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* block clicks */ },
            contentAlignment = alignment
        ) {
            Card(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(min = 280.dp, max = 320.dp)
                    .padding(top = if (alignment == Alignment.TopCenter) 60.dp else 0.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = title, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = description, 
                        fontSize = 13.sp, 
                        lineHeight = 19.sp,
                        color = Color(0xFF424242),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (totalSteps > 0) {
                            Text("$currentStep/$totalSteps", fontSize = 11.sp, color = Color.Gray)
                        } else { Spacer(Modifier.width(1.dp)) }
                        
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B6E4E)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(buttonText, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
