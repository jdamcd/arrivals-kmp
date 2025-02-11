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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jdamcd.arrivals.Arrival

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
        Column(
            modifier = Modifier
                .padding(top = 20.dp, bottom = 8.dp, start = 32.dp, end = 32.dp)
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            state.result.arrivals.forEach {
                ArrivalRow(it)
            }
        }
        Row(
            modifier = Modifier
                .background(color = Footer)
                .padding(start = 32.dp, end = 28.dp)
                .fillMaxWidth()
                .height(70.dp),
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
    Column(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        LedText(message)
    }
}

@Composable
fun ArrivalRow(arrival: Arrival) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        LedText(arrival.destination)
        if (arrival.secondsToStop < 60) {
            FlashingLedText(arrival.time)
        } else {
            LedText(arrival.time)
        }
    }
}

@Composable
fun LedText(string: String) {
    Text(
        text = string,
        color = LedYellow,
        maxLines = 1,
        style = TextStyle(
            fontFamily = LurFontFamily,
            fontSize = 58.sp
        )
    )
}

@Composable
fun FlashingLedText(string: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Text(
        text = string,
        color = LedYellow.copy(alpha = alpha),
        maxLines = 1,
        style = TextStyle(
            fontFamily = LurFontFamily,
            fontSize = 58.sp
        )
    )
}
