package com.example.pokemonmatrixwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object DotMatrixRenderer {

    fun toDotMatrix(
        src: Bitmap,
        gridWidth: Int = 32,
        gridHeight: Int = 32,
        cellSize: Int = 20  // NEW: default cell size
    ): Bitmap {
        // 1. Shrink the original sprite down to a small grid
        val scaled = Bitmap.createScaledBitmap(src, gridWidth, gridHeight, true)

        val outWidth = gridWidth * cellSize
        val outHeight = gridHeight * cellSize

        val outBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        canvas.drawColor(Color.parseColor("#1B1B1D"))


        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        for (y in 0 until gridHeight) {
            for (x in 0 until gridWidth) {
                val pixel = scaled.getPixel(x, y)

                val alpha = Color.alpha(pixel)
                if (alpha < 64) continue

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val brightness = (r + g + b) / 3

                if (brightness > 50) {
                    val cx = x * cellSize + cellSize / 2f
                    val cy = y * cellSize + cellSize / 2f
                    val radius = cellSize * 0.4f
                    canvas.drawCircle(cx, cy, radius, paint)
                }
            }
        }

        scaled.recycle()
        return outBitmap
    }

    fun toDotMatrixScaled(
        src: Bitmap,
        gridWidth: Int = 32,
        gridHeight: Int = 32,
        cellSize: Int = 12,
        scale: Float = 1.0f
    ): Bitmap {
        val base = toDotMatrix(src, gridWidth, gridHeight, cellSize)

        // No scaling requested
        if (scale == 1.0f) return base

        val newWidth = (base.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (base.height * scale).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(base, newWidth, newHeight, false)
        base.recycle()
        return scaled
    }
}
