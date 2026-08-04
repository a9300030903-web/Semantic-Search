#!/bin/bash
sed -i 's/ocrEngine.extractText(ioFile)/ocrEngine.extractTextFromImage(android.net.Uri.fromFile(ioFile))/g' app/src/main/java/com/example/feature/SmartManagerViewModel.kt

sed -i -e '/geminiService.generateTagsAndCategory(text)/c\                            val tagsResult = geminiService.generateAutoTags(file.name, file.type, file.mimeType, text)\n                            if (tagsResult.isNotBlank()) {\n                                val tags = tagsResult\n                                _suggestedTags.value = tags\n                                _suggestedCategory.value = "AI Tagged"\n                                \n                                val updated = file.copy(\n                                    ocrText = text,\n                                    tags = if (file.tags.isEmpty()) tags else "${file.tags}, $tags"\n                                )\n                                mediaFileRepository.updateFile(updated)\n                            }' app/src/main/java/com/example/feature/SmartManagerViewModel.kt

sed -i '/if (tagsResult.isSuccess)/,+14d' app/src/main/java/com/example/feature/SmartManagerViewModel.kt
