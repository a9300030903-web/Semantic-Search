package com.example.plugin.cloud

import android.accounts.Account
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.BuildConfig
import com.example.core.data.local.CloudSyncTaskDao
import com.example.core.data.local.CloudSyncTaskEntity
import com.example.core.data.local.MediaFileDao
import com.example.core.model.MediaFile
import com.example.core.security.VaultEncryptionManager
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class GoogleDriveProvider(
    private val context: Context,
    private val cloudSyncTaskDao: CloudSyncTaskDao,
    private val mediaFileDao: MediaFileDao
) : CloudProvider {

    override val providerId = "google_drive"
    override val providerName = "Google Drive"

    private val httpClient = OkHttpClient()
    private val encryptionManager = VaultEncryptionManager()
    private var accessToken: String? = null
    private var accountEmail: String? = null

    override fun isAuthenticated(): Boolean {
        return accessToken != null
    }

    override suspend fun authenticate() {
        withContext(Dispatchers.IO) {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("YOUR_WEB_CLIENT_ID_HERE")
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    accountEmail = googleIdTokenCredential.id
                    
                    val account = Account(accountEmail!!, "com.google")
                    val scope = "oauth2:https://www.googleapis.com/auth/drive.file"
                    
                    // Retrieve access token
                    accessToken = GoogleAuthUtil.getToken(context, account, scope)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    override suspend fun uploadFile(file: MediaFile): Result<String> = withContext(Dispatchers.IO) {
        if (accessToken == null) return@withContext Result.failure(Exception("Not authenticated"))
        
        try {
            val originalFile = File(file.path)
            if (!originalFile.exists()) {
                return@withContext Result.failure(Exception("File not found"))
            }

            // Check if there is an existing task to resume
            val existingTask = cloudSyncTaskDao.getTaskForFile(file.id, "UPLOAD")
            
            // For now, always create a new upload session, but we should use resumable if uri exists
            // This implementation handles multipart upload as requested.
            
            // Encrypt before upload
            val encryptedFile = File(context.cacheDir, "enc_${originalFile.name}")
            encryptionManager.encryptFile(originalFile, encryptedFile, file.id.toString())

            val metadata = JSONObject().apply {
                put("name", originalFile.name)
            }.toString()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata", 
                    null, 
                    metadata.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addFormDataPart(
                    "file", 
                    encryptedFile.name, 
                    encryptedFile.asRequestBody("application/octet-stream".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Upload failed with code ${response.code}: ${response.message}")
                }
                
                val responseBody = response.body?.string()
                val json = JSONObject(responseBody ?: "")
                val fileId = json.optString("id")
                
                encryptedFile.delete()
                
                if (fileId.isNotEmpty()) {
                    if (existingTask != null) {
                        cloudSyncTaskDao.deleteTask(existingTask.id)
                    }
                    Result.success(fileId)
                } else {
                    Result.failure(Exception("Failed to parse file ID from Drive response"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Add to offline queue
            queueUploadTask(file, e.message)
            Result.failure(e)
        }
    }

    override suspend fun downloadFile(cloudFileId: String, destinationPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (accessToken == null) return@withContext Result.failure(Exception("Not authenticated"))
        
        try {
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$cloudFileId?alt=media")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Download failed with code ${response.code}")
                
                val encryptedFile = File(context.cacheDir, "enc_download_$cloudFileId")
                
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(encryptedFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val destinationFile = File(destinationPath)
                encryptionManager.decryptFile(encryptedFile, destinationFile, destinationFile.nameWithoutExtension) 
                
                encryptedFile.delete()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun syncChanges(): Result<Unit> = withContext(Dispatchers.IO) {
        if (accessToken == null) return@withContext Result.failure(Exception("Not authenticated"))
        
        try {
            val pendingTasks = cloudSyncTaskDao.getPendingTasks().filter { it.providerId == providerId }
            
            for (task in pendingTasks) {
                cloudSyncTaskDao.updateTask(task.copy(status = "IN_PROGRESS"))
                
                try {
                    val mediaFileEntity = mediaFileDao.getFileById(task.fileId)
                    if (mediaFileEntity == null) {
                        cloudSyncTaskDao.deleteTask(task.id)
                        continue
                    }
                    
                    val file = MediaFile(
                        id = mediaFileEntity.id,
                        name = mediaFileEntity.name,
                        path = mediaFileEntity.path,
                        type = mediaFileEntity.type,
                        mimeType = mediaFileEntity.mimeType,
                        size = mediaFileEntity.size,
                        createdAt = mediaFileEntity.createdAt,
                        modifiedAt = mediaFileEntity.modifiedAt,
                        isEncrypted = mediaFileEntity.isEncrypted,
                        tags = mediaFileEntity.tags,
                        ocrText = mediaFileEntity.ocrText,
                        semanticEmbedding = mediaFileEntity.semanticEmbedding
                    )

                    if (task.action == "UPLOAD") {
                        // Check for conflict (mock remote check as we don't have remote state mapping easily)
                        val remoteModifiedTime = getRemoteModifiedTime(file.name)
                        if (remoteModifiedTime > file.modifiedAt) {
                            cloudSyncTaskDao.updateTask(task.copy(
                                status = "FAILED", 
                                errorMessage = "Conflict: Remote file is newer",
                                lastAttempt = System.currentTimeMillis()
                            ))
                            continue
                        }

                        val result = uploadFile(file)
                        if (result.isSuccess) {
                            cloudSyncTaskDao.deleteTask(task.id)
                        } else {
                            cloudSyncTaskDao.updateTask(task.copy(
                                status = "FAILED",
                                errorMessage = result.exceptionOrNull()?.message,
                                retryCount = task.retryCount + 1,
                                lastAttempt = System.currentTimeMillis()
                            ))
                        }
                    }
                } catch (e: Exception) {
                    cloudSyncTaskDao.updateTask(task.copy(
                        status = "FAILED",
                        errorMessage = e.message,
                        retryCount = task.retryCount + 1,
                        lastAttempt = System.currentTimeMillis()
                    ))
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getSyncProgress(): Flow<Float> {
        return flowOf(0f)
    }

    private suspend fun queueUploadTask(file: MediaFile, error: String? = null) {
        val existingTask = cloudSyncTaskDao.getTaskForFile(file.id, "UPLOAD")
        if (existingTask == null) {
            val newTask = CloudSyncTaskEntity(
                fileId = file.id,
                action = "UPLOAD",
                providerId = providerId,
                cloudFileId = null,
                status = "PENDING",
                totalBytes = file.size,
                errorMessage = error
            )
            cloudSyncTaskDao.insertTask(newTask)
        } else {
            cloudSyncTaskDao.updateTask(existingTask.copy(
                status = "PENDING",
                errorMessage = error,
                lastAttempt = System.currentTimeMillis()
            ))
        }
    }

    private suspend fun getRemoteModifiedTime(fileName: String): Long {
        // In a real implementation, we would query Drive for the file by name to get its modifiedTime.
        // Returning 0L ensures local overwrites remote by default unless conflict is detected.
        return 0L
    }
}
