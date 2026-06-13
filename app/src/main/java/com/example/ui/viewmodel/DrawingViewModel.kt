package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.DrawingEntity
import com.example.data.model.DrawingStroke
import com.example.data.model.DrawingTool
import com.example.data.model.SymmetryMode
import com.example.data.model.StrokePoint
import com.example.data.model.StrokeSerializer
import com.example.data.model.CanvasBackgroundPattern
import com.example.data.repository.DrawingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DrawingViewModel(
    private val repository: DrawingRepository
) : ViewModel() {

    // Gallery List state
    val drawingsList: StateFlow<List<DrawingEntity>> = repository.allDrawings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Active Workspace States
    private val _currentDrawingId = MutableStateFlow(0L)
    val currentDrawingId = _currentDrawingId.asStateFlow()

    private val _currentDrawingTitle = MutableStateFlow("Untitled Drawing")
    val currentDrawingTitle = _currentDrawingTitle.asStateFlow()

    private val _strokes = MutableStateFlow<List<DrawingStroke>>(emptyList())
    val strokes = _strokes.asStateFlow()

    private val _currentStroke = MutableStateFlow<DrawingStroke?>(null)
    val currentStroke = _currentStroke.asStateFlow()

    // Layer Workspace States (Background, Linework, Detailing, etc.)
    private val _layers = MutableStateFlow<List<String>>(listOf("Layer 1 (Background)", "Layer 2 (Linework)", "Layer 3 (Detailing)"))
    val layers = _layers.asStateFlow()

    private val _activeLayer = MutableStateFlow<String>("Layer 2 (Linework)")
    val activeLayer = _activeLayer.asStateFlow()

    private val _hiddenLayers = MutableStateFlow<Set<String>>(emptySet())
    val hiddenLayers = _hiddenLayers.asStateFlow()

    private val _layerOpacities = MutableStateFlow<Map<String, Float>>(emptyMap())
    val layerOpacities = _layerOpacities.asStateFlow()

    fun selectLayer(layerId: String) {
        _activeLayer.value = layerId
    }

    fun toggleLayerVisibility(layerId: String) {
        val currentSet = _hiddenLayers.value.toMutableSet()
        if (currentSet.contains(layerId)) {
            currentSet.remove(layerId)
        } else {
            currentSet.add(layerId)
        }
        _hiddenLayers.value = currentSet
        autoSaveDraft()
    }

    fun setLayerOpacity(layerId: String, opacity: Float) {
        val currentMap = _layerOpacities.value.toMutableMap()
        currentMap[layerId] = opacity.coerceIn(0.0f, 1.0f)
        _layerOpacities.value = currentMap
        autoSaveDraft()
    }

    fun addNewLayer(name: String) {
        val currentList = _layers.value.toMutableList()
        val uniqueName = if (currentList.contains(name)) "$name (${currentList.size + 1})" else name
        currentList.add(uniqueName)
        _layers.value = currentList
        _activeLayer.value = uniqueName
        autoSaveDraft()
    }

    fun deleteLayer(layerId: String) {
        val currentList = _layers.value.toMutableList()
        if (currentList.size > 1) {
            currentList.remove(layerId)
            _layers.value = currentList
            if (_activeLayer.value == layerId) {
                _activeLayer.value = currentList.last()
            }
            // Filter out strokes that belong to the deleted layer
            _strokes.value = _strokes.value.filter { it.layerId != layerId }
            autoSaveDraft()
        }
    }

    fun renameLayer(oldId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == oldId) return
        val currentList = _layers.value.toMutableList()
        val index = currentList.indexOf(oldId)
        if (index != -1) {
            val uniqueName = if (currentList.contains(trimmed)) "$trimmed (${currentList.size + 1})" else trimmed
            currentList[index] = uniqueName
            _layers.value = currentList
            if (_activeLayer.value == oldId) {
                _activeLayer.value = uniqueName
            }
            // Update all strokes on oldId to point to uniqueName
            _strokes.value = _strokes.value.map {
                if (it.layerId == oldId) it.copy(layerId = uniqueName) else it
            }
            // Update opacities and hidden references
            val opacities = _layerOpacities.value.toMutableMap()
            if (opacities.containsKey(oldId)) {
                val o = opacities.remove(oldId)!!
                opacities[uniqueName] = o
                _layerOpacities.value = opacities
            }
            val hidden = _hiddenLayers.value.toMutableSet()
            if (hidden.contains(oldId)) {
                hidden.remove(oldId)
                hidden.add(uniqueName)
                _hiddenLayers.value = hidden
            }
            autoSaveDraft()
        }
    }

    // Undo & Redo Stacks
    private val undoHistory = mutableListOf<List<DrawingStroke>>()
    private val redoHistory = mutableListOf<List<DrawingStroke>>()

    // Symmetric Configuration Modes
    private val _symmetryMode = MutableStateFlow(SymmetryMode.NONE)
    val symmetryMode = _symmetryMode.asStateFlow()

    // Shapes & Standard Drawing Tools selection
    private val _currentTool = MutableStateFlow(DrawingTool.FREEHAND)
    val currentTool = _currentTool.asStateFlow()

    private val _activeCustomPluginTool = MutableStateFlow<String?>(null)
    val activeCustomPluginTool = _activeCustomPluginTool.asStateFlow()

    // Smart Custom Color history records
    private val _colorHistory = MutableStateFlow<List<Int>>(emptyList())
    val colorHistory = _colorHistory.asStateFlow()

    // Toolbar configurations
    private val _brushColor = MutableStateFlow(0xFFED2939.toInt()) // Standard crimson red
    val brushColor = _brushColor.asStateFlow()

    private val _brushWidth = MutableStateFlow(12f)
    val brushWidth = _brushWidth.asStateFlow()

    private val _isEraser = MutableStateFlow(false)
    val isEraser = _isEraser.asStateFlow()

    // Zoom and Pan States
    private val _scale = MutableStateFlow(1f)
    val scale = _scale.asStateFlow()

    private val _panOffset = MutableStateFlow(Offset.Zero)
    val panOffset = _panOffset.asStateFlow()

    private val _isZoomPanEnabled = MutableStateFlow(false)
    val isZoomPanEnabled = _isZoomPanEnabled.asStateFlow()

    // Canvas Background Color (always White by default)
    private val _canvasBackgroundColor = MutableStateFlow(0xFFFFFFFF.toInt())
    val canvasBackgroundColor = _canvasBackgroundColor.asStateFlow()

    // Background Grid Pattern Visualization Choice
    private val _backgroundPattern = MutableStateFlow(CanvasBackgroundPattern.NONE)
    val backgroundPattern = _backgroundPattern.asStateFlow()

    // Custom text stamp literal value
    private val _textStampValue = MutableStateFlow("Zen")
    val textStampValue = _textStampValue.asStateFlow()

    // Screen Canvas Width and Height (updated dynamically in UI)
    private var canvasWidth = 1080
    private var canvasHeight = 1080

    fun updateCanvasSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            canvasWidth = width
            canvasHeight = height
        }
    }

    // Symmetry control methods
    fun setSymmetryMode(mode: SymmetryMode) {
        _symmetryMode.value = mode
    }

    // Brush Tools selection controls
    fun setDrawingTool(tool: DrawingTool) {
        _currentTool.value = tool
        _activeCustomPluginTool.value = null
        _isEraser.value = false
    }

    fun selectCustomPluginTool(pluginName: String?) {
        _activeCustomPluginTool.value = pluginName
        if (pluginName != null) {
            _isEraser.value = false
        }
    }

    // Background Pattern guide configuration setter
    fun setBackgroundPattern(pattern: CanvasBackgroundPattern) {
        _backgroundPattern.value = pattern
    }

    // Text Stamp Literal custom value setter
    fun setTextStampValue(value: String) {
        _textStampValue.value = value
    }

    // Color controls
    fun setBrushColor(color: Int) {
        _brushColor.value = color
        _isEraser.value = false
        addColorToHistory(color)
    }

    fun setBrushWidth(width: Float) {
        _brushWidth.value = width
    }

    fun toggleEraser() {
        _isEraser.value = !_isEraser.value
    }

    fun toggleZoomPanEnabled() {
        _isZoomPanEnabled.value = !_isZoomPanEnabled.value
    }

    fun resetZoomPan() {
        _scale.value = 1f
        _panOffset.value = Offset.Zero
    }

    fun updateZoomPan(scaleChange: Float, panChange: Offset) {
        _scale.value = (_scale.value * scaleChange).coerceIn(0.5f, 10.0f)
        _panOffset.value += panChange
    }

    private fun addColorToHistory(color: Int) {
        val current = _colorHistory.value.toMutableList()
        current.remove(color) // Avoid duplicates
        current.add(0, color)
        if (current.size > 8) {
            current.removeAt(current.lastIndex)
        }
        _colorHistory.value = current
    }

    // Calculate symmetrical reflections of individual strokes on canvas
    fun getSymmetricStrokes(stroke: DrawingStroke): List<DrawingStroke> {
        val list = mutableListOf<DrawingStroke>()
        list.add(stroke) // Original master stroke

        val mode = _symmetryMode.value
        if (mode == SymmetryMode.NONE) return list

        val cx = canvasWidth / 2f
        val cy = canvasHeight / 2f

        when (mode) {
            SymmetryMode.NONE -> {}
            SymmetryMode.HORIZONTAL -> {
                val mirrored = stroke.points.map { StrokePoint(2f * cx - it.x, it.y) }
                list.add(stroke.copy(points = mirrored))
            }
            SymmetryMode.VERTICAL -> {
                val mirrored = stroke.points.map { StrokePoint(it.x, 2f * cy - it.y) }
                list.add(stroke.copy(points = mirrored))
            }
            SymmetryMode.FOUR_WAY -> {
                // Horizontal Mirror
                list.add(stroke.copy(points = stroke.points.map { StrokePoint(2f * cx - it.x, it.y) }))
                // Vertical Mirror
                list.add(stroke.copy(points = stroke.points.map { StrokePoint(it.x, 2f * cy - it.y) }))
                // Quad Mirror (Diagonal cross)
                list.add(stroke.copy(points = stroke.points.map { StrokePoint(2f * cx - it.x, 2f * cy - it.y) }))
            }
            SymmetryMode.RADIAL_6, SymmetryMode.RADIAL_8, SymmetryMode.RADIAL_12 -> {
                val sectors = when (mode) {
                    SymmetryMode.RADIAL_6 -> 6
                    SymmetryMode.RADIAL_8 -> 8
                    else -> 12
                }
                for (i in 1 until sectors) {
                    val angleRad = (2.0 * Math.PI * i) / sectors
                    val cosA = Math.cos(angleRad).toFloat()
                    val sinA = Math.sin(angleRad).toFloat()

                    val rotatedPoints = stroke.points.map { p ->
                        val dx = p.x - cx
                        val dy = p.y - cy
                        val rx = dx * cosA - dy * sinA
                        val ry = dx * sinA + dy * cosA
                        StrokePoint(cx + rx, cy + ry)
                    }
                    list.add(stroke.copy(points = rotatedPoints))
                }
            }
        }
        return list
    }

    private fun isDiscreteShapeOrStamp(tool: DrawingTool): Boolean {
        return tool != DrawingTool.FREEHAND &&
               tool != DrawingTool.BRUSH_CALLIGRAPHY &&
               tool != DrawingTool.BRUSH_NEON &&
               tool != DrawingTool.BRUSH_RAINBOW &&
               tool != DrawingTool.BRUSH_MARKER &&
               tool != DrawingTool.BRUSH_AIRBRUSH &&
               tool != DrawingTool.BRUSH_SHADOW
    }

    // Input touch gestures registration
    fun startNewStroke(startPoint: StrokePoint) {
        if (_isZoomPanEnabled.value) return
        
        saveToUndoHistory()
        redoHistory.clear()

        val color = if (_isEraser.value) _canvasBackgroundColor.value else _brushColor.value
        val tool = _currentTool.value
        val shapeTypeStr = if (_activeCustomPluginTool.value != null) {
            "PLUGIN_BRUSH:" + _activeCustomPluginTool.value
        } else if (tool == DrawingTool.STAMP_TEXT) {
            "STAMP_TEXT:" + _textStampValue.value
        } else {
            tool.name
        }

        _currentStroke.value = DrawingStroke(
            color = color,
            width = _brushWidth.value,
            points = listOf(startPoint),
            isEraser = _isEraser.value,
            shapeType = shapeTypeStr,
            layerId = _activeLayer.value
        )
    }

    fun appendPointToCurrentStroke(point: StrokePoint) {
        if (_isZoomPanEnabled.value) return
        val current = _currentStroke.value ?: return

        val isShape = isDiscreteShapeOrStamp(_currentTool.value)
        val updatedPoints = if (isShape) {
            // For standard vector shapes, hold only the starting origin and sizing end point
            listOf(current.points.first(), point)
        } else {
            // Otherwise, paint accumulates dragging segments
            current.points + point
        }

        _currentStroke.value = current.copy(points = updatedPoints)
    }

    fun completeCurrentStroke() {
        if (_isZoomPanEnabled.value) return
        val current = _currentStroke.value ?: return
        if (current.points.isNotEmpty()) {
            // Translate the master stroke into symmetry groupings
            val symmetricGroup = getSymmetricStrokes(current)
            _strokes.value = _strokes.value + symmetricGroup
        }
        _currentStroke.value = null
        
        autoSaveDraft()
    }

    // Clear Screen
    fun clearCanvas() {
        saveToUndoHistory()
        redoHistory.clear()
        _strokes.value = emptyList()
        _currentStroke.value = null
        autoSaveDraft()
    }

    // Undo & Redo actions
    fun undo() {
        if (undoHistory.isNotEmpty()) {
            redoHistory.add(_strokes.value)
            _strokes.value = undoHistory.removeAt(undoHistory.lastIndex)
            autoSaveDraft()
        }
    }

    fun redo() {
        if (redoHistory.isNotEmpty()) {
            undoHistory.add(_strokes.value)
            _strokes.value = redoHistory.removeAt(redoHistory.lastIndex)
            autoSaveDraft()
        }
    }

    private fun saveToUndoHistory() {
        if (undoHistory.size >= 50) {
            undoHistory.removeAt(0)
        }
        undoHistory.add(_strokes.value.toList())
    }

    // Direct Database saves/loads
    fun startNewDrawing() {
        _currentDrawingId.value = 0L
        _currentDrawingTitle.value = "Untitled Drawing"
        _strokes.value = emptyList()
        _currentStroke.value = null
        _symmetryMode.value = SymmetryMode.NONE
        _currentTool.value = DrawingTool.FREEHAND
        _layers.value = listOf("Layer 1 (Background)", "Layer 2 (Linework)", "Layer 3 (Detailing)")
        _activeLayer.value = "Layer 2 (Linework)"
        _hiddenLayers.value = emptySet()
        _layerOpacities.value = emptyMap()
        undoHistory.clear()
        redoHistory.clear()
        resetZoomPan()
    }

    fun loadDrawing(drawingId: Long) {
        viewModelScope.launch {
            val drawing = repository.getDrawingById(drawingId) ?: return@launch
            _currentDrawingId.value = drawing.id
            _currentDrawingTitle.value = drawing.title
            val loadedStrokes = StrokeSerializer.deserialize(drawing.strokesJson)
            _strokes.value = loadedStrokes
            _currentStroke.value = null
            _symmetryMode.value = SymmetryMode.NONE
            _currentTool.value = DrawingTool.FREEHAND
            
            val uniqueLayers = loadedStrokes.map { it.layerId }.distinct()
            if (uniqueLayers.isNotEmpty()) {
                _layers.value = uniqueLayers
                _activeLayer.value = uniqueLayers.last()
            } else {
                _layers.value = listOf("Layer 1 (Background)", "Layer 2 (Linework)", "Layer 3 (Detailing)")
                _activeLayer.value = "Layer 2 (Linework)"
            }
            _hiddenLayers.value = emptySet()
            _layerOpacities.value = emptyMap()

            undoHistory.clear()
            redoHistory.clear()
            resetZoomPan()
        }
    }

    fun renameDrawing(id: Long, newTitle: String, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateDrawingTitle(id, newTitle)
            if (_currentDrawingId.value == id) {
                _currentDrawingTitle.value = newTitle
            }
            onFinished()
        }
    }

    fun deleteDrawing(id: Long, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteDrawing(id)
            if (_currentDrawingId.value == id) {
                startNewDrawing()
            }
            onFinished()
        }
    }

    private fun autoSaveDraft() {
        val title = _currentDrawingTitle.value
        val strokesList = _strokes.value
        val id = _currentDrawingId.value
        
        viewModelScope.launch {
            val strokesJson = StrokeSerializer.serialize(strokesList)
            val bitmap = generateImageRepresentation()
            val savedId = repository.saveDrawing(id, title, strokesJson, bitmap)
            _currentDrawingId.value = savedId
        }
    }

    fun saveDrawingWithCustomTitle(title: String, onFinished: (Long) -> Unit) {
        _currentDrawingTitle.value = title
        val strokesList = _strokes.value
        val id = _currentDrawingId.value
        
        viewModelScope.launch {
            val strokesJson = StrokeSerializer.serialize(strokesList)
            val bitmap = generateImageRepresentation()
            val savedId = repository.saveDrawing(id, title, strokesJson, bitmap)
            _currentDrawingId.value = savedId
            onFinished(savedId)
        }
    }

    fun triggerManualExport(onSuccess: (String) -> Unit, onFailure: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                val bitmap = generateImageRepresentation()
                val result = repository.exportToPublicGallery(bitmap, _currentDrawingTitle.value)
                result.fold(
                    onSuccess = { onSuccess(it) },
                    onFailure = { onFailure(it) }
                )
            } catch (e: Exception) {
                onFailure(e)
            }
        }
    }

    fun shareCurrentDrawing(onReadyToShare: (Uri) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val bitmap = generateImageRepresentation()
            val uri = repository.getShareUriForBitmap(bitmap)
            if (uri != null) {
                onReadyToShare(uri)
            } else {
                onError()
            }
        }
    }

    // Off-screen Vector paint rasterizer (Draw shapes dynamically to standard PNG bitmaps)
    fun generateImageRepresentation(): Bitmap {
        val width = if (canvasWidth > 0) canvasWidth else 1080
        val height = if (canvasHeight > 0) canvasHeight else 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Draw Clean Canvas Background Paper
        canvas.drawColor(_canvasBackgroundColor.value)

        val strokePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }

        val fillPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.FILL
        }

        fun drawIndividualStroke(stroke: DrawingStroke) {
            if (stroke.points.isEmpty()) return
            val p0 = stroke.points[0]
            val shapeType = stroke.shapeType

            // Detect Custom Text Stamp
            if (shapeType.startsWith("STAMP_TEXT:")) {
                val text = shapeType.substringAfter("STAMP_TEXT:")
                val textPaint = android.graphics.Paint().apply {
                    color = stroke.color
                    textSize = Math.max(24f, stroke.width * 2f)
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    style = android.graphics.Paint.Style.FILL
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                canvas.drawText(text, p0.x, p0.y, textPaint)
                return
            }

            // Detect special brushes & shapes
            when (shapeType) {
                "FREEHAND" -> {
                    strokePaint.color = stroke.color
                    strokePaint.strokeWidth = stroke.width
                    strokePaint.alpha = 255
                    if (stroke.points.size < 2) {
                        canvas.drawPoint(p0.x, p0.y, strokePaint)
                    } else {
                        val path = android.graphics.Path()
                        path.moveTo(p0.x, p0.y)
                        for (i in 1 until stroke.points.size) {
                            path.lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                        canvas.drawPath(path, strokePaint)
                    }
                }
                "BRUSH_MARKER" -> {
                    strokePaint.color = stroke.color
                    strokePaint.strokeWidth = stroke.width
                    strokePaint.alpha = 100 // semi-transparent marker hue
                    if (stroke.points.size < 2) {
                        canvas.drawPoint(p0.x, p0.y, strokePaint)
                    } else {
                        val path = android.graphics.Path()
                        path.moveTo(p0.x, p0.y)
                        for (i in 1 until stroke.points.size) {
                            path.lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                        canvas.drawPath(path, strokePaint)
                    }
                }
                "BRUSH_SHADOW" -> {
                    // Draw offset shadow line
                    val shadowPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        color = android.graphics.Color.DKGRAY
                        alpha = 80
                        strokeWidth = stroke.width
                    }
                    if (stroke.points.size >= 2) {
                        val path = android.graphics.Path()
                        path.moveTo(p0.x + 8f, p0.y + 8f)
                        for (i in 1 until stroke.points.size) {
                            path.lineTo(stroke.points[i].x + 8f, stroke.points[i].y + 8f)
                        }
                        canvas.drawPath(path, shadowPaint)
                    }
                    // Draw main line
                    strokePaint.color = stroke.color
                    strokePaint.strokeWidth = stroke.width
                    strokePaint.alpha = 255
                    if (stroke.points.size < 2) {
                        canvas.drawPoint(p0.x, p0.y, strokePaint)
                    } else {
                        val path = android.graphics.Path()
                        path.moveTo(p0.x, p0.y)
                        for (i in 1 until stroke.points.size) {
                            path.lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                        canvas.drawPath(path, strokePaint)
                    }
                }
                "BRUSH_NEON" -> {
                    // Outer diffuse glow layer
                    val glowPaint2 = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        color = stroke.color
                        alpha = 80
                        strokeWidth = stroke.width * 2.2f
                    }
                    // Middle aura layer
                    val glowPaint1 = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        color = stroke.color
                        alpha = 180
                        strokeWidth = stroke.width * 1.2f
                    }
                    // Center white core
                    val corePaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        color = android.graphics.Color.WHITE
                        strokeWidth = stroke.width * 0.35f
                    }

                    if (stroke.points.size >= 2) {
                        val path = android.graphics.Path()
                        path.moveTo(p0.x, p0.y)
                        for (i in 1 until stroke.points.size) {
                            path.lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                        canvas.drawPath(path, glowPaint2)
                        canvas.drawPath(path, glowPaint1)
                        canvas.drawPath(path, corePaint)
                    }
                }
                "BRUSH_RAINBOW" -> {
                    for (i in 1 until stroke.points.size) {
                        val pStart = stroke.points[i - 1]
                        val pEnd = stroke.points[i]
                        val hue = (i * 12f) % 360f
                        val colorHsv = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                        val rainbowPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeCap = android.graphics.Paint.Cap.ROUND
                            strokeJoin = android.graphics.Paint.Join.ROUND
                            color = colorHsv
                            strokeWidth = stroke.width
                        }
                        canvas.drawLine(pStart.x, pStart.y, pEnd.x, pEnd.y, rainbowPaint)
                    }
                }
                "BRUSH_CALLIGRAPHY" -> {
                    val ribbonPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.SQUARE
                        color = stroke.color
                        strokeWidth = 2.5f
                    }
                    val offset = stroke.width / 2f
                    for (p in stroke.points) {
                        canvas.drawLine(p.x - offset, p.y + offset, p.x + offset, p.y - offset, ribbonPaint)
                    }
                }
                "BRUSH_AIRBRUSH" -> {
                    val airPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.FILL
                        color = stroke.color
                        alpha = 110
                    }
                    val rGen = java.util.Random(12345)
                    for (p in stroke.points) {
                        for (k in 0 until 9) {
                            val r = rGen.nextFloat() * stroke.width
                            val theta = rGen.nextFloat() * 2f * Math.PI.toFloat()
                            val dx = r * Math.cos(theta.toDouble()).toFloat()
                            val dy = r * Math.sin(theta.toDouble()).toFloat()
                            val sizeDot = 1.5f + rGen.nextFloat() * 2.5f
                            canvas.drawCircle(p.x + dx, p.y + dy, sizeDot, airPaint)
                        }
                    }
                }
                "LINE" -> {
                    if (stroke.points.size >= 2) {
                        strokePaint.color = stroke.color
                        strokePaint.strokeWidth = stroke.width
                        strokePaint.alpha = 255
                        val p1 = stroke.points[1]
                        canvas.drawLine(p0.x, p0.y, p1.x, p1.y, strokePaint)
                    }
                }
                "RECTANGLE_OUTLINE", "RECTANGLE_FILLED" -> {
                    if (stroke.points.size >= 2) {
                        val p1 = stroke.points[1]
                        val left = minOf(p0.x, p1.x)
                        val top = minOf(p0.y, p1.y)
                        val right = maxOf(p0.x, p1.x)
                        val bottom = maxOf(p0.y, p1.y)

                        if (shapeType == "RECTANGLE_FILLED") {
                            fillPaint.color = stroke.color
                            canvas.drawRect(left, top, right, bottom, fillPaint)
                        } else {
                            strokePaint.color = stroke.color
                            strokePaint.strokeWidth = stroke.width
                            strokePaint.alpha = 255
                            canvas.drawRect(left, top, right, bottom, strokePaint)
                        }
                    }
                }
                "CIRCLE_OUTLINE", "CIRCLE_FILLED" -> {
                    if (stroke.points.size >= 2) {
                        val p1 = stroke.points[1]
                        val radius = Math.sqrt(
                            ((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()
                        ).toFloat()

                        if (shapeType == "CIRCLE_FILLED") {
                            fillPaint.color = stroke.color
                            canvas.drawCircle(p0.x, p0.y, radius, fillPaint)
                        } else {
                            strokePaint.color = stroke.color
                            strokePaint.strokeWidth = stroke.width
                            strokePaint.alpha = 255
                            canvas.drawCircle(p0.x, p0.y, radius, strokePaint)
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
                    val path = android.graphics.Path()
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) size else size * 0.4f
                        val angle = i * Math.PI / 5 - Math.PI / 2
                        val x = p0.x + r * Math.cos(angle).toFloat()
                        val y = p0.y + r * Math.sin(angle).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    fillPaint.color = stroke.color
                    canvas.drawPath(path, fillPaint)
                }
                "STAMP_HEART" -> {
                    val size = if (stroke.points.size >= 2) {
                        val p1 = stroke.points[1]
                        Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
                    } else {
                        Math.max(30f, stroke.width * 2f)
                    }
                    val path = android.graphics.Path()
                    path.moveTo(p0.x, p0.y + size)
                    path.cubicTo(p0.x - size * 1.2f, p0.y - size * 0.3f, p0.x - size * 0.6f, p0.y - size * 1.1f, p0.x, p0.y - size * 0.4f)
                    path.cubicTo(p0.x + size * 0.6f, p0.y - size * 1.1f, p0.x + size * 1.2f, p0.y - size * 0.3f, p0.x, p0.y + size)
                    fillPaint.color = stroke.color
                    canvas.drawPath(path, fillPaint)
                }
                "STAMP_FLOWER" -> {
                    val size = if (stroke.points.size >= 2) {
                        val p1 = stroke.points[1]
                        Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
                    } else {
                        Math.max(30f, stroke.width * 2f)
                    }
                    fillPaint.color = stroke.color
                    val count = 6
                    for (i in 0 until count) {
                        val angle = i * 2 * Math.PI / count
                        val px = p0.x + size * 0.6f * Math.cos(angle).toFloat()
                        val py = p0.y + size * 0.6f * Math.sin(angle).toFloat()
                        canvas.drawCircle(px, py, size * 0.4f, fillPaint)
                    }
                    fillPaint.color = android.graphics.Color.YELLOW
                    canvas.drawCircle(p0.x, p0.y, size * 0.35f, fillPaint)
                }
                "STAMP_LEAF" -> {
                    val size = if (stroke.points.size >= 2) {
                        val p1 = stroke.points[1]
                        Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
                    } else {
                        Math.max(30f, stroke.width * 2f)
                    }
                    val path = android.graphics.Path()
                    path.moveTo(p0.x, p0.y + size)
                    path.quadTo(p0.x - size, p0.y, p0.x, p0.y - size)
                    path.quadTo(p0.x + size, p0.y, p0.x, p0.y + size)
                    fillPaint.color = stroke.color
                    canvas.drawPath(path, fillPaint)
                }
                "STAMP_SNOWFLAKE" -> {
                    val size = if (stroke.points.size >= 2) {
                        val p1 = stroke.points[1]
                        Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
                    } else {
                        Math.max(30f, stroke.width * 2f)
                    }
                    val flakePaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = stroke.color
                        strokeWidth = size * 0.15f
                    }
                    for (i in 0 until 6) {
                        val angle = (i * Math.PI / 3).toFloat()
                        val endX = p0.x + size * Math.cos(angle.toDouble()).toFloat()
                        val endY = p0.y + size * Math.sin(angle.toDouble()).toFloat()
                        canvas.drawLine(p0.x, p0.y, endX, endY, flakePaint)
                    }
                }
                "STAMP_CLOUD" -> {
                    val size = if (stroke.points.size >= 2) {
                        val p1 = stroke.points[1]
                        Math.max(16f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
                    } else {
                        Math.max(20f, stroke.width * 1.5f)
                    }
                    fillPaint.color = stroke.color
                    canvas.drawCircle(p0.x, p0.y, size, fillPaint)
                    canvas.drawCircle(p0.x - size * 0.9f, p0.y + size * 0.2f, size * 0.7f, fillPaint)
                    canvas.drawCircle(p0.x + size * 0.9f, p0.y + size * 0.2f, size * 0.7f, fillPaint)
                }
                "STAMP_SMILEY" -> {
                    val size = if (stroke.points.size >= 2) {
                        val p1 = stroke.points[1]
                        Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
                    } else {
                        Math.max(30f, stroke.width * 2f)
                    }
                    fillPaint.color = stroke.color
                    canvas.drawCircle(p0.x, p0.y, size, fillPaint)
                    
                    val smileyPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        color = android.graphics.Color.WHITE
                        strokeWidth = size * 0.12f
                        strokeCap = android.graphics.Paint.Cap.ROUND
                    }
                    val eyePaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.WHITE
                        style = android.graphics.Paint.Style.FILL
                    }
                    canvas.drawCircle(p0.x - size * 0.35f, p0.y - size * 0.22f, size * 0.12f, eyePaint)
                    canvas.drawCircle(p0.x + size * 0.35f, p0.y - size * 0.22f, size * 0.12f, eyePaint)
                    val rectF = android.graphics.RectF(p0.x - size * 0.5f, p0.y - size * 0.2f, p0.x + size * 0.5f, p0.y + size * 0.5f)
                    canvas.drawArc(rectF, 15f, 150f, false, smileyPaint)
                }
                "STAMP_SUN" -> {
                    val size = if (stroke.points.size >= 2) {
                        val p1 = stroke.points[1]
                        Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
                    } else {
                        Math.max(30f, stroke.width * 2f)
                    }
                    fillPaint.color = stroke.color
                    canvas.drawCircle(p0.x, p0.y, size * 0.5f, fillPaint)
                    val rayPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = stroke.color
                        strokeWidth = size * 0.12f
                    }
                    for (i in 0 until 8) {
                        val angle = i * Math.PI / 4
                        val x1 = p0.x + size * 0.6f * Math.cos(angle).toFloat()
                        val y1 = p0.y + size * 0.6f * Math.sin(angle).toFloat()
                        val x2 = p0.x + size * 0.95f * Math.cos(angle).toFloat()
                        val y2 = p0.y + size * 0.95f * Math.sin(angle).toFloat()
                        canvas.drawLine(x1, y1, x2, y2, rayPaint)
                    }
                }
                "STAMP_MOON" -> {
                    val size = if (stroke.points.size >= 2) {
                        val p1 = stroke.points[1]
                        Math.max(24f, Math.sqrt(((p1.x - p0.x) * (p1.x - p0.x) + (p1.y - p0.y) * (p1.y - p0.y)).toDouble()).toFloat())
                    } else {
                        Math.max(30f, stroke.width * 2f)
                    }
                    val path = android.graphics.Path()
                    path.moveTo(p0.x, p0.y - size)
                    path.cubicTo(p0.x - size * 1.3f, p0.y - size * 0.8f, p0.x - size * 1.3f, p0.y + size * 0.8f, p0.x, p0.y + size)
                    path.cubicTo(p0.x - size * 0.6f, p0.y + size * 0.7f, p0.x - size * 0.6f, p0.y - size * 0.7f, p0.x, p0.y - size)
                    fillPaint.color = stroke.color
                    canvas.drawPath(path, fillPaint)
                }
            }
        }

        // 1. Draw completed canvas lines
        for (stroke in _strokes.value) {
            drawIndividualStroke(stroke)
        }

        // 2. Render master active stroke & its instant symmetry reflections
        val active = _currentStroke.value
        if (active != null) {
            val radialGroup = getSymmetricStrokes(active)
            for (stroke in radialGroup) {
                drawIndividualStroke(stroke)
            }
        }

        return bitmap
    }
}
