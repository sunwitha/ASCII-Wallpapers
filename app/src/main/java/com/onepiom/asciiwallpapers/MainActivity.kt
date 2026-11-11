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
    private lateinit var title: TextView
    private lateinit var desc: TextView
    private val prefs by lazy { getSharedPreferences("wallpaper", MODE_PRIVATE) }
    private val names = arrayOf("Чёрная дыра · Куб", "Цифровой дождь", "Неоновая волна", "Летающая 3D-сфера", "Вращающийся тор", "Космическая спираль")
    private val descriptions = arrayOf(
        "Гравитационное кольцо и вращающийся ASCII-куб.",
        "Поток символов с регулируемой плотностью.",
        "Движущаяся многослойная неоновая волна.",
        "Объёмный шар плавно перемещается по всему экрану.",
        "Вращающийся объёмный тор из ASCII-символов.",
        "Космическая спираль, звёзды и светящееся ядро."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = prefs.getInt("mode", 0).coerceIn(names.indices)
        window.statusBarColor = Color.rgb(8, 10, 18)
        window.navigationBarColor = Color.rgb(8, 10, 18)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
            setBackgroundColor(Color.rgb(8, 10, 18))
        }
        page.addView(TextView(this).apply {
            text = "ASCII WALLPAPERS 2.0"
            textSize = 23f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setTextColor(Color.WHITE)
        })
        page.addView(TextView(this).apply {
            text = "Живые 3D-обои из ASCII-символов"
            textSize = 14f
            setTextColor(Color.rgb(151, 164, 190))
            setPadding(0, dp(4), 0, dp(12))
        })
        preview = AsciiPreview(this).apply { mode = selected }
        page.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f).apply { bottomMargin = dp(12) })
        title = TextView(this).apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }
        desc = TextView(this).apply { textSize = 13f; setTextColor(Color.rgb(157, 169, 192)); setPadding(0, dp(4), 0, dp(10)) }
        page.addView(title); page.addView(desc); updateText()
        val choose = makeButton("Выбрать обои") {
            AlertDialog.Builder(this).setTitle("Выбери анимацию")
                .setSingleChoiceItems(names, selected) { dialog, which ->
                    selected = which; preview.mode = selected; updateText(); dialog.dismiss()
                }.setNegativeButton("Закрыть", null).show()
        }
        page.addView(choose, LinearLayout.LayoutParams(-1, dp(46)))
        val settings = makeButton("Настройки анимации") { showSettings() }
        page.addView(settings, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(7) })
        val apply = makeButton("Установить живые обои") {
            prefs.edit().putInt("mode", selected).apply()
            try {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, AsciiWallpaperService::class.java))
                }
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Открой настройки обоев и выбери ASCII Wallpapers", Toast.LENGTH_LONG).show()
            }
        }
        apply.setBackgroundColor(Color.rgb(91, 82, 230)); apply.setTextColor(Color.WHITE)
        page.addView(apply, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(7) })
        setContentView(page)
    }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; setOnClickListener { action() }
    }
    private fun updateText() { title.text = names[selected]; desc.text = descriptions[selected] }

    private fun showSettings() {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(8), dp(22), 0) }
        fun addSlider(label: String, key: String, min: Int, max: Int, default: Int) {
            val caption = TextView(this).apply { setTextColor(Color.WHITE); textSize = 14f }
            val seek = SeekBar(this).apply { this.max = max - min; progress = (prefs.getInt(key, default) - min).coerceIn(0, max - min) }
            fun refresh(v: Int) { caption.text = "$label: ${v + min}" }
            refresh(seek.progress)
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) { refresh(value) }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
            container.addView(caption); container.addView(seek)
            seek.tag = key
        }
        addSlider("Скорость", "speed", 1, 10, 5)
        addSlider("Размер объектов", "size", 1, 10, 5)
        addSlider("Плотность символов", "density", 1, 10, 7)
        addSlider("Яркость", "brightness", 1, 10, 8)
        val paletteLabel = TextView(this).apply { text = "Цветовая тема"; setTextColor(Color.WHITE); textSize = 14f; setPadding(0, dp(12), 0, dp(4)) }
        container.addView(paletteLabel)
        val palettes = arrayOf("Синий", "Зелёный", "Розовый", "Оранжевый")
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, palettes)
            setSelection(prefs.getInt("palette", 0).coerceIn(0, 3))
        }
        container.addView(spinner)
        val scroll = ScrollView(this).apply { addView(container) }
        val dialog = AlertDialog.Builder(this).setTitle("Настройки обоев").setView(scroll)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                for (i in 0 until container.childCount) {
                    val view = container.getChildAt(i)
                    if (view is SeekBar) {
                        val key = view.tag as String
                        val min = 1
                        prefs.edit().putInt(key, view.progress + min).apply()
                    }
                }
                prefs.edit().putInt("palette", spinner.selectedItemPosition).putInt("mode", selected).apply()
                preview.invalidate()
                Toast.makeText(this, "Настройки сохранены. Примените обои заново, если они уже установлены.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

class AsciiPreview(context: android.content.Context) : View(context) {
    var mode = 0
        set(value) { field = value; invalidate() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("monospace", Typeface.BOLD); textSize = 11f }
    private var tick = 0
    private val handler = android.os.Handler()
    private val loop = object : Runnable { override fun run() { tick++; invalidate(); handler.postDelayed(this, 70) } }
    init { handler.post(loop) }
    override fun onDetachedFromWindow() { handler.removeCallbacks(loop); super.onDetachedFromWindow() }
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.rgb(3, 5, 13))
        val cellW = paint.measureText("M"); val cellH = paint.textSize * 1.15f
        val cols = (width / cellW).toInt().coerceAtLeast(1); val rows = (height / cellH).toInt().coerceAtLeast(1)
        val t = tick * .04f
        for (y in 0 until rows) for (x in 0 until cols) {
            val nx = (x - cols / 2f) / (cols * .5f); val ny = (y - rows / 2f) / (rows * .5f)
            val ch = when (mode) {
                1 -> if ((y + tick / 2 + x * 7) % 17 == (x * 17 + 3) % rows) "01#"[(x + y) % 3] else ' '
                2 -> if (kotlin.math.abs(y - (rows / 2f + kotlin.math.sin(x * .16f + t) * rows * .22f)) < 2) "@#*+."[(x + y + tick / 5) % 5] else ' '
                3 -> spherePreview(nx, ny, cols, rows, t)
                4 -> { val a=t*.65f; val xx=nx*kotlin.math.cos(a)-ny*kotlin.math.sin(a); val yy=nx*kotlin.math.sin(a)+ny*kotlin.math.cos(a); if (kotlin.math.abs(kotlin.math.sqrt(xx*xx+yy*yy)-.43f)<.07f) " .:*#@"[(x+y+tick/4)%6] else ' ' }
                5 -> { val r=kotlin.math.sqrt(nx*nx+ny*ny); if (r<.8f && kotlin.math.abs(kotlin.math.sin(kotlin.math.atan2(ny,nx)*3-r*17+t))<.13f) ".:*+#"[(x+y+tick/4)%5] else ' ' }
                else -> cubePreview(nx, ny, x, y, t)
            }
            if (ch != ' ') {
                paint.color = when (mode) { 1 -> Color.rgb(60,220,150); 2 -> Color.rgb(80,205,255); 3 -> Color.rgb(255,105,190); 4 -> Color.rgb(255,174,75); 5 -> Color.rgb(150,125,255); else -> Color.rgb(115,143,255) }
                paint.alpha = if ((x + y + tick / 3) % 7 == 0) 255 else 160
                canvas.drawText(ch.toString(), x * cellW, (y + 1) * cellH, paint)
            }
        }
        paint.alpha = 255
    }
    private fun spherePreview(nx:Float,ny:Float,cols:Int,rows:Int,t:Float):Char {
        val px=.45f*kotlin.math.sin(t*.62f); val py=.34f*kotlin.math.sin(t*.91f+1.2f)
        val sx=(nx-px)*(cols.toFloat()/rows.coerceAtLeast(1)); val sy=ny-py; val r2=sx*sx+sy*sy
        if(r2>.30f*.30f) return ' '
        val z=kotlin.math.sqrt((.09f-r2).coerceAtLeast(0f))/.30f
        val light=((-.45f*sx-.55f*sy+.75f*z)*.5f+.5f).coerceIn(0f,1f)
        val chars=" .,:-~=+*#%@"; return chars[(light*(chars.length-1)).toInt().coerceIn(0,chars.lastIndex)]
    }
    private fun cubePreview(nx:Float,ny:Float,x:Int,y:Int,t:Float):Char {
        val a=t*.8f; val xx=nx*kotlin.math.cos(a)-ny*kotlin.math.sin(a); val yy=nx*kotlin.math.sin(a)+ny*kotlin.math.cos(a)
        val edge=maxOf(kotlin.math.abs(xx),kotlin.math.abs(yy))
        return if(edge in .48f.. .58f) "#%+*"[(x+y+tick/5)%4] else if(edge<.47f && (x+y)%3==0) '.' else ' '
    }
}
