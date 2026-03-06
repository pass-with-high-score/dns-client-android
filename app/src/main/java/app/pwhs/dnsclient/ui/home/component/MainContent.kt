package app.pwhs.dnsclient.ui.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

        // Custom DNS toggle switch (NextDNS / 1.1.1.1 style)
        DnsToggleSwitch(
            isChecked = state.isConnected,
            onToggle = onToggle
        )

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