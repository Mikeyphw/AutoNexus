package io.github.mikeyphw.autonexus.termuxtasker

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Native, theme-aware and lightweight, for the original plugin process.
 * No Compose/material dependency is inserted into a foreign classloader.
 * Controls remain semantic Android widgets for TalkBack and font scaling. */
internal class EditorUiKit(private val activity: Activity) {
    private val dark = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
    val canvas: Int = Color.parseColor(if (dark) "#101820" else "#F4F7FA")
    private val cardColor = Color.parseColor(if (dark) "#1D2A35" else "#FFFFFF")
    private val ink = Color.parseColor(if (dark) "#F6F8FC" else "#172736")
    private val muted = Color.parseColor(if (dark) "#B7C6D0" else "#516273")
    private val accent = Color.parseColor(if (dark) "#8CE7CF" else "#006C57")
    private val border = Color.parseColor(if (dark) "#455562" else "#CCD8DF")

    private fun px(n: Int) = (n * activity.resources.displayMetrics.density).toInt()
    private fun bg(color: Int, radius: Int, stroke: Boolean = false) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = px(radius).toFloat()
        if (stroke) setStroke(px(1), border)
    }
    fun column() = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    fun heading(value: String) = TextView(activity).apply {
        text = value; textSize = 21f; setTextColor(ink)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, px(4), 0, px(6))
    }
    fun label(value: String) = TextView(activity).apply {
        text = value; textSize = 14f; setTextColor(ink)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, px(4), 0, px(4))
    }
    fun secondary(value: String) = TextView(activity).apply {
        text = value; textSize = 13f; setTextColor(muted)
        setPadding(0, px(3), 0, px(8))
    }
    fun card() = column().apply {
        background = bg(cardColor, 18, true)
        setPadding(px(14), px(12), px(14), px(13))
        val margin = LinearLayout.LayoutParams(-1, -2)
        margin.setMargins(0, px(12), 0, px(3))
        layoutParams = margin
        elevation = px(2).toFloat()
    }
    fun styleInput(edit: EditText) {
        edit.setTextColor(ink)
        edit.setHintTextColor(muted)
        edit.textSize = 15f
        edit.background = bg(cardColor, 12, true)
        edit.setPadding(px(13), px(10), px(13), px(10))
        edit.minHeight = px(48)
        edit.setSelectAllOnFocus(false)
    }
    fun action(label: String, primary: Boolean = false, onClick: () -> Unit) = Button(activity).apply {
        text = label; isAllCaps = false; textSize = 13f
        contentDescription = label
        background = bg(if (primary) accent else cardColor, 13, !primary)
        setTextColor(if (primary) canvas else ink)
        minHeight = px(48)
        setPadding(px(10), px(4), px(10), px(4))
        setOnClickListener { onClick() }
    }
    fun nav(label: String, selected: Boolean, onClick: () -> Unit) = Button(activity).apply {
        text = label; isAllCaps = false; textSize = 13f
        contentDescription = "$label section"
        background = bg(if (selected) accent else cardColor, 22, !selected)
        setTextColor(if (selected) canvas else ink)
        setPadding(px(14), 0, px(14), 0)
        minHeight = px(43)
        setOnClickListener { onClick() }
    }
}
