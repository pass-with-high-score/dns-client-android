package app.pwhs.dnsclient.ui.servers

import app.pwhs.dnsclient.domain.model.DnsServer

sealed class ServerListUiAction {
    data class OnSelectServer(val server: DnsServer) : ServerListUiAction()
    data class OnConfirmNextDns(val profileId: String) : ServerListUiAction()
    data class OnConfirmCustom(val url: String) : ServerListUiAction()
    data object OnDismissDialog : ServerListUiAction()
}