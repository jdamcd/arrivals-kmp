package com.jdamcd.arrivals.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jdamcd.arrivals.Arrival
import com.jdamcd.arrivals.Arrivals
import com.jdamcd.arrivals.initKoin

private val koin = initKoin().koin

fun main() = application {
    val windowState = rememberWindowState()

    var arrivalsState by remember { mutableStateOf(emptyList<Arrival>()) }

    val arrivalsApi = koin.get<Arrivals>()

    LaunchedEffect(true) {
        arrivalsState = arrivalsApi.latest().arrivals
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Arrivals"
    ) {
        ArrivalsList(arrivalsState)
    }
}

@Composable
fun ArrivalsList(
    arrivals: List<Arrival>
) {
    Column(
        modifier = Modifier
            .background(color = Color.Black)
            .padding(30.dp)
            .fillMaxSize()
    ) {
        arrivals.forEach {
            ArrivalRow(it)
        }
    }
}

@Composable
fun ArrivalRow(arrival: Arrival) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
            .padding(bottom = 30.dp)
    ) {
        Text(
            text = arrival.destination,
            color = Color.Yellow,
            style = TextStyle(
                fontFamily = lurFontFamily,
                fontSize = 50.sp
            )
        )
        Text(
            arrival.time,
            color = Color.Yellow,
            style = TextStyle(
                fontFamily = lurFontFamily,
                fontSize = 50.sp
            )
        )
    }
}

val lurFontFamily = FontFamily(
    fonts = listOf(
        Font(
            resource = "font/LUR.ttf",
            weight = FontWeight.W400,
            style = FontStyle.Normal
        )
    )
)
