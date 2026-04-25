package com.libreintel.ui.screens.tree

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.libreintel.domain.model.TreeNode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeScreen(
    viewModel: TreeViewModel = hiltViewModel(),
    onNodeClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LibreIntel") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onImportClick) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onExportClick) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Node")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pinned parent indicator
            if (uiState.pinnedParentId != null) {
                val pinnedNode = viewModel.getNodeById(uiState.pinnedParentId!!)
                if (pinnedNode != null) {
                    PinnedIndicator(
                        node = pinnedNode,
                        onUnpin = { viewModel.setPinnedParent(null) }
                    )
                }
            }
            
            // Empty state
            if (uiState.rootNodes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AccountTree,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No nodes yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Tap + to add your first note",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.rootNodes) { node ->
                        TreeNodeItem(
                            node = node,
                            depth = 0,
                            collapsedIds = uiState.collapsedNodeIds,
                            pinnedParentId = uiState.pinnedParentId,
                            selectedNodeId = uiState.selectedNodeId,
                            onNodeClick = onNodeClick,
                            onToggleCollapse = viewModel::toggleNodeCollapse,
                            onDelete = viewModel::deleteNode,
                            onPin = { viewModel.setPinnedParent(node.id) }
                        )
                    }
                }
            }
        }
    }
    
    // Add node dialog
    if (showAddDialog) {
        AddNodeDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { text, sourceUrl ->
                viewModel.addNode(text, sourceUrl)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PinnedIndicator(
    node: TreeNode,
    onUnpin: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Next push → child of \"${node.title.take(30)}\"",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onUnpin) {
                Icon(Icons.Default.Close, contentDescription = "Unpin")
            }
        }
    }
}

@Composable
fun TreeNodeItem(
    node: TreeNode,
    depth: Int,
    collapsedIds: Set<String>,
    pinnedParentId: String?,
    selectedNodeId: String?,
    onNodeClick: (String) -> Unit,
    onToggleCollapse: (String) -> Unit,
    onDelete: (String) -> Unit,
    onPin: () -> Unit
) {
    val isCollapsed = node.id in collapsedIds
    val isPinned = node.id == pinnedParentId
    val isSelected = node.id == selectedNodeId
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 16).dp)
            .clickable { onNodeClick(node.id) },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPinned -> MaterialTheme.colorScheme.primaryContainer
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Collapse toggle
                if (node.children.isNotEmpty()) {
                    IconButton(
                        onClick = { onToggleCollapse(node.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.ExpandMore,
                            contentDescription = if (isCollapsed) "Expand" else "Collapse"
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(32.dp))
                }
                
                // Node title
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        node.title.ifEmpty { "Untitled" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatTimestamp(node.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Action buttons
                IconButton(onClick = onPin) {
                    Icon(
                        if (isPinned) Icons.Default.PushPin else Icons.Default.Link,
                        contentDescription = if (isPinned) "Unpin" else "Pin as parent",
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = { onDelete(node.id) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Source URL indicator
            node.sourceUrl?.let { url ->
                Row(
                    modifier = Modifier.padding(start = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.InsertLink,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        extractDomain(url),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // Render children if not collapsed
    AnimatedVisibility(visible = !isCollapsed) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            node.children.forEach { child ->
                TreeNodeItem(
                    node = child,
                    depth = depth + 1,
                    collapsedIds = collapsedIds,
                    pinnedParentId = pinnedParentId,
                    selectedNodeId = selectedNodeId,
                    onNodeClick = onNodeClick,
                    onToggleCollapse = onToggleCollapse,
                    onDelete = onDelete,
                    onPin = onPin
                )
            }
        }
    }
}

@Composable
fun AddNodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String, sourceUrl: String?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Node") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sourceUrl,
                    onValueChange = { sourceUrl = it },
                    label = { Text("Source URL (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text, sourceUrl.ifBlank { null }) },
                enabled = text.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(date: Date): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(date)
}

private fun extractDomain(url: String): String {
    return try {
        java.net.URL(url).host
    } catch (e: Exception) {
        url.take(30)
    }
}