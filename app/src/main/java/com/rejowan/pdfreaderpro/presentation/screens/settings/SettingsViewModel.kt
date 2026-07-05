package com.rejowan.pdfreaderpro.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejowan.pdfreaderpro.domain.model.AppPreferences
import com.rejowan.pdfreaderpro.domain.model.QuickZoomPreset
import com.rejowan.pdfreaderpro.domain.model.ReadingTheme
import com.rejowan.pdfreaderpro.domain.model.ScreenOrientation
import com.rejowan.pdfreaderpro.domain.model.ScrollMode
import com.rejowan.pdfreaderpro.domain.model.ThemeMode
import com.rejowan.pdfreaderpro.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setShowToolsTab(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowToolsTab(enabled)
        }
    }

    fun setReaderBrightness(brightness: Float) {
        viewModelScope.launch {
            preferencesRepository.setReaderBrightness(brightness)
        }
    }

    fun setReaderScrollMode(mode: ScrollMode) {
        viewModelScope.launch {
            preferencesRepository.setReaderScrollMode(mode)
        }
    }

    fun setReaderAutoHideToolbar(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReaderAutoHideToolbar(enabled)
        }
    }

    fun setReaderQuickZoomPreset(preset: QuickZoomPreset) {
        viewModelScope.launch {
            preferencesRepository.setReaderQuickZoomPreset(preset)
        }
    }

    fun setReaderDoubleTapZoom(zoom: Float) {
        viewModelScope.launch {
            preferencesRepository.setReaderDoubleTapZoom(zoom)
        }
    }

    fun setReaderKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReaderKeepScreenOn(enabled)
        }
    }

    fun setReaderTheme(theme: ReadingTheme) {
        viewModelScope.launch {
            preferencesRepository.setReaderTheme(theme)
        }
    }

    fun setReaderSnapToPages(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setReaderSnapToPages(enabled)
        }
    }

    fun setReaderScreenOrientation(orientation: ScreenOrientation) {
        viewModelScope.launch {
            preferencesRepository.setReaderScreenOrientation(orientation)
        }
    }
}
