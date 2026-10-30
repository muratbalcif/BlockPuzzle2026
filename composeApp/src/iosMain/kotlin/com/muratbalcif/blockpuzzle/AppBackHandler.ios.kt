package com.muratbalcif.blockpuzzle

import androidx.compose.runtime.Composable

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // The iOS root controller has no system back action; visible controls provide navigation.
}
