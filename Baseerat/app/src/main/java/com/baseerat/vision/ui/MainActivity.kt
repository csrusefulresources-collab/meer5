package com.baseerat.vision.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baseerat.vision.BaseeratApp
import com.baseerat.vision.R
import com.baseerat.vision.service.OverlayService
import com.baseerat.vision.ui.theme.BaseeratTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle share intent
        val sharedUri = when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
            }
            else -> null
        }

        val actionFromOverlay = intent?.getStringExtra(EXTRA_ACTION)

        setContent {
            val app = application as BaseeratApp
            val language by app.settingsRepository.language.collectAsStateWithLifecycle(initialValue = "en")

            BaseeratTheme {
                MainScreen(
                    language = language,
                    initialSharedUri = sharedUri,
                    initialAction = actionFromOverlay
                )
            }
        }
    }

    companion object {
        const val EXTRA_ACTION = "extra_action"
        const val ACTION_DESCRIBE_SCREEN = "describe_screen"
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    language: String,
    initialSharedUri: Uri? = null,
    initialAction: String? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as BaseeratApp
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var overlayRunning by remember { mutableStateOf(false) }

    // Camera permission
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val intent = Intent(context, DescribeActivity::class.java).apply {
                putExtra(DescribeActivity.EXTRA_IMAGE_URI, it.toString())
            }
            context.startActivity(intent)
        }
    }

    // Handle initial shared image
    LaunchedEffect(initialSharedUri) {
        initialSharedUri?.let {
            val intent = Intent(context, DescribeActivity::class.java).apply {
                putExtra(DescribeActivity.EXTRA_IMAGE_URI, it.toString())
            }
            context.startActivity(intent)
        }
    }

    // Handle overlay action
    LaunchedEffect(initialAction) {
        if (initialAction == MainActivity.ACTION_DESCRIBE_SCREEN) {
            // For now open describe with a note; real screen capture needs MediaProjection
            Toast.makeText(
                context,
                if (language == "ur") "سکرین بیان کی خصوصیت جلد آ رہی ہے۔ فی الحال کیمرا یا گیلری استعمال کریں۔"
                else "Screen describe is coming soon. Please use Camera or Gallery for now.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun openOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }

    fun openAccessibilitySettings() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun toggleOverlay() {
        if (!Settings.canDrawOverlays(context)) {
            openOverlayPermission()
            return
        }
        if (overlayRunning) {
            context.stopService(Intent(context, OverlayService::class.java))
            overlayRunning = false
        } else {
            ContextCompat.startForegroundService(
                context,
                Intent(context, OverlayService::class.java)
            )
            overlayRunning = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.home_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = stringResource(R.string.home_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    val helpCd = stringResource(R.string.help_title)
                    val settingsCd = stringResource(R.string.btn_settings)
                    IconButton(
                        onClick = { showHelp = true },
                        modifier = Modifier.semantics {
                            contentDescription = helpCd
                        }
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = helpCd)
                    }
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.semantics {
                            contentDescription = settingsCd
                        }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = settingsCd)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Primary actions – large, clear, accessible
            BigActionButton(
                text = stringResource(R.string.btn_camera),
                icon = Icons.Default.CameraAlt,
                contentDesc = stringResource(R.string.cd_camera_button),
                onClick = {
                    if (cameraPermission.status.isGranted) {
                        // For simplicity we reuse gallery flow with camera capture in a future iteration.
                        // Open a simple camera intent or gallery for now.
                        galleryLauncher.launch("image/*")
                    } else {
                        cameraPermission.launchPermissionRequest()
                    }
                }
            )

            BigActionButton(
                text = stringResource(R.string.btn_gallery),
                icon = Icons.Default.PhotoLibrary,
                contentDesc = stringResource(R.string.cd_gallery_button),
                onClick = { galleryLauncher.launch("image/*") }
            )

            BigActionButton(
                text = stringResource(R.string.btn_screen),
                icon = Icons.Default.Screenshot,
                contentDesc = stringResource(R.string.cd_screen_button),
                onClick = {
                    Toast.makeText(
                        context,
                        if (language == "ur") "سکرین کیپچر جلد دستیاب ہوگا۔ فی الحال فلوٹنگ بٹن یا گیلری استعمال کریں۔"
                        else "Screen capture coming soon. Use floating button or gallery for now.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            BigActionButton(
                text = if (overlayRunning)
                    stringResource(R.string.btn_hide_overlay)
                else
                    stringResource(R.string.btn_overlay),
                icon = if (overlayRunning) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDesc = stringResource(R.string.cd_overlay_button),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { toggleOverlay() }
            )

            // Accessibility service card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.enable_accessibility),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.accessibility_service_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { openAccessibilitySettings() }) {
                        Text(stringResource(R.string.enable_accessibility))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSettings) {
        SettingsSheet(
            onDismiss = { showSettings = false },
            language = language,
            onLanguageChange = { code ->
                scope.launch {
                    app.settingsRepository.setLanguage(code)
                    app.speechHelper.setLanguage(code)
                }
            }
        )
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(stringResource(R.string.help_title)) },
            text = { Text(stringResource(R.string.help_body)) },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun BigActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics { contentDescription = contentDesc },
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    onDismiss: () -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as BaseeratApp
    val scope = rememberCoroutineScope()

    val autoSpeak by app.settingsRepository.autoSpeak.collectAsStateWithLifecycle(true)
    val detailed by app.settingsRepository.detailedDefault.collectAsStateWithLifecycle(false)
    val vibrate by app.settingsRepository.vibrate.collectAsStateWithLifecycle(true)
    val offline by app.settingsRepository.offlineOnly.collectAsStateWithLifecycle(true)
    val rate by app.settingsRepository.speechRate.collectAsStateWithLifecycle(1.0f)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(Modifier.height(8.dp))

            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = language == "en",
                    onClick = { onLanguageChange("en") },
                    label = { Text(stringResource(R.string.settings_language_en)) }
                )
                FilterChip(
                    selected = language == "ur",
                    onClick = { onLanguageChange("ur") },
                    label = { Text(stringResource(R.string.settings_language_ur)) }
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SettingSwitch(
                title = stringResource(R.string.settings_auto_speak),
                checked = autoSpeak,
                onCheckedChange = { scope.launch { app.settingsRepository.setAutoSpeak(it) } }
            )
            SettingSwitch(
                title = stringResource(R.string.settings_detailed_default),
                checked = detailed,
                onCheckedChange = { scope.launch { app.settingsRepository.setDetailedDefault(it) } }
            )
            SettingSwitch(
                title = stringResource(R.string.settings_vibrate),
                checked = vibrate,
                onCheckedChange = { scope.launch { app.settingsRepository.setVibrate(it) } }
            )
            SettingSwitch(
                title = stringResource(R.string.settings_offline_only),
                checked = offline,
                onCheckedChange = { scope.launch { app.settingsRepository.setOfflineOnly(it) } }
            )

            Text(
                text = stringResource(R.string.settings_speech_rate) + " (${"%.1f".format(rate)}x)",
                style = MaterialTheme.typography.titleSmall
            )
            Slider(
                value = rate,
                onValueChange = {
                    scope.launch {
                        app.settingsRepository.setSpeechRate(it)
                        app.speechHelper.setSpeechRate(it)
                    }
                },
                valueRange = 0.5f..2.0f,
                steps = 5
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
