package com.example.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.WorkerParameters
import com.example.core.data.local.AppDatabase
import com.example.core.data.local.MediaFileDao
import com.example.core.data.repository.MediaFileRepository
import com.example.core.data.repository.MediaFileRepositoryImpl
import com.example.feature.SmartManagerViewModel
import com.example.feature.background.BackgroundManager
import com.example.feature.background.CloudSyncWorker
import com.example.feature.background.MediaScanWorker
import com.example.feature.search.CoreSearchEngine
import com.example.plugin.cloud.GitHubSyncProvider
import com.example.plugin.cloud.GoogleDriveProvider
import com.example.plugin.semanticsearch.AutoTagger
import com.example.plugin.semanticsearch.DeepDuplicateCleaner
import com.example.plugin.semanticsearch.SemanticSearchEngine
import com.example.core.security.KeystoreManager
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
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module
import java.security.SecureRandom

val databaseModule = module {
    single {
        Json { 
            ignoreUnknownKeys = true 
            prettyPrint = true
            isLenient = true
        }
    }

    single {
        OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(get<okhttp3.logging.HttpLoggingInterceptor>())
            .build()
    }

    single {
        okhttp3.logging.HttpLoggingInterceptor().apply {
            level = if (com.example.BuildConfig.DEBUG) {
                okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
            } else {
                okhttp3.logging.HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    single {
        val dbName = "vvf_smart_manager.db"
        val dbFile = androidContext().getDatabasePath(dbName)

        val ftsCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                createFtsTriggers(db)
            }
            override fun onOpen(db: SupportSQLiteDatabase) {
                createFtsTriggers(db)
            }
            private fun createFtsTriggers(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_media_files_AFTER_INSERT AFTER INSERT ON media_files BEGIN INSERT INTO media_files_fts(docid, name, tags, ocrText) VALUES (NEW.id, NEW.name, NEW.tags, NEW.ocrText); END;")
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_media_files_BEFORE_UPDATE BEFORE UPDATE ON media_files BEGIN DELETE FROM media_files_fts WHERE docid=OLD.id; END;")
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_media_files_AFTER_UPDATE AFTER UPDATE ON media_files BEGIN INSERT INTO media_files_fts(docid, name, tags, ocrText) VALUES (NEW.id, NEW.name, NEW.tags, NEW.ocrText); END;")
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_media_files_BEFORE_DELETE BEFORE DELETE ON media_files BEGIN DELETE FROM media_files_fts WHERE docid=OLD.id; END;")
                } catch (e: Exception) {
                    // Triggers existing or ignored
                }
            }
        }

        fun buildDatabase(factory: SupportFactory): AppDatabase {
            val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_media_files_path` ON `media_files` (`path`)")
                }
            }

            return Room.databaseBuilder(
                androidContext(),
                AppDatabase::class.java,
                dbName
            )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .addCallback(ftsCallback)
            .build()
        }

        try {
            // SQLCipher configuration - Using a secure key from Android Keystore
            val passphrase = KeystoreManager.getDatabasePassphrase(androidContext())
            val factory = SupportFactory(passphrase)
            
            val db = buildDatabase(factory)
            
            // Trigger a database open to check if the key is correct
            try {
                db.openHelper.writableDatabase
                db
            } catch (e: Exception) {
                // If it fails (likely "file is not a database" due to wrong key), delete and retry
                val errorMsg = e.message ?: ""
                if (dbFile.exists() && (errorMsg.contains("file is not a database") || errorMsg.contains("file is encrypted"))) {
                    dbFile.delete()
                    // Delete shm and wal files if they exist
                    androidContext().getDatabasePath("$dbName-shm").delete()
                    androidContext().getDatabasePath("$dbName-wal").delete()
                }
                buildDatabase(factory)
            }
        } catch (t: Throwable) {
            // Fallback for JVM Robolectric testing environment ONLY
            val isRobolectric = try { Class.forName("org.robolectric.Robolectric"); true } catch (e: Exception) { false }
            if (isRobolectric) {
                Room.inMemoryDatabaseBuilder(
                    androidContext(),
                    AppDatabase::class.java
                )
                .allowMainThreadQueries()
                .build()
            } else {
                throw t
            }
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
    
    single { GitHubSyncProvider(get()) }
    
    single { BackgroundManager(get()) }
    
    single { androidx.work.WorkManager.getInstance(androidContext()) }
    
    single { VaultSessionManager(androidContext()) }
    
    single { VaultEncryptionManager() }

    single<OcrEngine> { MlKitOcrEngine(androidContext()) }

    single { CoreFileManager() }

    single { BiometricAuthManager(androidContext()) }

    single<GeminiApiService> {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    single { GeminiService(get()) }

    worker { params ->
        CloudSyncWorker(get(), params.get(), get(), get())
    }

    worker { params ->
        MediaScanWorker(get(), params.get(), get(), get())
    }

    viewModel {
        SmartManagerViewModel(
            mediaFileRepository = get(),
            semanticSearchEngine = get(),
            coreSearchEngine = get(),
            autoTagger = get(),
            deepDuplicateCleaner = get(),
            driveProvider = get(),
            gitHubSyncProvider = get(),
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
