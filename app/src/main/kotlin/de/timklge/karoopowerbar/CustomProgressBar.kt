package de.timklge.karoopowerbar

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import de.timklge.karoopowerbar.datatypes.SelectedSource
import kotlin.math.absoluteValue
import kotlin.math.pow

class CustomView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    var progressBars: Map<HorizontalPowerbarLocation, CustomProgressBar>? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw all progress bars
        progressBars?.values?.forEach { progressBar ->
            Log.d(KarooPowerbarExtension.TAG, "Drawing progress bar for source: ${progressBar.source} - location: ${progressBar.location} - horizontalLocation: ${progressBar.horizontalLocation}")
            progressBar.onDrawForeground(canvas)
        }
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
    }
}

class CustomProgressBar(private val view: CustomView,
                        val source: SelectedSource,
                        val location: PowerbarLocation,
                        val horizontalLocation: HorizontalPowerbarLocation) {
    var progress: Double? = 0.5
    var label: String = ""
    var minTarget: Double? = null
    var maxTarget: Double? = null
    var target: Double? = null
    var showLabel: Boolean = true
    /** When enabled, the progress bar is not drawn and the value box is pinned to the edge of the screen. */
    var stickToEdge: Boolean = false
        set(value) {
            field = value
            view.invalidate()
        }
    @ColorInt var progressColor: Int = 0xFF2b86e6.toInt()
    var drawMode: ProgressBarDrawMode = ProgressBarDrawMode.STANDARD
    var powerDelta: Double? = null
    var powerDeltaPercent: Double? = null
    @ColorInt var powerDeltaColor: Int? = null

    var fontSize = CustomProgressBarFontSize.MEDIUM
        set(value) {
            field = value
            textPaint.textSize = value.fontSize
            view.invalidate() // Redraw to apply new font size
        }

    var barSize = CustomProgressBarBarSize.MEDIUM
        set(value) {
            field = value
            targetZoneStrokePaint.strokeWidth = when(value){
                CustomProgressBarBarSize.NONE, CustomProgressBarBarSize.SMALL -> 3f
                CustomProgressBarBarSize.MEDIUM -> 6f
                CustomProgressBarBarSize.LARGE -> 8f
                CustomProgressBarBarSize.EXTRA_LARGE -> 10f
            }
            targetIndicatorPaint.strokeWidth = when(value){
                CustomProgressBarBarSize.NONE, CustomProgressBarBarSize.SMALL -> 6f
                CustomProgressBarBarSize.MEDIUM -> 8f
                CustomProgressBarBarSize.LARGE -> 10f
                CustomProgressBarBarSize.EXTRA_LARGE -> 12f
            }
            view.invalidate() // Redraw to apply new bar size
        }

    private val arrowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val arrowStrokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.WHITE
    }

    private val arrowBlackStrokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 10.5f // 4.5f white outline + 2 * 3.0f black outlines (3px black border)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = Color.BLACK
    }

    private val targetColor = 0xFF9933FF.toInt()

    private val targetZoneFillPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = targetColor
        alpha = 100 // Semi-transparent fill
    }

    private val targetZoneStrokePaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 6f
        style = Paint.Style.STROKE
        color = targetColor
    }

    private val linePaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 1f
        style = Paint.Style.FILL_AND_STROKE
        color = progressColor
    }

    private val lineStrokePaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 4f
        style = Paint.Style.STROKE
        color = progressColor
    }

    private val blurPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 6f
        style = Paint.Style.STROKE
        color = progressColor
        maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
    }

    private val blurPaintHighlight = Paint().apply {
        isAntiAlias = true
        strokeWidth = 10f
        style = Paint.Style.FILL_AND_STROKE
        color = ColorUtils.blendARGB(progressColor, 0xFFFFFF, 0.5f)
        maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
    }

    private val backgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(1.0f, 0f, 0f, 0f)
        strokeWidth = 2f
    }

    private val textBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(0.8f, 0f, 0f, 0f)
        strokeWidth = 2f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 3f
        textSize = fontSize.fontSize
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val targetIndicatorPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    fun onDrawForeground(canvas: Canvas) {
        // Determine if the current progress is within the target range
        val isTargetMet =
            progress != null && minTarget != null && maxTarget != null && progress!! >= minTarget!! && progress!! <= maxTarget!!

        linePaint.color = progressColor
        lineStrokePaint.color = progressColor
        blurPaint.color = progressColor
        blurPaintHighlight.color = ColorUtils.blendARGB(progressColor, 0xFFFFFF, 0.5f)

        val p = (progress ?: 0.0).coerceIn(0.0, 1.0)
        val fullWidth = canvas.width.toFloat()
        val halfWidth = fullWidth / 2f

        // Calculate bar left and right positions based on draw mode
        val (barLeft, barRight) = when (drawMode) {
            ProgressBarDrawMode.STANDARD -> {
                // Standard left-to-right progress bar
                when (horizontalLocation) {
                    HorizontalPowerbarLocation.LEFT -> Pair(0f, (halfWidth * p).toFloat())
                    HorizontalPowerbarLocation.RIGHT -> Pair(fullWidth - (halfWidth * p).toFloat(), fullWidth)
                    HorizontalPowerbarLocation.FULL -> Pair(0f, (fullWidth * p).toFloat())
                }
            }
            ProgressBarDrawMode.CENTER_OUT -> {
                // Center-outward progress bar: 0.5 = invisible, <0.5 = extend left, >0.5 = extend right
                when (horizontalLocation) {
                    HorizontalPowerbarLocation.LEFT -> {
                        val centerPoint = halfWidth / 2f  // Center of left half
                        when {
                            p == 0.5 -> Pair(centerPoint, centerPoint) // Invisible at 0.5
                            p < 0.5 -> {
                                val leftExtent = centerPoint * (0.5 - p) * 2.0 // Map 0.0-0.5 to full left extension
                                Pair((centerPoint - leftExtent).toFloat(), centerPoint)
                            }
                            else -> {
                                val rightExtent = centerPoint * (p - 0.5) * 2.0 // Map 0.5-1.0 to full right extension
                                Pair(centerPoint, (centerPoint + rightExtent).toFloat())
                            }
                        }
                    }
                    HorizontalPowerbarLocation.RIGHT -> {
                        val centerPoint = halfWidth + (halfWidth / 2f)  // Center of right half
                        when {
                            p == 0.5 -> Pair(centerPoint, centerPoint) // Invisible at 0.5
                            p < 0.5 -> {
                                val leftExtent = (halfWidth / 2f) * (0.5 - p) * 2.0 // Map 0.0-0.5 to full left extension
                                Pair((centerPoint - leftExtent).toFloat(), centerPoint)
                            }
                            else -> {
                                val rightExtent = (halfWidth / 2f) * (p - 0.5) * 2.0 // Map 0.5-1.0 to full right extension
                                Pair(centerPoint, (centerPoint + rightExtent).toFloat())
                            }
                        }
                    }
                    HorizontalPowerbarLocation.FULL -> {
                        val centerPoint = halfWidth  // Center of full width
                        when {
                            p == 0.5 -> Pair(centerPoint, centerPoint) // Invisible at 0.5
                            p < 0.5 -> {
                                val leftExtent = halfWidth * (0.5 - p) * 2.0 // Map 0.0-0.5 to full left extension
                                Pair((centerPoint - leftExtent).toFloat(), centerPoint)
                            }
                            else -> {
                                val rightExtent = halfWidth * (p - 0.5) * 2.0 // Map 0.5-1.0 to full right extension
                                Pair(centerPoint, (centerPoint + rightExtent).toFloat())
                            }
                        }
                    }
                }
            }
        }

        val minTargetX = when (horizontalLocation) {
            HorizontalPowerbarLocation.LEFT -> if (minTarget != null) (halfWidth * minTarget!!).toFloat() else 0f
            HorizontalPowerbarLocation.RIGHT -> if (maxTarget != null) fullWidth - (halfWidth * maxTarget!!).toFloat() else 0f
            HorizontalPowerbarLocation.FULL -> if (minTarget != null) (fullWidth * minTarget!!).toFloat() else 0f
        }
        val maxTargetX = when (horizontalLocation) {
            HorizontalPowerbarLocation.LEFT -> if (maxTarget != null) (halfWidth * maxTarget!!).toFloat() else 0f
            HorizontalPowerbarLocation.RIGHT -> if (minTarget != null) fullWidth - (halfWidth * minTarget!!).toFloat() else 0f
            HorizontalPowerbarLocation.FULL -> if (maxTarget != null) (fullWidth * maxTarget!!).toFloat() else 0f
        }
        val targetX = when (horizontalLocation) {
            HorizontalPowerbarLocation.LEFT -> if (target != null) (halfWidth * target!!).toFloat() else 0f
            HorizontalPowerbarLocation.RIGHT -> if (target != null) fullWidth - (halfWidth * target!!).toFloat() else 0f
            HorizontalPowerbarLocation.FULL -> if (target != null) (fullWidth * target!!).toFloat() else 0f
        }

        val backgroundLeft = when (horizontalLocation) {
            HorizontalPowerbarLocation.LEFT -> 0f
            HorizontalPowerbarLocation.RIGHT -> halfWidth
            HorizontalPowerbarLocation.FULL -> 0f
        }
        val backgroundRight = when (horizontalLocation) {
            HorizontalPowerbarLocation.LEFT -> halfWidth
            HorizontalPowerbarLocation.RIGHT -> fullWidth
            HorizontalPowerbarLocation.FULL -> fullWidth
        }

        when (location) {
            PowerbarLocation.TOP -> {
                val rect = RectF(
                    barLeft,
                    15f,
                    barRight,
                    15f + barSize.barHeight // barSize.barHeight will be 0f if NONE
                )

                // Draw bar components only if barSize is not NONE and not sticking to edge only
                if (barSize != CustomProgressBarBarSize.NONE && !stickToEdge) {
                    if (minTarget != null && maxTarget != null) {
                        canvas.drawRoundRect(
                            minTargetX,
                            15f,
                            maxTargetX,
                            15f + barSize.barHeight,
                            2f,
                            2f,
                            targetZoneFillPaint
                        )
                    }

                    if (progress != null) {
                        canvas.drawRoundRect(rect, 2f, 2f, blurPaint)
                        canvas.drawRoundRect(rect, 2f, 2f, linePaint)
                        canvas.drawRoundRect(rect.right-4, rect.top, rect.right+4, rect.bottom, 2f, 2f, blurPaintHighlight)
                    }
                }
                // Draw label (if progress is not null and showLabel is true)
                if (progress != null) {
                    // Draw target zone stroke after progress bar, before label (only when a bar is shown)
                    if (!stickToEdge && minTarget != null && maxTarget != null) {
                        // Draw stroked rounded rectangle for the target zone
                        canvas.drawRoundRect(
                            minTargetX,
                            15f,
                            maxTargetX,
                            15f + barSize.barHeight,
                            2f,
                            2f,
                            targetZoneStrokePaint
                        )
                    }

                    // Draw vertical target indicator line if target is present (only when a bar is shown)
                    if (!stickToEdge && target != null) {
                        targetIndicatorPaint.color = if (isTargetMet) Color.GREEN else Color.RED
                        canvas.drawLine(targetX, 15f, targetX, 15f + barSize.barHeight, targetIndicatorPaint)
                    }

                    if (showLabel){
                        textPaint.textSize = if (barSize == CustomProgressBarBarSize.EXTRA_LARGE) (fontSize.fontSize * 1.25f) else fontSize.fontSize
                        val hPadding = if (barSize == CustomProgressBarBarSize.EXTRA_LARGE) 36f else 20f
                        val vPadding = if (barSize == CustomProgressBarBarSize.EXTRA_LARGE) 6f else 0f

                        lineStrokePaint.color = if (target != null){
                            if (isTargetMet) Color.GREEN else Color.RED
                        } else progressColor

                        blurPaint.color = lineStrokePaint.color
                        blurPaintHighlight.color = ColorUtils.blendARGB(lineStrokePaint.color, 0xFFFFFF, 0.5f)

                        val textBounds = textPaint.measureText(label)
                        val xOffset = (textBounds + hPadding).coerceAtLeast(10f) / 2f

                        // Calculate label position based on draw mode
                        val x = if (stickToEdge) {
                            // When sticking to edge, pin the value box to the horizontal edge of the screen
                            when (horizontalLocation) {
                                HorizontalPowerbarLocation.LEFT -> backgroundLeft
                                HorizontalPowerbarLocation.RIGHT -> backgroundRight - xOffset * 2f
                                HorizontalPowerbarLocation.FULL -> backgroundRight - xOffset * 2f
                            }
                        } else when (drawMode) {
                            ProgressBarDrawMode.STANDARD -> {
                                // Original logic for standard mode
                                (if (horizontalLocation != HorizontalPowerbarLocation.RIGHT) rect.right - xOffset else rect.left - xOffset).coerceIn(backgroundLeft..backgroundRight-xOffset*2f)
                            }
                            ProgressBarDrawMode.CENTER_OUT -> {
                                // For center outward mode, position label at the edge of the bar
                                when {
                                    p == 0.5 -> {
                                        // When bar is invisible (at center), position at center
                                        when (horizontalLocation) {
                                            HorizontalPowerbarLocation.LEFT -> {
                                                val centerPoint = halfWidth / 2f
                                                (centerPoint - xOffset).coerceIn(backgroundLeft..backgroundRight-xOffset*2f)
                                            }
                                            HorizontalPowerbarLocation.RIGHT -> {
                                                val centerPoint = halfWidth + (halfWidth / 2f)
                                                (centerPoint - xOffset).coerceIn(backgroundLeft..backgroundRight-xOffset*2f)
                                            }
                                            HorizontalPowerbarLocation.FULL -> {
                                                val centerPoint = halfWidth
                                                (centerPoint - xOffset).coerceIn(backgroundLeft..backgroundRight-xOffset*2f)
                                            }
                                        }
                                    }
                                    p < 0.5 -> {
                                        // Bar extends left from center, place label at left edge
                                        (rect.left - xOffset).coerceIn(backgroundLeft..backgroundRight-xOffset*2f)
                                    }
                                    else -> {
                                        // Bar extends right from center, place label at right edge
                                        (rect.right - xOffset).coerceIn(backgroundLeft..backgroundRight-xOffset*2f)
                                    }
                                }
                            }
                        }
                        val r = x + xOffset * 2

                        val fm = textPaint.fontMetrics
                        val finalTextBoxTop: Float
                        val finalTextBaselineY: Float
                        val finalTextBoxBottom: Float
                        if (stickToEdge) {
                            // Pin the value box to the top edge of the screen
                            finalTextBoxTop = 2f
                            finalTextBaselineY = finalTextBoxTop - fm.ascent + vPadding
                            finalTextBoxBottom = finalTextBaselineY + fm.descent + vPadding
                        } else {
                            // barCenterY calculation uses barSize.barHeight, which is 0f for NONE,
                            // correctly centering the label on the 15f line.
                            val barCenterY = rect.top + barSize.barHeight / 2f
                            finalTextBaselineY = barCenterY - (fm.ascent + fm.descent) / 2f
                            finalTextBoxTop = (finalTextBaselineY + fm.ascent - vPadding).coerceAtLeast(0f)
                            finalTextBoxBottom = (finalTextBaselineY + fm.descent + vPadding).coerceAtMost(canvas.height.toFloat())
                        }

                        if (source == SelectedSource.POWER_30S && powerDelta != null) {
                            drawDeltaArrow(canvas, x, r, finalTextBoxTop, finalTextBoxBottom, backgroundLeft, backgroundRight)
                        }

                        canvas.drawRoundRect(x, finalTextBoxTop, r, finalTextBoxBottom, 3f, 3f, backgroundPaint)
                        canvas.drawRoundRect(x, finalTextBoxTop, r, finalTextBoxBottom, 3f, 3f, textBackgroundPaint)
                        canvas.drawRoundRect(x, finalTextBoxTop, r, finalTextBoxBottom, 3f, 3f, blurPaint)
                        canvas.drawRoundRect(x, finalTextBoxTop, r, finalTextBoxBottom, 3f, 3f, lineStrokePaint)
                        canvas.drawText(label.uppercase(), x + xOffset, finalTextBaselineY, textPaint)
                        }
                    }
            }

            PowerbarLocation.BOTTOM -> {
                val rect = RectF(
                    barLeft,
                    canvas.height.toFloat() - 1f - barSize.barHeight, // barSize.barHeight will be 0f if NONE
                    barRight,
                    canvas.height.toFloat()
                )

                // Draw bar components only if barSize is not NONE and not sticking to edge only
                if (barSize != CustomProgressBarBarSize.NONE && !stickToEdge) {
                    // Draw target zone fill behind the progress bar
                    if (minTarget != null && maxTarget != null) {
                        canvas.drawRoundRect(
                            minTargetX,
                            canvas.height.toFloat() - barSize.barHeight,
                            maxTargetX,
                            canvas.height.toFloat(),
                            2f,
                            2f,
                            targetZoneFillPaint
                        )
                    }

                    if (progress != null) {
                        canvas.drawRoundRect(rect, 2f, 2f, blurPaint)
                        canvas.drawRoundRect(rect, 2f, 2f, linePaint)
                        canvas.drawRoundRect(rect.right-4, rect.top, rect.right+4, rect.bottom, 2f, 2f, blurPaintHighlight)
                    }
                }

                // Draw label (if progress is not null and showLabel is true)
                if (progress != null) {
                    // Draw target zone stroke after progress bar, before label (only when a bar is shown)
                    if (!stickToEdge && minTarget != null && maxTarget != null) {
                        // Draw stroked rounded rectangle for the target zone
                        canvas.drawRoundRect(
                            minTargetX,
                            canvas.height.toFloat() - barSize.barHeight,
                            maxTargetX,
                            canvas.height.toFloat(),
                            2f,
                            2f,
                            targetZoneStrokePaint
                        )
                    }

                    // Draw vertical target indicator line if target is present (only when a bar is shown)
                    if (!stickToEdge && target != null) {
                        targetIndicatorPaint.color = if (isTargetMet) Color.GREEN else Color.RED
                        canvas.drawLine(targetX, canvas.height.toFloat() - barSize.barHeight, targetX, canvas.height.toFloat(), targetIndicatorPaint)
                    }

                    if (showLabel){
                        textPaint.textSize = if (barSize == CustomProgressBarBarSize.EXTRA_LARGE) (fontSize.fontSize * 1.25f) else fontSize.fontSize
                        val hPadding = if (barSize == CustomProgressBarBarSize.EXTRA_LARGE) 36f else 20f
                        val vPadding = if (barSize == CustomProgressBarBarSize.EXTRA_LARGE) 6f else 0f

                        lineStrokePaint.color = if (target != null){
                            if (isTargetMet) Color.GREEN else Color.RED
                        } else progressColor

                        blurPaint.color = lineStrokePaint.color
                        blurPaintHighlight.color = ColorUtils.blendARGB(lineStrokePaint.color, 0xFFFFFF, 0.5f)

                        val textBounds = textPaint.measureText(label)
                        val xOffset = (textBounds + hPadding).coerceAtLeast(10f) / 2f

                        // Calculate label position based on draw mode
                        val x = if (stickToEdge) {
                            // When sticking to edge, pin the value box to the horizontal edge of the screen
                            when (horizontalLocation) {
                                HorizontalPowerbarLocation.LEFT -> backgroundLeft
                                HorizontalPowerbarLocation.RIGHT -> backgroundRight - xOffset * 2f
                                HorizontalPowerbarLocation.FULL -> backgroundRight - xOffset * 2f
                            }
                        } else when (drawMode) {
                            ProgressBarDrawMode.STANDARD -> {
                                // Original logic for standard mode
                                (if (horizontalLocation != HorizontalPowerbarLocation.RIGHT) rect.right - xOffset else rect.left - xOffset).coerceIn(backgroundLeft..backgroundRight-xOffset*2f)
                            }
                            ProgressBarDrawMode.CENTER_OUT -> {
                                // For center outward mode, position label at the edge of the bar
                                when {
                                    p == 0.5 -> {
                                        // When bar is invisible (at center), position at center
                                        when (horizontalLocation) {
                                            HorizontalPowerbarLocation.LEFT -> {
                                                val centerPoint = halfWidth / 2f
                                                (centerPoint - xOffset).coerceIn(backgroundLeft..backgroundRight-xOffset*2f)
                                            }
                                            HorizontalPowerbarLocation.RIGHT -> {
                                                val centerPoint = halfWidth + (halfWidth / 2f)
                                                (centerPoint - xOffset).coerceIn(backgroundLeft..backgroundRight-xOffset*2f)
                                            }
                                            HorizontalPowerbarLocation.FULL -> {
                                                val centerPoint = halfWidth
                                                (centerPoint - xOffset).coerceIn(backgroundLeft..backgroundRight-xOffset*2f)
                                            }
                                        }
                                    }
                                    p < 0.5 -> {
                                        // Bar extends left from center, place label at left edge
                                        (rect.left - xOffset).coerceIn(backgroundLeft..backgroundRight-xOffset*2f)
                                    }
                                    else -> {
                                        // Bar extends right from center, place label at right edge
                                        (rect.right - xOffset).coerceIn(backgroundLeft..backgroundRight-xOffset*2f)
                                    }
                                }
                            }
                        }
                        val r = x + xOffset * 2

                        val fm = textPaint.fontMetrics
                        val textDrawBaselineY: Float
                        val yBox: Float
                        val bBox: Float
                        if (stickToEdge) {
                            // Pin the value box to the bottom edge of the screen
                            bBox = canvas.height.toFloat() - 2f
                            textDrawBaselineY = bBox - fm.descent - vPadding
                            yBox = (textDrawBaselineY + fm.ascent - vPadding).coerceAtLeast(0f)
                        } else {
                            val boxH = (-fm.ascent + fm.descent) + 2f * vPadding
                            bBox = (canvas.height.toFloat() - 2f).coerceAtMost(canvas.height.toFloat())
                            yBox = (bBox - boxH).coerceAtLeast(0f)
                            textDrawBaselineY = bBox - vPadding - fm.descent
                        }

                        if (source == SelectedSource.POWER_30S && powerDelta != null) {
                            drawDeltaArrow(canvas, x, r, yBox, bBox, backgroundLeft, backgroundRight)
                        }

                        canvas.drawRoundRect(x, yBox, r, bBox, 3f, 3f, backgroundPaint)
                        canvas.drawRoundRect(x, yBox, r, bBox, 3f, 3f, textBackgroundPaint)
                        canvas.drawRoundRect(x, yBox, r, bBox, 3f, 3f, blurPaint)
                        canvas.drawRoundRect(x, yBox, r, bBox, 3f, 3f, lineStrokePaint)
                        canvas.drawText(label.uppercase(), x + xOffset, textDrawBaselineY, textPaint)
                    }
                }
            }
        }
    }

    private fun drawDeltaArrow(
        canvas: Canvas,
        boxLeft: Float,
        boxRight: Float,
        boxTop: Float,
        boxBottom: Float,
        backgroundLeft: Float,
        backgroundRight: Float
    ) {
        val delta = powerDelta ?: return
        val absDelta = delta.absoluteValue
        if (absDelta < 0.5 && (powerDeltaPercent?.absoluteValue ?: 0.0) < 0.005) return // No arrow at 0 delta

        val pointsForward = delta > 0
        val pointsRight = when (horizontalLocation) {
            HorizontalPowerbarLocation.RIGHT -> !pointsForward
            else -> pointsForward
        }

        // Relative delta ratio:
        val ratio = powerDeltaPercent?.let { it.absoluteValue.coerceIn(0.0, 1.0).toFloat() }
            ?: (absDelta / 250.0).coerceIn(0.0, 1.0).toFloat()

        // Sizing for XL and other bar sizes:
        val fullHeadHeight = (barSize.barHeight * 1.35f).coerceIn(32f, 66f)
        val shaftHeight = (fullHeadHeight * 0.46f).coerceIn(15f, 30f)
        val fullHeadWidth = (fullHeadHeight * 0.72f).coerceIn(24f, 48f)

        // Non-linear scale: slightly more pronounced curve (pow 0.68) - bigger at low deltas, smoothly compressed at high deltas:
        val scaledRatio = ratio.toDouble().pow(0.68).toFloat()
        val arrowLength = scaledRatio * 380f
        if (arrowLength < 1.0f) return

        // Arrow color uses the same colour scheme as 30s square relatively to its power:
        val arrowColor = powerDeltaColor ?: progressColor
        arrowPaint.color = arrowColor

        val centerY = (boxTop + boxBottom) / 2f

        // Threshold for right screen edge wrap: @360W for 340W FTP scale (~0.665 progress) or within 85px of right edge:
        val thresholdProgress = 360.0 / 541.0
        val isNearRightEdge = (progress ?: 0.0) >= thresholdProgress || (boxRight >= backgroundRight - 85f)
        val wrapToLeftEdge = pointsRight && isNearRightEdge

        val anchorX = if (wrapToLeftEdge) {
            backgroundLeft
        } else if (pointsRight) {
            boxRight
        } else {
            boxLeft
        }

        // Constrain tip safely within screen bounds without throwing empty range exceptions:
        val tipX: Float
        val actualLen: Float
        if (wrapToLeftEdge) {
            val maxAllowed = (boxLeft - 10f).coerceAtMost(backgroundRight - 4f)
            tipX = (anchorX + arrowLength).coerceAtMost(maxAllowed)
            actualLen = tipX - anchorX
        } else if (pointsRight) {
            val maxRight = backgroundRight - 4f
            if (anchorX >= maxRight - 1.5f) return
            tipX = (anchorX + arrowLength).coerceAtMost(maxRight)
            actualLen = tipX - anchorX
        } else {
            val minLeft = backgroundLeft + 4f
            if (anchorX <= minLeft + 1.5f) return
            tipX = (anchorX - arrowLength).coerceAtLeast(minLeft)
            actualLen = anchorX - tipX
        }

        if (actualLen < 1.0f) return

        val sHalf = shaftHeight / 2f
        val hHalf = fullHeadHeight / 2f
        val path = Path()

        // Tuck base 10px inside the 30s square so it emerges seamlessly from behind it:
        val tuck = 10f
        val baseStartX = if (pointsRight) anchorX - tuck else anchorX + tuck

        if (actualLen < fullHeadWidth) {
            // Emerging arrowhead: only a triangle peeking out from behind the 30s square
            val currentH = hHalf * (actualLen / fullHeadWidth)
            path.moveTo(baseStartX, centerY - currentH)
            path.lineTo(tipX, centerY)
            path.lineTo(baseStartX, centerY + currentH)
            path.close()
        } else {
            // Full arrowhead emerged, shaft connects baseStartX to neckX
            if (pointsRight) {
                val neckX = tipX - fullHeadWidth
                path.moveTo(baseStartX, centerY - sHalf)
                path.lineTo(neckX, centerY - sHalf)
                path.lineTo(neckX, centerY - hHalf)
                path.lineTo(tipX, centerY)
                path.lineTo(neckX, centerY + hHalf)
                path.lineTo(neckX, centerY + sHalf)
                path.lineTo(baseStartX, centerY + sHalf)
                path.close()
            } else {
                val neckX = tipX + fullHeadWidth
                path.moveTo(baseStartX, centerY - sHalf)
                path.lineTo(neckX, centerY - sHalf)
                path.lineTo(neckX, centerY - hHalf)
                path.lineTo(tipX, centerY)
                path.lineTo(neckX, centerY + hHalf)
                path.lineTo(neckX, centerY + sHalf)
                path.lineTo(baseStartX, centerY + sHalf)
                path.close()
            }
        }

        // Layer 1: Solid arrow filled with power color
        canvas.drawPath(path, arrowPaint)
        // Layer 2: 1px black thin outlines framing the white outline on both edges
        canvas.drawPath(path, arrowBlackStrokePaint)
        // Layer 3: Bold white outline
        canvas.drawPath(path, arrowStrokePaint)
    }

    fun invalidate() {
        // Invalidate the view to trigger a redraw
        view.invalidate()
    }
}
