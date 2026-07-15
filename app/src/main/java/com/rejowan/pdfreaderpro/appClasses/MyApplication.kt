package com.rejowan.pdfreaderpro.appClasses

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.itextpdf.kernel.utils.XmlProcessorCreator
import com.rejowan.pdfreaderpro.BuildConfig
import com.rejowan.pdfreaderpro.di.dataStoreModule
import com.rejowan.pdfreaderpro.di.databaseModule
import com.rejowan.pdfreaderpro.di.repositoryModule
import com.rejowan.pdfreaderpro.di.viewModelModule
import com.rejowan.pdfreaderpro.domain.repository.PreferencesRepository
import com.rejowan.pdfreaderpro.util.AndroidSafeXmlParserFactory
import com.rejowan.pdfreaderpro.util.AppLocaleManager
import com.rejowan.pdfreaderpro.util.GlobalErrorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class MyApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        // Critical path: Initialize Timber first for error logging
        initTimber()

        // iText XMP on Android — before any PDF tool opens a document
        XmlProcessorCreator.setXmlParserFactory(AndroidSafeXmlParserFactory())

        // Critical path: Initialize Koin (required for dependency injection)
        initKoin()

        // Restore persisted app language (SYSTEM uses empty locale list = device default)
        applicationScope.launch {
            val language = getKoin().get<PreferencesRepository>().preferences.first().appLanguage
            AppLocaleManager.apply(language)
        }

        // Defer non-critical initialization to avoid blocking startup
        Handler(Looper.getMainLooper()).post {
            // Setup global error handler (can be deferred)
            GlobalErrorHandler.setup(this)
            Timber.d("Application initialized successfully")
        }
    }

    private fun initTimber() {
        // Only plant Timber tree if logging is enabled (debug builds only)
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(Timber.DebugTree())
        }
        // In release builds: no trees planted = no logging overhead
    }

    private fun initKoin() {
        startKoin {
            // Disable Koin logging in release to minimize overhead
            androidLogger(if (BuildConfig.ENABLE_LOGGING) Level.DEBUG else Level.NONE)
            androidContext(this@MyApplication)
            modules(
                // Load modules in dependency order
                databaseModule,      // Core database (lazy singleton)
                dataStoreModule,     // Preferences (lazy singleton)
                repositoryModule,    // Repositories (lazy singletons)
                viewModelModule      // ViewModels (factory - created on demand)
            )
        }
    }
}
