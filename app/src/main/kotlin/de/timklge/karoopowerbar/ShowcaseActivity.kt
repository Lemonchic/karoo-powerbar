package de.timklge.karoopowerbar

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import de.timklge.karoopowerbar.datatypes.SelectedSource
import io.hammerhead.karooext.models.UserProfile

class ShowcaseActivity : ComponentActivity() {

    // 340W FTP Power Zones (Standard Coggan 7-zone model):
    private val userZones = listOf(
        UserProfile.Zone(0, 187),     // Zone 1: Active Recovery (0 - 55% FTP)
        UserProfile.Zone(188, 255),   // Zone 2: Endurance (56 - 75% FTP)
        UserProfile.Zone(256, 306),   // Zone 3: Tempo (76 - 90% FTP)
        UserProfile.Zone(307, 357),   // Zone 4: Threshold (91 - 105% FTP)
        UserProfile.Zone(358, 408),   // Zone 5: VO2 Max (106 - 120% FTP)
        UserProfile.Zone(409, 510),   // Zone 6: Anaerobic (121 - 150% FTP)
        UserProfile.Zone(511, 2000)   // Zone 7: Neuromuscular (>150% FTP)
    )

    // Original extension scaling (Window.kt lines 846-848):
    // minPower = powerZones.first().min (= 0)
    // maxPower = powerZones.last().min + 30 (= 511 + 30 = 541)
    private val minPower = userZones.first().min.toDouble()
    private val maxPower = (userZones.last().min + 30).toDouble()

    private fun getPowerColor(watts: Int): Int {
        val zone = getZone(userZones, watts)
        return ContextCompat.getColor(this, zone?.colorResource ?: R.color.zone7)
    }

    private fun getZoneName(watts: Int): String {
        val zone = getZone(userZones, watts)
        return when (zone) {
            Zone.Zone1 -> "Zone 1 (Recovery)"
            Zone.Zone2 -> "Zone 2 (Endurance)"
            Zone.Zone3 -> "Zone 3 (Tempo)"
            Zone.Zone5 -> "Zone 4 (Threshold)"
            Zone.Zone6 -> "Zone 5 (VO2 Max)"
            Zone.Zone7 -> "Zone 6 (Anaerobic)"
            Zone.Zone8 -> "Zone 7 (Neuromuscular)"
            else -> "Zone ?"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_showcase)

        val customView = findViewById<CustomView>(R.id.customView)

        val powerbar = CustomProgressBar(
            customView,
            SelectedSource.POWER_30S,
            PowerbarLocation.TOP,
            HorizontalPowerbarLocation.FULL
        )

        // Top bar with Extra Large size option:
        powerbar.barSize = CustomProgressBarBarSize.EXTRA_LARGE
        powerbar.fontSize = CustomProgressBarFontSize.MEDIUM
        powerbar.showLabel = true
        powerbar.stickToEdge = false

        val map = mapOf(HorizontalPowerbarLocation.FULL to powerbar)
        customView.progressBars = map

        val tvSlider30sVal = findViewById<TextView>(R.id.tvSlider30sVal)
        val tvSlider5sVal = findViewById<TextView>(R.id.tvSlider5sVal)
        val tv30sVal = findViewById<TextView>(R.id.tv30sVal)
        val tv30sZone = findViewById<TextView>(R.id.tv30sZone)
        val tv5sVal = findViewById<TextView>(R.id.tv5sVal)
        val tv5sZone = findViewById<TextView>(R.id.tv5sZone)
        val tvDeltaVal = findViewById<TextView>(R.id.tvDeltaVal)
        val tvArrowDirection = findViewById<TextView>(R.id.tvArrowDirection)
        val tvArrowColor = findViewById<TextView>(R.id.tvArrowColor)

        val sb30s = findViewById<SeekBar>(R.id.sb30sPower)
        val sb5s = findViewById<SeekBar>(R.id.sb5sPower)

        fun updateDisplay(p30s: Int, p5s: Int) {
            // Original scaling:
            val progress = remap(p30s.toDouble(), minPower, maxPower, 0.0, 1.0)
            val color30s = getPowerColor(p30s)
            val color5s = getShadedArrowColor(this, userZones, p5s)

            powerbar.label = "${p30s}W"
            powerbar.progress = progress
            powerbar.progressColor = color30s

            val delta = (p5s - p30s).toDouble()
            val deltaPercent = if (p30s > 0) delta / p30s.toDouble() else 0.0

            powerbar.powerDelta = delta
            powerbar.powerDeltaPercent = deltaPercent
            // Arrow has the SAME colour as 30s square relatively to its power:
            powerbar.powerDeltaColor = color5s

            customView.invalidate()

            // Update UI widgets:
            tvSlider30sVal.text = "${p30s} W"
            tvSlider30sVal.setTextColor(color30s)

            tvSlider5sVal.text = "${p5s} W"
            tvSlider5sVal.setTextColor(color5s)

            tv30sVal.text = "${p30s} W"
            tv30sVal.setTextColor(color30s)
            tv30sZone.text = getZoneName(p30s)

            tv5sVal.text = "${p5s} W"
            tv5sVal.setTextColor(color5s)
            tv5sZone.text = getZoneName(p5s)

            val sign = if (delta > 0) "+" else ""
            val pctStr = String.format("%.0f%%", deltaPercent * 100)
            tvDeltaVal.text = "${sign}${delta.toInt()}W (${sign}${pctStr})"
            tvDeltaVal.setTextColor(color5s)

            tvArrowDirection.text = when {
                delta > 1.0 -> "Pointing Right ->"
                delta < -1.0 -> "Pointing Left <-"
                else -> "Neutral (No Delta)"
            }
            tvArrowDirection.setTextColor(color5s)

            val hexColor = String.format("#%06X", 0xFFFFFF and color5s)
            tvArrowColor.text = hexColor
            tvArrowColor.setTextColor(color5s)
        }

        val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateDisplay(sb30s.progress, sb5s.progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        sb30s.setOnSeekBarChangeListener(seekBarListener)
        sb5s.setOnSeekBarChangeListener(seekBarListener)

        findViewById<Button>(R.id.btnPreset250).setOnClickListener {
            sb30s.progress = 250
            sb5s.progress = 125
        }

        findViewById<Button>(R.id.btnPreset340).setOnClickListener {
            sb30s.progress = 340
            sb5s.progress = 170
        }

        // Initial setup: 250W with 125W (-50% delta)
        updateDisplay(250, 125)
    }
}
