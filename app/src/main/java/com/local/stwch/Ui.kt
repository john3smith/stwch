package com.local.stwch

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.*
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

object Palette {
    val background = Color.rgb(248,245,240)
    val ink = Color.rgb(35,36,33)
    val muted = Color.rgb(113,115,109)
    val coral = Color.rgb(236,118,94)
    val peach = Color.rgb(251,224,212)
    val mint = Color.rgb(223,234,219)
}

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density + .5f).toInt()
fun Context.card(color: Int, radius: Int = 24): GradientDrawable = GradientDrawable().apply {
    setColor(color); cornerRadius = dp(radius).toFloat()
}
fun Context.label(text: String, size: Float = 16f, color: Int = Palette.ink, bold: Boolean = false) = TextView(this).apply {
    this.text = text; textSize = size; setTextColor(color)
    if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
}
fun Context.pill(text: String, color: Int = Palette.peach) = Button(this).apply {
    this.text = text; isAllCaps = false; textSize = 15f; setTextColor(Palette.ink)
    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    background = RippleDrawable(ColorStateList.valueOf(0x22000000), card(color, 30), null)
    setPadding(dp(18), dp(10), dp(18), dp(10)); minHeight = dp(52); minimumWidth = 0
}

fun Activity.page(): LinearLayout {
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setBackgroundColor(Palette.background)
    }
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        view.setPadding(dp(22) + bars.left, dp(12) + bars.top, dp(22) + bars.right, dp(14) + bars.bottom)
        insets
    }
    setContentView(root); ViewCompat.requestApplyInsets(root)
    return root
}

class StopwatchFace(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val playPath = Path()
    private val clockTypeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    var elapsed = 0L
    var status = WatchStatus.IDLE
    init { id = R.id.center_timer; isClickable = true; isFocusable = true }
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onDraw(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val cx = width / 2f; val cy = height / 2f
        val radius = minOf(width, height) / 2f - 7 * density
        paint.shader = null; paint.style = Paint.Style.FILL; paint.color = Palette.peach
        canvas.drawCircle(cx, cy, radius, paint)
        paint.color = 0xFFFDF7F1.toInt(); canvas.drawCircle(cx, cy, radius - 12 * density, paint)
        paint.color = Palette.coral; paint.style = Paint.Style.STROKE; paint.strokeWidth = 3 * density
        canvas.drawArc(cx - radius + 7*density, cy-radius+7*density,
            cx+radius-7*density, cy+radius-7*density, -90f,
            if (status == WatchStatus.RUNNING) (elapsed % 60000) / 60000f * 360 else 36f, false, paint)
        paint.style = Paint.Style.FILL; paint.color = Palette.ink
        paint.typeface = clockTypeface; paint.textAlign = Paint.Align.CENTER
        val formatted = WatchFormat.time(elapsed)
        val main = formatted.substringBeforeLast('.')
        paint.textSize = 55 * density
        if (paint.measureText(main) > radius * 1.65f) paint.textSize *= radius * 1.65f / paint.measureText(main)
        canvas.drawText(main, cx, cy - 4 * density, paint)
        paint.textSize = 27 * density; paint.color = Palette.muted
        canvas.drawText("." + formatted.substringAfterLast('.'), cx, cy + 32 * density, paint)
        paint.color = Palette.coral
        val iconY = cy - 79 * density
        if (status == WatchStatus.RUNNING) {
            canvas.drawRoundRect(cx-7*density, iconY-7*density, cx-2*density, iconY+7*density, 2*density, 2*density, paint)
            canvas.drawRoundRect(cx+2*density, iconY-7*density, cx+7*density, iconY+7*density, 2*density, 2*density, paint)
        } else {
            playPath.reset(); playPath.moveTo(cx-5*density,iconY-8*density)
            playPath.lineTo(cx+8*density,iconY); playPath.lineTo(cx-5*density,iconY+8*density); playPath.close()
            canvas.drawPath(playPath,paint)
        }
        paint.textSize = 13 * density; paint.color = Palette.muted
        canvas.drawText(context.getString(when(status) {
            WatchStatus.RUNNING -> R.string.tap_pause
            WatchStatus.PAUSED -> R.string.tap_resume
            else -> R.string.tap_start
        }), cx, cy + 74 * density, paint)
    }
}
