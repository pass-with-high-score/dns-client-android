package app.pwhs.dnsclient.core.dns

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import timber.log.Timber

/**
 * DNS-over-HTTPS resolver.
 * Sends raw DNS query bytes via HTTP POST to a DoH endpoint and returns raw DNS response bytes.
 */
class DohResolver(private val httpClient: HttpClient) {

    /**
     * Resolve a DNS query via DoH.
     *
     * @param dohUrl The DoH endpoint URL (e.g., "https://cloudflare-dns.com/dns-query")
     * @param queryData Raw DNS query bytes (UDP payload)
     * @return Raw DNS response bytes, or null on failure
     */
    suspend fun resolve(dohUrl: String, queryData: ByteArray): ByteArray? {
        return try {
            val response = httpClient.post(dohUrl) {
                contentType(ContentType("application", "dns-message"))
                header("Accept", "application/dns-message")
                setBody(queryData)
            }

            if (response.status.value in 200..299) {
                response.bodyAsBytes()
            } else {
                Timber.w("DoH response error: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "DoH resolution failed for URL: $dohUrl")
            null
        }
    }
}
