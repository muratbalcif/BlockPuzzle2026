@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.muratbalcif.blockpuzzle.persistence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSUserDefaults

class IosProgressStore(private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults) :
    ProgressStore {
    override suspend fun read(): String? = withContext(Dispatchers.Default) {
        defaults.stringForKey("blockpuzzle_progress_v1")
    }
    override suspend fun write(value: String) {
        withContext(Dispatchers.Default) {
            defaults.setObject(value, forKey = "blockpuzzle_progress_v1")
        }
    }
}

@Composable
actual fun rememberProgressStore(): ProgressStore = remember { IosProgressStore() }
