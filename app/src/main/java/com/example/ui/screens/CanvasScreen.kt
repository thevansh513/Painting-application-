package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.data.model.FeatureDatabase
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DrawingStroke
import com.example.data.model.DrawingTool
import com.example.data.model.SymmetryMode
import com.example.data.model.StrokePoint
import com.example.data.model.CanvasBackgroundPattern
import com.example.ui.viewmodel.DrawingViewModel
import androidx.compose.ui.graphics.nativeCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    viewModel: DrawingViewModel,
    onBackToGallery: () -> Unit,
    onOpenFeatureLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe active drawing workspace states
    val currentDrawingId by viewModel.currentDrawingId.collectAsStateWithLifecycle()
    val currentTitle by viewModel.currentDrawingTitle.collectAsStateWithLifecycle()
    val strokes by viewModel.strokes.collectAsStateWithLifecycle()
    val currentStroke by viewModel.currentStroke.collectAsStateWithLifecycle()
    
    val brushColor by viewModel.brushColor.collectAsStateWithLifecycle()
    val brushWidth by viewModel.brushWidth.collectAsStateWithLifecycle()
    val isEraser by viewModel.isEraser.collectAsStateWithLifecycle()
    
    val scale by viewModel.scale.collectAsStateWithLifecycle()
    val panOffset by viewModel.panOffset.collectAsStateWithLifecycle()
    val isZoomPanEnabled by viewModel.isZoomPanEnabled.collectAsStateWithLifecycle()
    val canvasBgColor by viewModel.canvasBackgroundColor.collectAsStateWithLifecycle()

    // Observe symmetry and shapetools attributes
    val symmetryMode by viewModel.symmetryMode.collectAsStateWithLifecycle()
    val currentTool by viewModel.currentTool.collectAsStateWithLifecycle()
    val colorHistory by viewModel.colorHistory.collectAsStateWithLifecycle()
    val backgroundPattern by viewModel.backgroundPattern.collectAsStateWithLifecycle()
    val textStampValue by viewModel.textStampValue.collectAsStateWithLifecycle()

    val layers by viewModel.layers.collectAsStateWithLifecycle()
    val activeLayer by viewModel.activeLayer.collectAsStateWithLifecycle()
    val hiddenLayers by viewModel.hiddenLayers.collectAsStateWithLifecycle()
    val layerOpacities by viewModel.layerOpacities.collectAsStateWithLifecycle()
    val activeCustomPluginTool by viewModel.activeCustomPluginTool.collectAsStateWithLifecycle()

    // Tab state (0: Brushes & Shapes, 1: Symmetry & Mandala, 2: Colors & History, 3: Layers)
    var selectedBottomTab by remember { mutableStateOf(0) }

    // Layer Dialog Controllers
    var showAddLayerDialog by remember { mutableStateOf(false) }
    var newLayerNameInput by remember { mutableStateOf("") }
    var showRenameLayerDialog by remember { mutableStateOf(false) }
    var renamingLayerId by remember { mutableStateOf("") }
    var renamingLayerNameInput by remember { mutableStateOf("") }

    // Screen State Controllers
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showSaveNamingDialog by remember { mutableStateOf(false) }
    var tempTitleInput by remember { mutableStateOf("") }
    var showSymmetryInfoDialog by remember { mutableStateOf(false) }

    // Preset color catalog
    val presetColors = remember {
        listOf(
            0xFF212121.toInt(), // Charcoal
            0xFFED2939.toInt(), // Crimson Red
            0xFFFF5722.toInt(), // Orange
            0xFFFFB400.toInt(), // Vibrant Yellow
            0xFF386A20.toInt(), // Emerald green
            0xFF009688.toInt(), // Dark teal
            0xFF0061A4.toInt(), // Professional blue
            0xFF7D5260.toInt(), // Berry red
            0xFF9C27B0.toInt(), // Deep violet
            0xFF795548.toInt()  // Brown Earth
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (currentDrawingId == 0L) "New Mandala (Unsaved)" else "Saved local draft",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToGallery,
                        modifier = Modifier.testTag("back_to_gallery_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Gallery"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        modifier = Modifier.testTag("undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo"
                        )
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        modifier = Modifier.testTag("redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo"
                        )
                    }
                    IconButton(
                        onClick = {
                            tempTitleInput = currentTitle
                            showSaveNamingDialog = true
                        },
                        modifier = Modifier.testTag("save_as_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Drawing"
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.shareCurrentDrawing(
                                onReadyToShare = { uri ->
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Share Painting"))
                                },
                                onError = {
                                    Toast.makeText(context, "Could not load image to share", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.testTag("share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Painting"
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.triggerManualExport(
                                onSuccess = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "Export failed: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        modifier = Modifier.testTag("export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export to Gallery"
                        )
                    }
                    IconButton(
                        onClick = onOpenFeatureLab,
                        modifier = Modifier.testTag("canvas_open_speculative_lab_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Speculative Lab",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            
            // Zoom Pan active warning
            AnimatedVisibility(visible = isZoomPanEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom Icon",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Zoom Pan Locked (Scale: ${String.format("%.1fx", scale)})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    TextButton(
                        onClick = { viewModel.resetZoomPan() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Reset view", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Interactive Painting Studio Workspace
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant) // Easel grey stand
                    .onSizeChanged { size ->
                        viewModel.updateCanvasSize(size.width, size.height)
                    },
                contentAlignment = Alignment.Center
            ) {
                
                // Pure Canvas Board
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("drawing_canvas")
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(canvasBgColor))
                        .pointerInput(isZoomPanEnabled) {
                            if (isZoomPanEnabled) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    viewModel.updateZoomPan(zoom, pan)
                                }
                            } else {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val canvasX = (offset.x - panOffset.x) / scale
                                        val canvasY = (offset.y - panOffset.y) / scale
                                        viewModel.startNewStroke(StrokePoint(canvasX, canvasY))
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val offset = change.position
                                        val canvasX = (offset.x - panOffset.x) / scale
                                        val canvasY = (offset.y - panOffset.y) / scale
                                        viewModel.appendPointToCurrentStroke(StrokePoint(canvasX, canvasY))
                                    },
                                    onDragEnd = {
                                        viewModel.completeCurrentStroke()
                                    }
                                )
                            }
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            }
                    ) {
                        // Draw guidelines (GRID, DOTS, PERSPECTIVE, TEXTURE)
                        drawBackgroundGuides(this, backgroundPattern, size.width, size.height)

                        // A: Canvas grid/radial symmetry indicators (if enabled)
                        drawSymmetryGuides(this, symmetryMode, size.width, size.height)

                        // B: Render previously completed drawing strokes
                        for (stroke in strokes) {
                            if (!hiddenLayers.contains(stroke.layerId)) {
                                val o = layerOpacities[stroke.layerId] ?: 1.0f
                                drawDrawingStrokeOnCompose(this, stroke, o)
                            }
                        }

                        // C: Render active path + all mirror counterparts dynamically
                        val active = currentStroke
                        if (active != null && !hiddenLayers.contains(active.layerId)) {
                            val activeSymmetryGroup = viewModel.getSymmetricStrokes(active)
                            val o = layerOpacities[active.layerId] ?: 1.0f
                            for (stroke in activeSymmetryGroup) {
                                drawDrawingStrokeOnCompose(this, stroke, o)
                            }
                        }
                    }
                }

                // Small circular floating functional utility actions over paint canvas
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Lock mode to zoom / pan
                    FloatingActionButton(
                        onClick = { viewModel.toggleZoomPanEnabled() },
                        modifier = Modifier.size(48.dp),
                        containerColor = if (isZoomPanEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isZoomPanEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isZoomPanEnabled) Icons.Default.Brush else Icons.Default.ZoomIn,
                            contentDescription = "Toggle Zoom Pan mode"
                        )
                    }

                    FloatingActionButton(
                        onClick = { viewModel.clearCanvas() },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Canvas"
                        )
                    }
                }
            }

            // Tab Content Expanded Studio Dashboard (Brush configurations, Symmetry settings, Palettes)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    
                    // Display tab specific dashboard controls
                    when (selectedBottomTab) {
                        0 -> {
                            // TAB 0: BRUSH & TOOL SELECTION
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Drawing Tool: ${currentTool.displayName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Size: ${brushWidth.toInt()}px",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                 // Interactive Tools Chips
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(DrawingTool.values()) { tool ->
                                        val isSelected = currentTool == tool && !isEraser
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setDrawingTool(tool) },
                                            leadingIcon = {
                                                val emoji = when (tool) {
                                                    DrawingTool.FREEHAND -> "🖌️"
                                                    DrawingTool.BRUSH_CALLIGRAPHY -> "✍️"
                                                    DrawingTool.BRUSH_NEON -> "⚡"
                                                    DrawingTool.BRUSH_RAINBOW -> "🌈"
                                                    DrawingTool.BRUSH_MARKER -> "🖍️"
                                                    DrawingTool.BRUSH_AIRBRUSH -> "💨"
                                                    DrawingTool.BRUSH_SHADOW -> "🌘"
                                                    DrawingTool.LINE -> "📏"
                                                    DrawingTool.RECTANGLE_OUTLINE -> "⬜"
                                                    DrawingTool.RECTANGLE_FILLED -> "🟦"
                                                    DrawingTool.CIRCLE_OUTLINE -> "⭕"
                                                    DrawingTool.CIRCLE_FILLED -> "🔵"
                                                    DrawingTool.STAMP_STAR -> "⭐"
                                                    DrawingTool.STAMP_HEART -> "❤️"
                                                    DrawingTool.STAMP_FLOWER -> "🌸"
                                                    DrawingTool.STAMP_LEAF -> "🍁"
                                                    DrawingTool.STAMP_SNOWFLAKE -> "❄️"
                                                    DrawingTool.STAMP_CLOUD -> "☁️"
                                                    DrawingTool.STAMP_SMILEY -> "😊"
                                                    DrawingTool.STAMP_SUN -> "☀️"
                                                    DrawingTool.STAMP_MOON -> "🌙"
                                                    DrawingTool.STAMP_TEXT -> "🔠"
                                                }
                                                Text(emoji, fontSize = 12.sp)
                                            },
                                            label = { Text(tool.displayName, fontSize = 12.sp) }
                                        )
                                    }
                                    
                                    // Eraser Tool
                                    item {
                                        FilterChip(
                                            selected = isEraser,
                                            onClick = { viewModel.toggleEraser() },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.AutoFixNormal,
                                                    contentDescription = "Eraser tool",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text("Eraser", fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        )
                                    }
                                }

                                // Speculative Custom Plugins Section (if any are active)
                                val activatedPlugins = remember { FeatureDatabase.allFeatures.filter { it.isActivated } }
                                if (activatedPlugins.isNotEmpty()) {
                                    Text(
                                        text = "Compiled Custom Plugins (${activatedPlugins.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(activatedPlugins) { feature ->
                                            val isSelected = activeCustomPluginTool == feature.name
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.selectCustomPluginTool(if (isSelected) null else feature.name) },
                                                leadingIcon = {
                                                    Text("🔌", fontSize = 12.sp)
                                                },
                                                label = { Text(feature.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            )
                                        }
                                    }
                                }

                                if (currentTool == DrawingTool.STAMP_TEXT) {
                                    OutlinedTextField(
                                        value = textStampValue,
                                        onValueChange = { viewModel.setTextStampValue(it) },
                                        label = { Text("Stamp Text Glyphs", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .testTag("text_stamp_input"),
                                        trailingIcon = {
                                            Text("🔠", modifier = Modifier.padding(end = 8.dp))
                                        }
                                    )
                                }

                                // Interactive Stroke Width Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LineWeight,
                                        contentDescription = "Width",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Slider(
                                        value = brushWidth,
                                        onValueChange = { viewModel.setBrushWidth(it) },
                                        valueRange = 2f..100f,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("brush_size_slider")
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    // Visual Brush Cap Preview indicator
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isEraser) MaterialTheme.colorScheme.outline else Color(brushColor)
                                            )
                                    )
                                }
                            }
                        }
                        1 -> {
                            // TAB 1: SYMMETRY & MANDALA SECTOR SELECTION
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AllInclusive,
                                            contentDescription = "Symmetry",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Symmetry: ${symmetryMode.displayName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { showSymmetryInfoDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "About Symmetry",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                // Horizontally scrollable list of symmetry patterns
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(SymmetryMode.values()) { mode ->
                                        val isSelected = symmetryMode == mode
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setSymmetryMode(mode) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = when (mode) {
                                                        SymmetryMode.NONE -> Icons.Default.Block
                                                        SymmetryMode.HORIZONTAL -> Icons.Default.Compare
                                                        SymmetryMode.VERTICAL -> Icons.Default.AlignVerticalCenter
                                                        SymmetryMode.FOUR_WAY -> Icons.Default.Grid4x4
                                                        SymmetryMode.RADIAL_6, SymmetryMode.RADIAL_8, SymmetryMode.RADIAL_12 -> Icons.Default.FilterVintage
                                                    },
                                                    contentDescription = mode.displayName,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            label = { Text(mode.displayName, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                Text(
                                    text = "Draw anywhere on the canvas. Your lines are automatically mirrored across boundaries in real time to draw gorgeous mandalas and repeat tiles!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )

                                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GridOn,
                                        contentDescription = "Canvas Background guidelines",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Canvas Background Guidelines",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(CanvasBackgroundPattern.values()) { pattern ->
                                        val isSelected = backgroundPattern == pattern
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setBackgroundPattern(pattern) },
                                            leadingIcon = {
                                                val emoji = when (pattern) {
                                                    CanvasBackgroundPattern.NONE -> "⬜"
                                                    CanvasBackgroundPattern.GRID_CLEAN -> "📐"
                                                    CanvasBackgroundPattern.GRID_DOTS -> "░"
                                                    CanvasBackgroundPattern.GRID_ISOMETRIC -> "🧊"
                                                    CanvasBackgroundPattern.GRID_RADIAL -> "🎯"
                                                    CanvasBackgroundPattern.TEXTURE_PAPER -> "📜"
                                                }
                                                Text(emoji, fontSize = 12.sp)
                                            },
                                            label = { Text(pattern.displayName, fontSize = 12.sp) }
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            // TAB 2: SMART PALETTE & COLOR HISTORY RECORDER
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Color Palettes",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(onClick = { showColorPickerDialog = true }) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Colorize, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text("Custom Picker", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Presets Palette
                                Text(
                                    text = "Preset Shades",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(presetColors) { color ->
                                        val isSelected = brushColor == color && !isEraser
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(color))
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.setBrushColor(color) }
                                        )
                                    }
                                }

                                // Smart Color History row
                                Text(
                                    text = "Recently Used",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (colorHistory.isEmpty()) {
                                    Text(
                                        text = "No custom colors used yet. Open Custom Picker to mix shades!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(colorHistory) { color ->
                                            val isSelected = brushColor == color && !isEraser
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(color))
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { viewModel.setBrushColor(color) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            // TAB 3: LAYERS & COMPOSITION PANEL
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Layer Composition",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Active: $activeLayer",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            newLayerNameInput = "Layer ${layers.size + 1}"
                                            showAddLayerDialog = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Layer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Handle zero/empty layers fallback
                                if (layers.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No active layers", style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 180.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(layers) { layerId ->
                                            val isActive = layerId == activeLayer
                                            val isHidden = hiddenLayers.contains(layerId)
                                            val currentOpacity = layerOpacities[layerId] ?: 1.0f

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(
                                                        width = if (isActive) 1.5.dp else 0.5.dp,
                                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(8.dp)
                                                    ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Active bullet selector
                                                        RadioButton(
                                                            selected = isActive,
                                                            onClick = { viewModel.selectLayer(layerId) },
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        
                                                        // Layer name
                                                        Text(
                                                            text = layerId,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable { viewModel.selectLayer(layerId) }
                                                        )

                                                        // Rename icon
                                                        IconButton(
                                                            onClick = {
                                                                renamingLayerId = layerId
                                                                renamingLayerNameInput = layerId
                                                                showRenameLayerDialog = true
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "Rename layer",
                                                                modifier = Modifier.size(16.dp),
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }

                                                        // Visibility icon
                                                        IconButton(
                                                            onClick = { viewModel.toggleLayerVisibility(layerId) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                                contentDescription = "Toggle Visibility",
                                                                modifier = Modifier.size(16.dp),
                                                                tint = if (isHidden) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                                                            )
                                                        }

                                                        // Delete icon (disable if only one layer is left)
                                                        IconButton(
                                                            onClick = { viewModel.deleteLayer(layerId) },
                                                            enabled = layers.size > 1,
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete Layer",
                                                                modifier = Modifier.size(16.dp),
                                                                tint = if (layers.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                            )
                                                        }
                                                    }

                                                    // Opacity slider for layer
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 32.dp, top = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Opacity: ${(currentOpacity * 100).toInt()}%",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.width(72.dp)
                                                        )
                                                        Slider(
                                                            value = currentOpacity,
                                                            onValueChange = { viewModel.setLayerOpacity(layerId, it) },
                                                            valueRange = 0.0f..1.0f,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Four tab selector icons at the bottom of panel
                    TabRow(
                        selectedTabIndex = selectedBottomTab,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedBottomTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedBottomTab == 0,
                            onClick = { selectedBottomTab = 0 },
                            text = { Text("Brush & Tools", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(imageVector = Icons.Default.Brush, contentDescription = "Tools") }
                        )
                        Tab(
                            selected = selectedBottomTab == 1,
                            onClick = { selectedBottomTab = 1 },
                            text = { Text("Symmetry", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(imageVector = Icons.Default.AllInclusive, contentDescription = "Symmetry") }
                        )
                        Tab(
                            selected = selectedBottomTab == 2,
                            onClick = { selectedBottomTab = 2 },
                            text = { Text("Palettes", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(imageVector = Icons.Default.Palette, contentDescription = "Palette") }
                        )
                        Tab(
                            selected = selectedBottomTab == 3,
                            onClick = { selectedBottomTab = 3 },
                            text = { Text("Layers", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(imageVector = Icons.Default.Layers, contentDescription = "Layers") }
                        )
                    }
                }
            }
        }
    }

    // Color Picker Dialog
    if (showColorPickerDialog) {
        CustomColorPickerDialog(
            initialColor = Color(brushColor),
            onDismiss = { showColorPickerDialog = false },
            onColorSelected = { selected ->
                viewModel.setBrushColor(selected.toArgb())
                showColorPickerDialog = false
            }
        )
    }

    // Add Layer Dialog
    if (showAddLayerDialog) {
        AlertDialog(
            onDismissRequest = { showAddLayerDialog = false },
            title = { Text("Create New Composition Layer") },
            text = {
                OutlinedTextField(
                    value = newLayerNameInput,
                    onValueChange = { newLayerNameInput = it },
                    label = { Text("Layer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newLayerNameInput.isNotBlank()) {
                            viewModel.addNewLayer(newLayerNameInput.trim())
                        }
                        showAddLayerDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLayerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Layer Dialog
    if (showRenameLayerDialog) {
        AlertDialog(
            onDismissRequest = { showRenameLayerDialog = false },
            title = { Text("Rename Composition Layer") },
            text = {
                OutlinedTextField(
                    value = renamingLayerNameInput,
                    onValueChange = { renamingLayerNameInput = it },
                    label = { Text("Layer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renamingLayerNameInput.isNotBlank()) {
                            viewModel.renameLayer(renamingLayerId, renamingLayerNameInput.trim())
                        }
                        showRenameLayerDialog = false
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameLayerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Symmetry Info Dialog Explainers 10000 plus value features
    if (showSymmetryInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSymmetryInfoDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FilterVintage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Mandala Symmetry Guide")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Symmetry modes help draw elegant, perfectly rotated or mirrored patterns automatically:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Horizontal / Vertical: Mirrors strokes across the canvas center-lines for bilateral patterns.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• 4-Way Mirror: Reflects lines in four directions simultaneously.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Radial Mandala (6, 8, 12): Splits the canvas into equal circular wedges. Drawing in one segment automatically recreates beautiful kaleidoscopic mandala geometry. Extremely satisfying for patterns!",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSymmetryInfoDialog = false }) {
                    Text("Got it!")
                }
            }
        )
    }

    // Save Naming title Dialog
    if (showSaveNamingDialog) {
        AlertDialog(
            onDismissRequest = { showSaveNamingDialog = false },
            title = { Text("Save Artwork Title") },
            text = {
                OutlinedTextField(
                    value = tempTitleInput,
                    onValueChange = { tempTitleInput = it },
                    label = { Text("Painting Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = tempTitleInput.trim()
                        if (title.isNotBlank()) {
                            viewModel.saveDrawingWithCustomTitle(title) {
                                Toast.makeText(context, "Saved Successfully!", Toast.LENGTH_SHORT).show()
                            }
                            showSaveNamingDialog = false
                        } else {
                            Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("save_confirm_button")
                ) {
                    Text("Confirm Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveNamingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Draw guidelines representing mirror divisions
fun drawSymmetryGuides(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    mode: SymmetryMode,
    width: Float,
    height: Float
) {
    if (mode == SymmetryMode.NONE) return
    val cx = width / 2f
    val cy = height / 2f
    
    // Smooth grid guidelines
    val guideColor = Color.Magenta.copy(alpha = 0.22f)
    val strokeWidth = 2f
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)

    when (mode) {
        SymmetryMode.NONE -> {}
        SymmetryMode.HORIZONTAL -> {
            drawScope.drawLine(
                color = guideColor,
                start = Offset(cx, 0f),
                end = Offset(cx, height),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )
        }
        SymmetryMode.VERTICAL -> {
            drawScope.drawLine(
                color = guideColor,
                start = Offset(0f, cy),
                end = Offset(width, cy),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )
        }
        SymmetryMode.FOUR_WAY -> {
            drawScope.drawLine(
                color = guideColor,
                start = Offset(cx, 0f),
                end = Offset(cx, height),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )
            drawScope.drawLine(
                color = guideColor,
                start = Offset(0f, cy),
                end = Offset(width, cy),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )
        }
        SymmetryMode.RADIAL_6, SymmetryMode.RADIAL_8, SymmetryMode.RADIAL_12 -> {
            val sectors = when (mode) {
                SymmetryMode.RADIAL_6 -> 6
                SymmetryMode.RADIAL_8 -> 8
                else -> 12
            }
            val radius = Math.sqrt((cx * cx + cy * cy).toDouble()).toFloat()
            val halfSectors = sectors / 2
            
            for (i in 0 until halfSectors) {
                val angleRad = (Math.PI * i) / halfSectors
                val cosA = Math.cos(angleRad).toFloat()
                val sinA = Math.sin(angleRad).toFloat()
                
                val startX = cx - radius * cosA
                val startY = cy - radius * sinA
                val endX = cx + radius * cosA
                val endY = cy + radius * sinA
                
                drawScope.drawLine(
                    color = guideColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth,
                    pathEffect = pathEffect
                )
            }
        }
    }
}

// Modular rendering helper for drawing distinct vector shapes on Compose Canvas
fun drawDrawingStrokeOnCompose(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    stroke: DrawingStroke,
    opacity: Float = 1.0f
) {
    if (stroke.points.isEmpty()) return
    val p0 = stroke.points[0]
    val shapeType = stroke.shapeType
    val baseColor = Color(stroke.color).copy(alpha = Color(stroke.color).alpha * opacity)

    // 1. Render Custom Text stamp
    if (shapeType.startsWith("STAMP_TEXT:")) {
        val text = shapeType.substringAfter("STAMP_TEXT:")
        val paint = android.graphics.Paint().apply {
            color = stroke.color
            textSize = Math.max(24f, stroke.width * 2f)
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            style = android.graphics.Paint.Style.FILL
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
        }
        drawScope.drawContext.canvas.nativeCanvas.drawText(text, p0.x, p0.y, paint)
        return
    }

    // 2. Custom Speculative Plugin Brush Engine (10000+ spec items)
    if (shapeType.startsWith("PLUGIN_BRUSH:")) {
        val pluginName = shapeType.substringAfter("PLUGIN_BRUSH:")
        val isQuantum = pluginName.contains("Quantum", ignoreCase = true) || pluginName.contains("Procedural", ignoreCase = true)
        val isNeural = pluginName.contains("Neural", ignoreCase = true) || pluginName.contains("Synaptic", ignoreCase = true) || pluginName.contains("Vector", ignoreCase = true)
        val isSpectra = pluginName.contains("Spectra", ignoreCase = true) || pluginName.contains("Prismatic", ignoreCase = true) || pluginName.contains("Rainbow", ignoreCase = true)
        val isVortex = pluginName.contains("Vortex", ignoreCase = true) || pluginName.contains("Fluidic", ignoreCase = true)
        val isLuminescent = pluginName.contains("Luminescent", ignoreCase = true) || pluginName.contains("Holographic", ignoreCase = true) || pluginName.contains("Glow", ignoreCase = true)
        val isCrystalline = pluginName.contains("Crystalline", ignoreCase = true) || pluginName.contains("Synthetic", ignoreCase = true)

        if (isQuantum) {
            // Quantum Scattering particles
            val rGen = java.util.Random((12345 + pluginName.hashCode()).toLong())
            if (stroke.points.size >= 2) {
                val path = Path().apply {
                    moveTo(p0.x, p0.y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                drawScope.drawPath(path = path, color = baseColor, style = Stroke(width = stroke.width, cap = StrokeCap.Round), alpha = opacity)
            }
            for (p in stroke.points) {
                for (k in 0 until 5) {
                    val radius = 3f + rGen.nextFloat() * 4f
                    val rx = p.x + (rGen.nextFloat() - 0.5f) * stroke.width * 4f
                    val ry = p.y + (rGen.nextFloat() - 0.5f) * stroke.width * 4f
                    drawScope.drawCircle(color = baseColor, radius = radius, center = Offset(rx, ry), alpha = opacity * 0.7f)
                }
            }
        } else if (isNeural) {
            // Neural Synapses
            if (stroke.points.size >= 2) {
                val path = Path().apply {
                    moveTo(p0.x, p0.y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                drawScope.drawPath(path = path, color = baseColor, style = Stroke(width = stroke.width, cap = StrokeCap.Round), alpha = opacity)
                
                // Draw neural nodes and connections
                for (i in 0 until stroke.points.size step 3) {
                    val p = stroke.points[i]
                    drawScope.drawCircle(color = Color.Cyan, radius = stroke.width * 0.9f, center = Offset(p.x, p.y), style = Stroke(width = 2f), alpha = opacity)
                    if (i + 3 < stroke.points.size) {
                        val pNext = stroke.points[i + 3]
                        drawScope.drawLine(color = Color.Cyan, start = Offset(p.x, p.y), end = Offset(pNext.x, pNext.y), strokeWidth = 1.5f, alpha = opacity * 0.5f)
                    }
                }
            }
        } else if (isSpectra) {
            // Rainbow spectra
            for (i in 1 until stroke.points.size) {
                val pStart = stroke.points[i - 1]
                val pEnd = stroke.points[i]
                val hue = (i * 15f + pluginName.hashCode()) % 360f
                drawScope.drawLine(
                    color = Color.hsv(hue = hue, saturation = 0.95f, value = 0.95f),
                    start = Offset(pStart.x, pStart.y),
                    end = Offset(pEnd.x, pEnd.y),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                    alpha = opacity
                )
            }
        } else if (isVortex) {
            // Vortex orbit spirals
            if (stroke.points.size >= 2) {
                val path = Path().apply {
                    moveTo(p0.x, p0.y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                drawScope.drawPath(path = path, color = baseColor, style = Stroke(width = stroke.width), alpha = opacity)
                
                for (i in 0 until stroke.points.size step 4) {
                    val p = stroke.points[i]
                    val radius = stroke.width * 2.2f
                    drawScope.drawCircle(
                        color = baseColor.copy(alpha = baseColor.alpha * 0.3f),
                        radius = radius,
                        center = Offset(p.x, p.y),
                        style = Stroke(width = 2.5f),
                        alpha = opacity
                    )
                }
            }
        } else if (isLuminescent) {
            // Translucent glowing trail
            if (stroke.points.size >= 2) {
                val path = Path().apply {
                    moveTo(p0.x, p0.y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                drawScope.drawPath(path = path, color = baseColor, style = Stroke(width = stroke.width * 2.8f), alpha = opacity * 0.25f)
                drawScope.drawPath(path = path, color = baseColor, style = Stroke(width = stroke.width * 1.5f), alpha = opacity * 0.6f)
                drawScope.drawPath(path = path, color = Color.White, style = Stroke(width = stroke.width * 0.4f), alpha = opacity)
            }
        } else if (isCrystalline) {
            // Hex shards/crystals
            if (stroke.points.size >= 2) {
                val path = Path().apply {
                    moveTo(p0.x, p0.y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                drawScope.drawPath(path = path, color = baseColor, style = Stroke(width = 2f), alpha = opacity * 0.5f)
                for (i in 0 until stroke.points.size step 5) {
                    val p = stroke.points[i]
                    val size = stroke.width * 1.5f
                    val shard = Path().apply {
                        moveTo(p.x, p.y - size)
                        lineTo(p.x + size * 0.8f, p.y - size * 0.3f)
                        lineTo(p.x + size * 0.5f, p.y + size * 0.7f)
                        lineTo(p.x - size * 0.5f, p.y + size * 0.7f)
                        lineTo(p.x - size * 0.8f, p.y - size * 0.3f)
                        close()
                    }
                    drawScope.drawPath(path = shard, color = baseColor, alpha = opacity)
                }
            }
        } else {
            // Fallback decorative calligraphy ribbon
            if (stroke.points.size >= 2) {
                val path = Path().apply {
                    moveTo(p0.x, p0.y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                drawScope.drawPath(path = path, color = baseColor, style = Stroke(width = stroke.width), alpha = opacity)
                drawScope.drawPath(path = path, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = stroke.width * 0.3f), alpha = opacity)
            }
        }
        return
    }

    // 3. Render normal brushes, special brushes, shapes, and graphical stamps
    when (shapeType) {
        "FREEHAND" -> {
            if (stroke.points.size < 2) {
                drawScope.drawCircle(
                    color = Color(stroke.color),
                    radius = stroke.width / 2,
                    center = Offset(p0.x, p0.y)
                )
            } else {
                val path = Path().apply {
                    moveTo(p0.x, p0.y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                drawScope.drawPath(
                    path = path,
                    color = Color(stroke.color),
                    style = Stroke(
                        width = stroke.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
        "BRUSH_MARKER" -> {
            if (stroke.points.size >= 2) {
                val path = Path().apply {
                    moveTo(p0.x, p0.y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                drawScope.drawPath(
                    path = path,
                    color = Color(stroke.color).copy(alpha = 0.40f),
                    style = Stroke(
                        width = stroke.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
        "BRUSH_SHADOW" -> {
            if (stroke.points.size >= 2) {
                val path = Path().apply {
                    moveTo(p0.x, p0.y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                // draw offset shadow
                drawScope.drawPath(
                    path = path,
                    color = Color.DarkGray.copy(alpha = 0.35f),
                    style = Stroke(
                        width = stroke.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.DarkGray.copy(alpha = 0.35f))
                )
                // draw main ribbon
                drawScope.drawPath(
                    path = path,
                    color = Color(stroke.color),
                    style = Stroke(
                        width = stroke.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
        "BRUSH_NEON" -> {
            if (stroke.points.size >= 2) {
                val path = Path().apply {
                    moveTo(p0.x, p0.y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                // layer 1: wide under-glow
                drawScope.drawPath(
                    path = path,
                    color = Color(stroke.color).copy(alpha = 0.35f),
                    style = Stroke(width = stroke.width * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // layer 2: middle aura
                drawScope.drawPath(
                    path = path,
                    color = Color(stroke.color).copy(alpha = 0.70f),
                    style = Stroke(width = stroke.width * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // layer 3: solid center
                drawScope.drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = stroke.width * 0.35f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        "BRUSH_RAINBOW" -> {
            for (i in 1 until stroke.points.size) {
                val pStart = stroke.points[i - 1]
                val pEnd = stroke.points[i]
                val hue = (i * 12f) % 360f
                drawScope.drawLine(
                    color = Color.hsv(hue = hue, saturation = 1f, value = 1f),
                    start = Offset(pStart.x, pStart.y),
                    end = Offset(pEnd.x, pEnd.y),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }
        }
        "BRUSH_CALLIGRAPHY" -> {
            val offset = stroke.width / 2f
            for (p in stroke.points) {
                drawScope.drawLine(
                    color = Color(stroke.color),
                    start = Offset(p.x - offset, p.y + offset),
                    end = Offset(p.x + offset, p.y - offset),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Square
                )
            }
        }
        "BRUSH_AIRBRUSH" -> {
            // Re-create spray splatter physics using randomized seeds
            val rGen = java.util.Random(12345)
            for (p in stroke.points) {
                for (k in 0 until 9) {
                    val r = rGen.nextFloat() * stroke.width
                    val theta = rGen.nextFloat() * 2f * Math.PI.toFloat()
                    val dx = r * Math.cos(theta.toDouble()).toFloat()
                    val dy = r * Math.sin(theta.toDouble()).toFloat()
                    val dSize = 1.5f + rGen.nextFloat() * 2.5f
                    drawScope.drawCircle(
                        color = Color(stroke.color).copy(alpha = 0.45f),
                        radius = dSize,
                        center = Offset(p.x + dx, p.y + dy)
                    )
                }
            }
        }
        "LINE" -> {
            if (stroke.points.size >= 2) {
                val p1 = stroke.points[1]
                drawScope.drawLine(
                    color = Color(stroke.color),
                    start = Offset(p0.x, p0.y),
                    end = Offset(p1.x, p1.y),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }
        }
        "RECTANGLE_OUTLINE", "RECTANGLE_FILLED" -> {
            if (stroke.points.size >= 2) {
                val p1 = stroke.points[1]
                val left = minOf(p0.x, p1.x)
                val top = minOf(p0.y, p1.y)
                val w = Math.abs(p0.x - p1.x)
                val h = Math.abs(p0.y - p1.y)

                if (shapeType == "RECTANGLE_FILLED") {
                    drawScope.drawRect(
                        color = Color(stroke.color),
                        topLeft = Offset(left, top),
                        size = Size(w, h)
                    )
                } else {
                    drawScope.drawRect(
                        color = Color(stroke.color),
                        topLeft = Offset(left, top),
                        size = Size(w, h),
                        style = Stroke(width = stroke.width)
                    )
                }
            }
        }
        "CIRCLE_OUTLINE", "CIRCLE_FILLED" -> {
            if (stroke.points.size >= 2) {
                val p1 = stroke.points[1]
                val r = Math.sqrt(
                    ((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()
                ).toFloat()

                if (shapeType == "CIRCLE_FILLED") {
                    drawScope.drawCircle(
                        color = Color(stroke.color),
                        radius = r,
                        center = Offset(p0.x, p0.y)
                    )
                } else {
                    drawScope.drawCircle(
                        color = Color(stroke.color),
                        radius = r,
                        center = Offset(p0.x, p0.y),
                        style = Stroke(width = stroke.width)
                    )
                }
            }
        }
        "STAMP_STAR" -> {
            val size = if (stroke.points.size >= 2) {
                val p1 = stroke.points[1]
                Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
            } else {
                Math.max(30f, stroke.width * 2f)
            }
            val path = Path()
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) size else size * 0.4f
                val angle = i * Math.PI / 5 - Math.PI / 2
                val x = p0.x + r * Math.cos(angle).toFloat()
                val y = p0.y + r * Math.sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawScope.drawPath(path, Color(stroke.color))
        }
        "STAMP_HEART" -> {
            val size = if (stroke.points.size >= 2) {
                val p1 = stroke.points[1]
                Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
            } else {
                Math.max(30f, stroke.width * 2f)
            }
            val path = Path().apply {
                moveTo(p0.x, p0.y + size)
                cubicTo(p0.x - size * 1.2f, p0.y - size * 0.3f, p0.x - size * 0.6f, p0.y - size * 1.1f, p0.x, p0.y - size * 0.4f)
                cubicTo(p0.x + size * 0.6f, p0.y - size * 1.1f, p0.x + size * 1.2f, p0.y - size * 0.3f, p0.x, p0.y + size)
            }
            drawScope.drawPath(path, Color(stroke.color))
        }
        "STAMP_FLOWER" -> {
            val size = if (stroke.points.size >= 2) {
                val p1 = stroke.points[1]
                Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
            } else {
                Math.max(30f, stroke.width * 2f)
            }
            val count = 6
            for (i in 0 until count) {
                val angle = i * 2 * Math.PI / count
                val px = p0.x + size * 0.6f * Math.cos(angle).toFloat()
                val py = p0.y + size * 0.6f * Math.sin(angle).toFloat()
                drawScope.drawCircle(Color(stroke.color), radius = size * 0.4f, center = Offset(px, py))
            }
            drawScope.drawCircle(Color.Yellow, radius = size * 0.35f, center = Offset(p0.x, p0.y))
        }
        "STAMP_LEAF" -> {
            val size = if (stroke.points.size >= 2) {
                val p1 = stroke.points[1]
                Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
            } else {
                Math.max(30f, stroke.width * 2f)
            }
            val path = Path().apply {
                moveTo(p0.x, p0.y + size)
                quadraticTo(p0.x - size, p0.y, p0.x, p0.y - size)
                quadraticTo(p0.x + size, p0.y, p0.x, p0.y + size)
            }
            drawScope.drawPath(path, Color(stroke.color))
        }
        "STAMP_SNOWFLAKE" -> {
            val size = if (stroke.points.size >= 2) {
                val p1 = stroke.points[1]
                Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
            } else {
                Math.max(30f, stroke.width * 2f)
            }
            for (i in 0 until 6) {
                val angle = (i * Math.PI / 3).toFloat()
                val endX = p0.x + size * Math.cos(angle.toDouble()).toFloat()
                val endY = p0.y + size * Math.sin(angle.toDouble()).toFloat()
                drawScope.drawLine(Color(stroke.color), start = Offset(p0.x, p0.y), end = Offset(endX, endY), strokeWidth = size * 0.15f)
            }
        }
        "STAMP_CLOUD" -> {
            val size = if (stroke.points.size >= 2) {
                val p1 = stroke.points[1]
                Math.max(16f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
            } else {
                Math.max(20f, stroke.width * 1.5f)
            }
            drawScope.drawCircle(Color(stroke.color), radius = size, center = Offset(p0.x, p0.y))
            drawScope.drawCircle(Color(stroke.color), radius = size * 0.7f, center = Offset(p0.x - size * 0.9f, p0.y + size * 0.2f))
            drawScope.drawCircle(Color(stroke.color), radius = size * 0.7f, center = Offset(p0.x + size * 0.9f, p0.y + size * 0.2f))
        }
        "STAMP_SMILEY" -> {
            val size = if (stroke.points.size >= 2) {
                val p1 = stroke.points[1]
                Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
            } else {
                Math.max(30f, stroke.width * 2f)
            }
            drawScope.drawCircle(Color(stroke.color), radius = size, center = Offset(p0.x, p0.y))
            drawScope.drawCircle(Color.White, radius = size * 0.12f, center = Offset(p0.x - size * 0.35f, p0.y - size * 0.22f))
            drawScope.drawCircle(Color.White, radius = size * 0.12f, center = Offset(p0.x + size * 0.35f, p0.y - size * 0.22f))
            val smilePath = Path().apply {
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        p0.x - size * 0.5f,
                        p0.y - size * 0.2f,
                        p0.x + size * 0.5f,
                        p0.y + size * 0.5f
                    ),
                    startAngleDegrees = 15f,
                    sweepAngleDegrees = 150f,
                    forceMoveTo = true
                )
            }
            drawScope.drawPath(
                path = smilePath,
                color = Color.White,
                style = Stroke(width = size * 0.12f, cap = StrokeCap.Round)
            )
        }
        "STAMP_SUN" -> {
            val size = if (stroke.points.size >= 2) {
                val p1 = stroke.points[1]
                Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
            } else {
                Math.max(30f, stroke.width * 2f)
            }
            drawScope.drawCircle(Color(stroke.color), radius = size * 0.5f, center = Offset(p0.x, p0.y))
            for (i in 0 until 8) {
                val angle = i * Math.PI / 4
                val x1 = p0.x + size * 0.6f * Math.cos(angle).toFloat()
                val y1 = p0.y + size * 0.6f * Math.sin(angle).toFloat()
                val x2 = p0.x + size * 0.95f * Math.cos(angle).toFloat()
                val y2 = p0.y + size * 0.95f * Math.sin(angle).toFloat()
                drawScope.drawLine(Color(stroke.color), start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = size * 0.12f)
            }
        }
        "STAMP_MOON" -> {
            val size = if (stroke.points.size >= 2) {
                val p1 = stroke.points[1]
                Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
            } else {
                Math.max(30f, stroke.width * 2f)
            }
            val path = Path().apply {
                moveTo(p0.x, p0.y - size)
                cubicTo(p0.x - size * 1.3f, p0.y - size * 0.8f, p0.x - size * 1.3f, p0.y + size * 0.8f, p0.x, p0.y + size)
                cubicTo(p0.x - size * 0.6f, p0.y + size * 0.7f, p0.x - size * 0.6f, p0.y - size * 0.7f, p0.x, p0.y - size)
            }
            drawScope.drawPath(path, Color(stroke.color))
        }
    }
}

fun drawBackgroundGuides(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    pattern: CanvasBackgroundPattern,
    width: Float,
    height: Float
) {
    if (pattern == CanvasBackgroundPattern.NONE) return
    val gridColor = Color.LightGray.copy(alpha = 0.22f)
    val strokeWidth = 1f

    when (pattern) {
        CanvasBackgroundPattern.NONE -> {}
        CanvasBackgroundPattern.GRID_CLEAN -> {
            val step = 60f
            // Vertical lines
            var x = 0f
            while (x <= width) {
                drawScope.drawLine(gridColor, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = strokeWidth)
                x += step
            }
            // Horizontal lines
            var y = 0f
            while (y <= height) {
                drawScope.drawLine(gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = strokeWidth)
                y += step
            }
        }
        CanvasBackgroundPattern.GRID_DOTS -> {
            val step = 40f
            var x = step
            while (x < width) {
                var y = step
                while (y < height) {
                    drawScope.drawCircle(gridColor, radius = 2f, center = Offset(x, y))
                    y += step
                }
                x += step
            }
        }
        CanvasBackgroundPattern.GRID_ISOMETRIC -> {
            val step = 60f
            val rad30 = Math.toRadians(30.0)
            val cos30 = Math.cos(rad30).toFloat()
            val sin30 = Math.sin(rad30).toFloat()
            val tan30 = sin30 / cos30

            var x = -height * cos30
            while (x <= width + height * cos30) {
                // Slope to right down
                drawScope.drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x + height / tan30, height),
                    strokeWidth = strokeWidth
                )
                // Slope to left down
                drawScope.drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x - height / tan30, height),
                    strokeWidth = strokeWidth
                )
                x += step
            }
        }
        CanvasBackgroundPattern.GRID_RADIAL -> {
            val cx = width / 2f
            val cy = height / 2f
            for (r in 100..800 step 100) {
                drawScope.drawCircle(
                    color = gridColor,
                    radius = r.toFloat(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f))
                )
            }
        }
        CanvasBackgroundPattern.TEXTURE_PAPER -> {
            val rand = java.util.Random(42)
            for (i in 0 until 180) {
                val x1 = rand.nextFloat() * width
                val y1 = rand.nextFloat() * height
                val len = 10f + rand.nextFloat() * 25f
                val horiz = rand.nextBoolean()
                if (horiz) {
                    drawScope.drawLine(
                        color = Color.DarkGray.copy(alpha = 0.05f),
                        start = Offset(x1, y1),
                        end = Offset(x1 + len, y1),
                        strokeWidth = 1f
                    )
                } else {
                    drawScope.drawLine(
                        color = Color.DarkGray.copy(alpha = 0.05f),
                        start = Offset(x1, y1),
                        end = Offset(x1, y1 + len),
                        strokeWidth = 1f
                    )
                }
            }
        }
    }
}

// Full, robust Custom Color Picker Dialog
@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var r by remember { mutableFloatStateOf(initialColor.red * 255f) }
    var g by remember { mutableFloatStateOf(initialColor.green * 255f) }
    var b by remember { mutableFloatStateOf(initialColor.blue * 255f) }

    val currentColor = remember(r, g, b) {
        Color(r.toInt(), g.toInt(), b.toInt())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Custom Color Composer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Color Preview circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )

                // Hex text
                Text(
                    text = "Hex Color: #${String.format("%02X%02X%02X", r.toInt(), g.toInt(), b.toInt())}",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Red channel
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Red Channel", style = MaterialTheme.typography.bodySmall)
                        Text(text = "${r.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = r,
                        onValueChange = { r = it },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red.copy(alpha = 0.5f))
                    )
                }

                // Green channel
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Green Channel", style = MaterialTheme.typography.bodySmall)
                        Text(text = "${g.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = g,
                        onValueChange = { g = it },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green.copy(alpha = 0.5f))
                    )
                }

                // Blue channel
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Blue Channel", style = MaterialTheme.typography.bodySmall)
                        Text(text = "${b.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = b,
                        onValueChange = { b = it },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue.copy(alpha = 0.5f))
                    )
                }

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onColorSelected(currentColor) }
                    ) {
                        Text("Select Color")
                    }
                }
            }
        }
    }
}
