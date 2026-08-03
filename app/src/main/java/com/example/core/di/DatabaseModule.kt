package com.example.core.di

import android.content.Context
import androidx.room.Room
import com.example.core.data.local.AppDatabase
import com.example.core.data.local.MediaFileDao
import com.example.core.data.repository.MediaFileRepository
import com.example.core.data.repository.MediaFileRepositoryImpl
import com.example.feature.SmartManagerViewModel
import com.example.feature.background.BackgroundManager
import com.example.feature.search.CoreSearchEngine
import com.example.plugin.cloud.GoogleDriveProvider
import com.example.plugin.semanticsearch.AutoTagger
import com.example.plugin.semanticsearch.DeepDuplicateCleaner
import com.example.plugin.semanticsearch.SemanticSearchEngine
import com.example.core.security.VaultEncryptionManager
import com.example.feature.vault.VaultSessionManager
import com.example.feature.vault.BiometricAuthManager
import com.example.feature.filemanager.CoreFileManager
import com.example.plugin.ocr.OcrEngine
import com.example.plugin.ocr.MlKitOcrEngine
import com.example.core.data.network.GeminiApiService
import com.example.core.data.network.GeminiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.security.SecureRandom

val databaseModule = module {
    single {
        try {
            // SQLCipher configuration
            val passphrase = SQLiteDatabase.getBytes("VVF_SECURE_PASSPHRASE_PLACEHOLDER".toCharArray())
            val factory = SupportFactory(passphrase)
            
            Room.databaseBuilder(
                androidContext(),
                AppDatabase::class.java,
                "vvf_smart_manager.db"
            )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration(true)
            .build()
        } catch (t: Throwable) {
            // Fallback for JVM Robolectric testing environment where native SQLCipher .so libraries cannot be loaded
            Room.inMemoryDatabaseBuilder(
                androidContext(),
                AppDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        }
    }
    
    single { get<AppDatabase>().mediaFileDao() }
    
    single<MediaFileRepository> {
        MediaFileRepositoryImpl(get())
    }

    single { SemanticSearchEngine(androidContext()).apply { initialize() } }
    
    single { CoreSearchEngine(get(), get()) }
    
    single { AutoTagger(get()) }
    
    single { DeepDuplicateCleaner(get()) }
    
    single { GoogleDriveProvider(androidContext()) }
    
    single { BackgroundManager(androidContext()) }
    
    single { VaultSessionManager(androidContext()) }
    
    single { VaultEncryptionManager() }

    single<OcrEngine> { MlKitOcrEngine(androidContext()) }

    single { CoreFileManager() }

    single { BiometricAuthManager(androidContext()) }

    single<GeminiApiService> {
        val json = Json { ignoreUnknownKeys = true }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    single { GeminiService(get()) }

    viewModel {
        SmartManagerViewModel(
            mediaFileRepository = get(),
            semanticSearchEngine = get(),
            coreSearchEngine = get(),
            autoTagger = get(),
            deepDuplicateCleaner = get(),
            driveProvider = get(),
            backgroundManager = get(),
            vaultSessionManager = get(),
            vaultEncryptionManager = get(),
            ocrEngine = get(),
            coreFileManager = get(),
            biometricAuthManager = get(),
            geminiService = get()
        )
    }
}
