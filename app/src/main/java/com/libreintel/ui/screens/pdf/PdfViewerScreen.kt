package com.libreintel.ui.screens.pdf

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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
    
    // Try to open PDF when screen loads
    LaunchedEffect(pdfUri) {
        openPdfWithAnyApp(context, pdfUri)
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
                "Opening PDF...",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "If the PDF doesn't open, tap the button below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (!openPdfWithAnyApp(context, pdfUri)) {
                        showInstallPrompt = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open PDF")
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
            title = { Text("No PDF Viewer Found") },
            text = { 
                Column {
                    Text("Your phone doesn't seem to have a PDF viewer app installed.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Would you like to install Google Files (free) from the Play Store?")
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
                    Text("Install Google Files")
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
        
        // Try method 1: Standard VIEW intent with MIME type
        val intent1 = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        if (intent1.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent1)
            return true
        }
        
        // Try method 2: Just VIEW with the URI
        val intent2 = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        if (intent2.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent2)
            return true
        }
        
        // Try method 3: Open with Google Files directly
        try {
            val googleFilesIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                setPackage("com.google.android.apps.nb.files")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(googleFilesIntent)
            return true
        } catch (e: Exception) {
            // Google Files not installed, try other methods
        }
        
        false
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        false
    }
}