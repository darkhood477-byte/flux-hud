#!/bin/bash
sed -i 's/import androidx.lifecycle.ViewModelStoreOwner/import androidx.lifecycle.ViewModelStoreOwner\nimport androidx.lifecycle.lifecycleScope\nimport kotlinx.coroutines.flow.collectLatest\nimport kotlinx.coroutines.launch\nimport androidx.compose.runtime.collectAsState\nimport androidx.compose.runtime.getValue\n/g' /app/applet/app/src/main/java/com/example/overlay/OverlayService.kt

