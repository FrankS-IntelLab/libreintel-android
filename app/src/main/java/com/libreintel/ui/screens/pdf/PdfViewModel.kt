package com.libreintel.ui.screens.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
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
    
    fun loadPdf(path: String) {
        viewModelScope.launch {
            _uiState.value = PdfUiState(isLoading = true)
            
            try {
                withContext(Dispatchers.IO) {
                    // Try direct file first
                    val file = File(path)
                    
                    if (file.exists() && file.canRead()) {
                        openPdfFile(file)
                    } else {
                        // Try as content:// URI (requires context)
                        try {
                            val uri = Uri.parse(path)
                            if (uri.scheme == "content") {
                                openPdfFromUri(context, uri)
                                return@withContext
                            }
                        } catch (e: Exception) {
                            // Try file:// URI instead
                        }
                        
                        // Try as file:// URI
                        try {
                            val uri = Uri.parse(path)
                            if (uri.scheme == "file") {
                                val fileFromUri = File(uri.path ?: "")
                                if (fileFromUri.exists() && fileFromUri.canRead()) {
                                    openPdfFile(fileFromUri)
                                    return@withContext
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore URI parse errors
                        }
                        
                        // Last try: treat as a direct file path without leading slash
                        val directFile = File(path)
                        if (directFile.exists() && directFile.canRead()) {
                            openPdfFile(directFile)
                            return@withContext
                        }
                        
                        // Failed
                        throw Exception("Cannot access PDF file: $path")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    private fun openPdfFromUri(context: Context, uri: Uri) {
        try {
            // Get a file descriptor from the content resolver
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw Exception("Cannot open PDF file")
            
            this.fileDescriptor = fileDescriptor
            pdfRenderer = PdfRenderer(fileDescriptor!!)
            val totalPages = pdfRenderer!!.pageCount

            // Get file name from display name
            var fileName: String? = null
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
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
            throw Exception("Cannot open PDF: ${e.message}")
        }
    }
    
    private fun openPdfFile(file: File) {
        try {
            fileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            
            pdfRenderer = PdfRenderer(fileDescriptor!!)
            val totalPages = pdfRenderer!!.pageCount
            
            _uiState.value = _uiState.value.copy(
                fileName = file.name,
                filePath = file.absolutePath,
                totalPages = totalPages,
                currentPage = 0,
                isLoading = false
            )
            
            renderPage(0)
        } catch (e: Exception) {
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
                }
            }
        } catch (e: Exception) {
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
            // Ignore cleanup errors
        }
    }
}