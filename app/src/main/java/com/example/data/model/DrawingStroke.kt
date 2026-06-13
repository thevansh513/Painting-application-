package com.example.data.model

data class StrokePoint(val x: Float, val y: Float)

data class DrawingStroke(
    val color: Int,
    val width: Float,
    val points: List<StrokePoint>,
    val isEraser: Boolean = false,
    val shapeType: String = "FREEHAND",
    val layerId: String = "Layer 2"
)

object StrokeSerializer {
    fun serialize(strokes: List<DrawingStroke>): String {
        val sb = StringBuilder()
        for (i in strokes.indices) {
            val stroke = strokes[i]
            if (i > 0) sb.append("::")
            sb.append(stroke.color).append("|")
            sb.append(stroke.width).append("|")
            sb.append(if (stroke.isEraser) 1 else 0).append("|")
            
            // Serialize points
            for (j in stroke.points.indices) {
                val p = stroke.points[j]
                if (j > 0) sb.append(";")
                sb.append(p.x).append(",").append(p.y)
            }
            sb.append("|")
            sb.append(stroke.shapeType).append("|")
            sb.append(stroke.layerId)
        }
        return sb.toString()
    }

    fun deserialize(data: String): List<DrawingStroke> {
        if (data.isBlank()) return emptyList()
        val strokes = mutableListOf<DrawingStroke>()
        try {
            val parts = data.split("::")
            for (part in parts) {
                if (part.isBlank()) continue
                val strokeParts = part.split("|")
                if (strokeParts.size < 4) continue
                val color = strokeParts[0].toLongOrNull()?.toInt() ?: strokeParts[0].toIntOrNull() ?: 0xFF000000.toInt()
                val width = strokeParts[1].toFloatOrNull() ?: 5f
                val isEraser = strokeParts[2] == "1"
                val pointsStr = strokeParts[3]
                
                val shapeType = if (strokeParts.size >= 5) {
                    strokeParts[4]
                } else {
                    "FREEHAND"
                }

                val layerId = if (strokeParts.size >= 6) {
                    strokeParts[5]
                } else {
                    "Layer 2"
                }

                val points = mutableListOf<StrokePoint>()
                if (pointsStr.isNotBlank()) {
                    val pts = pointsStr.split(";")
                    for (pt in pts) {
                        val coords = pt.split(",")
                        if (coords.size == 2) {
                            val x = coords[0].toFloatOrNull() ?: 0f
                            val y = coords[1].toFloatOrNull() ?: 0f
                            points.add(StrokePoint(x, y))
                        }
                    }
                }
                strokes.add(DrawingStroke(color, width, points, isEraser, shapeType, layerId))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strokes
    }
}
