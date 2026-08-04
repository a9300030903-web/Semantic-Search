#!/bin/bash
awk '
/fun runOcrOnSelectedFile/ {
    in_func=1
    print "    fun runOcrOnSelectedFile() {"
    print "        val file = _selectedOcrFile.value ?: return"
    print "        _isOcrLoading.value = true"
    print "        viewModelScope.launch {"
    print "            try {"
    print "                val ioFile = java.io.File(file.path)"
    print "                if (ioFile.exists() && ioFile.canRead()) {"
    print "                    val result = ocrEngine.extractTextFromImage(android.net.Uri.fromFile(ioFile))"
    print "                    if (result.isSuccess) {"
    print "                        val text = result.getOrNull() ?: \"\""
    print "                        _extractedOcrText.value = text"
    print "                        "
    print "                        try {"
    print "                            val tags = geminiService.generateAutoTags(file.name, file.type, file.mimeType, text)"
    print "                            if (tags.isNotBlank()) {"
    print "                                _suggestedTags.value = tags"
    print "                                _suggestedCategory.value = \"AI Tagged\""
    print "                                "
    print "                                val updated = file.copy("
    print "                                    ocrText = text,"
    print "                                    tags = if (file.tags.isEmpty()) tags else \"${file.tags}, $tags\""
    print "                                )"
    print "                                mediaFileRepository.updateFile(updated)"
    print "                            }"
    print "                        } catch (e: Exception) {}"
    print "                    } else {"
    print "                        _extractedOcrText.value = \"OCR Failed: ${result.exceptionOrNull()?.message}\""
    print "                    }"
    print "                }"
    print "            } catch(e: Exception) {"
    print "            } finally {"
    print "                _isOcrLoading.value = false"
    print "            }"
    print "        }"
    print "    }"
    next
}
in_func {
    if (/^\s*fun / || /^\s*\/\//) {
        if (!/fun runOcrOnSelectedFile/) {
            in_func=0
            print $0
        }
    }
    next
}
{ print }
' app/src/main/java/com/example/feature/SmartManagerViewModel.kt > app/src/main/java/com/example/feature/SmartManagerViewModel.kt.new
mv app/src/main/java/com/example/feature/SmartManagerViewModel.kt.new app/src/main/java/com/example/feature/SmartManagerViewModel.kt
