/*
 * Copyright @ 2018 - present 8x8, Inc.
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

package org.jitsi.rtp.new_scheme2.rtcp

import org.jitsi.rtp.Serializable
import org.jitsi.rtp.new_scheme2.ConstructableFromBuffer
import org.jitsi.rtp.rtcp.RtcpByePacket
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors.toList

private class RtcpByeData(
    //NOTE(brian): the ssrcs passed here do include the one held in the RTCP header, but we don't
    // include it when calculating the size of this data portion
    var ssrcs: MutableList<Long> = mutableListOf(),
    var reason: String? = null
) : Serializable {
    val sizeBytes: Int
        get() {
            val ssrcSize = (ssrcs.size - 1) * 4
            val reasonSize: Int = reason?.let {
                val fieldSize = 1 + it.toByteArray(StandardCharsets.US_ASCII).size
                var paddingSize = 0
                while (fieldSize + paddingSize % 4 != 0) {
                    paddingSize++
                }
                fieldSize + paddingSize
            } ?: 0

            return ssrcSize + reasonSize
        }

    override fun getBuffer(): ByteBuffer {
        val buf = ByteBuffer.allocate(sizeBytes)
        serializeTo(buf)
        return buf.rewind() as ByteBuffer
    }

    //TODO(brian): using an incremental scheme of writing here instead of
    // writing things at their absolute position, since some behavior changed
    // (this RtcpByeData doesn't include the ssrc held in the RTCP header
    // portion)--so the absolute methods don't work anyway, so trying out
    // writing things in order using relative positions.  this means
    // buf's position should be after the RTCP header
    override fun serializeTo(buf: ByteBuffer) {
        // Drop the first element when serializing, as it has already been written via
        // the header
        ssrcs.drop(1).stream()
                .map(Long::toInt)
                .forEach { buf.putInt(it) }
        reason?.let {
            val reasonBuf = ByteBuffer.wrap(it.toByteArray(StandardCharsets.US_ASCII))
            buf.put(reasonBuf.limit().toByte())
            buf.put(reasonBuf)
            while (buf.position() % 4 != 0) {
                buf.put(0x00)
            }
        }
    }

    companion object {
        //NOTE: buffer's position 0 is after the RtcpHeader
        /**
         * [buf] is a ByteBuffer who's current position is after the RTCP header in an RTCP BYE packet
         * [firstSsrc] is the SSRC that is stored in the RTCP header. [remainingSsrcCount] is the amount
         * of remaining SSRCs to be parsed from the payload.
         * [hasReason] denotes whether or not the RTCP BYE packet payload contains a reason (which requires
         * examining the RTCP header to determine, which this class doesn't deal with)
         */
        fun fromBuffer(buf: ByteBuffer, firstSsrc: Long, remainingSsrcCount: Int, hasReason: Boolean): RtcpByeData {
            val ssrcs = mutableListOf(firstSsrc)
            val remainingSsrcs = (0 until remainingSsrcCount)
                .map { buf.int }
                .map { it.toLong() }
                .toCollection(ssrcs)

            val reason = if (hasReason) {
                val reasonLength = buf.get().toInt()
                String(buf.array(), buf.position(), reasonLength)
            } else {
                null
            }
            return RtcpByeData(ssrcs, reason)
        }
    }
}

class ImmutableRtcpByePacket(
    override val header: ImmutableRtcpHeader = ImmutableRtcpHeader(),
    ssrcs: List<Long>,
    reason: String? = null,
    backingBuffer: ByteBuffer? = null
) : ImmutableRtcpPacket() {
    //TODO(brian): it's unfortunate we have to convert the ssrcs to a mutable
    // list here, since we have to copy but may not actually need it.  maybe can
    // come up with something there
    private val rtcpByeData = RtcpByeData(ssrcs.toMutableList(), reason)

    val sizeBytes = header.sizeBytes + rtcpByeData.sizeBytes

    override val dataBuf: ByteBuffer by lazy {
        val b = ByteBufferUtils.ensureCapacity(backingBuffer, sizeBytes)
        header.serializeTo(b)
        rtcpByeData.serializeTo(b)
        b.rewind() as ByteBuffer
    }

    companion object : ConstructableFromBuffer<ImmutableRtcpByePacket> {
        override fun fromBuffer(buf: ByteBuffer): ImmutableRtcpByePacket {
            buf.mark()
            val header = ImmutableRtcpHeader.fromBuffer(buf)
            buf.position(buf.position() + header.sizeBytes)
            val hasReason = run {
                val packetLength = header.length
                val headerAndSsrcsLength = header.sizeBytes + (header.reportCount - 1) * 4
                headerAndSsrcsLength < packetLength
            }
            val data = RtcpByeData.fromBuffer(
                buf, header.senderSsrc, header.reportCount - 1, hasReason)
            buf.reset()

            return ImmutableRtcpByePacket(header, data.ssrcs, data.reason, buf)
        }
    }
}