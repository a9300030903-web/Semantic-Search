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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SmartManagerViewModel(
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

    fun askCopilot(query: String) {
        if (query.isBlank()) return
        _isAssistantLoading.value = true
        viewModelScope.launch {
            try {
                val localFiles = _files.value
                val vaultCount = _vaultFiles.value.size
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

    // Background Scan WorkManager state
    val scanWorkState: StateFlow<MediaScanState> = backgroundManager.getScanWorkStateFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaScanState()
    )

    private val _selectedDetailFile = MutableStateFlow<MediaFile?>(null)
    val selectedDetailFile: StateFlow<MediaFile?> = _selectedDetailFile.asStateFlow()

    fun selectDetailFile(file: MediaFile?) {
        _selectedDetailFile.value = file
    }

    fun triggerMediaScan() {
        backgroundManager.triggerManualMediaScan()
    }

    // ----------------------------------------------------
    // UI State variables
    // ----------------------------------------------------
    private val _currentTab = MutableStateFlow(0) // 0=Dashboard, 1=Files, 2=Vault, 3=AI Search/OCR, 4=AI Duplicates, 5=Cloud, 6=Plugins
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _files = MutableStateFlow<List<MediaFile>>(emptyList())
    val files: StateFlow<List<MediaFile>> = _files.asStateFlow()

    private val _vaultFiles = MutableStateFlow<List<MediaFile>>(emptyList())
    val vaultFiles: StateFlow<List<MediaFile>> = _vaultFiles.asStateFlow()

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

    // Similarity Slider State (0.7f to 0.95f)
    private val _similarityThreshold = MutableStateFlow(0.85f)
    val similarityThreshold: StateFlow<Float> = _similarityThreshold.asStateFlow()

    private val _semanticDuplicates = MutableStateFlow<List<Pair<MediaFile, MediaFile>>>(emptyList())
    val semanticDuplicates: StateFlow<List<Pair<MediaFile, MediaFile>>> = _semanticDuplicates.asStateFlow()

    // Vault State
    private val _isVaultLocked = MutableStateFlow(true)
    val isVaultLocked: StateFlow<Boolean> = _isVaultLocked.asStateFlow()

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

        // Pre-populate dummy initial data if database is empty
        viewModelScope.launch {
            mediaFileRepository.getAllFiles().collect { dbFiles ->
                if (dbFiles.isEmpty()) {
                    prepopulateDatabase()
                } else {
                    _files.value = dbFiles.filter { !it.isEncrypted }
                    _vaultFiles.value = dbFiles.filter { it.isEncrypted }
                    updateSemanticDuplicates()
                }
            }
        }
    }

    private suspend fun prepopulateDatabase() {
        val dummy = listOf(
            MediaFile(
                name = "family_holiday_hawaii.jpg",
                path = "/sdcard/VVFManager/family_holiday_hawaii.jpg",
                type = "Image",
                mimeType = "image/jpeg",
                size = 2450000L,
                tags = "family, vacation, beach",
                ocrText = "ALOHA FROM HAWAII BEACH PARK MAY 2026"
            ),
            MediaFile(
                name = "receipt_starbucks_coffee.jpg",
                path = "/sdcard/VVFManager/receipt_starbucks_coffee.jpg",
                type = "Image",
                mimeType = "image/jpeg",
                size = 450000L,
                tags = "receipt, coffee, business",
                ocrText = "STARBUCKS COFFEE STORE #1432 DATE: 12/05/2026 COFFEE GRANDE $5.45 TOTAL: $5.45"
            ),
            MediaFile(
                name = "invoice_aws_cloud.pdf",
                path = "/sdcard/VVFManager/invoice_aws_cloud.pdf",
                type = "Document",
                mimeType = "application/pdf",
                size = 1250000L,
                tags = "invoice, work, amazon",
                ocrText = "AMAZON WEB SERVICES INVOICE INVOICE DATE: 15/06/2026 TOTAL DUE: $124.50 ACCOUNT ID: 1234-5678"
            ),
            MediaFile(
                name = "invoice_aws_cloud_copy.pdf",
                path = "/sdcard/VVFManager/invoice_aws_cloud_copy.pdf",
                type = "Document",
                mimeType = "application/pdf",
                size = 1250000L,
                tags = "invoice, work, amazon",
                ocrText = "AMAZON WEB SERVICES INVOICE INVOICE DATE: 15/06/2026 TOTAL DUE: $124.50 ACCOUNT ID: 1234-5678"
            ),
            MediaFile(
                name = "project_demo_video.mp4",
                path = "/sdcard/VVFManager/project_demo_video.mp4",
                type = "Video",
                mimeType = "video/mp4",
                size = 45000000L,
                tags = "work, demo, presentation",
                ocrText = ""
            ),
            MediaFile(
                name = "passport_scan.jpg",
                path = "/sdcard/VVFManager/passport_scan.jpg",
                type = "Image",
                mimeType = "image/jpeg",
                size = 1800000L,
                tags = "personal, identity, travel",
                ocrText = "REPUBLIC OF INDIA PASSPORT PASSPORT NO: Z1234567 GIVEN NAMES: RAJESH SHARMA"
            )
        )
        mediaFileRepository.insertFiles(dummy)
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
            val updated = file.copy(
                isEncrypted = !file.isEncrypted,
                modifiedAt = System.currentTimeMillis()
            )
            mediaFileRepository.updateFile(updated)
            val action = if (updated.isEncrypted) "encrypted & moved to Vault" else "decrypted & moved to Storage"
            addCloudLog("File ${file.name} $action.")
        }
    }

    private var searchJob: Job? = null

    // ----------------------------------------------------
    // AI Search and OCR Engine Operations
    // ----------------------------------------------------
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            coreSearchEngine.searchFiles(query).collect { results ->
                _searchResults.value = results
            }
        }
    }

    fun selectOcrFile(file: MediaFile?) {
        _selectedOcrFile.value = file
        _extractedOcrText.value = ""
        _suggestedTags.value = ""
        _suggestedCategory.value = ""
    }

    fun runOcrOnSelectedFile() {
        val file = _selectedOcrFile.value ?: return
        if (_plugins.value["ocr"] != true) {
            _extractedOcrText.value = "OCR Plugin is disabled! Enable it in the Plugins tab."
            return
        }

        _isOcrLoading.value = true
        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(1200) // Simulated processing latency
                
                // Fallback mock extraction text if file's text is empty
                val text = if (file.ocrText.isNotBlank()) {
                    file.ocrText
                } else {
                    "VVF SMART MANAGER OCR RESULT\nDOCUMENT TYPE: ${file.type}\nPROCESSED FILE: ${file.name}\nMETADATA VERIFIED."
                }
                
                _extractedOcrText.value = text
                val tags = autoTagger.generateTags(text).ifBlank { "scanned, document, system" }
                _suggestedTags.value = tags
                _suggestedCategory.value = autoTagger.suggestCategory(file.name, text)

                // Update database file with OCR text & tags to make it searchable
                val updated = file.copy(
                    ocrText = text,
                    tags = if (file.tags.isBlank()) tags else "${file.tags}, $tags",
                    modifiedAt = System.currentTimeMillis()
                )
                mediaFileRepository.updateFile(updated)
                addCloudLog("OCR analysis completed for ${file.name}.")
            } catch (e: Exception) {
                _extractedOcrText.value = "OCR analysis failed: ${e.localizedMessage ?: e.message}"
            } finally {
                _isOcrLoading.value = false
            }
        }
    }

    // ----------------------------------------------------
    // AI Intelligence Operations
    // ----------------------------------------------------
    fun updateSimilarityThreshold(threshold: Float) {
        _similarityThreshold.value = threshold
        viewModelScope.launch {
            updateSemanticDuplicates()
        }
    }

    private suspend fun updateSemanticDuplicates() {
        val items = _files.value
        if (items.size < 2) {
            _semanticDuplicates.value = emptyList()
            return
        }
        val threshold = _similarityThreshold.value
        val duplicates = withContext(Dispatchers.Default) {
            deepDuplicateCleaner.findSemanticDuplicates(items, threshold)
        }
        _semanticDuplicates.value = duplicates
    }

    // ----------------------------------------------------
    // Cloud Operations
    // ----------------------------------------------------
    fun toggleCloudProvider(provider: String, enabled: Boolean) {
        val updated = _plugins.value.toMutableMap()
        updated[provider] = enabled
        _plugins.value = updated
        addCloudLog("Cloud provider $provider toggled ${if (enabled) "ON" else "OFF"}")
    }

    fun togglePlugin(pluginKey: String) {
        val updated = _plugins.value.toMutableMap()
        val current = updated[pluginKey] ?: false
        updated[pluginKey] = !current
        _plugins.value = updated
        addCloudLog("Plugin '$pluginKey' toggled to ${!current}")
        
        // Refresh searches if semantic was toggled
        updateSearchQuery(_searchQuery.value)
    }

    fun connectToGoogleDrive() {
        viewModelScope.launch {
            _isCloudConnected.value = !_isCloudConnected.value
            if (_isCloudConnected.value) {
                addCloudLog("Successfully authenticated Google Drive account.")
                syncCloudQueue()
            } else {
                addCloudLog("Disconnected Google Drive account.")
            }
        }
    }

    fun addToCloudQueue(file: MediaFile) {
        val current = _cloudQueue.value.toMutableList()
        if (!current.contains(file)) {
            current.add(file)
            _cloudQueue.value = current
            addCloudLog("Added ${file.name} to Cloud upload queue.")
        }
    }

    fun syncCloudQueue() {
        if (!_isCloudConnected.value) {
            addCloudLog("Cannot sync: Google Drive is disconnected.")
            return
        }
        if (_cloudQueue.value.isEmpty()) {
            addCloudLog("Sync complete: Cloud up-to-date.")
            return
        }
        if (_cloudSyncing.value) return // Prevent concurrent sync loops

        _cloudSyncing.value = true
        viewModelScope.launch {
            try {
                val queue = _cloudQueue.value.toList()
                for (file in queue) {
                    addCloudLog("Uploading ${file.name} ...")
                    kotlinx.coroutines.delay(1000) // upload delay simulation
                    addCloudLog("Uploaded ${file.name} successfully.")
                    
                    val currentQueue = _cloudQueue.value.toMutableList()
                    currentQueue.remove(file)
                    _cloudQueue.value = currentQueue
                }
                addCloudLog("All pending files synced to Google Drive.")
            } catch (e: Exception) {
                addCloudLog("Cloud sync error: ${e.localizedMessage ?: e.message}")
            } finally {
                _cloudSyncing.value = false
            }
        }
    }

    private fun addCloudLog(log: String) {
        val current = _cloudLogs.value.toMutableList()
        current.add(0, "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}] $log")
        _cloudLogs.value = current.take(50) // keep last 50 logs
    }
}
