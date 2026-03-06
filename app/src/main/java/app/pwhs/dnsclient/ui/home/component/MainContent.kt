package app.pwhs.dnsclient.ui.home.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pwhs.dnsclient.ui.home.HomeUiState
import app.pwhs.dnsclient.ui.home.formatUptime
import app.pwhs.dnsclient.ui.theme.StatusOnline
import app.pwhs.dnsclient.ui.theme.StatusOnlineDark

@Composable
internal fun MainContent(
    state: HomeUiState,
    uptimeSeconds: Long,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonScale by animateFloatAsState(
        targetValue = if (state.isConnected) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "buttonScale"
    )

    val bgGradientColors = if (state.isConnected) {
        listOf(StatusOnlineDark.copy(alpha = 0.15f), Color.Transparent)
    } else {
        listOf(Color.Transparent, Color.Transparent)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradientColors))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (state.isConnected) "Protected" else "Not Protected",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (state.isConnected) StatusOnline
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${state.serverEmoji} ${state.serverName}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        val buttonColor by animateColorAsState(
            targetValue = if (state.isConnected) StatusOnline
            else MaterialTheme.colorScheme.surfaceVariant,
            animationSpec = tween(400),
            label = "buttonColor"
        )
        val iconColor by animateColorAsState(
            targetValue = if (state.isConnected) Color.White
            else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(400),
            label = "iconColor"
        )

        Box(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer { scaleX = buttonScale; scaleY = buttonScale }
                .shadow(
                    elevation = if (state.isConnected) 24.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = if (state.isConnected) StatusOnline.copy(alpha = 0.4f)
                    else Color.Black.copy(alpha = 0.1f),
                    spotColor = if (state.isConnected) StatusOnline.copy(alpha = 0.4f)
                    else Color.Black.copy(alpha = 0.1f)
                )
                .clip(CircleShape)
                .background(buttonColor)
                .border(
                    width = 3.dp,
                    color = if (state.isConnected) StatusOnline.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = if (state.isConnected) "Disconnect" else "Connect",
                modifier = Modifier.size(64.dp),
                tint = iconColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (state.isConnected) "Tap to disconnect" else "Tap to connect",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.QueryStats,
                label = "Queries",
                value = if (state.isConnected) "${state.totalQueries}" else "—"
            )
            StatItem(
                icon = Icons.Default.Schedule,
                label = "Uptime",
                value = if (state.isConnected) formatUptime(uptimeSeconds) else "—"
            )
        }
    }
}