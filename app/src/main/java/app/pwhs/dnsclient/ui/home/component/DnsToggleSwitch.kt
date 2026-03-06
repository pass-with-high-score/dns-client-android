package app.pwhs.dnsclient.ui.home.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.pwhs.dnsclient.ui.theme.DNSClientTheme
import app.pwhs.dnsclient.ui.theme.StatusOnline
import app.pwhs.dnsclient.ui.theme.StatusOnlineDark

/**
 * A large, pill-shaped toggle switch inspired by NextDNS and 1.1.1.1 apps.
 *
 * Features:
 * - Pill track (~200×88dp) with animated color gradient
 * - Sliding circular thumb with spring physics
 * - Icon inside thumb changes from power → shield when connected
 * - Outer glow effect when ON
 * - Haptic feedback on toggle
 */
@Composable
fun DnsToggleSwitch(
    isChecked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    trackWidth: Dp = 200.dp,
    trackHeight: Dp = 88.dp,
    thumbSize: Dp = 72.dp,
    thumbPadding: Dp = 8.dp,
) {
    val haptics = LocalHapticFeedback.current

    // Thumb offset animation with spring
    val thumbTravel = trackWidth - thumbSize - thumbPadding * 2
    val thumbOffsetX by animateDpAsState(
        targetValue = if (isChecked) thumbTravel else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thumbOffset"
    )

    // Track colors
    val trackColor by animateColorAsState(
        targetValue = if (isChecked) StatusOnline.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(400),
        label = "trackColor"
    )
    val trackBorderColor by animateColorAsState(
        targetValue = if (isChecked) StatusOnline.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        animationSpec = tween(400),
        label = "trackBorder"
    )

    // Thumb colors
    val thumbColor by animateColorAsState(
        targetValue = if (isChecked) StatusOnline
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        animationSpec = tween(350),
        label = "thumbColor"
    )
    val thumbIconColor by animateColorAsState(
        targetValue = if (isChecked) Color.White
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        animationSpec = tween(350),
        label = "thumbIcon"
    )

    // Shadow / glow intensity
    val glowElevation by animateDpAsState(
        targetValue = if (isChecked) 20.dp else 4.dp,
        animationSpec = tween(500),
        label = "glow"
    )

    // Thumb scale bounce on state change
    val thumbScale by animateFloatAsState(
        targetValue = if (isChecked) 1.0f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumbScale"
    )

    val trackShape = RoundedCornerShape(50)

    // Outer container (track)
    Box(
        modifier = modifier
            .shadow(
                elevation = glowElevation,
                shape = trackShape,
                ambientColor = if (isChecked) StatusOnline.copy(alpha = 0.3f)
                else Color.Transparent,
                spotColor = if (isChecked) StatusOnline.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .size(width = trackWidth, height = trackHeight)
            .clip(trackShape)
            .background(
                brush = if (isChecked) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            StatusOnlineDark.copy(alpha = 0.3f),
                            StatusOnline.copy(alpha = 0.25f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(trackColor, trackColor)
                    )
                }
            )
            .border(
                width = 1.5.dp,
                color = trackBorderColor,
                shape = trackShape
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            }
            .padding(thumbPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetX.roundToPx(), 0) }
                .shadow(
                    elevation = if (isChecked) 12.dp else 4.dp,
                    shape = CircleShape,
                    ambientColor = if (isChecked) StatusOnline.copy(alpha = 0.5f)
                    else Color.Black.copy(alpha = 0.1f),
                    spotColor = if (isChecked) StatusOnline.copy(alpha = 0.5f)
                    else Color.Black.copy(alpha = 0.1f)
                )
                .size(thumbSize * thumbScale)
                .clip(CircleShape)
                .background(thumbColor)
                .border(
                    width = 2.dp,
                    color = if (isChecked) StatusOnline.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isChecked) Icons.Default.Shield
                else Icons.Default.PowerSettingsNew,
                contentDescription = if (isChecked) "Connected" else "Disconnected",
                modifier = Modifier.size(32.dp),
                tint = thumbIconColor
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
private fun DnsToggleSwitchOffPreview() {
    DNSClientTheme(dynamicColor = false) {
        Box(
            modifier = Modifier.padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            DnsToggleSwitch(isChecked = false, onToggle = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
private fun DnsToggleSwitchOnPreview() {
    DNSClientTheme(dynamicColor = false) {
        Box(
            modifier = Modifier.padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            DnsToggleSwitch(isChecked = true, onToggle = {})
        }
    }
}
