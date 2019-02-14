/*
 * Copyright @ 2018 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.rtp.new_scheme.rtcp

import org.jitsi.rtp.Packet
import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.rtcp.RtcpByePacket
import org.jitsi.rtp.util.ByteBufferUtils
import toUInt
import unsigned.toUByte
import unsigned.toUInt
import unsigned.toULong
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * https://tools.ietf.org/html/rfc3550#section-6.6
 *
 *       0                   1                   2                   3
 *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |V=2|P|    SC   |   PT=BYE=203  |             length            |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       |                           SSRC/CSRC                           |
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *       :                              ...                              :
 *       +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * (opt) |     length    |               reason for leaving            ...
 *       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 */
class ReadOnlyRtcpByePacket(
    val header: ReadOnlyRtcpHeader = ReadOnlyRtcpHeader(),
    val ssrcs: List<Long>,
    val reason: String? = null,
    buf: ByteBuffer? = null
) : ReadOnlyRtcpPacket() {
    override val dataBuf: ByteBuffer

    override val sizeBytes: Int
        get() {
            val dataSize = header.sizeBytes - 4 + ssrcs.size * 4
            val reasonSize = if (reason != null) {
                val fieldSize = reason.toByteArray(StandardCharsets.US_ASCII).size + 1
                var paddingSize = 0
                while ((fieldSize + paddingSize) % 4 != 0) {
                    paddingSize++
                }
                fieldSize + paddingSize
            } else {
                0
            }

            return dataSize + reasonSize
        }

    companion object {
        fun fromBuffer(buf: ByteBuffer): ReadOnlyRtcpByePacket {
            val header = ReadOnlyRtcpHeader.fromBuffer(buf)
            val ssrcs = org.jitsi.rtp.rtcp.RtcpByePacket.getSsrcs(buf, header.reportCount)
            val reason = if (org.jitsi.rtp.rtcp.RtcpByePacket.hasReason(buf)) {
                String(org.jitsi.rtp.rtcp.RtcpByePacket.getReason(buf).array(), StandardCharsets.US_ASCII)
            } else {
                null
            }
            return ReadOnlyRtcpByePacket(header, ssrcs, reason)
        }
    }

    init {
        val b = ByteBufferUtils.ensureCapacity(buf, sizeBytes)
        b.rewind()
        b.limit(sizeBytes)

        b.put(header.getBuffer())
        // Rewind the buffer's position 4 bytes, since the first ssrc uses the last 4 bytes of the header
        b.position(b.position() - 4)
        RtcpByePacket.setSsrcs(b, ssrcs)

        if (reason != null) {
            RtcpByePacket.setReason(b, ByteBuffer.wrap(reason.toByteArray(StandardCharsets.US_ASCII)))
        }

        b.rewind()
        dataBuf = b
    }
}
