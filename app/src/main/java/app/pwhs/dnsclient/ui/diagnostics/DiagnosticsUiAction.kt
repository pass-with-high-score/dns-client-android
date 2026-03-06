package app.pwhs.dnsclient.ui.diagnostics

sealed class DiagnosticsUiAction {
    data object OnRefresh : DiagnosticsUiAction()
}