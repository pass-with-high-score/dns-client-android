package app.pwhs.dnsclient.ui.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pwhs.dnsclient.domain.model.DnsServer

@Composable
internal fun ServerSheetContent(
    serverState: app.pwhs.dnsclient.ui.servers.ServerListUiState,
    onSelectServer: (DnsServer) -> Unit,
    onConfirmNextDns: (String) -> Unit,
    onConfirmCustom: (String) -> Unit,
    onDismissDialog: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "DNS Servers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(400.dp)
        ) {
            items(serverState.servers) { server ->
                val isSelected = serverState.selectedServerKey == server.key
                ServerItem(
                    server = server,
                    isSelected = isSelected,
                    subtitle = if (server is DnsServer.NextDns && serverState.nextDnsProfileId.isNotEmpty()) {
                        "Profile: ${serverState.nextDnsProfileId}"
                    } else null,
                    onClick = { onSelectServer(server) }
                )
            }

            if (serverState.selectedServerKey == "Custom" && serverState.customDohUrl.isNotEmpty()) {
                item {
                    ServerItem(
                        server = DnsServer.Custom(customDohUrl = serverState.customDohUrl),
                        isSelected = true,
                        onClick = { onSelectServer(DnsServer.Custom()) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectServer(DnsServer.Custom()) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add custom",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add custom DoH server",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Dialogs
    if (serverState.showNextDnsDialog) {
        var profileId by remember { mutableStateOf(serverState.nextDnsProfileId) }
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text("NextDNS Profile") },
            text = {
                Column {
                    Text(
                        text = "Enter your NextDNS profile ID (from nextdns.io)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = profileId,
                        onValueChange = { profileId = it },
                        label = { Text("Profile ID") },
                        placeholder = { Text("e.g. abc123") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmNextDns(profileId) },
                    enabled = profileId.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDialog) { Text("Cancel") }
            }
        )
    }

    if (serverState.showCustomDialog) {
        var customUrl by remember { mutableStateOf(serverState.customDohUrl) }
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text("Custom DoH Server") },
            text = {
                Column {
                    Text(
                        text = "Enter the DNS-over-HTTPS endpoint URL",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("DoH URL") },
                        placeholder = { Text("https://example.com/dns-query") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmCustom(customUrl) },
                    enabled = customUrl.startsWith("https://")
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDialog) { Text("Cancel") }
            }
        )
    }
}
