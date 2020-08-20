package org.jitsi.rtp.rtp

import org.jitsi.rtp.Packet.Companion.BYTES_TO_LEAVE_AT_END_OF_PACKET
import org.jitsi.rtp.extensions.bytearray.getBitAsBool
import org.jitsi.rtp.extensions.unsigned.toPositiveInt
import org.jitsi.rtp.rtp.RtpHeader.Companion.FIXED_HEADER_SIZE_BYTES
import org.jitsi.rtp.rtp.RtpPacket.Companion.BYTES_TO_LEAVE_AT_START_OF_PACKET
import org.jitsi.rtp.util.BufferPool
import org.jitsi.rtp.util.getBitsAsInt
import java.lang.IllegalArgumentException

/**
 * Parses RED (RFC2198) packets.
 */
class RedPacketParser<PacketType : RtpPacket>(
    /**
     * A function to create packets from redundancy blocks (used so we can create a packet with the correct class).
     */
    val createPacket: (ByteArray, Int, Int) -> PacketType
) {

    /**
     * Removes the RED encapsulation from a RED packet. The operation is done in-place, changing the payload type
     * and offset of the packet. Redundancy blocks are ignored.
     */
    fun decapsulate(rtpPacket: RtpPacket) {
        parse(rtpPacket, false)
    }

    /**
     * Removes the RED encapsulation from a RED packet. The operation is done in-place, changing the payload type
     * and offset of the packet. Redundancy blocks are parsed as [PacketType] into newly allocated buffers.
     *
     * If redundancy is not needed, use [decapsulate] instead.
     */
    fun parseRedundancyAndDecapsulate(rtpPacket: RtpPacket) = parse(rtpPacket, true) as List<PacketType>

    /**
     * Destructively parses a specific [RtpPacket] as RED. The redundant RED blocks are removed from the payload,
     * leaving only the primary block as payload, and the payload type is changed to the payload type of the primary
     * block. The transformed packet is no longer a RED packet, so the operation must not be performed again.
     *
     * If [parseRedundancy] is set, the redundancy blocks are parsed as [PacketType] into newly allocated buffers.
     *
     * @param rtpPacket the packet to parse
     * @param parseRedundancy whether to parse
     * @return The list of parsed redundancy packets (possibly empty) if [parseRedundancy] is set, and `null` otherwise.
     */
    private fun parse(
        rtpPacket: RtpPacket,
        parseRedundancy: Boolean
    ): List<PacketType>? = with(rtpPacket) {
        var currentOffset = payloadOffset

        val redundancyBlockHeaders = mutableListOf<RedundancyBlockHeader>()
        lateinit var primaryBlockHeader: PrimaryBlockHeader
        do {
            if (currentOffset > length) {
                throw IllegalArgumentException(
                    "Invalid RED packet: no last header block found within the allowed length.")
            }
            val blockHeader = BlockHeader.parse(buffer, currentOffset)
            when (blockHeader) {
                is PrimaryBlockHeader -> primaryBlockHeader = blockHeader
                is RedundancyBlockHeader -> redundancyBlockHeaders.add(blockHeader)
            }
            currentOffset += blockHeader.headerLength
        } while (blockHeader is RedundancyBlockHeader)

        val packets = if (parseRedundancy) mutableListOf<PacketType>() else null
        for ((index, it) in redundancyBlockHeaders.withIndex()) {
            val blockLength = it.length
            if (currentOffset + blockLength > offset + length) {
                throw IllegalArgumentException("Invalid RED packet: blocks extend past the packet length.")
            }

            if (parseRedundancy) {
                val byteArray = BufferPool.getArray(
                    BYTES_TO_LEAVE_AT_START_OF_PACKET +
                        FIXED_HEADER_SIZE_BYTES + blockLength +
                        BYTES_TO_LEAVE_AT_END_OF_PACKET
                )

                System.arraycopy(
                    buffer, offset,
                    byteArray, BYTES_TO_LEAVE_AT_START_OF_PACKET,
                    FIXED_HEADER_SIZE_BYTES
                )
                RtpHeader.setCsrcCount(byteArray, BYTES_TO_LEAVE_AT_START_OF_PACKET, 0)
                RtpHeader.setHasExtensions(byteArray, BYTES_TO_LEAVE_AT_START_OF_PACKET, false)
                System.arraycopy(
                    buffer, currentOffset,
                    byteArray, BYTES_TO_LEAVE_AT_START_OF_PACKET + FIXED_HEADER_SIZE_BYTES,
                    blockLength
                )

                val redundancyPacket =
                    createPacket(byteArray, BYTES_TO_LEAVE_AT_START_OF_PACKET, FIXED_HEADER_SIZE_BYTES + blockLength)
                        .apply {
                            payloadType = it.pt.toInt()
                            timestamp -= it.timestampOffset
                            // RFC2198 describes how to reconstruct the timestamp and payload type, but not an RTP
                            // sequence number. However, we operate on the RTP level and need to reconstruct an RTP
                            // packet. So we make the assumption that the redundant blocks represent the packets
                            // directly proceeding the primary.
                            sequenceNumber =
                                (sequenceNumber - (redundancyBlockHeaders.size - index)).toRtpSequenceNumber().value
                        }

                packets?.add(redundancyPacket)
            }
            currentOffset += blockLength
        }

        // We've now read past the redundancy blocks, and currentOffset points to the payload of the primary block.
        // Remove RED "in-place" by shifting the header.
        val headerLength = headerLength
        val newOffset = currentOffset - headerLength
        val newLength = length - currentOffset + offset + headerLength
        System.arraycopy(
            buffer, offset,
            buffer, newOffset,
            headerLength
        )
        offset = newOffset
        length = newLength
        payloadType = primaryBlockHeader.pt.toInt()

        return packets
    }
}

