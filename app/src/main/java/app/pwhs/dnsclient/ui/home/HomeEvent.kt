package app.pwhs.dnsclient.ui.home

import android.content.Intent

sealed class HomeEvent {
    data class RequestVpnPermission(val intent: Intent) : HomeEvent()
}