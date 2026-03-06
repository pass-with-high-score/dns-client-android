package app.pwhs.dnsclient.core.dns

import java.nio.ByteBuffer

/**
 * Minimal DNS packet parser.
 * Extracts domain name, query type, and transaction ID from raw DNS query bytes.
 */
object DnsPacketParser {

    data class DnsQuery(
        val transactionId: Int,
        val domain: String,
        val queryType: Int,
        val rawBytes: ByteArray
    ) {
        val queryTypeName: String
            get() = when (queryType) {
                1 -> "A"
                28 -> "AAAA"
                5 -> "CNAME"
                15 -> "MX"
                2 -> "NS"
                12 -> "PTR"
                6 -> "SOA"
                16 -> "TXT"
                33 -> "SRV"
                65 -> "HTTPS"
                else -> "TYPE$queryType"
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DnsQuery) return false
            return transactionId == other.transactionId && domain == other.domain && queryType == other.queryType
        }

        override fun hashCode(): Int {
            var result = transactionId
            result = 31 * result + domain.hashCode()
            result = 31 * result + queryType
            return result
        }
    }

    /**
     * Parse a raw DNS query packet (the UDP payload, without IP/UDP headers).
     */
    fun parseQuery(data: ByteArray): DnsQuery? {
        if (data.size < 12) return null // DNS header is 12 bytes minimum

        val buffer = ByteBuffer.wrap(data)
        val transactionId = buffer.short.toInt() and 0xFFFF

        // Skip flags (2 bytes), qdcount (2 bytes), ancount (2 bytes), nscount (2 bytes), arcount (2 bytes)
        buffer.position(12)

        // Parse the domain name (QNAME)
        val domain = parseDomainName(buffer) ?: return null

        if (buffer.remaining() < 4) return null

        val queryType = buffer.short.toInt() and 0xFFFF
        // val queryClass = buffer.short.toInt() and 0xFFFF // skipped, always IN (1)

        return DnsQuery(
            transactionId = transactionId,
            domain = domain,
            queryType = queryType,
            rawBytes = data
        )
    }

    /**
     * Extract response code from a DNS response.
     * RCODE is in the lower 4 bits of the flags (byte offset 3).
     */
    fun extractResponseCode(responseData: ByteArray): Int {
        if (responseData.size < 4) return -1
        return responseData[3].toInt() and 0x0F
    }

    private fun parseDomainName(buffer: ByteBuffer): String? {
        val parts = mutableListOf<String>()
        var length = buffer.get().toInt() and 0xFF

        while (length > 0) {
            if (length >= 64) {
                // Compressed pointer — not expected in query section but handle gracefully
                return null
            }
            if (buffer.remaining() < length) return null

            val label = ByteArray(length)
            buffer.get(label)
            parts.add(String(label, Charsets.US_ASCII))

            if (buffer.remaining() < 1) return null
            length = buffer.get().toInt() and 0xFF
        }

        return if (parts.isNotEmpty()) parts.joinToString(".") else null
    }
}
