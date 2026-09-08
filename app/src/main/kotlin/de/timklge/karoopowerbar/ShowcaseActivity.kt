package de.timklge.karoopowerbar

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import de.timklge.karoopowerbar.datatypes.SelectedSource

class ShowcaseActivity : ComponentActivity() {

    private var is340wThresholdState = false

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

        fun applyState(useThresholdVal: Boolean) {
            val tv30sVal = findViewById<TextView>(R.id.tv30sVal)
            val tv30sZone = findViewById<TextView>(R.id.tv30sZone)
            val tv5sVal = findViewById<TextView>(R.id.tv5sVal)
            val tv5sZone = findViewById<TextView>(R.id.tv5sZone)
            val tvDeltaVal = findViewById<TextView>(R.id.tvDeltaVal)
            val tvArrowColor = findViewById<TextView>(R.id.tvArrowColor)
            val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)

            if (!useThresholdVal) {
                // Showcase at 340W FTP:
                // 30s avg = 250W (73.5% of 340W FTP -> Zone 2 Endurance: #60EEB2)
                powerbar.progress = 250.0 / (340.0 * 1.3)
                powerbar.progressColor = ContextCompat.getColor(this, R.color.zone2)
                powerbar.label = "250W"

                // 5s avg = 125W (-50% delta = -125W -> Zone 1 Recovery: #00B988)
                powerbar.powerDelta = -125.0
                powerbar.powerDeltaPercent = -0.50
                // Arrow color in default power zone color scheme:
                powerbar.powerDeltaColor = ContextCompat.getColor(this, R.color.zone1)

                tvSubtitle?.text = "Top Bar (XL Size) - 340W FTP Showcase - Delta: -50%"
                tv30sVal?.text = "250 W"
                tv30sVal?.setTextColor(ContextCompat.getColor(this, R.color.zone2))
                tv30sZone?.text = "Zone 2 (Endurance - 74% FTP)"

                tv5sVal?.text = "125 W"
                tv5sVal?.setTextColor(ContextCompat.getColor(this, R.color.zone1))
                tv5sZone?.text = "Zone 1 (Recovery - 37% FTP)"

                tvDeltaVal?.text = "-125W (-50%)"
                tvArrowColor?.text = "#00B988"
            } else {
                // 30s avg = 340W (100% of 340W FTP -> Zone 4 Threshold: #FDC84C)
                powerbar.progress = 340.0 / (340.0 * 1.3)
                powerbar.progressColor = ContextCompat.getColor(this, R.color.zone4)
                powerbar.label = "340W"

                // 5s avg = 170W (-50% delta = -170W -> Zone 1 Recovery: #00B988)
                powerbar.powerDelta = -170.0
                powerbar.powerDeltaPercent = -0.50
                powerbar.powerDeltaColor = ContextCompat.getColor(this, R.color.zone1)

                tvSubtitle?.text = "Top Bar (XL Size) - 340W FTP Showcase - Delta: -50%"
                tv30sVal?.text = "340 W"
                tv30sVal?.setTextColor(ContextCompat.getColor(this, R.color.zone4))
                tv30sZone?.text = "Zone 4 (Threshold - 100% FTP)"

                tv5sVal?.text = "170 W"
                tv5sVal?.setTextColor(ContextCompat.getColor(this, R.color.zone1))
                tv5sZone?.text = "Zone 1 (Recovery - 50% FTP)"

                tvDeltaVal?.text = "-170W (-50%)"
                tvArrowColor?.text = "#00B988"
            }

            customView.invalidate()
        }

        val map = mapOf(HorizontalPowerbarLocation.FULL to powerbar)
        customView.progressBars = map

        applyState(false) // 250W Zone 2 showcase

        findViewById<android.view.View>(android.R.id.content).setOnClickListener {
            is340wThresholdState = !is340wThresholdState
            applyState(is340wThresholdState)
        }
    }
}
