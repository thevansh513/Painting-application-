package com.example.data.model

enum class DrawingTool(val displayName: String) {
    // Basic & Special Brushes
    FREEHAND("Solid Paint"),
    BRUSH_CALLIGRAPHY("Calligraphy Ribbon"),
    BRUSH_NEON("Neon Glow Ink"),
    BRUSH_RAINBOW("Rainbow Ink"),
    BRUSH_MARKER("Translucent Marker"),
    BRUSH_AIRBRUSH("Spray Airbrush"),
    BRUSH_SHADOW("Drop Shadow Pencil"),
    
    // Geometry Shapes
    LINE("Line Vector"),
    RECTANGLE_OUTLINE("Rectangle Wireframe"),
    RECTANGLE_FILLED("Filled Rectangle"),
    CIRCLE_OUTLINE("Circle Wireframe"),
    CIRCLE_FILLED("Filled Circle"),
    
    // Decorative Stamp Art tools
    STAMP_STAR("Star Stamp"),
    STAMP_HEART("Heart Stamp"),
    STAMP_FLOWER("Flower Bloom Stamp"),
    STAMP_LEAF("Maple Leaf Stamp"),
    STAMP_SNOWFLAKE("Snowflake Stamp"),
    STAMP_CLOUD("Cumulus Cloud Stamp"),
    STAMP_SMILEY("Happy Smiley Stamp"),
    STAMP_SUN("Radiant Sun Stamp"),
    STAMP_MOON("Crescent Moon Stamp"),
    STAMP_TEXT("Text Glyph Stamp")
}
