package com.petermccoy.applock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.petermccoy.applock.R

val UNLOCK_DURATION_OPTIONS_MINUTES = listOf(5, 10, 15, 30, 45, 60)

/**
 * Shown immediately after successful biometric auth, before the lock overlay
 * dismisses, so the user can pick how long the unlocked grace period lasts.
 */
@Composable
fun UnlockDurationPicker(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedMinutes by remember {
        mutableIntStateOf(
            if (initialMinutes in UNLOCK_DURATION_OPTIONS_MINUTES) {
                initialMinutes
            } else {
                UNLOCK_DURATION_OPTIONS_MINUTES.first()
            }
        )
    }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.padding(24.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.duration_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(text = durationLabel(selectedMinutes))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        UNLOCK_DURATION_OPTIONS_MINUTES.forEach { minutes ->
                            DropdownMenuItem(
                                text = { Text(durationLabel(minutes)) },
                                onClick = {
                                    selectedMinutes = minutes
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { onConfirm(selectedMinutes) }) {
                    Text(text = stringResource(R.string.duration_picker_confirm))
                }
            }
        }
    }
}

@Composable
private fun durationLabel(minutes: Int): String = when (minutes) {
    5 -> stringResource(R.string.duration_5_min)
    10 -> stringResource(R.string.duration_10_min)
    15 -> stringResource(R.string.duration_15_min)
    30 -> stringResource(R.string.duration_30_min)
    45 -> stringResource(R.string.duration_45_min)
    60 -> stringResource(R.string.duration_60_min)
    else -> "$minutes min"
}
