package com.example.feature

import com.example.core.data.network.GeminiService
import com.example.core.data.repository.MediaFileRepository
import com.example.core.model.MediaFile
import com.example.core.util.HashUtil
import com.example.core.security.VaultEncryptionManager
import com.example.feature.background.BackgroundManager
import com.example.feature.filemanager.CoreFileManager
import com.example.feature.search.CoreSearchEngine
import com.example.feature.vault.VaultSessionManager
import com.example.feature.vault.BiometricAuthManager
import com.example.plugin.cloud.GoogleDriveProvider
import com.example.plugin.ocr.OcrEngine
import com.example.plugin.ocr.MlKitOcrEngine
import com.example.plugin.semanticsearch.AutoTagger
import com.example.plugin.semanticsearch.DeepDuplicateCleaner
import com.example.plugin.semanticsearch.SemanticSearchEngine
import com.example.core.model.MediaScanState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.update
import java.io.File

class SmartManagerViewModel(
    private val applicationContext: android.content.Context,
    private val mediaFileRepository: MediaFileRepository,
    private val semanticSearchEngine: SemanticSearchEngine,
    private val coreSearchEngine: CoreSearchEngine,
    private val autoTagger: AutoTagger,
    private val deepDuplicateCleaner: DeepDuplicateCleaner,
    private val driveProvider: GoogleDriveProvider,
    private val backgroundManager: BackgroundManager,
    private val vaultSessionManager: VaultSessionManager,
    private val vaultEncryptionManager: VaultEncryptionManager,
    private val ocrEngine: OcrEngine,
    private val coreFileManager: CoreFileManager,
    private val biometricAuthManager: BiometricAuthManager,
    private val geminiService: GeminiService
) : ViewModel() {

    // Expose Gemini co-pilot flow
    private val _assistantResponse = MutableStateFlow("Namaste! I am your VVF AI Assistant. How can I help you organize or search your media files today?")
    val assistantResponse: StateFlow<String> = _assistantResponse.asStateFlow()

    private val _isAssistantLoading = MutableStateFlow(false)
    val isAssistantLoading: StateFlow<Boolean> = _isAssistantLoading.asStateFlow()

    // Auto-Tagging state
    private val _isAutoTagging = MutableStateFlow(false)
    val isAutoTagging: StateFlow<Boolean> = _isAutoTagging.asStateFlow()

    private val _autoTagStatus = MutableStateFlow("")
    val autoTagStatus: StateFlow<String> = _autoTagStatus.asStateFlow()

    fun autoTagFile(file: MediaFile) {
        viewModelScope.launch {
            _isAutoTagging.value = true
            _autoTagStatus.value = "Analyzing '${file.name}' with Gemini AI..."
            try {
                val generatedTags = autoTagger.generateTagsWithGemini(file, geminiService)
                val newTags = if (file.tags.isBlank()) generatedTags else "${file.tags}, $generatedTags"
                val updatedFile = file.copy(
                    tags = newTags.split(",").map { it.trim() }.distinct().joinToString(", ")
                )
                mediaFileRepository.updateFile(updatedFile)
                _selectedDetailFile.update { current ->
                    if (current?.id == file.id) updatedFile else current
                }
                _autoTagStatus.value = "Generated tags for '${file.name}': $generatedTags"
            } catch (e: Exception) {
                _autoTagStatus.value = "Failed to tag file: ${e.localizedMessage ?: e.message}"
            } finally {
                _isAutoTagging.value = false
            }
        }
    }

    fun autoTagAllFiles() {
        viewModelScope.launch {
            val allFilesList = files.value
            if (allFilesList.isEmpty()) {
                _autoTagStatus.value = "No files available to auto-tag."
                return@launch
            }
            _isAutoTagging.value = true
            _autoTagStatus.value = "Starting Gemini AI batch auto-tagging..."
            try {
                var taggedCount = 0
                for (file in allFilesList) {
                    _autoTagStatus.value = "Auto-tagging [${taggedCount + 1}/${allFilesList.size}]: ${file.name}"
                    val generatedTags = autoTagger.generateTagsWithGemini(file, geminiService)
                    val updatedFile = file.copy(
                        tags = generatedTags.split(",").map { it.trim() }.distinct().joinToString(", ")
                    )
                    mediaFileRepository.updateFile(updatedFile)
                    taggedCount++
                }
                _autoTagStatus.value = "Successfully auto-tagged $taggedCount files with Gemini AI!"
            } catch (e: Exception) {
                _autoTagStatus.value = "Batch auto-tagging error: ${e.localizedMessage ?: e.message}"
            } finally {
                _isAutoTagging.value = false
            }
        }
    }

    fun askCopilot(query: String) {
        if (query.isBlank()) return
        _isAssistantLoading.value = true
        viewModelScope.launch {
            try {
                val localFiles = files.value
                val vaultCount = vaultFiles.value.size
                val isCloud = _isCloudConnected.value
                val contextPrompt = "You are VVF Smart Manager AI Co-Pilot. Local files: $localFiles. Vault count: $vaultCount. Cloud status connected: $isCloud. User asks: $query"
                _assistantResponse.value = geminiService.analyzeMedia(contextPrompt)
            } catch (e: Exception) {
                _assistantResponse.value = "Error contacting AI Assistant: ${e.localizedMessage ?: e.message}"
            } finally {
                _isAssistantLoading.value = false
            }
        }
    }

    // Background WorkManager state
    val scanWorkState: StateFlow<MediaScanState> = backgroundManager.getScanWorkStateFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaScanState()
    )

    val cloudSyncWorkState: StateFlow<MediaScanState> = backgroundManager.getSyncWorkStateFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaScanState()
    )


    fun selectDetailFile(file: MediaFile?) {
        _selectedDetailFile.value = file
    }

    fun triggerMediaScan() {
        backgroundManager.triggerManualMediaScan()
    }

    /**
     * Phase 6: Secure Vault - Startup Authentication
     * Uses biometric authentication to protect the entire media manager.
     */
    fun authenticateOnStartup(activity: androidx.fragment.app.FragmentActivity, onResult: (Boolean) -> Unit) {
        if (!biometricAuthManager.isBiometricAvailable()) {
            // Skip if no biometrics or device credentials (PIN/Pattern) enrolled
            onResult(true)
            return
        }
        biometricAuthManager.promptBiometricAuth(
            activity = activity,
            title = "VVF Smart Manager Secure Login",
            subtitle = "Authenticate to access your private media",
            onSuccess = {
                onResult(true)
            },
            onError = { _ ->
                onResult(false)
            }
        )
    }

    // ----------------------------------------------------
    // UI State variables
    // ----------------------------------------------------
    private val _selectedDetailFile = MutableStateFlow<MediaFile?>(null)
    val selectedDetailFile: StateFlow<MediaFile?> = _selectedDetailFile.asStateFlow()
    private val _currentTab = MutableStateFlow(0) // 0=Dashboard, 1=Files, 2=Vault, 3=AI Search/OCR, 4=AI Duplicates, 5=Cloud, 6=Plugins
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MediaFile>>(emptyList())
    val searchResults: StateFlow<List<MediaFile>> = _searchResults.asStateFlow()

    // Plugins State
    private val _plugins = MutableStateFlow(
        mapOf(
            "ocr" to true,
            "semantic" to true,
            "onedrive" to false,
            "dropbox" to false,
            "nextcloud" to false
        )
    )
    val plugins: StateFlow<Map<String, Boolean>> = _plugins.asStateFlow()

    // ----------------------------------------------------
    // Files State Management (Optimized with stateIn)
    // ----------------------------------------------------
    private val allFilesFlow = mediaFileRepository.getAllFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val files: StateFlow<List<MediaFile>> = allFilesFlow.map { list ->
        list.filter { !it.isEncrypted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultFiles: StateFlow<List<MediaFile>> = allFilesFlow.map { list ->
        list.filter { it.isEncrypted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // OCR Screen State
    private val _selectedOcrFile = MutableStateFlow<MediaFile?>(null)
    val selectedOcrFile: StateFlow<MediaFile?> = _selectedOcrFile.asStateFlow()

    private val _extractedOcrText = MutableStateFlow("")
    val extractedOcrText: StateFlow<String> = _extractedOcrText.asStateFlow()

    private val _suggestedTags = MutableStateFlow("")
    val suggestedTags: StateFlow<String> = _suggestedTags.asStateFlow()

    private val _suggestedCategory = MutableStateFlow("")
    val suggestedCategory: StateFlow<String> = _suggestedCategory.asStateFlow()

    private val _isOcrLoading = MutableStateFlow(false)
    val isOcrLoading: StateFlow<Boolean> = _isOcrLoading.asStateFlow()

    // Similarity Slider State (0.70f to 0.95f)
    private val _similarityThreshold = MutableStateFlow(0.85f)
    val similarityThreshold: StateFlow<Float> = _similarityThreshold.asStateFlow()

    private val _semanticDuplicates = MutableStateFlow<List<Pair<MediaFile, MediaFile>>>(emptyList())
    val semanticDuplicates: StateFlow<List<Pair<MediaFile, MediaFile>>> = _semanticDuplicates.asStateFlow()

    private val _visualDuplicates = MutableStateFlow<Map<String, List<MediaFile>>>(emptyMap())
    val visualDuplicates: StateFlow<Map<String, List<MediaFile>>> = _visualDuplicates.asStateFlow()

    // Vault State
    private val _isVaultLocked = MutableStateFlow(true)
    val isVaultLocked: StateFlow<Boolean> = _isVaultLocked.asStateFlow()
    val lockoutRemainingSeconds: StateFlow<Long> = vaultSessionManager.lockoutRemainingSeconds
    private val _vaultTimeoutMinutes = MutableStateFlow(2)
    val vaultTimeoutMinutes: StateFlow<Int> = _vaultTimeoutMinutes.asStateFlow()

    private val _vaultPinInput = MutableStateFlow("")
    val vaultPinInput: StateFlow<String> = _vaultPinInput.asStateFlow()

    private val _vaultErrorMessage = MutableStateFlow("")
    val vaultErrorMessage: StateFlow<String> = _vaultErrorMessage.asStateFlow()

    // Cloud State
    private val _cloudSyncing = MutableStateFlow(false)
    val cloudSyncing: StateFlow<Boolean> = _cloudSyncing.asStateFlow()

    private val _cloudQueue = MutableStateFlow<List<MediaFile>>(emptyList())
    val cloudQueue: StateFlow<List<MediaFile>> = _cloudQueue.asStateFlow()

    private val _cloudLogs = MutableStateFlow<List<String>>(listOf("Cloud Client initialized.", "Offline Queue empty."))
    val cloudLogs: StateFlow<List<String>> = _cloudLogs.asStateFlow()

    private val _isCloudConnected = MutableStateFlow(false)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    init {
        // Set default secure PIN if not present
        if (!vaultSessionManager.hasPin()) {
            vaultSessionManager.setPin("1234")
        }

        // Trigger background media scan on first startup
        viewModelScope.launch {
            val initialFiles = mediaFileRepository.getAllFiles().first()
            if (initialFiles.isEmpty()) {
                backgroundManager.triggerManualMediaScan()
            }
        }

        // Debounced Semantic & Visual Duplicate Calculation
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            files.debounce(500L).collect {
                updateSemanticDuplicates()
                updateVisualDuplicates()
            }
        }

        // Setup reactive debounced search flow pipeline
        viewModelScope.launch {
            @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
            _searchQuery
                .debounce(200L)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        flowOf(emptyList())
                    } else {
                        coreSearchEngine.searchFiles(query)
                    }
                }
                .collect { results ->
                    _searchResults.value = results
                }
        }
    }

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    // ----------------------------------------------------
    // File Manager Operations
    // ----------------------------------------------------
    fun addFile(name: String, type: String, sizeKb: Long, customOcr: String = "", customTags: String = "") {
        viewModelScope.launch {
            val extension = if (type == "Image") ".jpg" else if (type == "Video") ".mp4" else ".pdf"
            val mime = if (type == "Image") "image/jpeg" else if (type == "Video") "video/mp4" else "application/pdf"
            val file = MediaFile(
                name = name + extension,
                path = "/sdcard/VVFManager/" + name + extension,
                type = type,
                mimeType = mime,
                size = sizeKb * 1024,
                tags = customTags,
                ocrText = customOcr
            )
            mediaFileRepository.insertFile(file)
            addCloudLog("Created local file: ${file.name}")
        }
    }

    fun deleteFile(fileId: Int, fileName: String) {
        viewModelScope.launch {
            mediaFileRepository.deleteFileById(fileId)
            addCloudLog("Deleted file: $fileName")
        }
    }

    fun renameFile(file: MediaFile, newName: String) {
        viewModelScope.launch {
            val cleanName = if (newName.contains(".")) newName else newName + file.name.substring(file.name.lastIndexOf('.'))
            val updated = file.copy(
                name = cleanName,
                path = file.path.substringBeforeLast('/') + "/" + cleanName,
                modifiedAt = System.currentTimeMillis()
            )
            mediaFileRepository.updateFile(updated)
            addCloudLog("Renamed file to: $cleanName")
        }
    }

    // ----------------------------------------------------
    // Secure Vault Operations
    // ----------------------------------------------------
    fun setPinInput(pin: String) {
        _vaultPinInput.value = pin
        _vaultErrorMessage.value = ""
    }

    fun unlockVault() {
        if (vaultSessionManager.verifyPin(_vaultPinInput.value)) {
            _isVaultLocked.value = false
            _vaultPinInput.value = ""
            _vaultErrorMessage.value = ""
            addCloudLog("Secure Vault unlocked successfully.")
        } else {
            _vaultErrorMessage.value = "Incorrect PIN. Default is '1234'."
        }
    }

    fun unlockWithBiometrics() {
        vaultSessionManager.unlockVault()
        _isVaultLocked.value = false
        _vaultErrorMessage.value = ""
        addCloudLog("Secure Vault unlocked via biometric credentials.")
    }

    fun lockVault() {
        vaultSessionManager.lockVault()
        _isVaultLocked.value = true
        addCloudLog("Secure Vault locked.")
    }

    fun toggleFileEncryption(file: MediaFile) {
        viewModelScope.launch {
            try {
                val originalFile = java.io.File(file.path)
                
                if (!file.isEncrypted) {
                    val vaultDir = java.io.File(applicationContext.filesDir, "vault")
                    if (!vaultDir.exists()) vaultDir.mkdirs()
                    
                    val encryptedFile = java.io.File(vaultDir, "enc_${file.id}_${originalFile.name}")
                    
                    vaultEncryptionManager.encryptFile(originalFile, encryptedFile, file.id.toString())
                    
                    if (originalFile.exists()) {
                        originalFile.delete() // Simple delete, shredding could be implemented later
                    }
                    
                    val updated = file.copy(
                        isEncrypted = true,
                        path = encryptedFile.absolutePath,
                        modifiedAt = System.currentTimeMillis()
                    )
                    mediaFileRepository.updateFile(updated)
                    addCloudLog("File ${file.name} encrypted & moved to Vault.")
                } else {
                    val decryptedDir = java.io.File(applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir, "VVFManager")
                    if (!decryptedDir.exists()) decryptedDir.mkdirs()
                    
                    val decryptedFile = java.io.File(decryptedDir, "dec_${file.id}_${originalFile.name.removePrefix("enc_${file.id}_")}")
                    
                    vaultEncryptionManager.decryptFile(originalFile, decryptedFile, file.id.toString())
                    
                    if (originalFile.exists()) {
                        originalFile.delete()
                    }
                    
                    val updated = file.copy(
                        isEncrypted = false,
                        path = decryptedFile.absolutePath,
                        modifiedAt = System.currentTimeMillis()
                    )
                    mediaFileRepository.updateFile(updated)
                    addCloudLog("File ${file.name} decrypted & moved to Storage.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                addCloudLog("Encryption/Decryption failed for ${file.name}: ${e.message}")
            }
        }
    }

    // ----------------------------------------------------
    // AI Search and OCR Engine Operations
    // ----------------------------------------------------
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectOcrFile(file: MediaFile?) {
        _selectedOcrFile.value = file
        _extractedOcrText.value = ""
        _suggestedTags.value = ""
        _suggestedCategory.value = ""
    }

    fun runOcrOnSelectedFile() {
        val file = _selectedOcrFile.value ?: return
        _isOcrLoading.value = true
        viewModelScope.launch {
            try {
                val ioFile = java.io.File(file.path)
                if (ioFile.exists() && ioFile.canRead()) {
                    val result = ocrEngine.extractTextFromImage(android.net.Uri.fromFile(ioFile))
                    if (result.isSuccess) {
                        val text = result.getOrNull() ?: ""
                        _extractedOcrText.value = text
                        
                        try {
                            val tags = geminiService.generateAutoTags(file.name, file.type, file.mimeType, text)
                            if (tags.isNotBlank()) {
                                _suggestedTags.value = tags
                                _suggestedCategory.value = "AI Tagged"
                                
                                val updated = file.copy(
                                    ocrText = text,
                                    tags = if (file.tags.isEmpty()) tags else "${file.tags}, $tags"
                                )
                                mediaFileRepository.updateFile(updated)
                            }
                        } catch (e: Exception) {}
                    } else {
                        _extractedOcrText.value = "OCR Failed: ${result.exceptionOrNull()?.message}"
                    }
                }
            } catch(e: Exception) {
            } finally {
                _isOcrLoading.value = false
            }
        }
    }

    private fun updateSemanticDuplicates() {
        viewModelScope.launch {
            val allFiles = mediaFileRepository.getAllFiles().first()
            val dups = deepDuplicateCleaner.findSemanticDuplicates(allFiles, _similarityThreshold.value)
            _semanticDuplicates.value = dups
        }
    }

    private fun updateVisualDuplicates() {
        viewModelScope.launch {
            val allFiles = mediaFileRepository.getAllFiles().first()
            val dups = deepDuplicateCleaner.findVisualDuplicates(allFiles)
            _visualDuplicates.value = dups
        }
    }

    // ----------------------------------------------------
    // AI Duplicates Operations
    // ----------------------------------------------------
    fun updateSimilarityThreshold(threshold: Float) {
        _similarityThreshold.value = threshold
    }

    // ----------------------------------------------------
    // Cloud Operations
    // ----------------------------------------------------
    fun addCloudLog(log: String) {
        _cloudLogs.value = listOf(log) + _cloudLogs.value.take(49)
    }

    fun connectToGoogleDrive() {
        viewModelScope.launch {
            if (!_isCloudConnected.value) {
                addCloudLog("Connecting to Google Drive...")
                driveProvider.authenticate()
                if (driveProvider.isAuthenticated()) {
                    _isCloudConnected.value = true
                    addCloudLog("Connected to Google Drive successfully.")
                } else {
                    addCloudLog("Failed to connect to Google Drive.")
                }
            } else {
                _isCloudConnected.value = false
                addCloudLog("Disconnected from Google Drive.")
            }
        }
    }

    fun toggleCloudProvider(provider: String, isEnabled: Boolean) {
        val current = _plugins.value.toMutableMap()
        current[provider] = isEnabled
        _plugins.value = current
    }

    fun addToCloudQueue(file: MediaFile) {
        val current = _cloudQueue.value.toMutableList()
        if (current.none { it.id == file.id }) {
            current.add(file)
            _cloudQueue.value = current
            addCloudLog("Added ${file.name} to upload queue.")
        }
    }

    fun syncCloudQueue() {
        viewModelScope.launch {
            if (_cloudQueue.value.isEmpty() || !_isCloudConnected.value) return@launch
            _cloudSyncing.value = true
            addCloudLog("Starting sync for ${_cloudQueue.value.size} files...")
            
            val result = driveProvider.syncChanges()
            if (result.isSuccess) {
                _cloudQueue.value = emptyList()
                addCloudLog("Sync completed successfully.")
            } else {
                addCloudLog("Sync failed: ${result.exceptionOrNull()?.message}")
            }
            _cloudSyncing.value = false
        }
    }

    fun togglePlugin(pluginId: String, isEnabled: Boolean) {
        val current = _plugins.value.toMutableMap()
        current[pluginId] = isEnabled
        _plugins.value = current
    }
    fun triggerGitHubSync() {
        viewModelScope.launch {
            addCloudLog("Triggering WorkManager Drive Sync...")
            backgroundManager.triggerImmediateCloudSync()
        }
    }
}
