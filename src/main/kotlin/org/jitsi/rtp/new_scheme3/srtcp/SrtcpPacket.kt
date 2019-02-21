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

package org.jitsi.rtp.new_scheme3.srtcp

import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.extensions.unsigned.position
import org.jitsi.rtp.extensions.unsigned.subBuffer
import org.jitsi.rtp.extensions.unsigned.ulimit
import org.jitsi.rtp.new_scheme3.Packet
import org.jitsi.rtp.new_scheme3.rtcp.RtcpHeader
import org.jitsi.rtp.new_scheme3.rtcp.RtcpPacket
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

@ExperimentalUnsignedTypes
class SrtcpPacketForDecryption(
    header: RtcpHeader = RtcpHeader(),
    private val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    backingBuffer: ByteBuffer? = null
) : RtcpPacket(header, backingBuffer) {

    fun getPayload(): ByteBuffer {
        // We assume that if the payload is retrieved that it's being modified
        payloadModified()
        return payload
    }

    override val sizeBytes: UInt
        get() = header.sizeBytes + payload.ulimit()

    override fun clone(): Packet {
        return SrtcpPacketForDecryption(_header.clone(), payload.clone())
    }

    override fun serializeTo(buf: ByteBuffer) {
        super.serializeTo(buf)
        payload.rewind()
        buf.put(payload)
    }
}

@ExperimentalUnsignedTypes
class SrtcpPacket(
    header: RtcpHeader = RtcpHeader(),
    private var srtcpPayload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    backingBuffer: ByteBuffer? = null
) : RtcpPacket(header, backingBuffer) {

    override val sizeBytes: UInt
        get() = header.sizeBytes + srtcpPayload.ulimit()

    fun prepareForDecryption(): SrtcpPacketForDecryption {
        //TODO: do we need to expose the backing buffer in RtcpPacket
        // so we can pass it here?
        return SrtcpPacketForDecryption(_header, srtcpPayload)
    }

    fun getAuthTag(tagLen: Int): ByteBuffer =
        srtcpPayload.subBuffer(srtcpPayload.ulimit() - tagLen.toUInt())

    fun getSrtcpIndex(tagLen: Int): Int =
        srtcpPayload.getInt(srtcpPayload.limit() - (4 + tagLen)) and SRTCP_INDEX_MASK

    fun isEncrypted(tagLen: Int): Boolean =
        (srtcpPayload.getInt(srtcpPayload.limit() - (4 + tagLen)) and IS_ENCRYPTED_MASK) == IS_ENCRYPTED_MASK

    //TODO: could we work all the auth tag/index operations into modifyPayload?
    //TODO: do we need to re-assign srtcpPayload after each of these operations?
    fun removeAuthTagAndSrtcpIndex(tagLen: Int) {
        srtcpPayload.limit(srtcpPayload.limit() - (4 + tagLen))
        payloadModified()
    }

    fun addAuthTag(authTag: ByteBuffer) {
        TODO()
        payloadModified()
    }

    fun addSrtcpIndex(srtcpIndex: Int, tagLen: Int) {
        TODO()
        payloadModified()
    }

    override fun clone(): Packet =
        SrtcpPacket(_header.clone(), srtcpPayload.clone())

    override fun serializeTo(buf: ByteBuffer) {
        _header.serializeTo(buf)
        srtcpPayload.rewind()
        //TODO(brian): _header.serialize uses absolute positioning, so we
        // need to manually set the buffer's position here.  in the future
        // i think we'll change everything to relative positioning when
        // serializing
        buf.position(_header.sizeBytes)
        buf.put(srtcpPayload.duplicate())
    }

    companion object {
        private const val IS_ENCRYPTED_MASK = 0x80000000.toInt()
        private const val SRTCP_INDEX_MASK = IS_ENCRYPTED_MASK.inv()
        fun create(buf: ByteBuffer): SrtcpPacket {
            val header = RtcpHeader.create(buf)
            val payload = buf.subBuffer(header.sizeBytes)

            return SrtcpPacket(header, payload, buf)
        }
    }
}