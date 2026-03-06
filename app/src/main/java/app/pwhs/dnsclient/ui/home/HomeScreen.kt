package app.pwhs.dnsclient.ui.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pwhs.dnsclient.domain.model.DnsServer
import app.pwhs.dnsclient.ui.servers.ServerListViewModel
import app.pwhs.dnsclient.ui.theme.StatusOnline
import app.pwhs.dnsclient.ui.theme.StatusOnlineDark
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.QueryLogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    homeViewModel: HomeViewModel = koinViewModel(),
    serverViewModel: ServerListViewModel = koinViewModel()
) {
    val ctx = LocalContext.current
    val homeState by homeViewModel.uiState.collectAsState()
    val serverState by serverViewModel.uiState.collectAsState()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            homeViewModel.launchVpnService(ctx)
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.events.collect { event ->
            when (event) {
                is HomeEvent.RequestVpnPermission -> {
                    vpnPermissionLauncher.launch(event.intent)
                }
            }
        }
    }

    // Uptime ticker
    var uptimeSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(homeState.connectedSince) {
        if (homeState.connectedSince != null) {
            while (true) {
                uptimeSeconds = (System.currentTimeMillis() - homeState.connectedSince!!) / 1000
                delay(1000)
            }
        } else {
            uptimeSeconds = 0
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = {
                    Text("DNS Client", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { navigator.navigate(QueryLogScreenDestination) }) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Query Logs"
                        )
                    }
                    IconButton(onClick = { navigator.navigate(SettingsScreenDestination) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        sheetPeekHeight = 120.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        sheetContent = {
            ServerSheetContent(
                serverState = serverState,
                onSelectServer = { serverViewModel.selectServer(it) },
                onConfirmNextDns = { serverViewModel.confirmNextDns(it) },
                onConfirmCustom = { serverViewModel.confirmCustom(it) },
                onDismissDialog = { serverViewModel.dismissDialog() }
            )
        }
    ) { padding ->
        MainContent(
            state = homeState,
            uptimeSeconds = uptimeSeconds,
            onToggle = { homeViewModel.toggleVpn(ctx) },
            modifier = Modifier.padding(padding)
        )
    }
}

// ── Main Content (VPN toggle + stats) ──────────────────────────────────────────

@Composable
private fun MainContent(
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
                .scale(buttonScale)
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

@Composable
private fun StatItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatUptime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}

// ── Bottom Sheet Content (DNS Server list) ─────────────────────────────────────

@Composable
private fun ServerSheetContent(
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

@Composable
private fun ServerItem(
    server: DnsServer,
    isSelected: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = server.iconEmoji, fontSize = 26.sp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle ?: server.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isSelected) "Selected" else "Not selected",
            tint = if (isSelected) StatusOnline else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp)
        )
    }
}
