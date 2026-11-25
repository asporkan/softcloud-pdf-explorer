package com.rejowan.pdfreaderpro.presentation.screens.reader.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import com.rejowan.pdfreaderpro.presentation.theme.AccentTone

/**
 * Slider colours for an accent tone.
 *
 * The Material defaults draw the active ticks in `onPrimary` (dark purple) on top of the active
 * track, which is nearly invisible on a dark surface. Here the ticks use the tone's own pair:
 * `onColor` on the active track (`color`) and `color` on the inactive track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun accentSliderColors(tone: AccentTone): SliderColors = SliderDefaults.colors(
    thumbColor = tone.color,
    activeTrackColor = tone.color,
    inactiveTrackColor = tone.color.copy(alpha = INACTIVE_TRACK_ALPHA),
    activeTickColor = tone.onColor,
    inactiveTickColor = tone.color
)

/**
 * Switch colours for an accent tone: the checked track is the solid accent `color`, the thumb
 * the matching `onColor`. Unchecked keeps the Material outline look.
 */
@Composable
internal fun accentSwitchColors(tone: AccentTone): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = tone.onColor,
    checkedTrackColor = tone.color,
    checkedBorderColor = tone.color
)

/** Inactive slider tracks are a faint wash of the accent; ticks stay full-strength `color`. */
private const val INACTIVE_TRACK_ALPHA = 0.32f
