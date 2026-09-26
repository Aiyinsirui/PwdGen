package com.pwdgen.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Small label + value row used across the generator result card. */
@Composable
fun LabeledRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/** A titled settings group. */
@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        content()
    }
}

/** Simple 0..100 strength meter. */
@Composable
fun StrengthBar(percent: Int, modifier: Modifier = Modifier) {
    val clamped = percent.coerceIn(0, 100)
    val color = when {
        clamped >= 80 -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        clamped >= 55 -> androidx.compose.ui.graphics.Color(0xFFF9A825)
        else -> androidx.compose.ui.graphics.Color(0xFFC62828)
    }
    androidx.compose.material3.LinearProgressIndicator(
        progress = { clamped / 100f },
        modifier = modifier.fillMaxWidth(),
        color = color
    )
}