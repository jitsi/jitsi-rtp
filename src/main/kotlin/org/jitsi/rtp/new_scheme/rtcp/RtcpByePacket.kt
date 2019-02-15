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

import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.new_scheme.CanBecomeModifiable
import org.jitsi.rtp.new_scheme.CanBecomeReadOnly
import org.jitsi.rtp.new_scheme.ConstructableFromBuffer
import org.jitsi.rtp.new_scheme.ModifiablePacket
import org.jitsi.rtp.new_scheme.ModifiableRtpHeader
import org.jitsi.rtp.rtcp.RtcpByePacket
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

private class RtcpByeData(
    var header: ModifiableRtcpHeader = ModifiableRtcpHeader(),
    var ssrcs: MutableList<Long> = mutableListOf(),
    var reason: String? = null
) {

    companion object : ConstructableFromBuffer<RtcpByeData> {
        override fun fromBuffer(buf: ByteBuffer): RtcpByeData {
            val header = ModifiableRtcpHeader.fromBuffer(buf)
            val ssrcs = org.jitsi.rtp.rtcp.RtcpByePacket.getSsrcs(buf, header.reportCount)
            val reason = if (org.jitsi.rtp.rtcp.RtcpByePacket.hasReason(buf)) {
                String(org.jitsi.rtp.rtcp.RtcpByePacket.getReason(buf).array(), StandardCharsets.US_ASCII)
            } else {
                null
            }
            return RtcpByeData(header, ssrcs, reason)
        }
    }

    val sizeBytes: Int
        get() {
            val dataSize = header.sizeBytes - 4 + ssrcs.size * 4
            val reasonSize = reason?.let {
                val fieldSize = it.toByteArray(StandardCharsets.US_ASCII).size + 1
                var paddingSize = 0
                while ((fieldSize + paddingSize) % 4 != 0) {
                    paddingSize++
                }
                fieldSize + paddingSize

            } ?: 0

            return dataSize + reasonSize
        }
}

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
    header: ReadOnlyRtcpHeader = ReadOnlyRtcpHeader(),
    ssrcs: List<Long>,
    reason: String? = null,
    buf: ByteBuffer? = null
) : ReadOnlyRtcpPacket(), CanBecomeModifiable<ModifiableRtcpByePacket> {
    override val dataBuf: ByteBuffer

    private val rtcpByeData = RtcpByeData(header.modifyInPlace(), ssrcs.toMutableList(), reason)

    override val sizeBytes: Int = rtcpByeData.sizeBytes

    override val header: ReadOnlyRtcpHeader = rtcpByeData.header.toReadOnly()
    val ssrcs: List<Long> = rtcpByeData.ssrcs
    val reason: String? = rtcpByeData.reason

    companion object {
        fun fromBuffer(buf: ByteBuffer): ReadOnlyRtcpByePacket {
            val rtcpByeData = RtcpByeData.fromBuffer(buf)
            return ReadOnlyRtcpByePacket(rtcpByeData.header.toReadOnly(), rtcpByeData.ssrcs, rtcpByeData.reason, buf)
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

    override val payload: ByteBuffer = dataBuf.subBuffer(header.sizeBytes)

    override fun modifyInPlace(): ModifiableRtcpByePacket =
            ModifiableRtcpByePacket(rtcpByeData.header, rtcpByeData.ssrcs, rtcpByeData.reason, dataBuf)

    //TODO(brian): some awkwardness here due to storing things as modifiable in RtcpByeData, but i think
    // it'd be worse if we held it as read only?
    override fun getModifiableCopy(): ModifiableRtcpByePacket {
        val reasonCopy = rtcpByeData.reason?.let { it + "" }
        return ModifiableRtcpByePacket(rtcpByeData.header.toReadOnly().getModifiableCopy(), rtcpByeData.ssrcs.toMutableList(),
                reasonCopy, dataBuf.clone())
    }


}

class ModifiableRtcpByePacket(
    header: ModifiableRtcpHeader,
    ssrcs: MutableList<Long>,
    reason: String?,
    private val dataBuf: ByteBuffer? = null
) : ModifiableRtcpPacket(), CanBecomeReadOnly<ReadOnlyRtcpByePacket> {
    private val rtcpByeData = RtcpByeData(header, ssrcs, reason)

    override var header: ModifiableRtcpHeader
        get() = rtcpByeData.header
        set(header) {
            rtcpByeData.header = header
        }
    var ssrcs: MutableList<Long>
        get() = rtcpByeData.ssrcs
        set(ssrcs) {
            rtcpByeData.ssrcs = ssrcs
        }

    var reason: String?
        get() = rtcpByeData.reason
        set(reason) {
            rtcpByeData.reason = reason
        }

//    override var payload: ByteBuffer by lazy {
//        if (dataBuf.limit() )
//    }

    companion object : ConstructableFromBuffer<ModifiableRtcpByePacket> {
        override fun fromBuffer(buf: ByteBuffer): ModifiableRtcpByePacket {
            val header = ModifiableRtcpHeader.fromBuffer(buf)
            val ssrcs = org.jitsi.rtp.rtcp.RtcpByePacket.getSsrcs(buf, header.reportCount)
            val reason = if (org.jitsi.rtp.rtcp.RtcpByePacket.hasReason(buf)) {
                String(org.jitsi.rtp.rtcp.RtcpByePacket.getReason(buf).array(), StandardCharsets.US_ASCII)
            } else {
                null
            }
            return ModifiableRtcpByePacket(header, ssrcs, reason)
        }
    }

    init {
    }

    override fun toReadOnly(): ReadOnlyRtcpByePacket = ReadOnlyRtcpByePacket(header.toReadOnly(), ssrcs, reason, dataBuf)
}
