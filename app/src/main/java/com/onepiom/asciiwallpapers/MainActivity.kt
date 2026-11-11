package com.onepiom.asciiwallpapers

import android.app.Activity
import android.app.WallpaperManager
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
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
    private val bg = Color.rgb(7, 8, 18)
    private val panel = Color.rgb(17, 18, 38)
    private val purple = Color.rgb(153, 102, 255)
    private val muted = Color.rgb(166, 169, 198)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected = prefs.getInt("mode", 0).coerceIn(names.indices)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = text("◇", 34f, purple, true).apply { gravity = Gravity.CENTER; setPadding(0,0,dp(12),0) }
        header.addView(logo, LinearLayout.LayoutParams(dp(48), dp(54)))
        val heading = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        heading.addView(text("ASCII WALLPAPERS 2.0", 22f, Color.WHITE, true))
        heading.addView(text("Живые 3D-обои из ASCII-символов", 13f, muted))
        header.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(text("⚙", 26f, purple, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { showSettings() }
        }, LinearLayout.LayoutParams(dp(42), dp(48)))
        root.addView(header, LinearLayout.LayoutParams(-1, dp(64)))

        preview = AsciiPreview(this).apply { mode = selected; background = rounded(Color.BLACK, 20, purple, 1) }
        root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(14); bottomMargin = dp(12) })

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel, 16, Color.rgb(55,45,95), 1)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        info.addView(text("◇", 25f, purple, true), LinearLayout.LayoutParams(dp(38), -2))
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        title = text("", 17f, Color.WHITE, true)
        desc = text("", 12f, muted)
        labels.addView(title); labels.addView(desc)
        info.addView(labels, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(10) })
        info.addView(text("3D", 12f, purple, true).apply { background = rounded(Color.rgb(48,28,85), 8); setPadding(dp(8),dp(4),dp(8),dp(4)) })
        root.addView(info, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        updateText()

        root.addView(actionCard("▧", "Выбор обоев", "Просмотр и выбор анимации", "›") { showWallpaperPicker() },
            LinearLayout.LayoutParams(-1, dp(82)).apply { bottomMargin = dp(10) })
        root.addView(actionCard("⚙", "Настройки", "Скорость, размер, цвет и эффекты", "›") { showSettings() },
            LinearLayout.LayoutParams(-1, dp(82)).apply { bottomMargin = dp(12) })
        val apply = TextView(this).apply {
            text = "◉     УСТАНОВИТЬ ЖИВЫЕ ОБОИ      ›"
            textSize = 15f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); background = rounded(purple, 18)
            setOnClickListener {
                prefs.edit().putInt("mode", selected).apply()
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
        root.addView(apply, LinearLayout.LayoutParams(-1, dp(56)))
        setContentView(root)
    }

    private fun actionCard(icon: String, heading: String, sub: String, arrow: String, action: () -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = rounded(panel, 17, purple, 1)
            isClickable = true; isFocusable = true; setOnClickListener { action() }
            addView(text(icon, 27f, purple, true), LinearLayout.LayoutParams(dp(48), -2))
            val block = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            block.addView(text(heading, 17f, Color.WHITE, true))
            block.addView(text(sub, 12f, muted))
            addView(block, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(10) })
            addView(text(arrow, 30f, purple, true))
        }

    private fun showWallpaperPicker() {
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
        val scroll = ScrollView(this).apply { addView(list) }
        val dialog = AlertDialog.Builder(this).setTitle("Выбери свои обои").setView(scroll)
            .setNegativeButton("Закрыть", null).create()
        names.indices.forEach { i ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                background = rounded(if (i == selected) Color.rgb(43,31,75) else panel, 14,
                    if (i == selected) purple else Color.rgb(53,54,80), 1)
                setPadding(dp(10), dp(8), dp(12), dp(8))
                isClickable = true; isFocusable = true
                setOnClickListener {
                    selected = i; preview.mode = selected; updateText()
                    prefs.edit().putInt("mode", selected).apply()
                    dialog.dismiss()
                }
            }
            val mini = AsciiPreview(this).apply { mode = i }
            row.addView(mini, LinearLayout.LayoutParams(dp(72), dp(72)))
            val textBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            textBox.addView(text(names[i], 15f, Color.WHITE, true))
            textBox.addView(text(descriptions[i], 11f, muted))
            row.addView(textBox, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(12) })
            row.addView(text(if (i == selected) "✓" else "›", 22f, purple, true))
            list.addView(row, LinearLayout.LayoutParams(-1, dp(92)).apply { bottomMargin = dp(8) })
        }
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(rounded(Color.rgb(13,14,29), 22))
            styleDialogButtons(dialog)
        }
        dialog.show()
        dialog.window?.setBackgroundDrawable(rounded(Color.rgb(13,14,29), 22))
        styleDialogButtons(dialog)
    }

    private fun showSettings() {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(8), dp(22), dp(4)) }
        fun addSlider(label: String, key: String, min: Int, max: Int, default: Int) {
            val caption = text("", 14f, Color.WHITE, true)
            val seek = SeekBar(this).apply {
                this.max = max - min
                progress = (prefs.getInt(key, default) - min).coerceIn(0, max - min)
                progressTintList = android.content.res.ColorStateList.valueOf(purple)
                thumbTintList = android.content.res.ColorStateList.valueOf(purple)
            }
            fun refresh(v: Int) { caption.text = "$label     ${v + min}/$max" }
            refresh(seek.progress)
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) { refresh(value) }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
            container.addView(caption); container.addView(seek, LinearLayout.LayoutParams(-1, dp(42)))
            seek.tag = "$key|$min"
        }
        addSlider("Скорость анимации", "speed", 1, 10, 5)
        addSlider("Размер объектов", "size", 1, 10, 5)
        addSlider("Плотность символов", "density", 1, 10, 7)
        addSlider("Яркость свечения", "brightness", 1, 10, 8)
        container.addView(text("Цветовая тема", 14f, Color.WHITE, true).apply { setPadding(0, dp(12), 0, dp(6)) })
        val palettes = arrayOf("Синий", "Зелёный", "Розовый", "Оранжевый")
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, palettes)
            setSelection(prefs.getInt("palette", 0).coerceIn(0, 3))
        }
        container.addView(spinner)
        val scroll = ScrollView(this).apply { addView(container) }
        val dialog = AlertDialog.Builder(this).setTitle("Настройки анимации").setView(scroll)
            .setNegativeButton("Отмена", null).setPositiveButton("Сохранить", null).create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(rounded(Color.rgb(13,14,29), 22))
            styleDialogButtons(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                for (i in 0 until container.childCount) {
                    val view = container.getChildAt(i)
                    if (view is SeekBar) {
                        val parts = (view.tag as String).split("|")
                        prefs.edit().putInt(parts[0], view.progress + parts[1].toInt()).apply()
                    }
                }
                prefs.edit().putInt("palette", spinner.selectedItemPosition).putInt("mode", selected).apply()
                preview.invalidate()
                Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
        dialog.window?.setBackgroundDrawable(rounded(Color.rgb(13,14,29), 22))
        styleDialogButtons(dialog)
    }
private fun updateText() {
    preview.mode = selected
    preview.invalidate()
}
    private fun styleDialogButtons(dialog: AlertDialog) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(purple)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(muted)
    }
    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }
    private fun rounded(color: Int, radius: Int, stroke: Int? = null, strokeDp: Int = 1) =
        GradientDrawable().apply {
            setColor(color); cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(strokeDp), stroke)
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
