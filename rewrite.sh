#!/bin/bash
cat << 'INNER_EOF' > /app/applet/app/src/main/java/com/example/MainActivity.kt
package com.example

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.SettingsManager
import com.example.overlay.OverlayService
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            startService(Intent(this, OverlayService::class.java))
        }
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var canWriteSettings by remember { mutableStateOf(Settings.System.canWrite(context)) }
    
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    var isDNDGranted by remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPermission = Settings.canDrawOverlays(context)
                canDrawOverlays = hasPermission
                canWriteSettings = Settings.System.canWrite(context)
                isDNDGranted = notificationManager.isNotificationPolicyAccessGranted
                
                if (hasPermission) {
                    context.startService(Intent(context, OverlayService::class.java))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { AppRepository(database.shortcutDao(), database.clipboardDao()) }
    
    var isServiceRunning by remember { mutableStateOf(true) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }
    
    LaunchedEffect(Unit) {
        if (repository.getShortcut(0) == null) {
            repository.saveShortcut(0, "UTILITY", "QR")
            repository.saveShortcut(1, "UTILITY", "Clipboard")
            repository.saveShortcut(2, "UTILITY", "Caffeine")
            repository.saveShortcut(3, "UTILITY", "Timer")
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Permissions", "HUD Config", "Shortcuts")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Flux Settings", 
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 24.dp,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, style = MaterialTheme.typography.bodyMedium) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            when (selectedTab) {
                0 -> PermissionsSection(
                        canDrawOverlays = canDrawOverlays,
                        canWriteSettings = canWriteSettings,
                        isDNDGranted = isDNDGranted,
                        context = context,
                        isServiceRunning = isServiceRunning,
                        onServiceToggle = { isServiceRunning = it },
                        permissionLauncher = permissionLauncher
                    )
                1 -> HudConfigSection(settingsManager)
                2 -> ShortcutsConfigSection(repository)
            }
        }
    }
}

@Composable
fun PermissionsSection(
    canDrawOverlays: Boolean,
    canWriteSettings: Boolean,
    isDNDGranted: Boolean,
    context: Context,
    isServiceRunning: Boolean,
    onServiceToggle: (Boolean) -> Unit,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    if (canDrawOverlays) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable HUD", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isServiceRunning,
                onCheckedChange = { isChecked ->
                    onServiceToggle(isChecked)
                    if (isChecked) {
                        context.startService(Intent(context, OverlayService::class.java))
                    } else {
                        context.stopService(Intent(context, OverlayService::class.java))
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (!canDrawOverlays) {
        PermissionCard(
            title = "Overlay Permission Missing",
            description = "Required to display the HUD.",
            buttonText = "Grant Overlay",
            onClick = {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                context.startActivity(intent)
            },
            isError = true
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (!canWriteSettings) {
        PermissionCard(
            title = "Write Settings Permission",
            description = "Required to adjust screen brightness.",
            buttonText = "Grant Write Settings",
            onClick = {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                context.startActivity(intent)
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (!isDNDGranted) {
        PermissionCard(
            title = "Do Not Disturb Access",
            description = "Required to toggle DND mode.",
            buttonText = "Grant DND Access",
            onClick = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                context.startActivity(intent)
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    PermissionCard(
        title = "Accessibility Service",
        description = "Enable Flux Accessibility to take instant screenshots.",
        buttonText = "Open Accessibility Settings",
        onClick = {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }
    )
    Spacer(modifier = Modifier.height(24.dp))

    PermissionCard(
        title = "Standard Permissions",
        description = "Required for Flashlight and Bluetooth.",
        buttonText = "Request Runtime Permissions",
        onClick = {
            val perms = mutableListOf(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            permissionLauncher.launch(perms.toTypedArray())
        }
    )
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
    isError: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp), 
            horizontalAlignment = Alignment.Start
        ) {
            val titleColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = com.example.ui.theme.MutedGray)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun HudConfigSection(settingsManager: com.example.data.SettingsManager) {
    val ringOpacity by settingsManager.ringOpacity.collectAsState()
    val labelStyle by settingsManager.labelStyle.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.Start) {
                Text("Ring Opacity", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("0.1", style = MaterialTheme.typography.bodySmall, color = com.example.ui.theme.MutedGray)
                    Slider(
                        value = ringOpacity,
                        onValueChange = { settingsManager.setRingOpacity(it) },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                    )
                    Text("1.0", style = MaterialTheme.typography.bodySmall, color = com.example.ui.theme.MutedGray)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.Start) {
                Text("Label Style", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(24.dp))
                
                val options = listOf("ICONS_ONLY" to "Icons Only", "WORDS_ONLY" to "Words Only", "ICONS_AND_WORDS" to "Icons & Words")
                options.forEach { (key, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsManager.setLabelStyle(key) }
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = labelStyle == key,
                            onClick = { settingsManager.setLabelStyle(key) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    }
}
INNER_EOF
