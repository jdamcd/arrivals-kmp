package com.jdamcd.arrivals.desktop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.filterLedChars

@Composable
fun ArrivalsView(
    state: ArrivalsState,
    onClickRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(color = Background)
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        when (state) {
            is ArrivalsState.Loading -> Loading()
            is ArrivalsState.Data -> Data(state, onClickRefresh)
            is ArrivalsState.Error -> Error(state.message)
        }
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = LedYellow)
    }
}

@Composable
private fun Data(state: ArrivalsState.Data, onClickRefresh: () -> Unit) {
    Column {
        BoxWithConstraints(
            modifier = Modifier.weight(1f)
                .padding(top = 16.dp, bottom = 8.dp, start = 32.dp, end = 32.dp)
        ) {
            val rowHeight = maxHeight / 3
            val textSize = (rowHeight * TEXT_SCALE).value.sp

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                state.result.arrivals.forEach {
                    ArrivalRow(it, textSize)
                }
            }
        }
        Row(
            modifier = Modifier
                .background(color = Footer)
                .padding(start = 32.dp, end = 28.dp)
                .fillMaxWidth()
                .height(footerHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(bottom = 4.dp),
                text = state.result.station,
                color = Text,
                style = TextStyle(
                    fontSize = 32.sp
                )
            )
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.refreshing) {
                    CircularProgressIndicator(color = LedYellow, modifier = Modifier.size(22.dp))
                } else {
                    TextButton(
                        onClick = { onClickRefresh() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Text,
                            backgroundColor = Footer
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(32.dp),
                            tint = Text
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Error(message: String) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .padding(top = 16.dp, bottom = 8.dp, start = 32.dp, end = 32.dp)
    ) {
        val rowHeight = (maxHeight - footerHeight) / 3 // Match text size in Data composable
        val textSize = (rowHeight * TEXT_SCALE).value.sp

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            LedText(message, textSize)
        }
    }
}

@Composable
fun ArrivalRow(arrival: Arrival, textSize: TextUnit) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        LedText(arrival.destination, textSize, modifier = Modifier.weight(1f))
        if (arrival.secondsToStop < 60) {
            FlashingLedText(arrival.time, textSize)
        } else {
            LedText(arrival.time, textSize)
        }
    }
}

@Composable
fun LedText(
    string: String,
    textSize: TextUnit,
    color: Color = LedYellow,
    modifier: Modifier = Modifier
) {
    Text(
        text = filterLedChars(string),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontFamily = LurFontFamily,
            fontSize = textSize
        ),
        modifier = modifier
    )
}

@Composable
fun FlashingLedText(string: String, textSize: TextUnit) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    LedText(string, textSize, LedYellow.copy(alpha = alpha))
}

private const val TEXT_SCALE = 0.58f
private val footerHeight = 70.dp
