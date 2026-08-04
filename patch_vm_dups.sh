#!/bin/bash
sed -i -e '/fun updateSemanticDuplicates(/,+2d' app/src/main/java/com/example/feature/SmartManagerViewModel.kt
sed -i -e '/fun updateVisualDuplicates(/,+2d' app/src/main/java/com/example/feature/SmartManagerViewModel.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/feature/SmartManagerViewModel.kt

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
}
INNER_EOF
