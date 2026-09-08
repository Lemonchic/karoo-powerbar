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
        strokeWidth = 2.5f
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
                        lineStrokePaint.color = if (target != null){
                            if (isTargetMet) Color.GREEN else Color.RED
                        } else progressColor

                        blurPaint.color = lineStrokePaint.color
                        blurPaintHighlight.color = ColorUtils.blendARGB(lineStrokePaint.color, 0xFFFFFF, 0.5f)

                        val textBounds = textPaint.measureText(label)
                        val xOffset = (textBounds + 20).coerceAtLeast(10f) / 2f

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
                            finalTextBaselineY = finalTextBoxTop - fm.ascent
                            finalTextBoxBottom = finalTextBaselineY + fm.descent
                        } else {
                            // barCenterY calculation uses barSize.barHeight, which is 0f for NONE,
                            // correctly centering the label on the 15f line.
                            val barCenterY = rect.top + barSize.barHeight / 2f
                            val centeredTextBaselineY = barCenterY - (fm.ascent + fm.descent) / 2f
                            val calculatedTextBoxTop = centeredTextBaselineY + fm.ascent
                            finalTextBoxTop = calculatedTextBoxTop.coerceAtLeast(0f)
                            finalTextBaselineY = finalTextBoxTop - fm.ascent
                            finalTextBoxBottom = finalTextBaselineY + fm.descent
                        }

                        if (source == SelectedSource.POWER_30S && powerDelta != null) {
                            drawDeltaArrow(canvas, x, r, finalTextBoxTop, finalTextBoxBottom, backgroundLeft, backgroundRight)
                        }

                        canvas.drawRoundRect(x, finalTextBoxTop, r, finalTextBoxBottom, 2f, 2f, textBackgroundPaint)
                        canvas.drawRoundRect(x, finalTextBoxTop, r, finalTextBoxBottom, 2f, 2f, blurPaint)
                        canvas.drawRoundRect(x, finalTextBoxTop, r, finalTextBoxBottom, 2f, 2f, lineStrokePaint)
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
                        lineStrokePaint.color = if (target != null){
                            if (isTargetMet) Color.GREEN else Color.RED
                        } else progressColor

                        blurPaint.color = lineStrokePaint.color
                        blurPaintHighlight.color = ColorUtils.blendARGB(lineStrokePaint.color, 0xFFFFFF, 0.5f)

                        val textBounds = textPaint.measureText(label)
                        val xOffset = (textBounds + 20).coerceAtLeast(10f) / 2f

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

                        val textDrawBaselineY: Float
                        val yBox: Float
                        val bBox: Float
                        if (stickToEdge) {
                            // Pin the value box to the bottom edge of the screen
                            bBox = canvas.height.toFloat() - 2f
                            textDrawBaselineY = bBox - textPaint.descent()
                            yBox = textDrawBaselineY + textPaint.ascent()
                        } else {
                            // textDrawBaselineY calculation uses rect.top and barSize.barHeight.
                            // If NONE, barSize.barHeight is 0f. rect.top becomes canvas.height - 1f.
                            // So, baseline is (canvas.height - 1f) + 0f - 1f = canvas.height - 2f.
                            textDrawBaselineY = rect.top + barSize.barHeight - 1f
                            yBox = textDrawBaselineY + textPaint.ascent()
                            bBox = textDrawBaselineY + textPaint.descent()
                        }

                        if (source == SelectedSource.POWER_30S && powerDelta != null) {
                            drawDeltaArrow(canvas, x, r, yBox, bBox, backgroundLeft, backgroundRight)
                        }

                        canvas.drawRoundRect(x, yBox, r, bBox, 2f, 2f, textBackgroundPaint)
                        canvas.drawRoundRect(x, yBox, r, bBox, 2f, 2f, blurPaint)
                        canvas.drawRoundRect(x, yBox, r, bBox, 2f, 2f, lineStrokePaint)
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
        if (absDelta < 2.0 && (powerDeltaPercent?.absoluteValue ?: 0.0) < 0.02) return // Deadband to prevent twitching around 0 delta

        // Direction:
        // When power is surging (delta > 0), arrow points in the direction the bar advances.
        // For standard left-to-right bar (FULL or LEFT): right is advancing.
        // For RIGHT horizontal location (right-to-left): left is advancing.
        val pointsForward = delta > 0
        val pointsRight = when (horizontalLocation) {
            HorizontalPowerbarLocation.RIGHT -> !pointsForward
            else -> pointsForward
        }

        // Dynamic Length & Size scaling:
        // Use powerDeltaPercent if available, otherwise normalize absDelta from 2W to 50W+
        val ratio = powerDeltaPercent?.let { it.absoluteValue.coerceIn(0.0, 1.0).toFloat() }
            ?: ((absDelta - 2.0) / 48.0).coerceIn(0.0, 1.0).toFloat()

        // Sizing for XL and other bar sizes:
        val headHeight = (barSize.barHeight * 1.6f).coerceIn(32f, 60f)
        val shaftHeight = (headHeight * 0.50f).coerceIn(15f, 30f)
        val headWidth = (headHeight * 0.80f).coerceIn(24f, 48f)

        // Arrow length extends prominently from the value box (twice bigger for 50% delta):
        val baseLength = headWidth + 25f
        val maxExtra = 210f
        val desiredLength = baseLength + ratio * maxExtra

        // Arrow color uses the same colour scheme as 30s square relatively to its power:
        val arrowColor = powerDeltaColor ?: progressColor
        arrowPaint.color = arrowColor

        val centerY = (boxTop + boxBottom) / 2f
        val anchorX = if (pointsRight) boxRight - 1f else boxLeft + 1f

        // Constrain tip safely within screen bounds without throwing empty range exceptions:
        val tipX = if (pointsRight) {
            val maxAllowed = backgroundRight - 2f
            val minNeeded = anchorX + 4f
            if (minNeeded >= maxAllowed) return
            (anchorX + desiredLength).coerceIn(minNeeded, maxAllowed)
        } else {
            val minAllowed = backgroundLeft + 2f
            val maxNeeded = anchorX - 4f
            if (minAllowed >= maxNeeded) return
            (anchorX - desiredLength).coerceIn(minAllowed, maxNeeded)
        }

        val actualLen = (tipX - anchorX).absoluteValue
        if (actualLen < 8f) return

        val effectiveHeadWidth = headWidth.coerceAtMost(actualLen * 0.65f)
        val sHalf = shaftHeight / 2f
        val hHalf = headHeight / 2f
        val path = Path()

        if (pointsRight) {
            val neckX = (tipX - effectiveHeadWidth).coerceAtLeast(anchorX + 2f)
            path.moveTo(anchorX, centerY - sHalf)
            path.lineTo(neckX, centerY - sHalf)
            path.lineTo(neckX, centerY - hHalf)
            path.lineTo(tipX, centerY)
            path.lineTo(neckX, centerY + hHalf)
            path.lineTo(neckX, centerY + sHalf)
            path.lineTo(anchorX, centerY + sHalf)
            path.close()
        } else {
            val neckX = (tipX + effectiveHeadWidth).coerceAtMost(anchorX - 2f)
            path.moveTo(anchorX, centerY - sHalf)
            path.lineTo(neckX, centerY - sHalf)
            path.lineTo(neckX, centerY - hHalf)
            path.lineTo(tipX, centerY)
            path.lineTo(neckX, centerY + hHalf)
            path.lineTo(neckX, centerY + sHalf)
            path.lineTo(anchorX, centerY + sHalf)
            path.close()
        }

        // Layer 1: Solid arrow filled with default power color scheme
        canvas.drawPath(path, arrowPaint)
        // Layer 2: Clean outline matching the value box border
        canvas.drawPath(path, arrowStrokePaint)
    }

    fun invalidate() {
        // Invalidate the view to trigger a redraw
        view.invalidate()
    }
}
