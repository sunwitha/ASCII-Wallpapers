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
        private var mode = 0
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("monospace", Typeface.NORMAL)
        }
        private val drawRunner = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, 90)
            }
        }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            if (visible) { mode = getSharedPreferences("wallpaper", MODE_PRIVATE).getInt("mode", 0); handler.removeCallbacks(drawRunner); handler.post(drawRunner) }
            else handler.removeCallbacks(drawRunner)
        }
        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawFrame()
        }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false; handler.removeCallbacks(drawRunner); super.onSurfaceDestroyed(holder)
        }
        override fun onDestroy() {
            visible = false; handler.removeCallbacks(drawRunner); super.onDestroy()
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) render(canvas)
            } catch (_: Exception) {
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
            frame++
        }

        private fun render(c: Canvas) {
            val w=c.width; val h=c.height
            c.drawColor(Color.rgb(2, 4, 12))
            val size = (w / 48f).coerceIn(10f, 22f)
            paint.textSize=size
            val cw=paint.measureText("M")
            val ch=size*1.18f
            val cols=(w/cw).toInt().coerceAtLeast(1)
            val rows=(h/ch).toInt().coerceAtLeast(1)
            for (y in 0 until rows) for (x in 0 until cols) {
                val char = when(mode) {
                    1 -> rain(x,y,rows)
                    2 -> wave(x,y,rows)
                    else -> blackHole(x,y,cols,rows)
                }
                if (char != ' ') {
                    paint.color=when(mode) {
                        1 -> Color.rgb(62,220,151)
                        2 -> Color.rgb(74,190,255)
                        else -> Color.rgb(125,145,255)
                    }
                    paint.alpha=if ((x*3+y+frame/4)%9==0) 255 else 145
                    c.drawText(char.toString(),x*cw,(y+1)*ch,paint)
                }
            }
        }
        private fun blackHole(x:Int,y:Int,cols:Int,rows:Int):Char {
            val a=frame*.025f
            val nx=(x-cols/2f)/(cols*.30f)
            val ny=(y-rows/2f)/(rows*.28f)
            val xx=nx*cos(a)-ny*sin(a); val yy=nx*sin(a)+ny*cos(a)
            val d=max(abs(xx),abs(yy))
            if (d in .45f.. .54f) return "#%+*"[((x+y+frame/8) ushr 1)%4]
            val hole=sqrt(nx*nx+ny*ny)
            if (hole<.25f) return if ((x+y)%5==0) '.' else ' '
            val ring=abs(sqrt((nx*nx)*.7f+ny*ny)-.38f)
            return if (ring<.035f) ".*o0"[((x+y+frame/6)%4)] else ' '
        }
        private fun rain(x:Int,y:Int,rows:Int):Char {
            val head=(x*17+3)%max(1,rows)
            return if ((y+frame/2+x*7)%max(1,rows)==head || (x*13+y*3+frame/3)%31==0) "01アイ"[((x+y+frame/3)%4)] else ' '
        }
        private fun wave(x:Int,y:Int,rows:Int):Char {
            val center=rows/2f+sin(x*.16f+frame*.055f)*rows*.22f
            val d=abs(y-center)
            return if (d<1.5f) "@#*+."[((x+y+frame/5)%5)] else if (d<3f && (x+y)%2==0) ':' else ' '
        }
    }
}
