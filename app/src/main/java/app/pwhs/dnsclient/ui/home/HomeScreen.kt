package app.pwhs.dnsclient.ui.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pwhs.dnsclient.ui.home.component.MainContent
import app.pwhs.dnsclient.ui.home.component.ServerSheetContent
import app.pwhs.dnsclient.ui.servers.ServerListUiAction
import app.pwhs.dnsclient.ui.servers.ServerListUiState
import app.pwhs.dnsclient.ui.servers.ServerListViewModel
import app.pwhs.dnsclient.ui.theme.DNSClientTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.QueryLogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(
    navigator: DestinationsNavigator,
    homeViewModel: HomeViewModel = koinViewModel(),
    serverViewModel: ServerListViewModel = koinViewModel()
) {
    val ctx = LocalContext.current
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val serverState by serverViewModel.uiState.collectAsStateWithLifecycle()

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

    HomeUI(
        homeState = homeState,
        serverListUiState = serverState,
        navigateToQueryLogs = { navigator.navigate(QueryLogScreenDestination) },
        navigateToSettings = { navigator.navigate(SettingsScreenDestination) },
        uiAction = { homeViewModel.onAction(it) },
        serverListUiAction = { serverViewModel.onAction(it) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeUI(
    modifier: Modifier = Modifier,
    homeState: HomeUiState = HomeUiState(),
    serverListUiState: ServerListUiState = ServerListUiState(),
    navigateToQueryLogs: () -> Unit = {},
    navigateToSettings: () -> Unit = {},
    uiAction: (HomeUiAction) -> (Unit) = {},
    serverListUiAction: (ServerListUiAction) -> (Unit) = {}
) {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    // Uptime ticker
    var uptimeSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(homeState.connectedSince) {
        if (homeState.connectedSince != null) {
            while (true) {
                uptimeSeconds = (System.currentTimeMillis() - homeState.connectedSince) / 1000
                delay(1000)
            }
        } else {
            uptimeSeconds = 0
        }
    }

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = {
                    Text("DNS Client", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = navigateToQueryLogs) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Query Logs"
                        )
                    }
                    IconButton(onClick = navigateToSettings) {
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
                serverState = serverListUiState,
                onSelectServer = { serverListUiAction(ServerListUiAction.OnSelectServer(it)) },
                onConfirmNextDns = { serverListUiAction(ServerListUiAction.OnConfirmNextDns(it)) },
                onConfirmCustom = { serverListUiAction(ServerListUiAction.OnConfirmCustom(it)) },
                onDismissDialog = { serverListUiAction(ServerListUiAction.OnDismissDialog) }
            )
        }
    ) { padding ->
        MainContent(
            state = homeState,
            uptimeSeconds = uptimeSeconds,
            onToggle = { uiAction(HomeUiAction.OnToggleVpn) },
            modifier = Modifier.padding(padding)
        )
    }
}

@Preview
@Composable
private fun HomeUiPreview() {
    DNSClientTheme {
        HomeUI()
    }
}

internal fun formatUptime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
    else String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

