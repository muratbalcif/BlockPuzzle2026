package com.muratbalcif.blockpuzzle.persistence

import androidx.compose.runtime.Composable

interface ProgressStore {
    suspend fun read(): String?
    suspend fun write(value: String)
}

@Composable
expect fun rememberProgressStore(): ProgressStore
