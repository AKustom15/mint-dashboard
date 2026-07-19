package com.akustom15.mint.library.ui.screens.icons

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.akustom15.mint.library.ui.theme.MintTheme

class MintIconPickerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MintTheme {
                IconsPreviewScreen(
                    onNavigateBack = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    isPickerMode = true,
                    onIconPicked = { resId, _ ->
                        val resultIntent = Intent()
                        resultIntent.putExtra("icon", resId)
                        resultIntent.putExtra(
                            "android.intent.extra.shortcut.ICON_RESOURCE",
                            Intent.ShortcutIconResource.fromContext(this, resId)
                        )
                        resultIntent.data = Uri.parse("android.resource://$packageName/$resId")
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                )
            }
        }
    }
}
