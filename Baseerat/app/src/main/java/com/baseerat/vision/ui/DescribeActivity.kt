package com.baseerat.vision.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.baseerat.vision.BaseeratApp
import com.baseerat.vision.R
import com.baseerat.vision.ui.theme.BaseeratTheme
import com.baseerat.vision.vision.VisionEngine
import kotlinx.coroutines.launch

class DescribeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        val imageUri = imageUriString?.let { Uri.parse(it) }

        setContent {
            val app = application as BaseeratApp
            val language by app.settingsRepository.language.collectAsStateWithLifecycle("en")

            BaseeratTheme {
                DescribeScreen(
                    imageUri = imageUri,
                    language = language,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescribeScreen(
    imageUri: Uri?,
    language: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as BaseeratApp
    val scope = rememberCoroutineScope()

    val autoSpeak by app.settingsRepository.autoSpeak.collectAsStateWithLifecycle(true)
    val detailedDefault by app.settingsRepository.detailedDefault.collectAsStateWithLifecycle(false)
    val vibrateEnabled by app.settingsRepository.vibrate.collectAsStateWithLifecycle(true)

    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<VisionEngine.Result?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var useDetailed by remember { mutableStateOf(detailedDefault) }
    var followUpText by remember { mutableStateOf("") }
    var showFollowUp by remember { mutableStateOf(false) }

    val engine = remember { VisionEngine(context) }

    fun runDescribe(detailed: Boolean) {
        if (imageUri == null) {
            error = context.getString(R.string.describe_error)
            return
        }
        isLoading = true
        error = null
        result = null
        scope.launch {
            try {
                val r = engine.describe(imageUri, detailed = detailed, language = language)
                result = r
                isLoading = false

                if (vibrateEnabled) {
                    vibrate(context)
                }
                if (autoSpeak) {
                    app.speechHelper.setLanguage(language)
                    app.speechHelper.speak(if (detailed) r.detailed else r.summary)
                }
            } catch (e: Exception) {
                isLoading = false
                error = context.getString(R.string.describe_error)
            }
        }
    }

    // Auto-run on first composition
    LaunchedEffect(imageUri, detailedDefault) {
        useDetailed = detailedDefault
        runDescribe(detailedDefault)
    }

    DisposableEffect(Unit) {
        onDispose { engine.close() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.describe_title),
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            result?.let {
                                app.speechHelper.speak(if (useDetailed) it.detailed else it.summary)
                            }
                        },
                        enabled = result != null
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = stringResource(R.string.btn_speak))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image preview
            if (imageUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null, // Decorative; result text is the important part
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !useDetailed,
                    onClick = {
                        useDetailed = false
                        runDescribe(false)
                    },
                    label = { Text(stringResource(R.string.describe_quick)) }
                )
                FilterChip(
                    selected = useDetailed,
                    onClick = {
                        useDetailed = true
                        runDescribe(true)
                    },
                    label = { Text(stringResource(R.string.describe_detailed)) }
                )
            }

            when {
                isLoading -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.describe_loading))
                    }
                }

                error != null -> {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = { runDescribe(useDetailed) }) {
                        Text(stringResource(R.string.retry))
                    }
                }

                result != null -> {
                    val r = result!!
                    val displayText = if (useDetailed) r.detailed else r.summary

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = context.getString(R.string.cd_result_text)
                            }
                    ) {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Baseerat", displayText))
                                Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_copy))
                        }
                        OutlinedButton(
                            onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, displayText)
                                }
                                context.startActivity(Intent.createChooser(send, null))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_share))
                        }
                    }

                    // Follow-up
                    OutlinedButton(
                        onClick = { showFollowUp = !showFollowUp },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_ask_followup))
                    }

                    if (showFollowUp) {
                        OutlinedTextField(
                            value = followUpText,
                            onValueChange = { followUpText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = context.getString(R.string.cd_ask_input)
                                },
                            label = { Text(stringResource(R.string.ask_hint)) },
                            supportingText = { Text(stringResource(R.string.ask_examples)) },
                            maxLines = 3
                        )
                        Button(
                            onClick = {
                                // Simple offline follow-up: answer from already extracted text/labels
                                val answer = buildFollowUpAnswer(followUpText, r, language)
                                app.speechHelper.speak(answer)
                                Toast.makeText(context, answer, Toast.LENGTH_LONG).show()
                            },
                            enabled = followUpText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.ask_send))
                        }
                    }
                }
            }
        }
    }
}

private fun buildFollowUpAnswer(
    question: String,
    result: VisionEngine.Result,
    language: String
): String {
    val q = question.lowercase()
    val isUrdu = language == "ur"

    return when {
        q.contains("text") || q.contains("لکھا") || q.contains("متن") -> {
            if (result.fullText.isNotBlank()) {
                if (isUrdu) "نظر آنے والا متن یہ ہے: ${result.fullText}"
                else "The visible text is: ${result.fullText}"
            } else {
                if (isUrdu) "اس تصویر میں کوئی واضح متن نہیں ملا۔"
                else "No clear text was found in this image."
            }
        }
        q.contains("object") || q.contains("کیا") || q.contains("what") || q.contains("اشیاء") -> {
            if (result.labels.isNotEmpty()) {
                if (isUrdu) "شناخت شدہ اشیاء: ${result.labels.joinToString("، ")}"
                else "Detected objects: ${result.labels.joinToString(", ")}"
            } else {
                if (isUrdu) "واضح اشیاء کی شناخت نہیں ہو سکی۔"
                else "Could not clearly detect specific objects."
            }
        }
        else -> {
            // Fallback to summary
            if (isUrdu) "موجودہ معلومات کی بنیاد پر: ${result.summary}"
            else "Based on current analysis: ${result.summary}"
        }
    }
}

private fun vibrate(context: Context) {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(40)
            }
        }
    } catch (_: Exception) { }
}
