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
                    1 -> rain(x, y, cols, rows, t, density)
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
            // Tilted glowing accretion disk + perspective 3D wireframe cube.
            val a = t * .28f
            val xx = nx * cos(a) - ny * sin(a)
            val yy = nx * sin(a) + ny * cos(a)
            val diskY = yy * 0.48f + 0.10f * sin(xx * 5f + t)
            val r = sqrt(xx * xx + diskY * diskY)
            val hole = sqrt(xx * xx * 0.82f + yy * yy * 0.18f)
            if (hole < .19f) return Pair(' ', 0)
            val ring = abs(r - .43f)
            if (ring < .025f + den * .012f) {
                val glow = (1f - ring / (.04f + den * .012f)).coerceIn(0f, 1f)
                val chars = " .:-=+*#%@"
                return Pair(chars[((glow * 9).toInt()).coerceIn(0, 9)], (120 + glow * 135).toInt())
            }
            // A projected rotating cube, with visible front and side faces.
            val rot = t * .52f
            val u = nx * cos(rot) - ny * sin(rot)
            val v = nx * sin(rot) + ny * cos(rot)
            val z = (0.58f + 0.12f * sin(rot + u * 3f))
            val edge = minOf(abs(abs(u) - .34f), abs(abs(v) - .34f), abs(abs(u + v * .34f) - .38f))
            if (edge < .012f && abs(u) < .43f && abs(v) < .43f) return Pair("+*#%"[(x + y + frame / 5) % 4], (150 + z * 120).toInt())
            return Pair(' ', 0)
        }

        private fun rain(x: Int, y: Int, cols: Int, rows: Int, t: Float, den: Float): Pair<Char, Int> {
            // Matrix streams recede toward a vanishing point, with varied depth and head glow.
            val center = cols / 2f
            val rel = (x * 1f - center) / cols.coerceAtLeast(1)
            val depth = (0.30f + abs(rel) * 1.7f).coerceIn(.3f, 1.8f)
            val col = x * 17 + 3
            val phase = (t * (7f + (x % 5)) * depth).toInt()
            val period = max(9, (rows * (0.34f + (x % 7) * .018f)).toInt())
            val head = Math.floorMod(col + phase, period)
            val dist = Math.floorMod(y - head + period, period)
            val active = dist < (2 + den * 7).toInt() && (x * 13 + y * 3) % max(2, (5f / den).toInt()) == 0
            if (!active) return Pair(' ', 0)
            val chars = " .,:;+=*#%@01"
            val idx = (chars.lastIndex - dist * 1.3f).toInt().coerceIn(0, chars.lastIndex)
            val alpha = (230 - dist * 22).coerceIn(45, 255)
            return Pair(chars[idx], alpha)
        }

        private fun wave(x: Int, y: Int, cols: Int, rows: Int, t: Float, den: Float): Pair<Char, Int> {
            // Several luminous sine ribbons with perspective and depth shading.
            val nx = (x - cols / 2f) / cols.coerceAtLeast(1)
            val horizon = rows * .50f
            val chars = " .:-=+*#%@"
            var best = 99f
            var depth = 0f
            for (layer in 0..3) {
                val z = layer / 3f
                val amp = rows * (.075f + z * .07f)
                val phase = nx * (8f + z * 5f) + t * (1.1f + z * .35f) + layer * 1.55f
                val wave = sin(phase) * amp + cos(nx * 5f - t * .65f + layer) * rows * .035f
                val perspective = .48f + z * .58f
                val lineY = horizon + wave * perspective + (layer - 1.5f) * rows * .085f
                val d = abs(y - lineY)
                if (d < best) { best = d; depth = z }
            }
            val thickness = 1.1f + den * 1.5f
            if (best > thickness + 2.4f) return Pair(' ', 0)
            val intensity = (1f - best / (thickness + 2.4f)).coerceIn(0f, 1f)
            val idx = ((intensity * .75f + depth * .25f) * chars.lastIndex).toInt().coerceIn(0, chars.lastIndex)
            return Pair(chars[idx], (70 + intensity * 150 + depth * 35).toInt())
        }

        private fun sphere(nx: Float, ny: Float, cols: Int, rows: Int, t: Float, den: Float): Pair<Char, Int> {
            // Shaded sphere with moving position, specular highlight and rim light.
            val px = .42f * sin(t * .42f); val py = .30f * sin(t * .61f + 1.2f)
            val sx = (nx - px) * (cols.toFloat() / rows.coerceAtLeast(1))
            val sy = ny - py
            val radius = .31f
            val r2 = sx * sx + sy * sy
            if (r2 > radius * radius) return Pair(' ', 0)
            val z = sqrt((radius * radius - r2).coerceAtLeast(0f)) / radius
            val lx = -.42f * cos(t * .25f); val ly = -.55f; val lz = .72f
            val dot = (-(sx / radius) * lx - (sy / radius) * ly + z * lz).coerceIn(0f, 1f)
            val rim = (1f - z).coerceIn(0f, 1f)
            val spec = exp(-((sx / radius + .28f) * (sx / radius + .28f) + (sy / radius + .35f) * (sy / radius + .35f)) * 24f)
            val light = (dot * .72f + spec * .35f + rim * .12f).coerceIn(0f, 1f)
            val chars = " .,:-~=+*#%@"
            val idx = (light * chars.lastIndex).roundToInt().coerceIn(0, chars.lastIndex)
            return Pair(chars[idx], (65 + light * 190).roundToInt())
        }

        private fun torus(nx: Float, ny: Float, t: Float, den: Float): Pair<Char, Int> {
            // Perspective torus: elliptical cross-section, rotating depth and lit front/back sides.
            val a = t * .48f
            val x = nx * cos(a) - ny * sin(a)
            val y = nx * sin(a) + ny * cos(a)
            val major = .39f
            val radial = sqrt(x * x + (y * 1.18f) * (y * 1.18f))
            val tube = sqrt((radial - major) * (radial - major) + (y * .42f) * (y * .42f))
            val d = abs(tube - .105f)
            val thickness = .012f + den * .012f
            if (d > thickness) return Pair(' ', 0)
            val front = (0.5f + 0.5f * sin(atan2(y, x) * 2f + t)).coerceIn(0f, 1f)
            val shade = (1f - d / thickness).coerceIn(0f, 1f) * .7f + front * .3f
            val chars = " .:-=+*#%@"
            return Pair(chars[(shade * chars.lastIndex).toInt().coerceIn(0, chars.lastIndex)], (90 + shade * 165).toInt())
        }

        private fun cosmos(nx: Float, ny: Float, x: Int, y: Int, t: Float, den: Float): Pair<Char, Int> {
            // Multi-arm galaxy with a raised glowing core, depth bands and twinkling stars.
            val r = sqrt(nx * nx + ny * ny)
            val angle = atan2(ny, nx)
            val twist = angle * 3f - r * 19f + t * .42f
            val arm = abs(sin(twist + .22f * sin(r * 22f)))
            val band = abs(arm - .08f)
            if (r < .075f) return Pair("@#"[(frame / 5) % 2], 255)
            if (r < .84f && band < .12f + den * .045f) {
                val depth = (1f - r / .9f).coerceIn(0f, 1f)
                val chars = " .,:-+*#%@"
                val shade = (1f - band / (.12f + den * .045f)) * .65f + depth * .35f
                return Pair(chars[(shade * chars.lastIndex).toInt().coerceIn(0, chars.lastIndex)], (80 + shade * 170).toInt())
            }
            val hash = ((x * 73856093) xor (y * 19349663)) and 1023
            return if (hash < (5 + den * 7).toInt() && (frame / 7 + hash) % 4 == 0)
                Pair(if (hash % 5 == 0) '+' else '.', 110 + (hash % 140)) else Pair(' ', 0)
        }

    }
}
