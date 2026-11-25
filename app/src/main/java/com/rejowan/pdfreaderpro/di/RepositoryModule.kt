package com.rejowan.pdfreaderpro.di

import com.rejowan.pdfreaderpro.data.repository.FavoriteRepositoryImpl
import com.rejowan.pdfreaderpro.data.repository.PdfFileRepositoryImpl
import com.rejowan.pdfreaderpro.data.repository.PdfToolsRepositoryImpl
import com.rejowan.pdfreaderpro.data.repository.PreferencesRepositoryImpl
import com.rejowan.pdfreaderpro.data.repository.RecentRepositoryImpl
import com.rejowan.pdfreaderpro.domain.repository.FavoriteRepository
import com.rejowan.pdfreaderpro.domain.repository.PdfFileRepository
import com.rejowan.pdfreaderpro.domain.repository.PdfToolsRepository
import com.rejowan.pdfreaderpro.domain.repository.PreferencesRepository
import com.rejowan.pdfreaderpro.domain.repository.RecentRepository
import com.rejowan.pdfreaderpro.util.FileOperations
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single<PdfFileRepository> { PdfFileRepositoryImpl(androidContext()) }
    single<RecentRepository> {
        val context = androidContext()
        RecentRepositoryImpl(get(), canVerifyFiles = { FileOperations.hasStorageAccess(context) })
    }
    single<FavoriteRepository> {
        val context = androidContext()
        FavoriteRepositoryImpl(get(), canVerifyFiles = { FileOperations.hasStorageAccess(context) })
    }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    single<PdfToolsRepository> { PdfToolsRepositoryImpl(androidContext()) }
}
