package com.onepiom.asciiwallpapers

import android.app.Activity
import android.app.WallpaperManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private var selected = 0
    private lateinit var preview: AsciiPreview
    private val names = arrayOf("Чёрная дыра · Куб", "Цифровой дождь", "Неоновая волна")
    private val descriptions = arrayOf(
        "Вращающийся ASCII-куб и тёмный горизонт событий.",
        "Падающие символы в стиле цифрового дождя.",
        "Плавная волна из символов, которая движется по экрану."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 10, 18)
        window.navigationBarColor = Color.rgb(8, 10, 18)
        window.decorView.systemUiVisibility = 0
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            setBackgroundColor(Color.rgb(8, 10, 18))
        }
        page.addView(TextView(this).apply {
            text = "ASCII WALLPAPERS"
            textSize = 23f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        page.addView(TextView(this).apply {
            text = "Живые обои из символов"
            textSize = 14f
            setTextColor(Color.rgb(151, 164, 190))
            setPadding(0, dp(4), 0, dp(16))
        })
        preview = AsciiPreview(this)
        page.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            bottomMargin = dp(16)
        })
        val title = TextView(this).apply {
            text = names[selected]
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val desc = TextView(this).apply {
            text = descriptions[selected]
            textSize = 13f
            setTextColor(Color.rgb(157, 169, 192))
            setPadding(0, dp(5), 0, dp(14))
        }
        page.addView(title)
        page.addView(desc)
        val choose = Button(this).apply {
            text = "Выбрать другие обои"
            isAllCaps = false
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Выбери анимацию")
                    .setSingleChoiceItems(names, selected) { dialog, which ->
                        selected = which
                        preview.mode = selected
                        title.text = names[selected]
                        desc.text = descriptions[selected]
                        dialog.dismiss()
                    }.setNegativeButton("Закрыть", null).show()
            }
        }
        page.addView(choose, LinearLayout.LayoutParams(-1, dp(48)))
        val apply = Button(this).apply {
            text = "Установить живые обои"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(91, 82, 230))
            setOnClickListener {
                getSharedPreferences("wallpaper", MODE_PRIVATE).edit().putInt("mode", selected).apply()
                try {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(this@MainActivity, AsciiWallpaperService::class.java))
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "Открой настройки обоев и выбери ASCII Wallpapers", Toast.LENGTH_LONG).show()
                }
            }
        }
        val params = LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(9) }
        page.addView(apply, params)
        setContentView(page)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

class AsciiPreview(context: android.content.Context) : View(context) {
    var mode = 0
        set(value) { field = value; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("monospace", Typeface.NORMAL)
        textSize = 11f
    }
    private var tick = 0
    private val handler = android.os.Handler()
    private val loop = object : Runnable {
        override fun run() { tick++; invalidate(); handler.postDelayed(this, 80) }
    }
    init { handler.post(loop) }
    override fun onDetachedFromWindow() { handler.removeCallbacks(loop); super.onDetachedFromWindow() }
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.rgb(3, 5, 13))
        val cellW = paint.measureText("M")
        val cellH = paint.textSize * 1.15f
        val cols = (width / cellW).toInt().coerceAtLeast(1)
        val rows = (height / cellH).toInt().coerceAtLeast(1)
        for (y in 0 until rows) for (x in 0 until cols) {
            val ch = when (mode) {
                0 -> cubeChar(x, y, cols, rows, tick)
                1 -> rainChar(x, y, tick)
                else -> waveChar(x, y, cols, rows, tick)
            }
            if (ch != ' ') {
                paint.color = if (mode == 0) Color.rgb(115, 143, 255)
                    else if (mode == 1) Color.rgb(60, 220, 150)
                    else Color.rgb(80, 205, 255)
                paint.alpha = if ((x + y + tick / 3) % 7 == 0) 255 else 155
                canvas.drawText(ch.toString(), x * cellW, (y + 1) * cellH, paint)
            }
        }
        paint.alpha = 255
    }
    private fun cubeChar(x:Int,y:Int,c:Int,r:Int,t:Int):Char {
        val nx=(x-c/2f)/(c*.34f); val ny=(y-r/2f)/(r*.34f)
        val a=t*.035f; val xx=nx*kotlin.math.cos(a)-ny*kotlin.math.sin(a)
        val yy=nx*kotlin.math.sin(a)+ny*kotlin.math.cos(a)
        val edge=maxOf(kotlin.math.abs(xx),kotlin.math.abs(yy))
        return if (edge in .48f..0.58f) "#%+*"[((x+y+t/5) ushr 1)%4] else if (edge<.47f && (x+y)%3==0) '.' else ' '
    }
    private fun rainChar(x:Int,y:Int,t:Int):Char {
        val head=(x*17+3)%maxOf(1,(height/paint.textSize).toInt())
        return if ((y+ t/2 + x*7)%19==head%19 || (x*13+y*3+t/4)%29==0) "01アイ"[((x+y+t/4)%4)] else ' '
    }
    private fun waveChar(x:Int,y:Int,c:Int,r:Int,t:Int):Char {
        val center=r/2f + kotlin.math.sin(x*.22f+t*.07f)*r*.22f
        val d=kotlin.math.abs(y-center)
        return if (d<1.3f) "@#*+."[((x+y+t/5)%5)] else if (d<2.5f && (x+y)%2==0) ':' else ' '
    }
}
