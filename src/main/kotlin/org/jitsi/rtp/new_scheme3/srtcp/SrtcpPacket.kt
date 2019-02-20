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
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.new_scheme3.Packet
import org.jitsi.rtp.new_scheme3.rtcp.RtcpHeader
import org.jitsi.rtp.new_scheme3.rtcp.RtcpPacket
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

class SrtcpPacket(
    header: RtcpHeader = RtcpHeader(),
    private val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    backingBuffer: ByteBuffer? = null
) : RtcpPacket(header, backingBuffer) {

    override val sizeBytes: Int
        get() = header.sizeBytes + payload.limit()

    fun getAuthTag(tagLen: Int): ByteBuffer =
        payload.subBuffer(payload.limit() - tagLen)

    fun getSrtcpIndex(tagLen: Int): Int =
        payload.getInt(payload.limit() - (4 + tagLen) and SRTCP_INDEX_MASK)

    fun isEncrypted(tagLen: Int): Boolean =
        (payload.getInt(payload.limit() - (4 + tagLen)) and IS_ENCRYPTED_MASK) == IS_ENCRYPTED_MASK

    fun removeAuthTagAndSrtcpIndex(tagLen: Int) {
        payload.limit(payload.limit() - (4 + tagLen))
        payloadModified()
    }

    override fun clone(): Packet =
        SrtcpPacket(_header.clone(), payload.clone())

    override fun serializeTo(buf: ByteBuffer) {
        _header.serializeTo(buf)
        payload.rewind()
        buf.put(payload)
    }

    companion object {
        private const val IS_ENCRYPTED_MASK = 0x80000000.toInt()
        private const val SRTCP_INDEX_MASK = IS_ENCRYPTED_MASK.inv().toInt()
        fun create(buf: ByteBuffer): SrtcpPacket {
            val header = RtcpHeader.create(buf)
            val payload = buf.subBuffer(header.sizeBytes)

            return SrtcpPacket(header, payload, buf.subBuffer(0, header.sizeBytes + payload.limit()))
        }
    }
}