package org.openkeyboard.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import org.openkeyboard.engine.OpenKeyboardEngine

class KeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnKeyboardActionListener {
        fun onKeyText(text: String)
        fun onBackspace()
        fun onSpace()
        fun onEnter()
        fun onSwitchLayout()
        fun onSelectKeymap(keymapId: String)
        fun onOpenSettings()
        fun onSearch()
        fun onSuggestion(suggestion: String)
    }

    data class DropdownItem(val id: String, val displayName: String, val isActive: Boolean)

    var listener: OnKeyboardActionListener? = null
    var engine: OpenKeyboardEngine? = null
        set(value) {
            field = value
            smallShiftSymbol = value?.parsedKeymap?.info?.smallShiftSymbol?.ifBlank { "★" } ?: "★"
            computeKeyLayout()
            invalidate()
        }

    var layoutName: String = "English"
        set(value) {
            field = value
            computeKeyLayout()
            invalidate()
        }

    var smallShiftSymbol: String = "★"
        set(value) {
            field = if (value.isBlank()) "★" else value
            computeKeyLayout()
            invalidate()
        }

    var currentBufferText: String = ""
        set(value) {
            field = value
            invalidate()
        }

    // Text suggestions for header suggestion strip
    var currentSuggestions: List<String> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    val isEnglishLayout: Boolean
        get() = layoutName.equals("English", ignoreCase = true) ||
                layoutName.equals("QWERTY", ignoreCase = true) ||
                (engine?.parsedKeymap?.info?.language?.equals("English", ignoreCase = true) == true)

    // Dropdown list of enabled keymaps
    var enabledKeymaps: List<DropdownItem> = emptyList()
        set(value) {
            field = value
            computeDropdownMenuRect()
            invalidate()
        }

    var isDropdownOpen = false
        private set

    private var pressedDropdownIndex = -1
    private val dropdownMenuRect = RectF()
    private val langKeyRect = RectF()

    // Shift Modes:
    // English layout: Standard Shift toggle (TAMIL = lowercase a-z, CAPS_ENGLISH = uppercase A-Z)
    // Tamil layout: 3-Stage cycle (TAMIL -> CAPS_ENGLISH -> SMALL_ENGLISH ★ -> TAMIL)
    enum class ShiftMode {
        TAMIL,          // Normal native / lowercase English mode
        CAPS_ENGLISH,   // 1st press: CAPS English (A, B, C...)
        SMALL_ENGLISH   // 2nd press (Tamil mode): small English (a, b, c...) with star symbol
    }

    var shiftMode = ShiftMode.TAMIL
        private set

    // Gboard Action Key Types (Enter vs Search vs Go vs Send etc.)
    enum class ActionKeyType {
        ENTER, SEARCH, GO, SEND, NEXT, DONE
    }

    var actionKeyType: ActionKeyType = ActionKeyType.ENTER
        set(value) {
            field = value
            computeKeyLayout()
            invalidate()
        }

    private var isSymbols = false
    private var isEmoji = false
    val isEmojiMode: Boolean get() = isEmoji

    // Gboard Official Dark Palette
    private val bgPaint = Paint().apply { color = Color.parseColor("#1C1B1F") }
    private val headerBgPaint = Paint().apply { color = Color.parseColor("#151417") }
    private val headerDividerPaint = Paint().apply { color = Color.parseColor("#2B2930") }
    private val chipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2B2930") }

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2B2930") }
    private val specialKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E1D22") }
    private val spaceKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2B2930") }
    private val enterKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#A8C7FA") } // Google Blue
    private val keyPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#44424A") }
    private val shiftActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#A8C7FA") }

    // Dropdown Palette
    private val dropdownBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#25232A") }
    private val dropdownBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F3D47")
        style = Paint.Style.STROKE
    }
    private val dropdownActiveBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#26364D") }
    private val dropdownPressedBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3D3A45") }
    private val dropdownTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6E1E5")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val dropdownActiveTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A8C7FA")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val dropdownSubTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C4C7C5")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val dropdownDividerPaint = Paint().apply {
        color = Color.parseColor("#38363F")
    }
    private val scrimPaint = Paint().apply { color = Color.parseColor("#77000000") }

    // Text paints
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6E1E5")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val specialTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C4C7C5")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val enterTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#062E6F")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7FCFFF") // Subtle cyan hint badge
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val spaceSubTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8E918F")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val suggestionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C4C7C5")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    private val suggestionCenterTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A8C7FA")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // Vector Stroke Paint for Gboard Search Magnifying Glass
    private val iconStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    // Popup Key Preview Bubble
    private val popupBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38363D")
        style = Paint.Style.FILL
    }
    private val popupTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private class KeyRect(
        val rect: RectF,
        val code: String,
        val label: String,
        val type: KeyType = KeyType.NORMAL,
        var isPressed: Boolean = false
    )

    private enum class KeyType {
        NORMAL, SPECIAL, SPACE, ENTER, SHIFT, DEL, SYM, SMILEY, LANG
    }

    private val keyRects = ArrayList<KeyRect>()
    private var pressedKey: KeyRect? = null

    private val HEADER_HEIGHT_DP = 36f
    private val KEY_MARGIN_DP = 3.5f
    private val KEY_CORNER_RADIUS_DP = 7f

    private val qwertyRow1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    private val qwertyRow2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    private val qwertyRow3 = listOf("z", "x", "c", "v", "b", "n", "m")

    private val symbolRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val symbolRow2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
    private val symbolRow3 = listOf("*", "\"", "'", ":", ";", "!", "?")

    // Smiley / Emoji Rows
    private val emojiRow1 = listOf("😀", "😂", "😍", "🥰", "😎", "😊", "🙏", "👍", "❤️", "🔥")
    private val emojiRow2 = listOf("🤣", "😅", "😋", "😘", "🤩", "🥳", "👏", "🎉", "✨")
    private val emojiRow3 = listOf("🤝", "🙌", "💐", "💯", "💪", "🤔", "👌")

    private val headerRect = RectF()
    private val suggestionRects = Array(3) { RectF() }
    private val searchHeaderRect = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val density = resources.displayMetrics.density
        val targetHeight = (density * 255).toInt()
        setMeasuredDimension(width, targetHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeKeyLayout()
        computeDropdownMenuRect()
    }

    fun toggleSymbols() {
        if (isDropdownOpen) isDropdownOpen = false
        if (isEmoji) isEmoji = false
        isSymbols = !isSymbols
        computeKeyLayout()
        invalidate()
    }

    fun toggleEmoji() {
        if (isDropdownOpen) isDropdownOpen = false
        if (isSymbols) isSymbols = false
        isEmoji = !isEmoji
        computeKeyLayout()
        invalidate()
    }

    fun toggleShift() {
        if (isDropdownOpen) isDropdownOpen = false
        if (isEmoji) isEmoji = false
        if (isEnglishLayout) {
            shiftMode = if (shiftMode == ShiftMode.CAPS_ENGLISH) ShiftMode.TAMIL else ShiftMode.CAPS_ENGLISH
        } else {
            shiftMode = when (shiftMode) {
                ShiftMode.TAMIL -> ShiftMode.CAPS_ENGLISH
                ShiftMode.CAPS_ENGLISH -> ShiftMode.SMALL_ENGLISH
                ShiftMode.SMALL_ENGLISH -> ShiftMode.TAMIL
            }
        }
        computeKeyLayout()
        invalidate()
    }

    fun resetShift() {
        shiftMode = ShiftMode.TAMIL
        isEmoji = false
        isSymbols = false
        isDropdownOpen = false
        computeKeyLayout()
        invalidate()
    }

    private fun dp(dpVal: Float): Float {
        return dpVal * resources.displayMetrics.density
    }

    private fun computeDropdownMenuRect() {
        val itemH = dp(38f)
        val totalItems = (enabledKeymaps.size + 1).coerceAtLeast(1)
        val menuH = totalItems * itemH + dp(8f)
        val menuW = dp(220f).coerceAtMost(width - dp(16f))
        val left = langKeyRect.left.coerceAtMost(width - menuW - dp(8f)).coerceAtLeast(dp(8f))
        val top = (langKeyRect.top - dp(6f) - menuH).coerceAtLeast(dp(HEADER_HEIGHT_DP) + dp(4f))
        val bottom = langKeyRect.top - dp(4f)
        dropdownMenuRect.set(left, top, left + menuW, bottom)
    }

    private fun computeKeyLayout() {
        keyRects.clear()
        val totalW = width.toFloat()
        val totalH = height.toFloat()
        if (totalW <= 0 || totalH <= 0) return

        val headerH = dp(HEADER_HEIGHT_DP)
        val keyboardH = totalH - headerH
        val margin = dp(KEY_MARGIN_DP)
        val rowH = (keyboardH - 5 * margin) / 4f

        // Top Header Rect (Dedicated for Text Suggestions!)
        headerRect.set(0f, 0f, totalW, headerH)
        val searchChipW = dp(36f)
        searchHeaderRect.set(totalW - margin - searchChipW, dp(6f), totalW - margin, headerH - dp(6f))

        // Divide Suggestion Strip into 3 Slots
        val availSuggestW = totalW - (if (actionKeyType == ActionKeyType.SEARCH) searchChipW + 2 * margin else 2 * margin)
        val slotW = availSuggestW / 3f
        for (i in 0..2) {
            suggestionRects[i].set(margin + i * slotW, 0f, margin + (i + 1) * slotW, headerH)
        }

        // Row 1 (10 keys)
        val r1 = when {
            isEmoji -> emojiRow1
            isSymbols -> symbolRow1
            else -> qwertyRow1
        }
        val r1KeyW = (totalW - (r1.size + 1) * margin) / r1.size
        val y1Top = headerH + margin
        val y1Bottom = y1Top + rowH
        for (i in r1.indices) {
            val key = r1[i]
            val xLeft = margin + i * (r1KeyW + margin)
            val code = if (!isEmoji && !isSymbols && shiftMode == ShiftMode.CAPS_ENGLISH) key.uppercase() else key
            keyRects.add(KeyRect(RectF(xLeft, y1Top, xLeft + r1KeyW, y1Bottom), code, code))
        }

        // Row 2 (9 keys) - Indented with 0.5 * keyWidth on both sides
        val r2 = when {
            isEmoji -> emojiRow2
            isSymbols -> symbolRow2
            else -> qwertyRow2
        }
        val r2KeyW = r1KeyW
        val r2Indent = (totalW - (r2.size * r2KeyW + (r2.size - 1) * margin)) / 2f
        val y2Top = y1Bottom + margin
        val y2Bottom = y2Top + rowH
        for (i in r2.indices) {
            val key = r2[i]
            val xLeft = r2Indent + i * (r2KeyW + margin)
            val code = if (!isEmoji && !isSymbols && shiftMode == ShiftMode.CAPS_ENGLISH) key.uppercase() else key
            keyRects.add(KeyRect(RectF(xLeft, y2Top, xLeft + r2KeyW, y2Bottom), code, code))
        }

        // Row 3: Shift/ABC (left), 7 center keys, Backspace (right)
        val r3 = when {
            isEmoji -> emojiRow3
            isSymbols -> symbolRow3
            else -> qwertyRow3
        }
        val y3Top = y2Bottom + margin
        val y3Bottom = y3Top + rowH
        val specialW = r1KeyW * 1.45f
        val centerW = (totalW - 2 * specialW - (r3.size + 3) * margin) / r3.size

        // Shift / ABC key
        val shiftKeyRect = RectF(margin, y3Top, margin + specialW, y3Bottom)
        if (isEmoji) {
            keyRects.add(KeyRect(shiftKeyRect, "ABC", "ABC", KeyType.SYM))
        } else if (isSymbols) {
            keyRects.add(KeyRect(shiftKeyRect, "ABC", "ABC", KeyType.SYM))
        } else {
            val shiftLabel = if (isEnglishLayout) {
                if (shiftMode == ShiftMode.CAPS_ENGLISH) "\u25B2" else "\u21E7" // ▲ (CAPS) or ⇧ (lowercase)
            } else {
                when (shiftMode) {
                    ShiftMode.TAMIL -> "\u21E7"          // ⇧
                    ShiftMode.CAPS_ENGLISH -> "\u25B2"   // ▲ (CAPS)
                    ShiftMode.SMALL_ENGLISH -> smallShiftSymbol // Star sign ★ by default, or XML-specified!
                }
            }
            keyRects.add(KeyRect(shiftKeyRect, "SHIFT", shiftLabel, KeyType.SHIFT))
        }

        // 7 Center keys
        var currentX = margin + specialW + margin
        for (i in r3.indices) {
            val key = r3[i]
            val code = if (!isEmoji && !isSymbols && shiftMode == ShiftMode.CAPS_ENGLISH) key.uppercase() else key
            keyRects.add(KeyRect(RectF(currentX, y3Top, currentX + centerW, y3Bottom), code, code))
            currentX += centerW + margin
        }

        // Backspace
        val delKeyRect = RectF(currentX, y3Top, totalW - margin, y3Bottom)
        keyRects.add(KeyRect(delKeyRect, "DEL", "\u232B", KeyType.DEL))

        // Row 4 (Bottom Bar): ?123, Smiley 😊, Keyboard 🌐 ▾ (next to smilys!), Space, Period, Enter/Search
        val y4Top = y3Bottom + margin
        val y4Bottom = y4Top + rowH
        val symW = r1KeyW * 1.22f
        val smileyW = r1KeyW * 0.95f
        val langW = r1KeyW * 0.95f
        val dotW = r1KeyW * 0.95f
        val enterW = r1KeyW * 1.35f
        val spaceW = totalW - (symW + smileyW + langW + dotW + enterW) - (7 * margin)

        var x4 = margin
        // 1. ?123 / ABC
        keyRects.add(KeyRect(RectF(x4, y4Top, x4 + symW, y4Bottom), "SYM", if (isSymbols || isEmoji) "ABC" else "?123", KeyType.SYM))
        x4 += symW + margin

        // 2. Smiley 😊 button
        val smileyLabel = if (isEmoji) "ABC" else "\uD83D\uDE0A" // 😊
        keyRects.add(KeyRect(RectF(x4, y4Top, x4 + smileyW, y4Bottom), "SMILEY", smileyLabel, KeyType.SMILEY))
        x4 += smileyW + margin

        // 3. Keyboard Selection Dropdown button 🌐 (Next to Smilys!)
        langKeyRect.set(x4, y4Top, x4 + langW, y4Bottom)
        keyRects.add(KeyRect(RectF(x4, y4Top, x4 + langW, y4Bottom), "LANG", "\uD83C\uDF10", KeyType.LANG))
        x4 += langW + margin

        // 4. Spacebar
        keyRects.add(KeyRect(RectF(x4, y4Top, x4 + spaceW, y4Bottom), "SPACE", "space", KeyType.SPACE))
        x4 += spaceW + margin

        // 5. Period
        keyRects.add(KeyRect(RectF(x4, y4Top, x4 + dotW, y4Bottom), ".", ".", KeyType.NORMAL))
        x4 += dotW + margin

        // 6. Enter / Search
        val enterLabel = when (actionKeyType) {
            ActionKeyType.SEARCH -> "\uD83D\uDD0D"
            ActionKeyType.GO -> "Go"
            ActionKeyType.SEND -> "\u27A4"
            ActionKeyType.NEXT -> "\u21E5"
            ActionKeyType.DONE -> "\u2713"
            ActionKeyType.ENTER -> "\u21B5"
        }
        keyRects.add(KeyRect(RectF(x4, y4Top, x4 + enterW, y4Bottom), "ENTER", enterLabel, KeyType.ENTER))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val cornerRadius = dp(KEY_CORNER_RADIUS_DP)

        // 1. Draw Header / Suggestion Strip
        canvas.drawRect(headerRect, headerBgPaint)
        headerDividerPaint.strokeWidth = dp(1f)
        canvas.drawLine(0f, headerRect.bottom, width.toFloat(), headerRect.bottom, headerDividerPaint)

        // Draw Vertical dividers between suggestions
        val divPaint = Paint().apply {
            color = Color.parseColor("#2B2930")
            strokeWidth = dp(1f)
        }
        for (i in 1..2) {
            val divX = suggestionRects[i].left
            canvas.drawLine(divX, dp(8f), divX, dp(HEADER_HEIGHT_DP) - dp(8f), divPaint)
        }

        // Suggestions or Live Buffer
        val displaySuggestions = if (currentSuggestions.isNotEmpty()) {
            currentSuggestions
        } else if (shiftMode == ShiftMode.TAMIL && currentBufferText.isNotEmpty()) {
            listOf("", "[$currentBufferText]", "")
        } else {
            val modeSuffix = when {
                isEmoji -> " [Emoji]"
                isEnglishLayout -> if (shiftMode == ShiftMode.CAPS_ENGLISH) " [CAPS]" else ""
                shiftMode == ShiftMode.TAMIL -> ""
                shiftMode == ShiftMode.CAPS_ENGLISH -> " [CAPS]"
                shiftMode == ShiftMode.SMALL_ENGLISH -> " [$smallShiftSymbol]"
                else -> ""
            }
            listOf(",", "$layoutName$modeSuffix", ".")
        }

        for (i in 0 until 3.coerceAtMost(displaySuggestions.size)) {
            val text = displaySuggestions[i]
            if (text.isEmpty()) continue
            val rect = suggestionRects[i]
            val isCenter = (i == 1)
            val p = if (isCenter) suggestionCenterTextPaint else suggestionTextPaint
            p.textSize = if (isCenter) dp(14f) else dp(12.5f)
            val textY = rect.centerY() - (p.descent() + p.ascent()) / 2
            canvas.drawText(text, rect.centerX(), textY, p)
        }

        // Header Search Icon Button (if search field)
        if (actionKeyType == ActionKeyType.SEARCH) {
            val chipRadius = dp(12f)
            canvas.drawRoundRect(searchHeaderRect, chipRadius, chipRadius, chipBgPaint)
            val sCx = searchHeaderRect.centerX()
            val sCy = searchHeaderRect.centerY()
            val sR = dp(4.2f)
            iconStrokePaint.color = Color.parseColor("#A8C7FA")
            iconStrokePaint.style = Paint.Style.STROKE
            iconStrokePaint.strokeWidth = dp(1.8f)
            canvas.drawCircle(sCx - dp(1.5f), sCy - dp(1.5f), sR, iconStrokePaint)
            iconStrokePaint.strokeCap = Paint.Cap.ROUND
            val sHandleStart = sR * 0.707f
            canvas.drawLine(sCx - dp(1.5f) + sHandleStart, sCy - dp(1.5f) + sHandleStart, sCx + dp(5f), sCy + dp(5f), iconStrokePaint)
        }

        // 2. Draw Keys
        specialTextPaint.textSize = dp(14f)
        enterTextPaint.textSize = dp(17f)
        badgePaint.textSize = dp(9.5f)
        spaceSubTextPaint.textSize = dp(11f)

        for (k in keyRects) {
            val paint = when {
                k.isPressed -> keyPressedPaint
                k.type == KeyType.ENTER -> enterKeyPaint
                k.type == KeyType.SHIFT && (shiftMode == ShiftMode.CAPS_ENGLISH || (!isEnglishLayout && shiftMode == ShiftMode.SMALL_ENGLISH)) -> shiftActivePaint
                k.type == KeyType.SMILEY && isEmoji -> shiftActivePaint
                k.type == KeyType.LANG && isDropdownOpen -> shiftActivePaint
                k.type == KeyType.NORMAL -> keyPaint
                k.type == KeyType.SPACE -> spaceKeyPaint
                else -> specialKeyPaint
            }

            canvas.drawRoundRect(k.rect, cornerRadius, cornerRadius, paint)

            // Draw Label
            when (k.type) {
                KeyType.ENTER -> {
                    if (actionKeyType == ActionKeyType.SEARCH) {
                        val cx = k.rect.centerX()
                        val cy = k.rect.centerY()
                        val r = dp(5.5f)
                        iconStrokePaint.color = Color.parseColor("#062E6F")
                        iconStrokePaint.style = Paint.Style.STROKE
                        iconStrokePaint.strokeWidth = dp(2.2f)
                        canvas.drawCircle(cx - dp(2f), cy - dp(2f), r, iconStrokePaint)

                        iconStrokePaint.strokeCap = Paint.Cap.ROUND
                        val handleStart = r * 0.707f
                        canvas.drawLine(cx - dp(2f) + handleStart, cy - dp(2f) + handleStart, cx + dp(6.5f), cy + dp(6.5f), iconStrokePaint)
                    } else {
                        val y = k.rect.centerY() - (enterTextPaint.descent() + enterTextPaint.ascent()) / 2
                        canvas.drawText(k.label, k.rect.centerX(), y, enterTextPaint)
                    }
                }
                KeyType.SPACE -> {
                    val y = k.rect.centerY() - (spaceSubTextPaint.descent() + spaceSubTextPaint.ascent()) / 2
                    val displayLayout = when {
                        isEmoji -> "Emojis 😊"
                        isEnglishLayout -> if (shiftMode == ShiftMode.CAPS_ENGLISH) "English (CAPS)" else "English"
                        shiftMode == ShiftMode.TAMIL -> layoutName
                        shiftMode == ShiftMode.CAPS_ENGLISH -> "English (CAPS)"
                        shiftMode == ShiftMode.SMALL_ENGLISH -> "English ($smallShiftSymbol)"
                        else -> layoutName
                    }
                    canvas.drawText(displayLayout, k.rect.centerX(), y, spaceSubTextPaint)
                }
                KeyType.SMILEY -> {
                    val tPaint = if (isEmoji) enterTextPaint else specialTextPaint
                    val y = k.rect.centerY() - (tPaint.descent() + tPaint.ascent()) / 2
                    tPaint.textSize = if (isEmoji) dp(13f) else dp(16f)
                    canvas.drawText(k.label, k.rect.centerX(), y, tPaint)
                }
                KeyType.LANG -> {
                    val tPaint = if (isDropdownOpen) enterTextPaint else specialTextPaint
                    val y = k.rect.centerY() - (tPaint.descent() + tPaint.ascent()) / 2
                    tPaint.textSize = dp(15f)
                    canvas.drawText(k.label, k.rect.centerX(), y, tPaint)
                }
                KeyType.SPECIAL, KeyType.SYM, KeyType.DEL, KeyType.SHIFT -> {
                    val isShiftActive = (k.type == KeyType.SHIFT && (shiftMode == ShiftMode.CAPS_ENGLISH || (!isEnglishLayout && shiftMode == ShiftMode.SMALL_ENGLISH)))
                    val tPaint = if (isShiftActive) enterTextPaint else specialTextPaint
                    val y = k.rect.centerY() - (tPaint.descent() + tPaint.ascent()) / 2
                    tPaint.textSize = if (k.type == KeyType.SHIFT && !isEnglishLayout && shiftMode == ShiftMode.SMALL_ENGLISH) dp(17f) else dp(14f)
                    canvas.drawText(k.label, k.rect.centerX(), y, tPaint)
                }
                KeyType.NORMAL -> {
                    val fontSize = if (isEmoji) dp(20f) else dp(18f)
                    textPaint.textSize = fontSize
                    val y = k.rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2 + dp(1.5f)
                    canvas.drawText(k.label, k.rect.centerX(), y, textPaint)

                    // Secondary XML letter badge (Shown ONLY in Tamil mode and NOT in English or Emoji mode!)
                    if (!isEnglishLayout && shiftMode == ShiftMode.TAMIL && !isSymbols && !isEmoji && k.code.length == 1) {
                        val badge = engine?.getBadgeForChar(k.code[0], "Tamil")
                        if (badge != null) {
                            val badgeX = k.rect.right - dp(4.5f)
                            val badgeY = k.rect.top + dp(11f)
                            canvas.drawText(badge, badgeX, badgeY, badgePaint)
                        }
                    }
                }
            }
        }

        // 3. Draw Gboard Key Popup Preview Bubble when finger is pressed (letters only)
        pressedKey?.let { pk ->
            if (pk.type == KeyType.NORMAL && pk.code.length == 1 && !isEmoji && !isDropdownOpen) {
                val popW = pk.rect.width() * 1.25f
                val popH = pk.rect.height() * 1.35f
                val popLeft = pk.rect.centerX() - popW / 2f
                val popRight = popLeft + popW
                val popBottom = pk.rect.top - dp(2f)
                val popTop = popBottom - popH

                val popRect = RectF(popLeft, popTop, popRight, popBottom)
                val popRadius = dp(10f)
                canvas.drawRoundRect(popRect, popRadius, popRadius, popupBgPaint)

                popupTextPaint.textSize = dp(24f)
                val popTextY = popRect.centerY() - (popupTextPaint.descent() + popupTextPaint.ascent()) / 2
                canvas.drawText(pk.label, popRect.centerX(), popTextY, popupTextPaint)
            }
        }

        // 4. Draw Dropdown Overlay Menu (if open)
        if (isDropdownOpen) {
            drawDropdownMenu(canvas)
        }
    }

    private fun drawDropdownMenu(canvas: Canvas) {
        // Scrim over keyboard
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        computeDropdownMenuRect()
        val menuRadius = dp(10f)

        // Dropdown Card Background
        canvas.drawRoundRect(dropdownMenuRect, menuRadius, menuRadius, dropdownBgPaint)
        dropdownBorderPaint.strokeWidth = dp(1.2f)
        canvas.drawRoundRect(dropdownMenuRect, menuRadius, menuRadius, dropdownBorderPaint)

        val itemH = dp(38f)
        var yTop = dropdownMenuRect.top + dp(4f)

        dropdownTextPaint.textSize = dp(13f)
        dropdownActiveTextPaint.textSize = dp(13f)
        dropdownSubTextPaint.textSize = dp(12f)

        for (i in enabledKeymaps.indices) {
            val item = enabledKeymaps[i]
            val itemRect = RectF(dropdownMenuRect.left + dp(4f), yTop, dropdownMenuRect.right - dp(4f), yTop + itemH)

            if (item.isActive) {
                canvas.drawRoundRect(itemRect, dp(6f), dp(6f), dropdownActiveBgPaint)
            } else if (pressedDropdownIndex == i) {
                canvas.drawRoundRect(itemRect, dp(6f), dp(6f), dropdownPressedBgPaint)
            }

            val p = if (item.isActive) dropdownActiveTextPaint else dropdownTextPaint
            val checkmark = if (item.isActive) "\u2713  " else "    "
            val textY = itemRect.centerY() - (p.descent() + p.ascent()) / 2
            canvas.drawText("$checkmark${item.displayName}", itemRect.left + dp(10f), textY, p)

            yTop += itemH
        }

        // Divider above Manage Keyboards
        val divY = yTop + dp(2f)
        dropdownDividerPaint.strokeWidth = dp(1f)
        canvas.drawLine(dropdownMenuRect.left + dp(8f), divY, dropdownMenuRect.right - dp(8f), divY, dropdownDividerPaint)
        yTop += dp(4f)

        // Manage Keyboards option
        val manageRect = RectF(dropdownMenuRect.left + dp(4f), yTop, dropdownMenuRect.right - dp(4f), yTop + itemH)
        if (pressedDropdownIndex == enabledKeymaps.size) {
            canvas.drawRoundRect(manageRect, dp(6f), dp(6f), dropdownPressedBgPaint)
        }
        val manageY = manageRect.centerY() - (dropdownSubTextPaint.descent() + dropdownSubTextPaint.ascent()) / 2
        canvas.drawText("\u2699 Manage Keyboards...", manageRect.left + dp(12f), manageY, dropdownSubTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isDropdownOpen) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (langKeyRect.contains(event.x, event.y)) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        isDropdownOpen = false
                        pressedDropdownIndex = -1
                        invalidate()
                        return true
                    }
                    if (dropdownMenuRect.contains(event.x, event.y)) {
                        val itemH = dp(38f)
                        val relY = event.y - (dropdownMenuRect.top + dp(4f))
                        val idx = (relY / itemH).toInt()
                        val maxIdx = enabledKeymaps.size
                        pressedDropdownIndex = if (idx in 0..maxIdx) idx else -1
                        invalidate()
                        return true
                    }
                    // Tapped outside dropdown -> dismiss dropdown
                    isDropdownOpen = false
                    pressedDropdownIndex = -1
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (dropdownMenuRect.contains(event.x, event.y)) {
                        val itemH = dp(38f)
                        val relY = event.y - (dropdownMenuRect.top + dp(4f))
                        val idx = (relY / itemH).toInt()
                        val maxIdx = enabledKeymaps.size
                        if (pressedDropdownIndex != idx && idx in 0..maxIdx) {
                            pressedDropdownIndex = idx
                            invalidate()
                        }
                    } else if (pressedDropdownIndex != -1) {
                        pressedDropdownIndex = -1
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val idx = pressedDropdownIndex
                    isDropdownOpen = false
                    pressedDropdownIndex = -1
                    invalidate()
                    if (idx in enabledKeymaps.indices) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        val selected = enabledKeymaps[idx]
                        listener?.onSelectKeymap(selected.id)
                    } else if (idx == enabledKeymaps.size) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        listener?.onOpenSettings()
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    isDropdownOpen = false
                    pressedDropdownIndex = -1
                    invalidate()
                    return true
                }
            }
            return true
        }

        // Standard Touch Handling when Dropdown is closed
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if tapped on Suggestion Strip
                if (event.y <= dp(HEADER_HEIGHT_DP)) {
                    if (searchHeaderRect.contains(event.x, event.y) && actionKeyType == ActionKeyType.SEARCH) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        listener?.onSearch()
                        return true
                    }

                    val displaySuggestions = if (currentSuggestions.isNotEmpty()) {
                        currentSuggestions
                    } else if (shiftMode == ShiftMode.TAMIL && currentBufferText.isNotEmpty()) {
                        listOf("", "[$currentBufferText]", "")
                    } else {
                        listOf(",", "", ".")
                    }

                    for (i in 0 until 3.coerceAtMost(displaySuggestions.size)) {
                        if (suggestionRects[i].contains(event.x, event.y)) {
                            val word = displaySuggestions[i]
                            if (word.isNotEmpty() && !word.startsWith("[")) {
                                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                listener?.onSuggestion(word)
                                return true
                            }
                        }
                    }
                    return true
                }

                val key = findKeyAt(event.x, event.y)
                if (key != null) {
                    pressedKey = key
                    key.isPressed = true
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val key = findKeyAt(event.x, event.y)
                if (key != pressedKey) {
                    pressedKey?.isPressed = false
                    pressedKey = key
                    key?.isPressed = true
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                pressedKey?.let { k ->
                    k.isPressed = false
                    handleKeyAction(k)
                    pressedKey = null
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedKey?.isPressed = false
                pressedKey = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findKeyAt(x: Float, y: Float): KeyRect? {
        return keyRects.firstOrNull { it.rect.contains(x, y) }
    }

    private fun handleKeyAction(key: KeyRect) {
        when (key.type) {
            KeyType.SHIFT -> toggleShift()
            KeyType.SYM -> {
                if (isEmoji) toggleEmoji() else toggleSymbols()
            }
            KeyType.SMILEY -> toggleEmoji()
            KeyType.LANG -> {
                isDropdownOpen = !isDropdownOpen
                computeDropdownMenuRect()
                invalidate()
            }
            KeyType.DEL -> listener?.onBackspace()
            KeyType.SPACE -> listener?.onSpace()
            KeyType.ENTER -> {
                if (actionKeyType == ActionKeyType.SEARCH) {
                    listener?.onSearch()
                } else {
                    listener?.onEnter()
                }
            }
            KeyType.NORMAL -> {
                if (isEmoji) {
                    listener?.onKeyText(key.code)
                } else if (isEnglishLayout) {
                    val out = if (shiftMode == ShiftMode.CAPS_ENGLISH) key.code.uppercase() else key.code.lowercase()
                    listener?.onKeyText(out)
                } else {
                    when (shiftMode) {
                        ShiftMode.CAPS_ENGLISH -> listener?.onKeyText(key.code.uppercase())
                        ShiftMode.SMALL_ENGLISH -> listener?.onKeyText(key.code.lowercase())
                        ShiftMode.TAMIL -> listener?.onKeyText(key.code)
                    }
                }
            }
            else -> listener?.onKeyText(key.code)
        }
    }
}
