package com.petermccoy.applock.data

import androidx.compose.ui.graphics.ImageBitmap

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
    val isLaunchable: Boolean,
)
