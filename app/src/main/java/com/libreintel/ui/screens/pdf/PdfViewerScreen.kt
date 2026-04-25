package com.libreintel.ui.screens.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfUri: String,
    onBack: () -> Unit,
    onPushToTree: (String, String?) -> Unit
) {
    val context = LocalContext.current
    var showInstallPrompt by remember { mutableStateOf(false) }
    var hasTriedOpening by remember { mutableStateOf(false) }
    
    // Try to open PDF when screen loads
    LaunchedEffect(pdfUri) {
        if (!hasTriedOpening) {
            hasTriedOpening = true
            val success = openPdfWithAnyApp(context, pdfUri)
            if (!success) {
                showInstallPrompt = true
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Viewer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Select a PDF App",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Choose your preferred app to view the PDF",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    val success = openPdfWithAnyApp(context, pdfUri)
                    if (!success) {
                        showInstallPrompt = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open with PDF App")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go Back to Settings")
            }
        }
    }
    
    // Dialog to prompt for PDF viewer installation
    if (showInstallPrompt) {
        AlertDialog(
            onDismissRequest = { showInstallPrompt = false },
            title = { Text("No PDF App Selected") },
            text = { 
                Column {
                    Text("Please select a PDF viewer app from the list that appears.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("If no app is available, you can install Google Files from the Play Store.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.nb.files")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open Play Store", Toast.LENGTH_SHORT).show()
                    }
                    showInstallPrompt = false
                }) {
                    Text("Get PDF Viewer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun openPdfWithAnyApp(context: Context, pdfUri: String): Boolean {
    return try {
        val uri = Uri.parse(pdfUri)
        
        // Grant URI permission explicitly
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            // Permission might already be granted or not persistable
        }
        
        // Try method 1: Standard VIEW intent with MIME type and chooser
        val intent1 = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent1, "Open PDF with...")
        if (chooser.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser)
            return true
        }
        
        // Try method 2: Just VIEW with the URI (no MIME type)
        val intent2 = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser2 = Intent.createChooser(intent2, "Open PDF with...")
        if (chooser2.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser2)
            return true
        }
        
        false
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        false
    }
}