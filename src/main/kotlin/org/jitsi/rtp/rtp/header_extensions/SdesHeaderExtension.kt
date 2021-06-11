package org.jitsi.rtp.rtp.header_extensions

import org.jitsi.rtp.rtp.RtpPacket
import org.jitsi.rtp.rtp.header_extensions.HeaderExtensionHelpers.Companion.getDataLengthBytes
import org.jitsi.rtp.util.BufferPool
import java.nio.charset.StandardCharsets

/**
 * https://datatracker.ietf.org/doc/html/rfc7941#section-4.1.1
 * Note: this is only the One-Byte Format, because we don't support Two-Byte yet.
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  ID   |  len  | SDES item text value ...                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
class SdesHeaderExtension {
    companion object {
        const val DATA_OFFSET = 1

        fun getTextValue(ext: RtpPacket.HeaderExtension): String =
            getTextValue(ext.currExtBuffer, ext.currExtOffset)
        fun setTextValue(ext: RtpPacket.HeaderExtension, sdesValue: String) =
            setTextValue(ext.currExtBuffer, ext.currExtOffset, sdesValue)

        fun getTextValue(buf: ByteArray, offset: Int): String {
            val dataLength = getDataLengthBytes(buf, offset)
            val copy = BufferPool.getArray(dataLength)
            System.arraycopy(buf, offset + SdesHeaderExtension.DATA_OFFSET, copy, 0, dataLength)
            return String(copy, StandardCharsets.US_ASCII)
        }
        fun setTextValue(buf: ByteArray, offset: Int, sdesValue: String) {
            // TODO
        }
    }
}
