package com.muratbalcif.blockpuzzle.persistence

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidProgressStore(context: Context, name: String = "blockpuzzle_progress") :
    ProgressStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        name,
        Context.MODE_PRIVATE,
    )
    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        preferences.getString("document", null)
    }

    // KTX edit(commit=true) returns Unit and hides commit failure; retain the checked Boolean.
    @SuppressLint("UseKtx")
    override suspend fun write(value: String) {
        withContext(Dispatchers.IO) {
            check(preferences.edit().putString("document", value).commit()) {
                "Progress write failed"
            }
        }
    }
}

@Composable
actual fun rememberProgressStore(): ProgressStore {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidProgressStore(context) }
}
