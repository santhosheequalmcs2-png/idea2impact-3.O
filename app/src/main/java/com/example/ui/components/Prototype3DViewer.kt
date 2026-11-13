package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.PurpleGlow
import com.example.util.StartupSoundPlayer
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class CadFinish(val label: String, val primary: Color, val accent: Color, val core: Color) {
    CYAN("Cyan", ElectricCyan, CobaltBlue, Color(0xFF8FF2FF)),
    EMERALD("Emerald", EmeraldMint, Color(0xFF059669), Color(0xFF6EE7B7)),
    PURPLE("Purple", PurpleGlow, Color(0xFF7C3AED), Color(0xFFD8B4FE)),
    GOLD("Gold", GoldAmber, Color(0xFFD97706), Color(0xFFFDE68A))
}

private data class Vec3(val x: Float, val y: Float, val z: Float)

@Composable
fun Prototype3DViewer(
    title: String,
    category: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var wireframe by remember { mutableStateOf(false) }
    var autoRotate by remember { mutableStateOf(true) }
    var exploded by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var yaw by remember { mutableFloatStateOf(0.45f) }
    var pitch by remember { mutableFloatStateOf(0.38f) }
    var gyroPhase by remember { mutableFloatStateOf(0f) }
    var finish by remember { mutableStateOf(CadFinish.CYAN) }
    val tapExplodePulse = remember { Animatable(0f) }

    LaunchedEffect(autoRotate) {
        var lastNs = 0L
        while (autoRotate) {
            withFrameNanos { ns ->
                if (lastNs != 0L) {
                    val dt = ((ns - lastNs) / 1_000_000_000f).coerceAtMost(0.05f)
                    yaw += dt * 0.55f
                    gyroPhase += dt * 1.6f
                }
                lastNs = ns
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0A101E),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ViewInAr,
                        contentDescription = null,
                        tint = finish.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.cad_viewer_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFEDF2FF)
                        )
                        Text(
                            text = "$category · Drag to orbit · Tap 3D to pulse",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8D9AB1)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    CadIconToggle(
                        active = autoRotate,
                        accent = finish.primary,
                        tag = "cad_auto_rotate",
                        contentDesc = "Toggle auto-rotation",
                        onClick = { autoRotate = !autoRotate }
                    ) {
                        Icon(Icons.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    CadIconToggle(
                        active = wireframe,
                        accent = finish.primary,
                        tag = "cad_wireframe",
                        contentDesc = "Toggle wireframe",
                        onClick = { wireframe = !wireframe }
                    ) {
                        Icon(Icons.Filled.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(1.8f) },
                        modifier = Modifier.size(40.dp).testTag("cad_zoom_in")
                    ) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in", tint = Color(0xFF8D9AB1), modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.65f) },
                        modifier = Modifier.size(40.dp).testTag("cad_zoom_out")
                    ) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out", tint = Color(0xFF8D9AB1), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 3D Viewport Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                finish.accent.copy(alpha = 0.16f),
                                Color(0xFF060A13)
                            )
                        )
                    )
                    .border(1.dp, finish.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            yaw += dragAmount.x * 0.01f
                            pitch = (pitch + dragAmount.y * 0.01f).coerceIn(-1.2f, 1.2f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            StartupSoundPlayer.playTap(context)
                            scope.launch {
                                tapExplodePulse.snapTo(1f)
                                tapExplodePulse.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val unit = (size.minDimension * 0.135f) * zoomScale
                    val explodeFactor = ((exploded / 100f) + tapExplodePulse.value * 0.55f).coerceAtMost(1.25f)

                    fun project(v: Vec3): Pair<Offset, Float> {
                        // Rotate Y (yaw), then X (pitch)
                        val cosY = cos(yaw)
                        val sinY = sin(yaw)
                        val x1 = v.x * cosY + v.z * sinY
                        val z1 = -v.x * sinY + v.z * cosY
                        val cosX = cos(pitch)
                        val sinX = sin(pitch)
                        val y2 = v.y * cosX - z1 * sinX
                        val z2 = v.y * sinX + z1 * cosX
                        val perspective = 7.5f / (7.5f - z2 * 0.65f).coerceAtLeast(2.2f)
                        return Offset(cx + x1 * unit * perspective, cy - y2 * unit * perspective) to z2
                    }

                    // 1. Isometric base CAD grid
                    val gridColor = Color(0xFF3B82F6).copy(alpha = 0.22f)
                    for (i in -3..3) {
                        val f = i * 0.95f
                        val (a1, _) = project(Vec3(f, -2.3f, -2.85f))
                        val (a2, _) = project(Vec3(f, -2.3f, 2.85f))
                        drawLine(gridColor, a1, a2, strokeWidth = 1.2f)

                        val (b1, _) = project(Vec3(-2.85f, -2.3f, f))
                        val (b2, _) = project(Vec3(2.85f, -2.3f, f))
                        drawLine(gridColor, b1, b2, strokeWidth = 1.2f)
                    }

                    val catLower = category.lowercase()
                    when {
                        catLower.contains("green") -> {
                            // Green Tech: 3D Rotating Wind Turbine Assembly
                            val mastBase = project(Vec3(0f, -2.25f, 0f)).first
                            val nacellePos = Vec3(0f, 0.75f + explodeFactor * 0.45f, 0f)
                            val mastTop = project(nacellePos).first
                            drawLine(finish.accent, mastBase, mastTop, strokeWidth = if (wireframe) 3f else 10f * zoomScale)

                            val hubZ = 0.52f + explodeFactor * 0.95f
                            val hubCenter = Vec3(0f, nacellePos.y, hubZ)
                            val (hubPt, _) = project(hubCenter)
                            if (explodeFactor > 0.05f) {
                                drawLine(finish.primary.copy(alpha = 0.4f), mastTop, hubPt, strokeWidth = 1.5f)
                            }
                            drawCircle(finish.core, radius = 9f * zoomScale, center = hubPt)

                            for (b in 0 until 3) {
                                val angle = gyroPhase * 1.4f + b * (2f * PI.toFloat() / 3f)
                                val bladeLen = 2.15f + explodeFactor * 0.35f
                                val tip = Vec3(bladeLen * cos(angle), hubCenter.y + bladeLen * sin(angle), hubZ + 0.1f)
                                val wing = Vec3(0.26f * -sin(angle), hubCenter.y + 0.26f * cos(angle), hubZ)
                                val (tipPt, _) = project(tip)
                                val (wingPt, _) = project(wing)
                                val path = Path().apply {
                                    moveTo(hubPt.x, hubPt.y)
                                    lineTo(wingPt.x, wingPt.y)
                                    lineTo(tipPt.x, tipPt.y)
                                    close()
                                }
                                if (!wireframe) drawPath(path, finish.primary.copy(alpha = 0.45f))
                                drawPath(path, finish.primary, style = Stroke(width = 2.2f))
                            }
                        }

                        catLower.contains("robot") || catLower.contains("ai") -> {
                            // AI & Robotics: 3D Autonomous Quadrotor Drone
                            val bodyY = explodeFactor * 0.35f
                            val (centerPt, _) = project(Vec3(0f, bodyY, 0f))
                            val armSpan = 2.0f + explodeFactor * 0.85f
                            val arms = listOf(
                                Vec3(armSpan, bodyY, armSpan),
                                Vec3(-armSpan, bodyY, armSpan),
                                Vec3(-armSpan, bodyY, -armSpan),
                                Vec3(armSpan, bodyY, -armSpan)
                            )
                            arms.forEachIndexed { idx, tip ->
                                val (tipPt, _) = project(tip)
                                drawLine(finish.accent, centerPt, tipPt, strokeWidth = if (wireframe) 2.5f else 6f * zoomScale)
                                // Spinning rotor ring
                                val rotorR = 0.72f
                                val dir = if (idx % 2 == 0) 1f else -1f
                                for (s in 0 until 14) {
                                    val a1 = gyroPhase * 3.2f * dir + (s / 14f) * (2f * PI.toFloat())
                                    val a2 = gyroPhase * 3.2f * dir + ((s + 1) / 14f) * (2f * PI.toFloat())
                                    val p1 = project(Vec3(tip.x + rotorR * cos(a1), tip.y + 0.22f + explodeFactor * 0.4f, tip.z + rotorR * sin(a1))).first
                                    val p2 = project(Vec3(tip.x + rotorR * cos(a2), tip.y + 0.22f + explodeFactor * 0.4f, tip.z + rotorR * sin(a2))).first
                                    drawLine(finish.primary, p1, p2, strokeWidth = 2f)
                                }
                            }
                            drawCircle(finish.core.copy(alpha = 0.32f), radius = 28f * zoomScale, center = centerPt)
                            drawCircle(finish.primary, radius = 12f * zoomScale, center = centerPt)
                        }

                        catLower.contains("app") || catLower.contains("web") || catLower.contains("soft") -> {
                            // Digital / Software / Apps: 3D Holographic Compute Stack
                            val layerOffsets = listOf(-1.05f, 0f, 1.05f)
                            layerOffsets.forEachIndexed { idx, baseY ->
                                val y = baseY * (1f + explodeFactor * 1.1f)
                                val w = 1.85f - idx * 0.14f
                                val d = 1.35f - idx * 0.1f
                                val pts = listOf(
                                    Vec3(-w, y, -d), Vec3(w, y, -d), Vec3(w, y, d), Vec3(-w, y, d)
                                ).map { project(it).first }
                                val path = Path().apply {
                                    moveTo(pts[0].x, pts[0].y); lineTo(pts[1].x, pts[1].y)
                                    lineTo(pts[2].x, pts[2].y); lineTo(pts[3].x, pts[3].y); close()
                                }
                                val layerColor = when (idx) {
                                    0 -> finish.accent
                                    1 -> finish.primary
                                    else -> finish.core
                                }
                                if (!wireframe) drawPath(path, layerColor.copy(alpha = 0.26f))
                                drawPath(path, layerColor, style = Stroke(width = 2.3f))
                            }
                            val (topBeam, _) = project(Vec3(0f, 1.35f * (1f + explodeFactor), 0f))
                            val (botBeam, _) = project(Vec3(0f, -1.35f * (1f + explodeFactor), 0f))
                            drawLine(finish.core.copy(alpha = 0.65f), topBeam, botBeam, strokeWidth = 2.5f)
                        }

                        else -> {
                            // 2. Outer Gyro Rings (exploded vertically)
                            drawGyroRing(
                                radius = 2.35f,
                                tiltZ = gyroPhase * 0.6f,
                                tiltX = 0.35f,
                                yOffset = explodeFactor * 1.15f,
                                color = finish.primary,
                                wireframe = wireframe,
                                project = ::project
                            )
                            drawGyroRing(
                                radius = 3.05f,
                                tiltZ = -gyroPhase * 0.35f,
                                tiltX = (PI / 2.0).toFloat(),
                                yOffset = -explodeFactor * 1.15f,
                                color = finish.accent.copy(alpha = 0.85f),
                                wireframe = wireframe,
                                project = ::project
                            )

                            // 3. Central Octahedral Core Hub
                            val coreRadius = 1.15f
                            val coreVertices = listOf(
                                Vec3(0f, coreRadius, 0f),
                                Vec3(coreRadius, 0f, 0f),
                                Vec3(0f, 0f, coreRadius),
                                Vec3(-coreRadius, 0f, 0f),
                                Vec3(0f, 0f, -coreRadius),
                                Vec3(0f, -coreRadius, 0f)
                            )
                            val faces = listOf(
                                Triple(0, 1, 2), Triple(0, 2, 3), Triple(0, 3, 4), Triple(0, 4, 1),
                                Triple(5, 2, 1), Triple(5, 3, 2), Triple(5, 4, 3), Triple(5, 1, 4)
                            )
                            val projectedCore = coreVertices.map(::project)
                            val sortedFaces = faces.map { (i, j, k) ->
                                val depth = (projectedCore[i].second + projectedCore[j].second + projectedCore[k].second) / 3f
                                Pair(Triple(i, j, k), depth)
                            }.sortedBy { it.second }

                            sortedFaces.forEach { (tri, depth) ->
                                val p1 = projectedCore[tri.first].first
                                val p2 = projectedCore[tri.second].first
                                val p3 = projectedCore[tri.third].first
                                val path = Path().apply {
                                    moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close()
                                }
                                if (!wireframe) {
                                    val shade = (0.35f + (depth + 1.5f) * 0.22f).coerceIn(0.22f, 0.88f)
                                    drawPath(path, color = finish.core.copy(alpha = shade))
                                }
                                drawPath(
                                    path,
                                    color = if (wireframe) finish.primary else finish.primary.copy(alpha = 0.9f),
                                    style = Stroke(width = if (wireframe) 2.2f else 1.5f)
                                )
                            }

                            // 4. Four Lateral Sensor / Actuator Pods (exploded radially)
                            val podDirections = listOf(
                                Vec3(1f, 0f, 0f),
                                Vec3(-1f, 0f, 0f),
                                Vec3(0f, 0f, 1f),
                                Vec3(0f, 0f, -1f)
                            )
                            podDirections.forEach { dir ->
                                val dist = 2.4f + explodeFactor * 1.45f
                                val centerVec = Vec3(dir.x * dist, dir.y * dist, dir.z * dist)
                                val (podTop, zTop) = project(centerVec.copy(y = centerVec.y + 0.48f))
                                val (podBottom, _) = project(centerVec.copy(y = centerVec.y - 0.48f))
                                val (podCenter, _) = project(centerVec)
                                val (hubOrigin, _) = project(Vec3(0f, 0f, 0f))

                                if (explodeFactor > 0.05f) {
                                    drawLine(
                                        color = finish.primary.copy(alpha = 0.35f),
                                        start = hubOrigin,
                                        end = podCenter,
                                        strokeWidth = 1.4f
                                    )
                                }

                                val podAlpha = (0.55f + (zTop + 2f) * 0.12f).coerceIn(0.4f, 0.98f)
                                drawLine(
                                    color = if (wireframe) finish.primary.copy(alpha = 0.8f) else finish.primary.copy(alpha = podAlpha),
                                    start = podTop,
                                    end = podBottom,
                                    strokeWidth = if (wireframe) 4f else 14f * zoomScale
                                )
                                drawCircle(
                                    color = finish.core,
                                    radius = 6f * zoomScale,
                                    center = podTop,
                                    style = if (wireframe) Stroke(width = 2f) else androidx.compose.ui.graphics.drawscope.Fill
                                )
                            }
                        }
                    }
                }

                // Overlay badge
                Surface(
                    color = Color(0xAA080B14),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = title.take(28),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEDF2FF),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Exploded View Slider + Finish Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.cad_exploded_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF8D9AB1)
                        )
                        Text(
                            text = "${exploded.toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = finish.primary
                        )
                    }
                    Slider(
                        value = exploded,
                        onValueChange = { exploded = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = finish.primary,
                            activeTrackColor = finish.primary
                        ),
                        modifier = Modifier.testTag("cad_exploded_slider")
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CadFinish.entries.forEach { item ->
                        val selected = item == finish
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(item.primary)
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { finish = item }
                                .testTag("cad_finish_${item.name.lowercase()}")
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGyroRing(
    radius: Float,
    tiltZ: Float,
    tiltX: Float,
    yOffset: Float,
    color: Color,
    wireframe: Boolean,
    project: (Vec3) -> Pair<Offset, Float>
) {
    val segments = 36
    val points = (0..segments).map { step ->
        val theta = (step.toFloat() / segments) * (2f * PI.toFloat())
        val x0 = radius * cos(theta)
        val y0 = radius * sin(theta)
        // Tilt around X then Z
        val y1 = y0 * cos(tiltX)
        val z1 = y0 * sin(tiltX)
        val x2 = x0 * cos(tiltZ) - y1 * sin(tiltZ)
        val y2 = x0 * sin(tiltZ) + y1 * cos(tiltZ) + yOffset
        project(Vec3(x2, y2, z1)).first
    }
    for (i in 0 until segments) {
        drawLine(
            color = color.copy(alpha = if (wireframe) 0.7f else 0.9f),
            start = points[i],
            end = points[i + 1],
            strokeWidth = if (wireframe) 1.6f else 3.2f
        )
    }
}

@Composable
private fun CadIconToggle(
    active: Boolean,
    accent: Color,
    tag: String,
    contentDesc: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (active) accent.copy(alpha = 0.2f) else Color(0xFF151E33),
        border = BorderStroke(1.dp, if (active) accent else Color(0xFF283550)),
        contentColor = if (active) accent else Color(0xFF8D9AB1),
        modifier = Modifier
            .size(40.dp)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
