package com.example.data.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SpeculativeFeature(
    val name: String,
    val category: String,
    val description: String,
    val isSpeculative: Boolean = true,
    initialFavorite: Boolean = false,
    initialActivated: Boolean = false
) {
    var isFavorite by mutableStateOf(initialFavorite)
    var isActivated by mutableStateOf(initialActivated)
}

object FeatureDatabase {
    val categories = listOf(
        "Raster & Brush Engine",
        "Vector & Typography Studio",
        "AI & Speculative Science",
        "Photo Manipulation & Retouching",
        "Layers, Masks & Blending",
        "Symmetry, Grids & Perspective",
        "3D Design, AR & VR Canvas",
        "Animation, Timeline & Sprite-sheet",
        "Publishing, Export & Layout",
        "Developer Labs & Custom Plugins"
    )

    val allFeatures: List<SpeculativeFeature> by lazy {
        val featuresSet = mutableSetOf<String>()
        val list = mutableListOf<SpeculativeFeature>()

        // Core standard features that match real and requested workflows
        val coreItems = listOf(
            "Text", "Box", "Color", "Brush", "Pencil", "Eraser", "Layer", "Gradient", 
            "Sticker", "Frame", "Download", "Export", "Undo", "Redo", "Lasso", 
            "Magic Wand", "Heal Brush", "Liquify", "Clone Stamp", "Perspective Guide", 
            "Mandala Grid", "Kaleidoscope Guide", "Autosave", "OCR Scan", "Barcode Generator", 
            "QR Creator", "Mockup Generator", "Watermark Draft", "GIF Animation", "Onion Skinning"
        )
        
        for (item in coreItems) {
            featuresSet.add(item)
            val cat = when (item) {
                "Brush", "Pencil", "Eraser" -> "Raster & Brush Engine"
                "Text", "Box", "Gradient", "Sticker", "Frame" -> "Vector & Typography Studio"
                "Watermark Draft", "Barcode Generator", "QR Creator", "Mockup Generator", "OCR Scan", "Download", "Export" -> "Publishing, Export & Layout"
                "GIF Animation", "Onion Skinning", "Undo", "Redo" -> "Animation, Timeline & Sprite-sheet"
                else -> "Layers, Masks & Blending"
            }
            list.add(
                SpeculativeFeature(
                    name = item,
                    category = cat,
                    description = "Core standard workspace component integrated for instant creative studio rendering.",
                    isSpeculative = false
                )
            )
        }

        // Master vocabulary grids to scale theSpeculative Feature Studio to 10,000+ entries
        val prefixes = listOf(
            "Quantum", "Neural", "Hyper", "Spectra", "Infinite", "Chrono", "Holographic",
            "Spatial", "Procedural", "Stochastic", "Thermal", "Synthetic", "Biomorphic",
            "Anisotropic", "Synaptic", "Symmetric", "Subatomic", "Celestial", "Interstellar",
            "Cybernetic", "Metabolic", "Amorphous", "Vortex", "Fluidic", "Luminescent",
            "Chameleonic", "Super-Resolution", "Context-Aware", "Volumetric", "Tessellated",
            "Atmospheric", "Optic", "Magnetic", "Acoustic", "Kinetic", "Vectorized", "Neo",
            "Organic", "Deep-Learning", "Vibrational", "Isomorphic", "Relativistic", "Astrophysical",
            "Crystalline", "Prismatic", "Supersonic", "Thermodynamic", "Gravitational", "Spectral", "Multiplex"
        ) // 50 elements

        val suffixes = listOf(
            "Engine", "Analyzer", "Synthesizer", "Optimizer", "Simulator", "Generator",
            "Stabilizer", "Multiplier", "Compressor", "Expander", "Interpolator", "Modulator",
            "Projector", "Transformer", "Blender", "Composer", "Sculpter", "Refiner",
            "Enhancer", "Calibrator", "Mapper", "Distorter", "Renderer", "Vectorizer",
            "Quantizer", "Rasterizer", "Visualizer", "Harmonizer", "Tracker", "Sensor",
            "Controller", "Trigger", "Automator", "Solver", "Decimator", "Reconstructor",
            "Validator", "Extractor", "Filter", "Processor", "Diagnostician", "Architect",
            "Synthesizer Unit", "Governor", "Modulator Core", "Scaler", "Synthesist", "Synthetix"
        ) // 48 elements

        val detailsMap = mapOf(
            "Raster & Brush Engine" to listOf(
                "Bristle Dynamic", "Impasto Flow", "Gouache Wash", "Charcoal Friction", "Sponge Splatter",
                "Chalk Grain", "Wet Mixing", "Acrylic Flake", "Watercolor Bleed", "Splat Scatter",
                "Scraper Pressure", "Crayon Texture", "Ink Viscosity", "Pastel Softness", "Airbrush Diffusion",
                "Fresco Splash", "Laser Burn", "Plasma Melt", "Vapor Trail", "Dust Filament",
                "Marker Absorb", "Dribble Ink", "Spatter Blend", "Coagulation", "Smear Smudge",
                "Dry Scratch", "Hairy Tip", "Sieve Particle", "Oil Smudge", "Sponge Dab"
            ),
            "Vector & Typography Studio" to listOf(
                "Bezier Node", "Anchor Point", "Path Scissor", "Subpatch Union", "Letter Kerning",
                "Font Extrusion", "Symmetric Joint", "Tangent Guide", "Glyph Baseline", "Vector Knife",
                "Curvature Map", "Polyline Segment", "NURBS Shell", "Parametric Path", "Flowing Text Align",
                "Lattice Warp", "Text Outline", "Stylus Handwriting", "Calligraphy Angle", "Font Weight Slider",
                "Monospace Grid", "Text Along Spline", "Node Snapping", "Stroke Mitre", "Corner Rounding",
                "Boolean Join", "Shape Expander", "Vector Slice", "Smart Kerning", "Typeface Pairing"
            ),
            "AI & Speculative Science" to listOf(
                "Inpaint Mask", "Subject Segmentation", "Neural Style Mimic", "Generative Outpaint", "Recolor Canvas",
                "Prompt Guidance Brush", "Depth Map Relight", "Vector Auto-Trace", "Background Purge", "Object Erase AI",
                "Detail Enhancer Map", "Facial Re-Pose", "Comic Inking Assistant", "Perspective Solver AI", "Auto-Colorizer",
                "Prompt To Layer", "Seamless Pattern AI", "Semantic Object Filter", "Palette Harmonic Suggestion", "Texture Synthesizer",
                "Sketch Clean-up AI", "Drape Mesh AI", "Pose Estimator", "Resolution Upscale", "Anomalous Artifact Suppressor",
                "Dynamic Inpainting", "Style Fusion", "Concept Dreamer", "Smart Subject Select", "Artistic Flow Optimizer"
            ),
            "Photo Manipulation & Retouching" to listOf(
                "Frequency Separation", "Dodge Highlight", "Burn Midtone", "Red-Eye Desaturate", "Clone Blend",
                "Healing Patch", "Chromatic Aberration Filter", "Blemish Eraser", "Skin Smoothen", "HDR Contrast Map",
                "Lens Blur Simulation", "Motion Blur Recovery", "Grain Matching", "Exposure Matcher", "Vignette Center",
                "Temperature Balance", "Shadow Recovery", "Highlight Dampen", "Tone Map curve", "Clarity Intensifier",
                "Dehaze Level", "De-noiser Threshold", "Color Temperature Balance", "Perspective Distorter", "Orthophoto Mapper",
                "Texture Overlay", "Fringe Contrast", "Level Align", "Luminance Match", "Halation Simulator"
            ),
            "Layers, Masks & Blending" to listOf(
                "Luminosity Masking", "Alpha Track Matte", "Clipping Boundary", "Layer Nesting Folder", "Composite Preview",
                "Non-Destructive Mesh Warp", "Smart Object Link", "Vector Boundary Mask", "Blend Mode Multiply", "Blend Mode Screen",
                "Blend Mode Overlay", "Color Dodge Engine", "Layer Pass-through", "Adjustment Curvature", "Channel Splitter",
                "Alpha Channel Extractor", "Layer Opacity Cascade", "Blend IFC Curve", "Dynamic Isolation", "Depth Layer Slice",
                "Merged Flatten Copy", "Layer Color Code", "Opacity Masking Link", "Smart Target Layer", "Active Nesting Toggle",
                "Blend Range Slider", "Mask Edge Feathering", "Target Layer Clip", "Transparency Lock", "Channel Mask Blender"
            ),
            "Symmetry, Grids & Perspective" to listOf(
                "Kaleidoscope Wedge", "Radial Sector Matrix", "Vanishing Point Guide", "isometric Surface Snap", "Hexagonal Grid Layout",
                "Bilateral Mirror Plane", "Rule of Thirds Overlay", "Golden Spiral Guide", "3-Point Perspective Grid", "Horizontal Symmetrical Axis",
                "Vertical Symmetrical Axis", "Diagonal Repeat Grid", "Dot Matrix Overlay", "Curvilinear Grid", "Mandala Slice Radial",
                "Perspective Horizon Angle", "Smart Align Snapping", "Fisheye Lens Grid", "Infinite Perspective Plane", "Symmetry Center-point Lock",
                "Grid Pitch Multiplier", "Guide Edge Magnetism", "Rotational Symmetry Anchor", "Symmetric Shift Offset", "Grid Angle Rotator",
                "Warp Grid Subdivision", "Symmetry Brush Link", "Orthogonal Projection Guide", "Triangular Mesh Grid", "Polar Coordinate Guide"
            ),
            "3D Design, AR & VR Canvas" to listOf(
                "UV Texture Map Wrap", "Holographic Depth Preview", "AR Reality Painting Anchor", "Spatial Depth Layer", "Volumetric Fog Render",
                "PBR Material Roughness Slider", "Specular Gloss Map", "Normal Vector Painter", "Ambient Occlusion Shader", "3D Camera Field-of-View",
                "Light Source Vector Cast", "AR Object Placement", "Realtime Raytrace Preview", "Subsurface Scatter Shader", "Voxel Clay Sculpt",
                "3D Mesh Exporter", "Texture Projection Mapping", "VR Environment Canvas", "Dynamic Point Cloud Render", "Gaze Control Pointer VR",
                "Material Metalness Map", "Parallax Height Shader", "AR Environment Occlusion", "VR Spatial Brush Path", "3D Gyroscope Canvas Control",
                "Voxel Grid Subdivision", "Anisotropic BRDF Painter", "3D Silhouette Extractor", "Spatial Layer Z-Buffer", "3D Spline Extruder"
            ),
            "Animation, Timeline & Sprite-sheet" to listOf(
                "Keyframe Interpolator", "Onion Skin Opacity Slider", "Frame rate FPS Cap", "Sprite Grid Slicer", "Frame Duplicate Loop",
                "Timeline Playback Reverse", "Vector Shape Morph Key", "Motion Path Guide", "Skeletal Bone Vector Rig", "Keyframe Easing Bezier",
                "Animated GIF Assembler", "Sprite Sheet Tile Sheet Pack", "FBF Traditional Animation Layer", "Audio Waveform Sync", "Multi-Track Timeline",
                "Onion Skin Range Forward", "Onion Skin Range Backward", "Frame Expose Hold Time", "Looping Range Segment", "Motion Blur Sub-frame Render",
                "Frame Tween Auto-Generator", "Keyframe Label Color Code", "Sprite Extractor Segmenter", "Animation Target Sub-path", "Easing Velocity Curve",
                "Frame Rate Conversion Solver", "Sprite Sheet Border Spacing", "Keyframe Layer Clip", "Bone Weight Painting Brush", "Onion Skin Tint Colors"
            ),
            "Publishing, Export & Layout" to listOf(
                "PDF Print CMYK Bleed", "Color Halftone Dither", "ICC Color Profile Tag", "Lossless PNG Compressor", "Batch WebP Converter",
                "DPI Density Calibrator", "Poster Grid Guides", "Vector SVG Exporter", "Dynamic Brand Watermark", "Mockup Scene Generator",
                "Packaging Die-cut Guide", "Crop Mark Alignment", "QR Code Vector Maker", "UPC Barcode Generator", "Rich Text Format Flow",
                "Metadata EXIF Imprinter", "Multi-Page Layout Manager", "Social Poster Canvas Preset", "Tonal Separator CMYK", "Raster Export Downsampler",
                "Print Bleed Margin Overlay", "E-Book EPUB Layout Generator", "Color Proofing Gamut Alarm", "Batch Watermark Applicator", "High-Fidelity PDF Writer",
                "Z-Fold Brochure Panel Guide", "Vector EPS Packaging Wrapper", "Asset Export Slices", "Palette Swatch Export", "Metadata Author Signet"
            ),
            "Developer Labs & Custom Plugins" to listOf(
                "GLSL Custom Shader Sandbox", "Python Automation Script Runner", "JSON Shortcut Mapper", "Performance Analytics Monitor", "Memory Heap Profiler",
                "Custom Brush JSON Importer", "API Webhook Callback Configuration", "Layout CSS Generator", "UI Theme Custom Style Editor", "Macro Action Recorder",
                "Plugin Extension Marketplace", "Debug Render Boundary Toggle", "Vulkan Shader Hot Reload", "GPU Draw Call Counter", "SVG Raw XML Vector Code Viewer",
                "Custom Shortcode Trigger", "Local Server WebSocket Sync", "Sandbox Runtime Sandbox Inspector", "Plugin API SDK Tester", "Experimental Lab Features Panel",
                "Crash Dump Log Decryptor", "Asset Catalog Database Editor", "Script Debugger Stack Trace", "Custom Gesture Shortcut Builder", "Shader Uniform Parameter Float",
                "Benchmark Canvas Painter", "Dynamic DLL Plugin Linker", "Canvas State XML Exporter", "Macro Batch Processor", "Android NDK Performance Link"
            )
        )

        // Iterate through categories and draw combinations to yield 1,020 items for each category
        // 10 categories * 1,020 items = 10,200 unique features perfectly structured
        for (cat in categories) {
            val baseList = detailsMap[cat] ?: emptyList()
            var itemIndex = 0

            // Add starting explicit bases
            for (base in baseList) {
                if (featuresSet.add(base)) {
                    list.add(
                        SpeculativeFeature(
                            name = base,
                            category = cat,
                            description = "Modern high-precision $cat tool optimized for offline creative workflow suites.",
                            isSpeculative = true
                        )
                    )
                }
            }

            // Loop prefixes and suffixes deterministically to create exactly 1,022 unique features per category
            outer@for (prefix in prefixes) {
                for (suffix in suffixes) {
                    val catListSize = list.count { it.category == cat }
                    if (catListSize >= 1022) {
                        break@outer
                    }
                    val baseNoun = baseList[itemIndex % baseList.size]
                    val formattedName = "$prefix $baseNoun $suffix"
                    itemIndex++
                    
                    if (featuresSet.add(formattedName)) {
                        val desc = "Speculative AI-assisted $cat module utilizing $prefix algorithms combined with high-performance $suffix integrations."
                        list.add(
                            SpeculativeFeature(
                                name = formattedName,
                                category = cat,
                                description = desc,
                                isSpeculative = true
                            )
                        )
                    }
                }
            }
        }

        list.toList()
    }
}
