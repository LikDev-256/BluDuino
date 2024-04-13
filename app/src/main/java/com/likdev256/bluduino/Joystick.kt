package com.likdev256.bluduino

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.likdev256.bluduino.ui.theme.BluDuinoTheme
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun JoyStick(
    stickColor: Brush,
    bgColor: Color,
    onOffsetChanged: (xOffset: Float, yOffset: Float) -> Unit
) {
    // Support RTL
    val layoutDirection = LocalLayoutDirection.current
    val directionFactor = if (layoutDirection == LayoutDirection.Rtl) -1 else 1

    val scope = rememberCoroutineScope()
    val buttonSize = 90.dp

    // Swipe size in px
    val buttonSizePx = with(LocalDensity.current) { buttonSize.toPx() }
    val dragSizePx = buttonSizePx * 1.5f

    // Drag offset
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(vertical = 50.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            Box(
                modifier = Modifier
                    .size(buttonSize * 4)
                    .drawWithCache {
                        val roundedPolygon = RoundedPolygon(
                            numVertices = 8,
                            radius = 300f,
                            centerX = size.width / 2,
                            centerY = size.height / 2,
                            rounding = CornerRounding(
                                size.minDimension / 5f,
                                smoothing = 1f
                            )
                        )
                        val roundedPolygonPath = roundedPolygon
                            .toPath()
                            .asComposePath()
                        onDrawBehind {
                            drawPath(roundedPolygonPath, color = bgColor)
                        }
                    }
                    .fillMaxSize()
                ,
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (offsetX.value).roundToInt(),
                                y = (offsetY.value).roundToInt()
                            )
                        }
                        .width(buttonSize)
                        .height(buttonSize)
                        .alpha(0.8f)
                        .background(stickColor, CircleShape)
                        .pointerInput(Unit) {

                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                },
                                onDragEnd = {
                                    scope.launch {
                                        offsetX.animateTo(0f)
                                    }
                                    scope.launch {
                                        offsetY.animateTo(0f)
                                    }
                                    isDragging = false
                                    onOffsetChanged(0f, 0f) // Report offset change
                                },
                                onDragCancel = {
                                    scope.launch {
                                        offsetX.animateTo(0f)
                                    }
                                    scope.launch {
                                        offsetY.animateTo(0f)
                                    }
                                    isDragging = false
                                    onOffsetChanged(0f, 0f) // Report offset change
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()

                                    val newOffsetX = offsetX.value + dragAmount.x * directionFactor
                                    val newOffsetY = offsetY.value + dragAmount.y
                                    val newDistance = sqrt(newOffsetX.pow(2) + newOffsetY.pow(2))

                                    if (newDistance < dragSizePx) {
                                        // Move within drag size
                                        scope.launch {
                                            offsetX.snapTo(newOffsetX)
                                            offsetY.snapTo(newOffsetY)
                                        }
                                    } else {
                                        // Restrict to circle boundary
                                        val angle = atan2(newOffsetY, newOffsetX)
                                        scope.launch {
                                            offsetX.snapTo(cos(angle) * dragSizePx)
                                            offsetY.snapTo(sin(angle) * dragSizePx)
                                        }
                                    }

                                    // Report offset change
                                    onOffsetChanged(offsetX.value, -offsetY.value)
                                }

                            )
                        }
                )
            }
            Row(horizontalArrangement = Arrangement.Absolute.Right, verticalAlignment = Alignment.Bottom) {
                // Display the current xOffset and yOffset values as text
                Text(
                    text = "xOffset: ${offsetX.value.toInt()}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "yOffset: ${(-offsetY.value).toInt()}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}


@Preview
@Composable
fun JoystickPreview() {
    BluDuinoTheme {
        val joystickColor = Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.inversePrimary,
                MaterialTheme.colorScheme.inverseOnSurface
            )
        )
        JoyStick(joystickColor, MaterialTheme.colorScheme.secondary) { _, _ ->
            // Use xOffset and yOffset here as needed
        }
    }
}
