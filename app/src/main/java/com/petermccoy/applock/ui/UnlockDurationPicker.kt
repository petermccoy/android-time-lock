package com.petermccoy.applock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.petermccoy.applock.R

/**
 * Shown immediately after successful biometric auth, before the lock overlay
 * dismisses, so the user can pick how long the unlocked grace period lasts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockDurationPicker(
    initialSelection: UnlockDuration,
    onConfirm: (UnlockDuration) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(initialSelection) }
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

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).width(240.dp),
                        readOnly = true,
                        value = durationLabel(selected),
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        UNLOCK_DURATION_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(durationLabel(option)) },
                                onClick = {
                                    selected = option
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { onConfirm(selected) }) {
                    Text(text = stringResource(R.string.duration_picker_confirm))
                }
            }
        }
    }
}

@Composable
private fun durationLabel(duration: UnlockDuration): String = when (duration) {
    UnlockDuration.UntilScreenOff -> stringResource(R.string.duration_until_screen_off)
    is UnlockDuration.Fixed -> when (duration.minutes) {
        5 -> stringResource(R.string.duration_5_min)
        10 -> stringResource(R.string.duration_10_min)
        15 -> stringResource(R.string.duration_15_min)
        30 -> stringResource(R.string.duration_30_min)
        45 -> stringResource(R.string.duration_45_min)
        60 -> stringResource(R.string.duration_60_min)
        else -> "${duration.minutes} min"
    }
}
