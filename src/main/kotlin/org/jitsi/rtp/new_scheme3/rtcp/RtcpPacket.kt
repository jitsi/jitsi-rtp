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

package org.jitsi.rtp.new_scheme3.rtcp

import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.extensions.unsigned.incrementPosition
import org.jitsi.rtp.new_scheme3.ImmutableAlias
import org.jitsi.rtp.new_scheme3.Packet
import org.jitsi.rtp.new_scheme3.rtcp.data.RtcpHeaderData
import org.jitsi.rtp.new_scheme3.rtcp.rtcpfb.RtcpFbPacket
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

class RtcpPacketForCrypto(
    header: RtcpHeader = RtcpHeader(),
    private val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    backingBuffer: ByteBuffer? = null
) : RtcpPacket(header, backingBuffer) {

    fun getPayload(): ByteBuffer {
        // We assume that if the payload is retrieved that it's being modified
        payloadModified()
        return payload.duplicate()
    }

    override val sizeBytes: Int
        get() = header.sizeBytes + payload.limit()

    override fun clone(): Packet {
        return RtcpPacketForCrypto(_header.clone(), payload.clone())
    }

    override fun serializeTo(buf: ByteBuffer) {
        super.serializeTo(buf)
        payload.rewind()
        buf.put(payload)
    }
}

abstract class RtcpPacket(
    protected val _header: RtcpHeader,
    private var backingBuffer: ByteBuffer?
) : Packet() {
    private var dirty: Boolean = true

    //TODO: we should handle adding padding and setting the hasPadding
    // bit (only) here in rtcppacket

    val header: ImmutableRtcpHeader by ImmutableAlias(::_header)

    /**
     * How many padding bytes are needed, if any
     * TODO: should sizeBytes be exposed publicly?  because it doesn't
     * include padding it could be misleading
     */
    private val numPaddingBytes: Int
        get() {
            //TODO: maybe we can only update this when dirty = true
            var paddingBytes = 0
            while ((sizeBytes + paddingBytes) % 4 != 0) {
                paddingBytes++
            }
            return paddingBytes
        }

    //TODO(brian): it'd be nice to not expose header data here.  maybe
    // RtcpHeader should add its own layer for each variabl
    fun modifyHeader(block: RtcpHeaderData.() -> Unit) {
        _header.modify(block)
        dirty = true
    }

    fun prepareForEncryption(): RtcpPacketForCrypto {
        return RtcpPacketForCrypto(_header, getBuffer().subBuffer(header.sizeBytes), backingBuffer)
    }

    /**
     * [sizeBytes] MUST including padding (i.e. it should be 32-bit word aligned)
     */
    private fun calculateLengthFieldValue(sizeBytes: Int): Int {
        if (sizeBytes % 4 != 0) {
            throw Exception("Invalid RTCP size value")
        }
        return (sizeBytes / 4) - 1
    }

    private fun updateHeaderFields() {
        _header.modify {
            //TODO: is this the right way to keep these in sync?  we need to do this for RTP as well.
            // other fields which are type-specific (like report count) will need to be updated
            // at lower layers
            hasPadding = numPaddingBytes > 0
            length = calculateLengthFieldValue(this@RtcpPacket.sizeBytes + numPaddingBytes)
        }
    }

    protected fun payloadModified() {
        //TODO: do we want to call updateHeaderFields here?
        dirty = true
    }

    @Suppress("UNCHECKED_CAST")
    fun <OtherType : RtcpPacket>toOtherRtcpPacketType(factory: (RtcpHeader, backingBuffer: ByteBuffer?) -> RtcpPacket): OtherType
        = factory(_header, backingBuffer) as OtherType

    final override fun getBuffer(): ByteBuffer {
        if (dirty) {
            updateHeaderFields()
            val b = ByteBufferUtils.ensureCapacity(backingBuffer, sizeBytes + numPaddingBytes)
            serializeTo(b)
            b.rewind()

            backingBuffer = b
            dirty = false
        }
        return backingBuffer!!
    }

    override fun serializeTo(buf: ByteBuffer) {
        header.serializeTo(buf)
    }

    companion object {
        fun parse(buf: ByteBuffer): RtcpPacket {
            val packetType = RtcpHeaderData.getPacketType(buf)
            return when (packetType) {
                RtcpSrPacket.PT -> RtcpSrPacket.fromBuffer(buf)
                RtcpRrPacket.PT -> RtcpRrPacket.fromBuffer(buf)
//                RtcpSdesPacket.PT -> RtcpSdesPacket(buf)
                RtcpByePacket.PT -> RtcpByePacket.create(buf)
                in RtcpFbPacket.PACKET_TYPES -> RtcpFbPacket.fromBuffer(buf)
                else -> throw Exception("Unsupported RTCP packet type $packetType")
            }
        }
        fun addPadding(buf: ByteBuffer) {
            while (buf.position() % 4 != 0) {
                buf.put(0x00)
            }
        }

        fun consumePadding(buf: ByteBuffer) {
            while (buf.position() % 4 != 0) {
                buf.put(0x00)
            }
        }
    }
}