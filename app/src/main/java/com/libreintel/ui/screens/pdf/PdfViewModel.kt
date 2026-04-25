package com.libreintel.ui.screens.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class PdfUiState(
    val fileName: String? = null,
    val filePath: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PdfUiState())
    val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()
    
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    
    companion object {
        private const val TAG = "PdfViewModel"
    }
    
    fun loadPdf(path: String) {
        viewModelScope.launch {
            _uiState.value = PdfUiState(isLoading = true)
            Log.d(TAG, "Loading PDF: $path")
            
            try {
                withContext(Dispatchers.IO) {
                    // First, try as content:// URI (most common from document picker)
                    if (path.startsWith("content://")) {
                        Log.d(TAG, "Trying as content:// URI")
                        try {
                            val uri = Uri.parse(path)
                            openPdfFromUri(context, uri)
                            Log.d(TAG, "Successfully opened as content:// URI")
                            return@withContext
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to open as content:// URI: ${e.message}")
                            // Continue to try other methods
                        }
                    }
                    
                    // Try as file:// URI
                    if (path.startsWith("file://")) {
                        Log.d(TAG, "Trying as file:// URI")
                        try {
                            val uri = Uri.parse(path)
                            val file = File(uri.path ?: "")
                            if (file.exists() && file.canRead()) {
                                openPdfFile(file)
                                Log.d(TAG, "Successfully opened as file:// URI")
                                return@withContext
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to open as file:// URI: ${e.message}")
                        }
                    }
                    
                    // Try as direct file path
                    Log.d(TAG, "Trying as direct file path")
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        openPdfFile(file)
                        Log.d(TAG, "Successfully opened as direct file")
                        return@withContext
                    }
                    
                    // If we got here, nothing worked
                    throw Exception("Cannot find or open PDF file. Path: $path")
                }
            } catch (e: Exception) {
                Log.e(TAG, "PDF loading failed: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Cannot open PDF: ${e.message}"
                )
            }
        }
    }

    private fun openPdfFromUri(context: Context, uri: Uri) {
        Log.d(TAG, "Opening PDF from URI: $uri")
        
        try {
            // Check if we have permission
            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                Log.d(TAG, "Successfully took persistent URI permission")
            } catch (e: SecurityException) {
                Log.w(TAG, "Could not take persistent permission (may already have it): ${e.message}")
            }
            
            // Try to open the file descriptor
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            if (fileDescriptor == null) {
                throw Exception("Cannot open file descriptor for URI")
            }
            
            this.fileDescriptor = fileDescriptor
            pdfRenderer = PdfRenderer(fileDescriptor)
            
            val totalPages = pdfRenderer!!.pageCount
            Log.d(TAG, "PDF opened successfully, pages: $totalPages")

            // Get file name from display name
            var fileName: String? = null
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not get file name: ${e.message}")
            }
            
            if (fileName == null) {
                fileName = uri.lastPathSegment ?: "Unknown PDF"
            }
            
            _uiState.value = _uiState.value.copy(
                fileName = fileName,
                filePath = uri.toString(),
                totalPages = totalPages,
                currentPage = 0,
                isLoading = false
            )
            
            renderPage(0)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening PDF from URI: ${e.message}")
            throw Exception("Cannot open PDF: ${e.message}")
        }
    }
    
    private fun openPdfFile(file: File) {
        Log.d(TAG, "Opening PDF file: ${file.absolutePath}")
        
        try {
            fileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            
            pdfRenderer = PdfRenderer(fileDescriptor!!)
            val totalPages = pdfRenderer!!.pageCount
            Log.d(TAG, "PDF file opened, pages: $totalPages")
            
            _uiState.value = _uiState.value.copy(
                fileName = file.name,
                filePath = file.absolutePath,
                totalPages = totalPages,
                currentPage = 0,
                isLoading = false
            )
            
            renderPage(0)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening PDF file: ${e.message}")
            throw Exception("Cannot open PDF: ${e.message}")
        }
    }
    
    private fun renderPage(pageIndex: Int) {
        try {
            pdfRenderer?.let { renderer ->
                if (pageIndex in 0 until renderer.pageCount) {
                    val page = renderer.openPage(pageIndex)
                    
                    val scale = 2.0f
                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()
                    
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    
                    _uiState.value = _uiState.value.copy(
                        currentPageBitmap = bitmap,
                        currentPage = pageIndex
                    )
                    Log.d(TAG, "Page $pageIndex rendered successfully")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering page: ${e.message}")
            _uiState.value = _uiState.value.copy(
                error = "Cannot render page: ${e.message}"
            )
        }
    }
    
    fun nextPage() {
        val current = _uiState.value.currentPage
        if (current < _uiState.value.totalPages - 1) {
            renderPage(current + 1)
        }
    }
    
    fun previousPage() {
        val current = _uiState.value.currentPage
        if (current > 0) {
            renderPage(current - 1)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        try {
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up: ${e.message}")
        }
    }
}