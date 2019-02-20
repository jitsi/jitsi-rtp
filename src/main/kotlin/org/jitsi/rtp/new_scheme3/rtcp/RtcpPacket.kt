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

import org.jitsi.rtp.new_scheme3.ImmutableAlias
import org.jitsi.rtp.new_scheme3.Packet
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

abstract class RtcpPacket(
    protected val _header: RtcpHeader,
    private var backingBuffer: ByteBuffer?
) : Packet() {
    private var dirty: Boolean = true

    val header: ImmutableRtcpHeader by ImmutableAlias(::_header)

    fun modifyHeader(block: RtcpHeader.() -> Unit) {
        with (_header) {
            block()
            dirty = true
        }
    }

    protected fun payloadModified() {
        dirty = true
    }

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
    }

    companion object {
        fun parse(buf: ByteBuffer): RtcpPacket {
            val packetType = org.jitsi.rtp.rtcp.RtcpHeader.getPacketType(buf)
            return when (packetType) {
                org.jitsi.rtp.rtcp.RtcpByePacket.PT -> RtcpByePacket.create(buf)
                else -> throw Exception("Unsupported RTCP packet type $packetType")
            }
        }
    }
}