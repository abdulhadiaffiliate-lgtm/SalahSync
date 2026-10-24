package com.salahsync.ui.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salahsync.core.GeoPoint
import com.salahsync.core.QiblaDirection
import com.salahsync.ui.components.EyebrowText
import com.salahsync.ui.components.GeometricPattern
import com.salahsync.ui.theme.ArabicStyle
import com.salahsync.ui.theme.Salah
import kotlin.math.abs
import kotlin.math.roundToInt

/** Device heading in degrees from true north, or null when there is no compass. */
@Composable
private fun rememberHeading(location: GeoPoint?): Float? {
    val context = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }
    DisposableEffect(location) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val declination = location?.let {
            GeomagneticField(it.latitude.toFloat(), it.longitude.toFloat(), 0f, System.currentTimeMillis()).declination
        } ?: 0f
        val matrix = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, orientation)
                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat() + declination
                heading = (azimuth + 360f) % 360f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    return heading
}

@Composable
fun QiblaScreen(location: GeoPoint?, placeName: String?) {
    val c = Salah.colors
    val heading = rememberHeading(location)
    val bearing = location?.let { QiblaDirection.bearingFrom(it) }
    val haptics = LocalHapticFeedback.current

    // Rotate along the shortest path so the dial never spins the long way round.
    var unwrapped by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(heading) {
        heading?.let {
            val current = ((unwrapped % 360f) + 360f) % 360f
            var delta = it - current
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            unwrapped += delta
        }
    }
    val dial by animateFloatAsState(-unwrapped, tween(250), label = "dial")

    val offBy = if (heading != null && bearing != null) {
        val d = ((bearing - heading + 540.0) % 360.0) - 180.0
        abs(d)
    } else null
    val aligned = offBy != null && offBy < 3.0
    var wasAligned by remember { mutableStateOf(false) }
    LaunchedEffect(aligned) {
        if (aligned && !wasAligned) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        wasAligned = aligned
    }

    Box(Modifier.fillMaxSize().background(c.background)) {
        GeometricPattern(Modifier.fillMaxWidth().height(320.dp))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EyebrowText(placeName ?: "Location not set")
            Text("Qibla", style = MaterialTheme.typography.headlineLarge, color = c.text)
            Text("القبلة", style = ArabicStyle, color = c.accent)
            Spacer(Modifier.height(28.dp))

            val description = when {
                bearing == null -> "Set your location to find the Qibla"
                heading == null -> "Qibla is ${bearing.roundToInt()} degrees from north"
                aligned -> "You are facing the Qibla"
                else -> "Turn ${offBy?.roundToInt()} degrees to face the Qibla"
            }
            Box(Modifier.size(280.dp).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(c.accentHairline, r - 1.dp.toPx(), center, style = Stroke(1.dp.toPx()))
                    drawCircle(c.hairline, r * 0.72f, center, style = Stroke(1.dp.toPx()))
                    rotate(if (heading != null) dial else 0f, center) {
                        for (i in 0 until 72) {
                            val major = i % 18 == 0
                            val len = if (major) 14.dp.toPx() else 6.dp.toPx()
                            rotate(i * 5f, center) {
                                drawLine(
                                    if (major) c.accent else c.textMuted.copy(alpha = 0.4f),
                                    Offset(center.x, center.y - r + 8.dp.toPx()),
                                    Offset(center.x, center.y - r + 8.dp.toPx() + len),
                                    strokeWidth = if (major) 1.5.dp.toPx() else 1.dp.toPx(),
                                )
                            }
                        }
                        if (bearing != null) {
                            rotate(bearing.toFloat(), center) {
                                val tip = Offset(center.x, center.y - r * 0.68f)
                                val needle = Path().apply {
                                    moveTo(tip.x, tip.y)
                                    lineTo(center.x + 10.dp.toPx(), center.y)
                                    lineTo(center.x, center.y + 14.dp.toPx())
                                    lineTo(center.x - 10.dp.toPx(), center.y)
                                    close()
                                }
                                drawPath(needle, if (aligned) c.accent else c.accent.copy(alpha = 0.75f))
                                // The Kaaba marker at the needle's tip.
                                val s = 16.dp.toPx()
                                drawRect(c.text, Offset(tip.x - s / 2, tip.y - s - 6.dp.toPx()), androidx.compose.ui.geometry.Size(s, s))
                                drawLine(c.accent, Offset(tip.x - s / 2, tip.y - s + 0.dp.toPx()), Offset(tip.x + s / 2, tip.y - s), strokeWidth = 2.dp.toPx())
                            }
                        }
                    }
                    drawCircle(c.background, 6.dp.toPx(), center)
                    drawCircle(c.accent, 6.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(description, style = MaterialTheme.typography.titleLarge, color = if (aligned) c.accent else c.text, textAlign = TextAlign.Center)
            if (location != null && bearing != null) {
                Spacer(Modifier.height(8.dp))
                val km = QiblaDirection.distanceKmFrom(location).roundToInt()
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${bearing.roundToInt()}° from true north · %,d km to Makkah".format(km), style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                    if (heading == null) {
                        Text("This phone has no compass sensor. Use the bearing above with a compass.", style = MaterialTheme.typography.bodySmall, color = c.textMuted, textAlign = TextAlign.Center)
                    } else {
                        Text("Hold the phone flat, away from metal and magnets.", style = MaterialTheme.typography.bodySmall, color = c.textMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
