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

package org.jitsi.rtp.new_scheme

import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer


open class ReadOnlyRtpPacket(
        val header: ReadOnlyRtpHeader = ReadOnlyRtpHeader(),
        val payload: ByteBuffer = ByteBuffer.allocate(0),
        buf: ByteBuffer? = null
) : ReadOnlyPacket(), CanBecomeModifiable<ModifiableRtpPacket> {
    //TODO(brian): will it work to make this final here?
    final override val dataBuf: ByteBuffer

    init {
        val sizeBytes = header.sizeBytes + payload.limit()
        val b = ByteBufferUtils.ensureCapacity(buf, sizeBytes)
        b.rewind()
        b.limit(sizeBytes)
        b.put(header.getBuffer())
        payload.rewind()
        b.put(payload)

        b.rewind()

        dataBuf = b
    }

    companion object {
        fun fromBuffer(buf: ByteBuffer): ReadOnlyRtpPacket {
            val header = ReadOnlyRtpHeader.fromBuffer(buf)
            val payload = buf.subBuffer(header.sizeBytes)
            return ReadOnlyRtpPacket(header, payload, buf)
        }
    }

    override fun modifyInPlace(): ModifiableRtpPacket = ModifiableRtpPacket(header.modifyInPlace(), payload, dataBuf)

    //NOTE(brian): instead of creating the modifiable copy by creating modifiable copies
    // of the subfields, we just clone the entire buffer and create it from that.  The reason
    // for this is that if we create modifiable copies of the individual fields, we'll create
    // multiple ByteBuffers to back them, but won't give the ModifiableRtpPacket a single buffer
    // it can use to eventually serialize.  If we clone the entire buffer and have it re-parse
    // the fields from that, it'll have a backing buffer it will (possibly) be able to use
    // down the line when it is eventually serialized
    override fun getModifiableCopy(): ModifiableRtpPacket = ModifiableRtpPacket.fromBuffer(dataBuf.clone())

//    override fun getBuffer(): ByteBuffer = dataBuf.asReadOnlyBuffer()
}

class ModifiableRtpPacket(
        var rtpHeader: ModifiableRtpHeader = ModifiableRtpHeader(),
        var payload: ByteBuffer = ByteBuffer.allocate(0),
        private val dataBuf: ByteBuffer? = null
) : ModifiablePacket {
    companion object {
        fun fromBuffer(buf: ByteBuffer): ModifiableRtpPacket {
            val header = ModifiableRtpHeader.fromBuffer(buf)
            val payload = buf.subBuffer(header.sizeBytes)

            return ModifiableRtpPacket(header, payload, buf)
        }
    }
    fun finalize(): ReadOnlyRtpPacket {
        //TODO: need to make sure all the header fields, etc. are set correctly
        // when doing this
        return ReadOnlyRtpPacket(rtpHeader.toReadOnly(), payload, dataBuf)
    }
}

