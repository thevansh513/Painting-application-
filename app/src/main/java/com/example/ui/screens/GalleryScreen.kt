package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.database.DrawingEntity
import com.example.ui.viewmodel.DrawingViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: DrawingViewModel,
    onOpenDrawing: (Long) -> Unit,
    onStartNewDrawing: () -> Unit,
    onOpenFeatureLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawings by viewModel.drawingsList.collectAsStateWithLifecycle()

    var showRenameDialog by remember { mutableStateOf<DrawingEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    
    var showDeleteConfirmationDialog by remember { mutableStateOf<DrawingEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Painting Gallery",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenFeatureLab,
                        modifier = Modifier.testTag("open_speculative_lab_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Speculative Lab",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    onStartNewDrawing()
                },
                icon = { Icon(Icons.Default.Add, "New Painting") },
                text = { Text("New Painting") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_painting_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (drawings.isEmpty()) {
                // Friendly artistic Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(110.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Studio Canvas is Blank",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create your very first physical vector painting and store them securely offline!",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onStartNewDrawing,
                        modifier = Modifier.testTag("empty_state_create_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Painting")
                    }
                }
            } else {
                // Responsive grid of Saved Drawings
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(drawings, key = { it.id }) { drawing ->
                        DrawingCard(
                            drawing = drawing,
                            onClick = { onOpenDrawing(drawing.id) },
                            onRename = { 
                                renameInputText = drawing.title
                                showRenameDialog = drawing 
                            },
                            onDelete = { showDeleteConfirmationDialog = drawing }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog popup
    if (showRenameDialog != null) {
        val activeDrawing = showRenameDialog!!
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Painting") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    label = { Text("New Painting Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = renameInputText.trim()
                        if (input.isNotBlank()) {
                            viewModel.renameDrawing(activeDrawing.id, input)
                            showRenameDialog = null
                        }
                    },
                    modifier = Modifier.testTag("rename_confirm_button")
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation Dialog popup
    if (showDeleteConfirmationDialog != null) {
        val activeDrawing = showDeleteConfirmationDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = null },
            title = { Text("Delete Painting") },
            text = { Text("Are you absolutely sure you want to delete '${activeDrawing.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDrawing(activeDrawing.id)
                        showDeleteConfirmationDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_confirm_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DrawingCard(
    drawing: DrawingEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val formattedTime = remember(drawing.updatedAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
        sdf.format(Date(drawing.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("drawing_item_card_${drawing.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Drawing PNG Preview Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                val file = File(drawing.filePath)
                if (file.exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(file),
                        contentDescription = "Thumbnail preview for ${drawing.title}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Info bar details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = drawing.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                // Tiny Options dropdown menu
                Box {
                    IconButton(
                        onClick = { expandedMenu = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("card_menu_button_${drawing.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options menu",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                expandedMenu = false
                                onRename()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("menu_rename_button_${drawing.id}")
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                expandedMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("menu_delete_button_${drawing.id}")
                        )
                    }
                }
            }
        }
    }
}
