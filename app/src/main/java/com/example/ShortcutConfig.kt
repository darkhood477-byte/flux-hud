package com.example

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.ActionType
import com.example.data.AppRepository
import com.example.features.ShortcutExtractor
import kotlinx.coroutines.launch

@Composable
fun ShortcutsConfigSection(repository: AppRepository) {
    val shortcuts by repository.shortcuts.collectAsState(initial = emptyList())
    var selectedSlot by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.Start) {
                Text("HUD Slots", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(24.dp))
                
                (0 until 4).forEach { i ->
                    val shortcut = shortcuts.find { it.id == i }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSlot = i }
                            .padding(vertical = 12.dp)
                    ) {
                        if (shortcut == null || shortcut.type == ActionType.EMPTY) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Empty", tint = com.example.ui.theme.MutedGray)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Slot ${i + 1}: Unassigned", color = com.example.ui.theme.MutedGray)
                        } else {
                            Icon(Icons.Default.Build, contentDescription = "Assigned", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            val title = if (shortcut.type == ActionType.APP) {
                                val pm = LocalContext.current.packageManager
                                try {
                                    pm.getApplicationInfo(shortcut.target, 0).loadLabel(pm).toString()
                                } catch (e: Exception) {
                                    shortcut.target
                                }
                            } else shortcut.target
                            Text("Slot ${i + 1}: ${shortcut.type} - $title", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }

    if (selectedSlot != null) {
        ActionPickerBottomSheet(
            slotId = selectedSlot!!,
            currentShortcut = shortcuts.find { it.id == selectedSlot } ?: com.example.data.ShortcutEntity(selectedSlot!!, ActionType.EMPTY, ""),
            repository = repository,
            onDismiss = { selectedSlot = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPickerBottomSheet(
    slotId: Int,
    currentShortcut: com.example.data.ShortcutEntity,
    repository: AppRepository,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Apps & Shortcuts", "Quick Actions", "Utilities")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Text(
                "Configure Slot ${slotId + 1}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> AppsAndShortcutsTab(slotId, repository, onDismiss)
                1 -> QuickActionsTab(slotId, repository, onDismiss)
                2 -> UtilitiesTab(slotId, currentShortcut, repository, onDismiss)
            }
        }
    }
}

data class AppInfo(val name: String, val packageName: String, val icon: Drawable)

@Composable
fun AppsAndShortcutsTab(slotId: Int, repository: AppRepository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pm = context.packageManager

    val apps = remember {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, 0).map { resolveInfo ->
            val appName = resolveInfo.loadLabel(pm).toString()
            val packageName = resolveInfo.activityInfo.packageName
            val icon = resolveInfo.loadIcon(pm)
            AppInfo(appName, packageName, icon)
        }.sortedBy { it.name.lowercase() }
    }

    var searchQuery by remember { mutableStateOf("") }
    var expandedAppPackage by remember { mutableStateOf<String?>(null) }
    var appShortcuts by remember { mutableStateOf<List<ShortcutExtractor.ExtractedShortcut>>(emptyList()) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Apps") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        val filteredApps = apps.filter { it.name.contains(searchQuery, ignoreCase = true) }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredApps) { app ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (expandedAppPackage == app.packageName) {
                                    expandedAppPackage = null
                                } else {
                                    expandedAppPackage = app.packageName
                                    appShortcuts = ShortcutExtractor.getShortcutsForPackage(context, app.packageName)
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = app.icon.toBitmap().asImageBitmap(),
                            contentDescription = app.name,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(app.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        
                        if (expandedAppPackage == app.packageName) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Collapse")
                        } else {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Expand")
                        }
                    }
                    
                    if (expandedAppPackage == app.packageName) {
                        Column(modifier = Modifier.padding(start = 56.dp, bottom = 8.dp)) {
                            // Launch App
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            repository.saveShortcut(slotId, ActionType.APP, app.packageName)
                                            onDismiss()
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = "Launch", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Launch App", style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            // Shortcuts
                            appShortcuts.forEach { shortcut ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch {
                                                repository.saveShortcut(slotId, ActionType.SHORTCUT, "${app.packageName}||${shortcut.id}")
                                                onDismiss()
                                            }
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (shortcut.icon != null) {
                                        Image(
                                            bitmap = shortcut.icon.toBitmap().asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(Icons.Default.Link, contentDescription = "Shortcut", modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(shortcut.shortLabel?.toString() ?: "Shortcut", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsTab(slotId: Int, repository: AppRepository, onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    
    val commonActions = listOf(
        Pair("New Note", "note://new"), // Placeholder schema
        Pair("Compose Email", "mailto:"),
        Pair("Direct Share...", "share://prompt"),
        Pair("Search Web", "https://google.com")
    )
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        items(commonActions) { (label, uri) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        coroutineScope.launch {
                            val actionType = if (uri.startsWith("http")) ActionType.URL else ActionType.DEEP_LINK
                            repository.saveShortcut(slotId, actionType, uri)
                            onDismiss()
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun UtilitiesTab(slotId: Int, currentShortcut: com.example.data.ShortcutEntity, repository: AppRepository, onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var showAdvanced by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf(currentShortcut.target) }
    var selectedAdvancedType by remember { mutableStateOf(ActionType.URL) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        val utilities = listOf("QR", "Clipboard", "Caffeine", "Timer")
        
        Text("Built-in Utilities", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        utilities.forEach { util ->
            Row(modifier = Modifier.fillMaxWidth().clickable {
                coroutineScope.launch {
                    repository.saveShortcut(slotId, ActionType.UTILITY, util)
                    onDismiss()
                }
            }.padding(vertical = 12.dp)) {
                Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(util)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().clickable { showAdvanced = !showAdvanced },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Advanced Configuration", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.weight(1f))
            Icon(if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
        
        if (showAdvanced) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = actionTarget,
                onValueChange = { actionTarget = it },
                label = { Text("Target URI / String") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FilterChip(
                    selected = selectedAdvancedType == ActionType.URL,
                    onClick = { selectedAdvancedType = ActionType.URL },
                    label = { Text("URL") }
                )
                FilterChip(
                    selected = selectedAdvancedType == ActionType.DEEP_LINK,
                    onClick = { selectedAdvancedType = ActionType.DEEP_LINK },
                    label = { Text("Deep Link") }
                )
                FilterChip(
                    selected = selectedAdvancedType == ActionType.BROADCAST,
                    onClick = { selectedAdvancedType = ActionType.BROADCAST },
                    label = { Text("Broadcast") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        repository.saveShortcut(slotId, selectedAdvancedType, actionTarget)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Advanced")
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        OutlinedButton(
            onClick = {
                coroutineScope.launch {
                    repository.saveShortcut(slotId, ActionType.EMPTY, "")
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Clear Slot")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
