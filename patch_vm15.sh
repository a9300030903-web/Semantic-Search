#!/bin/bash
sed -i 's/class SmartManagerViewModel(/class SmartManagerViewModel(\n    private val applicationContext: android.content.Context,/g' app/src/main/java/com/example/feature/SmartManagerViewModel.kt
sed -i 's/SmartManagerViewModel(/SmartManagerViewModel(\n            applicationContext = androidContext(),/g' app/src/main/java/com/example/core/di/DatabaseModule.kt
