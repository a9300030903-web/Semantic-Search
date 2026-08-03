package com.example.plugin.api

import android.content.Context

/**
 * Phase 14: Master Plugin System
 * The base interface for ALL dynamically loadable modules (OCR, Semantic AI, FTP, ZIP, Cloud).
 */
interface SmartManagerPlugin {
    /**
     * Unique identifier for the plugin (e.g., "com.vvf.plugin.ocr.mlkit")
     */
    val pluginId: String
    
    /**
     * Human-readable name.
     */
    val name: String
    
    /**
     * Version of the plugin to handle migrations/compatibility.
     */
    val version: String
    
    /**
     * Called when the plugin is first loaded by the Core application.
     */
    fun initialize(context: Context): Boolean
    
    /**
     * Lifecycle method when the plugin is being disabled or unloaded.
     */
    fun shutdown()
}
