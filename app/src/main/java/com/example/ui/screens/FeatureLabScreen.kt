package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeatureDatabase
import com.example.data.model.SpeculativeFeature
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureLabScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Query states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var filterFavoritesOnly by remember { mutableStateOf(false) }
    var filterSpeculativeOnly by remember { mutableStateOf(false) }

    // Dynamic Loaded tools database (Mutable list initialized from our companion object)
    val dynamicCustomFeatures = remember { mutableStateListOf<SpeculativeFeature>() }

    // Aggregate everything
    val allFeaturesList = remember {
        FeatureDatabase.allFeatures
    }

    val filteredList = remember(searchQuery, selectedCategory, filterFavoritesOnly, filterSpeculativeOnly, dynamicCustomFeatures.size) {
        val mergedList = allFeaturesList + dynamicCustomFeatures
        mergedList.filter { feature ->
            val matchesQuery = feature.name.contains(searchQuery, ignoreCase = true) || 
                               feature.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || feature.category == selectedCategory
            val matchesFavorites = !filterFavoritesOnly || feature.isFavorite
            val matchesSpeculative = !filterSpeculativeOnly || feature.isSpeculative

            matchesQuery && matchesCategory && matchesFavorites && matchesSpeculative
        }
    }

    // Dynamic Sandbox / Simulation states
    var activeSimulatingFeature by remember { mutableStateOf<SpeculativeFeature?>(null) }
    var simulationProgress by remember { mutableFloatStateOf(0f) }
    var simulationLog by remember { mutableStateOf("") }
    var isSimulating by remember { mutableStateOf(false) }

    // Custom asset custom JSON uploader state
    var showCustomToolImporterDialog by remember { mutableStateOf(false) }
    var uInputToolName by remember { mutableStateOf("") }
    var uInputCategory by remember { mutableStateOf(FeatureDatabase.categories[0]) }
    var uInputDescription by remember { mutableStateOf("") }

    // Functions to trigger simulation compiler
    val triggerSimulatedBuild: (SpeculativeFeature) -> Unit = { feature ->
        if (!isSimulating) {
            isSimulating = true
            activeSimulatingFeature = feature
            simulationProgress = 0f
            simulationLog = "Initializing dynamic linker core..."
            scope.launch {
                delay(400)
                simulationProgress = 0.25f
                simulationLog = "Checking GPU Vulkan compute compatibility..."
                delay(500)
                simulationProgress = 0.55f
                simulationLog = "Mapping vector nodes & AI weight thresholds..."
                delay(400)
                simulationProgress = 0.85f
                simulationLog = "Linking local symbol [${feature.name.replace(" ", "_")}]..."
                delay(300)
                simulationProgress = 1f
                simulationLog = "Succesfully bound & compiled: App architecture updated live!"
                delay(600)
                feature.isActivated = true
                isSimulating = false
                Toast.makeText(context, "${feature.name} is now loaded!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Speculative Tool Sandbox",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "10,000+ Pluggable Features Architectural Lab",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("lab_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to main app")
                    }
                },
                actions = {
                    // Simulated import dynamic plugin button
                    IconButton(
                        onClick = { showCustomToolImporterDialog = true },
                        modifier = Modifier.testTag("import_dynamic_plugin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload Custom Plugin Asset",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
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
            
            // Lab Stats Panel - High contrast dark glowing gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚀 SYSTEM SCALABILITY STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "API PLUGGED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 9.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unified Pluggable Design Index is active. There are ${allFeaturesList.size + dynamicCustomFeatures.size} unique modules listed dynamically. Select, search, or group any tool below to simulate live compilation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        lineHeight = 15.sp
                    )
                }
            }

            // Search Bar & Filter Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Input TextField
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tool_search_input"),
                        placeholder = { Text("Search 10,000+ spec tools...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    // Switches Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable { filterFavoritesOnly = !filterFavoritesOnly }
                        ) {
                            Checkbox(
                                checked = filterFavoritesOnly,
                                onCheckedChange = { filterFavoritesOnly = it },
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Favorites Only", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable { filterSpeculativeOnly = !filterSpeculativeOnly }
                        ) {
                            Checkbox(
                                checked = filterSpeculativeOnly,
                                onCheckedChange = { filterSpeculativeOnly = it },
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Speculative only", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Horizontally Scrollable Category Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 12.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All Categories (${allFeaturesList.size + dynamicCustomFeatures.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
                items(FeatureDatabase.categories) { category ->
                    val isSelected = selectedCategory == category
                    val count = remember(dynamicCustomFeatures.size) {
                        allFeaturesList.count { it.category == category } + dynamicCustomFeatures.count { it.category == category }
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = if (isSelected) null else category },
                        label = { Text("$category ($count)", fontSize = 11.sp) }
                    )
                }
            }

            // List of Features
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (filteredList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "", tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching feature keys found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Adjust features query filters or load custom JSON plugin assets at the top bar.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    // Render list dynamically (optimally using LazyColumn)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList) { feature ->
                            FeatureRowItem(
                                feature = feature,
                                onActivate = { triggerSimulatedBuild(feature) }
                            )
                        }
                    }
                }

                // SIMULATION DYNAMIC BUILD CONSOLE BAR Overlay at bottom
                androidx.compose.animation.AnimatedVisibility(
                    visible = activeSimulatingFeature != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 12.dp,
                        shadowElevation = 16.dp,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSimulating) Icons.Default.Sync else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Pluggable Sandbox: ${activeSimulatingFeature?.name}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = simulationLog,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                if (!isSimulating) {
                                    IconButton(onClick = { activeSimulatingFeature = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close console")
                                    }
                                }
                            }

                            LinearProgressIndicator(
                                progress = simulationProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Dynamic Importer custom Dialog
    if (showCustomToolImporterDialog) {
        AlertDialog(
            onDismissRequest = { showCustomToolImporterDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Load Dynamic JSON Plugin", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Specify custom assets, vectors, filters, or scripts. The system automatically updates the application model dynamically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = uInputToolName,
                        onValueChange = { uInputToolName = it },
                        label = { Text("Plugin / Tool Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Custom Selection for Category
                    Text("Plugin Segment Grouping", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        Text(
                            text = uInputCategory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownExpanded = true }
                                .padding(12.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            FeatureDatabase.categories.forEach { catName ->
                                DropdownMenuItem(
                                    text = { Text(catName, fontSize = 12.sp) },
                                    onClick = {
                                        uInputCategory = catName
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uInputDescription,
                        onValueChange = { uInputDescription = it },
                        label = { Text("Module Description / Prompt Specs") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nameStr = uInputToolName.trim()
                        val descStr = uInputDescription.trim().ifEmpty { "Dynamically imported user plugin module." }
                        if (nameStr.isNotEmpty()) {
                            val newFeat = SpeculativeFeature(
                                name = nameStr,
                                category = uInputCategory,
                                description = descStr,
                                isSpeculative = true,
                                initialFavorite = true
                            )
                            dynamicCustomFeatures.add(0, newFeat)
                            showCustomToolImporterDialog = false
                            uInputToolName = ""
                            uInputDescription = ""
                            Toast.makeText(context, "Successfully loaded $nameStr dynamically!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Please configure a valid unique name", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Assemble Plugin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomToolImporterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun FeatureRowItem(
    feature: SpeculativeFeature,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (feature.isActivated) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = feature.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (feature.isActivated) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "COMPILED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 8.sp
                            )
                        }
                    } else if (feature.isSpeculative) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "SPECULATIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = feature.category,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }

            // Action section: Favorite toggle & Compile Action button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { feature.isFavorite = !feature.isFavorite }) {
                    Icon(
                        imageVector = if (feature.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite tool",
                        tint = if (feature.isFavorite) Color.Red else MaterialTheme.colorScheme.outline
                    )
                }

                Button(
                    onClick = onActivate,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (feature.isActivated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = if (feature.isActivated) "Recompile" else "Compile", 
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
