package com.rejowan.pdfreaderpro.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavController

/**
 * Guards tool success exit against double-taps.
 *
 * Root cause of the white screen:
 * 1. Rapid Done called [NavController.popBackStack] more than once, emptying
 *    the NavHost (blank/white surface).
 * 2. Open-in-app navigated to the reader without popping the tool first, so a
 *    later Done could pop inconsistently.
 *
 * Contract: pop the tool destination at most once; optionally open the reader.
 * Do not clear success state before navigating — that briefly shows empty tool UI.
 *
 * [resultKey] resets the one-shot guard when a new success result appears.
 */
@Composable
fun rememberToolExitHandler(
    navController: NavController,
    resultKey: Any?
): ToolExitHandler {
    val handled = remember(resultKey) { mutableStateOf(false) }
    return remember(navController, resultKey) {
        ToolExitHandler(
            onDone = {
                if (handled.value) return@ToolExitHandler
                handled.value = true
                navController.popBackStack()
            },
            onOpenInApp = { path ->
                if (handled.value) return@ToolExitHandler
                handled.value = true
                navController.popBackStack()
                navController.navigateToReader(path)
            }
        )
    }
}

class ToolExitHandler(
    val onDone: () -> Unit,
    val onOpenInApp: (path: String) -> Unit
)
