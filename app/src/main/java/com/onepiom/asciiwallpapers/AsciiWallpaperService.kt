package com.onepiom.asciiwallpapers

import android.graphics.*
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.os.Handler
import android.os.Looper
import kotlin.math.*

class AsciiWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = AsciiEngine()

    inner class AsciiEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var frame = 0
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("monospace", Typeface.BOLD)
        }
        private val drawRunner = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, 45L)
            }
        }
        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (visible) { handler.removeCallbacks(drawRunner); handler.post(drawRunner) }
            else handler.removeCallbacks(drawRunner)
        }
        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height); drawFrame()
        }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false; handler.removeCallbacks(drawRunner); super.onSurfaceDestroyed(holder)
        }
        override fun onDestroy() { visible = false; handler.removeCallbacks(drawRunner); super.onDestroy() }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try { canvas = holder.lockCanvas(); if (canvas != null) render(canvas) }
            catch (_: Exception) { }
            finally { if (canvas != null) holder.unlockCanvasAndPost(canvas) }
            frame++
        }

        private fun render(c: Canvas) {
            val prefs = getSharedPreferences("wallpaper", MODE_PRIVATE)
            val mode = prefs.getInt("mode", 0)
            val speed = prefs.getInt("speed", 5) / 5f
            val scale = prefs.getInt("size", 5) / 5f
            val density = prefs.getInt("density", 7) / 10f
            val brightness = prefs.getInt("brightness", 8) / 10f
            val palette = prefs.getInt("palette", 0)
            val base = when (palette) {
                1 -> Color.rgb(60, 220, 150)
                2 -> Color.rgb(255, 105, 190)
                3 -> Color.rgb(255, 174, 75)
                else -> Color.rgb(112, 155, 255)
            }
            val w = c.width; val h = c.height
            c.drawColor(Color.rgb(2, 4, 12))
            paint.textSize = (w / (48f / scale.coerceIn(.55f, 1.8f))).coerceIn(8f, 25f)
            val cw = paint.measureText("M"); val ch = paint.textSize * 1.15f
            val cols = (w / cw).toInt().coerceAtLeast(1)
            val rows = (h / ch).toInt().coerceAtLeast(1)
            val t = frame * .035f * speed
            for (y in 0 until rows) for (x in 0 until cols) {
                val nx = (x - cols / 2f) / (cols * .5f)
                val ny = (y - rows / 2f) / (rows * .5f)
                val sample = when (mode) {
                    1 -> rain(x, y, rows, t, density)
                    2 -> wave(x, y, cols, rows, t, density)
                    3 -> sphere(nx, ny, cols, rows, t, density)
                    4 -> torus(nx, ny, t, density)
                    5 -> cosmos(nx, ny, x, y, t, density)
                    else -> blackHole(nx, ny, x, y, t, density)
                }
                if (sample.first != ' ') {
                    paint.color = base
                    paint.alpha = (sample.second * brightness).roundToInt().coerceIn(25, 255)
                    c.drawText(sample.first.toString(), x * cw, (y + 1) * ch, paint)
                }
            }
        }

        private fun blackHole(nx: Float, ny: Float, x: Int, y: Int, t: Float, den: Float): Pair<Char, Int> {
            val a = t * .35f
            val xx = nx * cos(a) - ny * sin(a); val yy = nx * sin(a) + ny * cos(a)
            val r = sqrt(nx * nx + ny * ny)
            val ring = abs(sqrt(xx * xx * .72f + yy * yy) - .43f)
            if (r < .19f) return Pair(' ', 0)
            if (ring < .035f * (0.7f + den)) return Pair(" .:*oO#"[((x + y + frame / 3) % 7)], 230)
            val cube = max(abs(xx * 1.15f), abs(yy * 1.15f))
            if (cube in .62f.. .66f) return Pair("#+*%"[((x + y + frame / 5) % 4)], 190)
            return Pair(' ', 0)
        }
        private fun rain(x: Int, y: Int, rows: Int, t: Float, den: Float): Pair<Char, Int> {
            val speedFrame = (t * 15).toInt()
            val head = (x * 17 + 3) % rows.coerceAtLeast(1)
            return if ((y + speedFrame + x * 7) % rows.coerceAtLeast(1) == head || (x * 13 + y * 3 + speedFrame) % (max(8, (42 / den).toInt())) == 0)
                Pair("01#@$%"[(x + y + speedFrame) % 6], 220) else Pair(' ', 0)
        }
        private fun wave(x: Int, y: Int, cols: Int, rows: Int, t: Float, den: Float): Pair<Char, Int> {
            val center = rows / 2f + sin(x * .16f + t) * rows * .22f + cos(x * .07f - t * .7f) * rows * .08f
            val d = abs(y - center)
            return if (d < 1.4f + den) Pair("@#*+.:"[(x + y + frame / 4) % 6], 230)
            else if (d < 3.5f && (x + y) % 2 == 0) Pair(':', 100) else Pair(' ', 0)
        }
        private fun sphere(nx: Float, ny: Float, cols: Int, rows: Int, t: Float, den: Float): Pair<Char, Int> {
            // Sphere travels horizontally and vertically, reflecting at the screen edges.
            val px = .48f * sin(t * .62f); val py = .38f * sin(t * .91f + 1.2f)
            val sx = (nx - px) * (cols.toFloat() / rows.coerceAtLeast(1))
            val sy = ny - py
            val r2 = sx * sx + sy * sy
            val radius = .30f
            if (r2 > radius * radius) return Pair(' ', 0)
            val z = sqrt((radius * radius - r2).coerceAtLeast(0f)) / radius
            val light = ((-sx * .45f - sy * .55f + z * .75f) * .5f + .5f).coerceIn(0f, 1f)
            val chars = " .,:-~=+*#%@"
            val idx = (light * (chars.length - 1)).roundToInt().coerceIn(0, chars.lastIndex)
            return Pair(chars[idx], (90 + light * 165).roundToInt())
        }
        private fun torus(nx: Float, ny: Float, t: Float, den: Float): Pair<Char, Int> {
            val a = t * .65f
            val x = nx * cos(a) - ny * sin(a); val y = nx * sin(a) + ny * cos(a)
            val r = sqrt(x * x + y * y)
            val d = abs(sqrt((r - .43f) * (r - .43f) + (y * .28f) * (y * .28f)) - .105f)
            return if (d < .025f + den * .012f) Pair(" .:-=+*#%@"[((x * 18 + y * 13 + t * 4).roundToInt().absoluteValue) % 10], 230) else Pair(' ', 0)
        }
        private fun cosmos(nx: Float, ny: Float, x: Int, y: Int, t: Float, den: Float): Pair<Char, Int> {
            val r = sqrt(nx * nx + ny * ny)
            val angle = atan2(ny, nx)
            val spiral = abs(sin(angle * 3f - r * 17f + t * .6f))
            if (r < .07f) return Pair('@', 255)
            if (r < .82f && spiral < .10f + den * .06f) return Pair(".:*+xX#"[((x + y + frame / 4) % 7)], 210)
            val star = ((x * 73856093) xor (y * 19349663)) and 255
            return if (star < 5 && (frame / 8 + star) % 3 == 0) Pair('.', 170) else Pair(' ', 0)
        }
    }
}
