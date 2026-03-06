package app.pwhs.dnsclient.ui.home

sealed class HomeUiAction {
    data object OnToggleVpn : HomeUiAction()
}