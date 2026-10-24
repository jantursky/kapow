package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jantursky.kapow.SpinAxis
import com.jantursky.kapow.SpinRate
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.web.examples.SpinExample

/** The controls around [SpinExample]. */
@Composable
internal fun SpinDemo() {
    var count by remember { mutableIntStateOf(0) }
    var axis by remember { mutableStateOf(SpinAxis.Y) }
    var rate by remember { mutableStateOf(SpinRate.Default) }
    ExampleScaffold(
        stage = { SpinExample(count, axis, rate) },
        controls = {
            PrimaryPill("Collect", onClick = { count++ })
            ChoiceRow("Axis", SpinAxis.entries, axis, { axis = it }, { it.name })
            ChoiceRow("Rate", SpinRate.entries, rate, { rate = it }, { it.name })
        },
    )
}
