package com.floatify.app

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Floating calculator body, built with plain Views (the service has no Compose host). */
object CalculatorView {

    fun create(context: Context): View {
        val d = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        var expr = ""
        val expression = TextView(context).apply {
            textSize = 14f; setTextColor(0xFF777777.toInt()); gravity = Gravity.END
            maxLines = 1
        }
        val result = TextView(context).apply {
            text = "0"; textSize = 30f; setTextColor(0xFF111111.toInt())
            gravity = Gravity.END; maxLines = 1
            setTypeface(typeface, Typeface.BOLD)
        }

        fun refresh() {
            expression.text = expr
            result.text = if (expr.isEmpty()) "0" else (Calc.eval(expr)?.let { Calc.format(it) } ?: "")
        }

        val grid = GridLayout(context).apply { columnCount = 4 }
        val keys = listOf(
            "C", "⌫", "%", "÷",
            "7", "8", "9", "×",
            "4", "5", "6", "-",
            "1", "2", "3", "+",
            "0", ".", "=", ""
        )
        for (k in keys) {
            if (k.isEmpty()) continue
            val isOp = k in listOf("÷", "×", "-", "+", "=")
            val btn = TextView(context).apply {
                text = k; textSize = 18f; gravity = Gravity.CENTER
                setTextColor(if (isOp) Color.WHITE else 0xFF222222.toInt())
                background = GradientDrawable().apply {
                    cornerRadius = 12 * d
                    setColor(if (isOp) 0xFF3F51B5.toInt() else 0xFFEEEEEE.toInt())
                }
                setOnClickListener {
                    when (k) {
                        "C" -> expr = ""
                        "⌫" -> expr = expr.dropLast(1)
                        "=" -> Calc.eval(expr)?.let { expr = Calc.format(it) }
                        else -> expr += k
                    }
                    refresh()
                }
            }
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply {
                width = 0; height = dp(48); setMargins(dp(3), dp(3), dp(3), dp(3))
                if (k == "0") columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            grid.addView(btn, lp)
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            addView(expression)
            addView(result)
            addView(grid)
        }
    }
}

/** Tiny recursive-descent evaluator: + - × ÷ %, parentheses-free, unary minus. */
object Calc {
    fun format(v: Double): String =
        if (v.isNaN() || v.isInfinite()) "Error"
        else if (v == Math.floor(v) && Math.abs(v) < 1e15) v.toLong().toString()
        else "%.10f".format(v).trimEnd('0').trimEnd('.')

    fun eval(input: String): Double? = try {
        Parser(input.replace('×', '*').replace('÷', '/')).parse()
    } catch (e: Exception) { null }

    private class Parser(val s: String) {
        var i = 0
        fun parse(): Double { val v = expr(); require(i == s.length); return v }
        fun expr(): Double {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]; val r = term(); v = if (op == '+') v + r else v - r
            }
            return v
        }
        fun term(): Double {
            var v = unary()
            while (i < s.length && (s[i] == '*' || s[i] == '/')) {
                val op = s[i++]; val r = unary(); v = if (op == '*') v * r else v / r
            }
            return v
        }
        fun unary(): Double {
            if (i < s.length && s[i] == '-') { i++; return -unary() }
            return number()
        }
        fun number(): Double {
            val st = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            var v = s.substring(st, i).toDouble()
            while (i < s.length && s[i] == '%') { i++; v /= 100 }
            return v
        }
    }
}
