package app.pwhs.dnsclient.ui.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pwhs.dnsclient.ui.diagnostics.component.InfoCard
import app.pwhs.dnsclient.ui.diagnostics.component.InfoRow
import app.pwhs.dnsclient.ui.diagnostics.component.SectionTitle
import app.pwhs.dnsclient.ui.diagnostics.component.StatCard
import app.pwhs.dnsclient.ui.theme.DNSClientTheme
import app.pwhs.dnsclient.ui.theme.StatusOnline
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun DiagnosticsScreen(
    navigator: DestinationsNavigator,
    viewModel: DiagnosticsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DiagnosticsUI(
        state = state,
        onAction = { viewModel.onAction(it) },
        onNavigateBack = { navigator.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsUI(
    modifier: Modifier = Modifier,
    state: DiagnosticsUiState = DiagnosticsUiState(),
    onAction: (DiagnosticsUiAction) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onAction(DiagnosticsUiAction.OnRefresh)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle("Your Device")

            InfoCard {
                InfoRow(
                    icon = Icons.Default.NetworkCheck,
                    label = "Connection Type",
                    value = state.connectionType
                )
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(
                    icon = Icons.Default.Language,
                    label = "Public IP",
                    value = state.publicIp,
                    isLoading = state.isLoadingIp
                )
            }

            SectionTitle("VPN Status")

            InfoCard {
                InfoRow(
                    icon = Icons.Default.Shield,
                    label = "Status",
                    value = if (state.isConnected) "Connected" else "Disconnected",
                    valueColor = if (state.isConnected) StatusOnline else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow(
                    icon = Icons.Default.Dns,
                    label = "Total Queries",
                    value = "${state.totalQueries}"
                )
            }

            SectionTitle("Traffic Stats")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Default.CloudDownload,
                    label = "Received",
                    value = formatBytes(state.bytesReceived),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.CloudUpload,
                    label = "Sent",
                    value = formatBytes(state.bytesSent),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview
@Composable
private fun DiagnosticsUIPreview() {
    DNSClientTheme {
        DiagnosticsUI()
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format(
            Locale.getDefault(),
            "%.2f MB",
            bytes / (1024.0 * 1024.0)
        )

        else -> String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
