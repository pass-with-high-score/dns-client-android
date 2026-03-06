package app.pwhs.dnsclient.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class DnsServer(
    val name: String,
    val description: String,
    val dohUrl: String,
    val primaryIp: String,
    val secondaryIp: String? = null,
    val iconEmoji: String = "🌐"
) {
    /** Unique key for DataStore persistence */
    val key: String get() = this::class.simpleName ?: name

    @Serializable
    data object Cloudflare : DnsServer(
        name = "Cloudflare",
        description = "1.1.1.1 — Fast & private",
        dohUrl = "https://cloudflare-dns.com/dns-query",
        primaryIp = "1.1.1.1",
        secondaryIp = "1.0.0.1",
        iconEmoji = "⚡"
    )

    @Serializable
    data object CloudflareSecurity : DnsServer(
        name = "Cloudflare Security",
        description = "1.1.1.2 — Malware blocking",
        dohUrl = "https://security.cloudflare-dns.com/dns-query",
        primaryIp = "1.1.1.2",
        secondaryIp = "1.0.0.2",
        iconEmoji = "🛡️"
    )

    @Serializable
    data object CloudflareFamily : DnsServer(
        name = "Cloudflare Family",
        description = "1.1.1.3 — Family safe",
        dohUrl = "https://family.cloudflare-dns.com/dns-query",
        primaryIp = "1.1.1.3",
        secondaryIp = "1.0.0.3",
        iconEmoji = "👨‍👩‍👧‍👦"
    )

    @Serializable
    data object Google : DnsServer(
        name = "Google",
        description = "8.8.8.8 — Reliable & fast",
        dohUrl = "https://dns.google/dns-query",
        primaryIp = "8.8.8.8",
        secondaryIp = "8.8.4.4",
        iconEmoji = "🔍"
    )

    @Serializable
    data class NextDns(val profileId: String = "") : DnsServer(
        name = "NextDNS",
        description = if (profileId.isNotEmpty()) "Profile: $profileId" else "Custom NextDNS profile",
        dohUrl = if (profileId.isNotEmpty()) "https://dns.nextdns.io/$profileId" else "https://dns.nextdns.io",
        primaryIp = "45.90.28.0",
        secondaryIp = "45.90.30.0",
        iconEmoji = "🔒"
    )

    @Serializable
    data class Custom(
        val customName: String = "Custom DNS",
        val customDohUrl: String = "",
        val customIp: String = ""
    ) : DnsServer(
        name = customName,
        description = customDohUrl.ifEmpty { "Custom DoH URL" },
        dohUrl = customDohUrl,
        primaryIp = customIp,
        iconEmoji = "⚙️"
    )

    companion object {
        /** All preset servers */
        val presets: List<DnsServer> = listOf(
            Cloudflare,
            CloudflareSecurity,
            CloudflareFamily,
            Google,
            NextDns(),
        )

        fun fromKey(key: String, profileId: String = "", customUrl: String = ""): DnsServer {
            return when (key) {
                "Cloudflare" -> Cloudflare
                "CloudflareSecurity" -> CloudflareSecurity
                "CloudflareFamily" -> CloudflareFamily
                "Google" -> Google
                "NextDns" -> NextDns(profileId)
                "Custom" -> Custom(customDohUrl = customUrl)
                else -> Cloudflare
            }
        }
    }
}
