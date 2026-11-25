package com.rejowan.pdfreaderpro.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet drag handle with the same look as the Material default but a touch area of at
 * least 48x48dp (the default handle is only 32dp wide).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        BottomSheetDefaults.DragHandle()
    }
}
