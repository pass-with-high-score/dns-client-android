package app.pwhs.dnsclient.service

import java.nio.ByteBuffer

/**
 * Handles VPN tunnel I/O: reads IP packets from TUN, extracts DNS queries,
 * constructs response IP/UDP packets to write back.
 */
object DnsVpnConnection {

    private const val IPv4_HEADER_MIN_SIZE = 20
    private const val UDP_HEADER_SIZE = 8
    private const val DNS_PORT = 53

    data class DnsUdpPacket(
        val sourceIp: ByteArray,
        val destIp: ByteArray,
        val sourcePort: Int,
        val destPort: Int,
        val dnsPayload: ByteArray,
        /** Full original IP packet for reference */
        val ipHeaderLength: Int,
        val originalPacket: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DnsUdpPacket) return false
            return sourcePort == other.sourcePort && destPort == other.destPort
        }

        override fun hashCode(): Int = sourcePort * 31 + destPort
    }

    /**
     * Extract DNS query from a raw IP packet read from the TUN interface.
     * Returns null if the packet is not a DNS query (not UDP port 53).
     */
    fun extractDnsQuery(packetData: ByteArray, length: Int): DnsUdpPacket? {
        if (length < IPv4_HEADER_MIN_SIZE) return null

        val buffer = ByteBuffer.wrap(packetData, 0, length)

        // Check IP version (must be IPv4)
        val versionAndIhl = buffer.get(0).toInt() and 0xFF
        val version = versionAndIhl shr 4
        if (version != 4) return null

        val ipHeaderLength = (versionAndIhl and 0x0F) * 4
        if (length < ipHeaderLength + UDP_HEADER_SIZE) return null

        // Check protocol (must be UDP = 17)
        val protocol = buffer.get(9).toInt() and 0xFF
        if (protocol != 17) return null

        // Extract source and destination IP
        val sourceIp = ByteArray(4)
        val destIp = ByteArray(4)
        buffer.position(12)
        buffer.get(sourceIp)
        buffer.get(destIp)

        // Parse UDP header
        buffer.position(ipHeaderLength)
        val sourcePort = buffer.short.toInt() and 0xFFFF
        val destPort = buffer.short.toInt() and 0xFFFF

        // Only intercept DNS queries (destination port 53)
        if (destPort != DNS_PORT) return null

        val udpLength = buffer.short.toInt() and 0xFFFF
        buffer.short // Skip UDP checksum

        // Extract DNS payload
        val dnsPayloadLength = udpLength - UDP_HEADER_SIZE
        if (dnsPayloadLength <= 0 || buffer.remaining() < dnsPayloadLength) return null

        val dnsPayload = ByteArray(dnsPayloadLength)
        buffer.get(dnsPayload)

        return DnsUdpPacket(
            sourceIp = sourceIp,
            destIp = destIp,
            sourcePort = sourcePort,
            destPort = destPort,
            dnsPayload = dnsPayload,
            ipHeaderLength = ipHeaderLength,
            originalPacket = packetData.copyOf(length)
        )
    }

    /**
     * Build a response IP/UDP packet from DNS response bytes.
     * Swaps source/destination addresses.
     */
    fun buildResponsePacket(
        originalQuery: DnsUdpPacket,
        dnsResponse: ByteArray
    ): ByteArray {
        val ipHeaderLength = 20 // Use standard 20-byte header
        val udpLength = UDP_HEADER_SIZE + dnsResponse.size
        val totalLength = ipHeaderLength + udpLength

        val response = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(response)

        // --- IPv4 Header ---
        buffer.put((0x45).toByte()) // Version 4, IHL 5 (20 bytes)
        buffer.put(0.toByte()) // DSCP/ECN
        buffer.putShort(totalLength.toShort()) // Total length
        buffer.putShort(0.toShort()) // Identification
        buffer.putShort(0x4000.toShort()) // Flags: Don't Fragment
        buffer.put(64.toByte()) // TTL
        buffer.put(17.toByte()) // Protocol: UDP
        buffer.putShort(0.toShort()) // Checksum (will calculate)
        buffer.put(originalQuery.destIp) // Source IP = original dest
        buffer.put(originalQuery.sourceIp) // Dest IP = original source

        // Calculate IP header checksum
        val ipChecksum = calculateChecksum(response, 0, ipHeaderLength)
        response[10] = ((ipChecksum shr 8) and 0xFF).toByte()
        response[11] = (ipChecksum and 0xFF).toByte()

        // --- UDP Header ---
        buffer.position(ipHeaderLength)
        buffer.putShort(originalQuery.destPort.toShort()) // Source port = original dest (53)
        buffer.putShort(originalQuery.sourcePort.toShort()) // Dest port = original source
        buffer.putShort(udpLength.toShort()) // UDP length
        buffer.putShort(0.toShort()) // UDP checksum (0 = no checksum)

        // --- DNS Payload ---
        buffer.put(dnsResponse)

        return response
    }

    private fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length - 1) {
            sum += ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (length % 2 != 0) {
            sum += (data[offset + length - 1].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }
}
