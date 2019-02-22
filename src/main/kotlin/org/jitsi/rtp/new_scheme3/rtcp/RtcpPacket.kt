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

    val header: ImmutableRtcpHeader by ImmutableAlias(::_header)

    //TODO(brian): it'd be nice to not expose header data here.  maybe
    // RtcpHeader should add its own layer for each variabl
    fun modifyHeader(block: RtcpHeaderData.() -> Unit) {
        _header.modify(block)
        dirty = true
    }

    fun prepareForEncryption(): RtcpPacketForCrypto {
        return RtcpPacketForCrypto(_header, getBuffer().subBuffer(_header.sizeBytes), backingBuffer)
    }

    protected fun payloadModified() {
        dirty = true
    }

    @Suppress("UNCHECKED_CAST")
    fun <OtherType : RtcpPacket>toOtherRtcpPacketType(factory: (RtcpHeader, backingBuffer: ByteBuffer?) -> RtcpPacket): OtherType
        = factory(_header, backingBuffer) as OtherType

    final override fun getBuffer(): ByteBuffer {
        if (dirty) {
            val b = ByteBufferUtils.ensureCapacity(backingBuffer, sizeBytes)
            serializeTo(b)
            b.rewind()

            backingBuffer = b
            dirty = false
        }
        return backingBuffer!!
    }

    override fun serializeTo(buf: ByteBuffer) {
        _header.serializeTo(buf)
        buf.position(_header.sizeBytes)
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
    }
}