sealed class BlockHeader(
    /**
     * The block's payload type.
     */
    val pt: Byte
) {
    abstract val headerLength: Int
    companion object {
        /**
         * See RFC2198.
         *
         * Header blocks before the last (for redundant blocks):
         *  0                   1                   2                   3
         *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
         * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         * |1|   block PT  |  timestamp offset         |   block length    |
         * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         *
         * The last header block (for the primary block):
         *  0
         *  0 1 2 3 4 5 6 7
         * +-+-+-+-+-+-+-+-+
         * |0|   block PT  |
         * +-+-+-+-+-+-+-+-+
         *
         */
        fun parse(buffer: ByteArray, offset: Int): BlockHeader {
            val follow = buffer.getBitAsBool(offset, 0)
            val pt = buffer.getBitsAsInt(offset, 1, 7).toByte()

            if (!follow) return PrimaryBlockHeader(pt)

            val timestampOffset =
                (buffer[offset + 1].toPositiveInt() shl 6) + buffer.getBitsAsInt(offset + 2, 0, 6)
            val blockLength =
                (buffer.getBitsAsInt(offset + 2, 6, 2) shl 8) + buffer[offset + 3].toPositiveInt()

            return RedundancyBlockHeader(pt, timestampOffset, blockLength)
        }
    }
}

class PrimaryBlockHeader(pt: Byte) : BlockHeader(pt) {
    override val headerLength = 1
}

class RedundancyBlockHeader(
    pt: Byte,
    val timestampOffset: Int,
    /**
    * The length of the block's payload in bytes, or `null` if this is the last block (whose length is not explicitly
    * specified).
    */
    val length: Int
) : BlockHeader(pt) {
    override val headerLength = 4
}

internal class RtpRedPacket(buffer: ByteArray, offset: Int, length: Int) : RtpPacket(buffer, offset, length) {
    override fun clone(): RtpRedPacket =
        RtpRedPacket(
            cloneBuffer(BYTES_TO_LEAVE_AT_START_OF_PACKET),
            BYTES_TO_LEAVE_AT_START_OF_PACKET,
            length)

    companion object {
        val parser = RedPacketParser { b, o, l -> RtpPacket(b, o, l) }
    }

    fun decapsulate() = parser.decapsulate(this)
    fun parseRedundancyAndDecapsulate(): List<RtpPacket> = parser.parseRedundancyAndDecapsulate(this)
}
