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

package org.jitsi.rtp.new_scheme2.srtp

import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.new_scheme2.ConstructableFromBuffer
import org.jitsi.rtp.new_scheme2.rtcp.ImmutableRtcpHeader
import org.jitsi.rtp.new_scheme2.rtcp.ImmutableRtcpPacket
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

class ImmutableSrtcpPacket(
    override val header: ImmutableRtcpHeader = ImmutableRtcpHeader(),
    val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    backingBuffer: ByteBuffer? = null
) : ImmutableRtcpPacket() {

    override val sizeBytes: Int = header.sizeBytes + payload.limit()

    override val dataBuf: ByteBuffer by lazy {
        val b = ByteBufferUtils.ensureCapacity(backingBuffer, sizeBytes)
        b.rewind()
        b.limit(sizeBytes)
        header.serializeTo(b)
        b.put(payload)

        b.rewind() as ByteBuffer
    }

    fun getAuthTag(tagLength: Int): ByteBuffer =
        dataBuf.subBuffer(dataBuf.limit() - tagLength)

    fun getSrtcpIndex(tagLength: Int): Int =
        dataBuf.getInt(dataBuf.limit() - (4 + tagLength) and SRTCP_INDEX_MASK)

    fun isEncrypted(tagLength: Int): Boolean =
        (dataBuf.getInt(dataBuf.limit() - (4 + tagLength)) and IS_ENCRYPTED_MASK) ==
            IS_ENCRYPTED_MASK


    companion object : ConstructableFromBuffer<ImmutableSrtcpPacket> {
        private const val IS_ENCRYPTED_MASK = 0x80000000.toInt()
        private const val SRTCP_INDEX_MASK = IS_ENCRYPTED_MASK.inv().toInt()
        override fun fromBuffer(buf: ByteBuffer): ImmutableSrtcpPacket {
            val header = ImmutableRtcpHeader.fromBuffer(buf)
            val payload = buf.subBuffer(header.sizeBytes)

            return ImmutableSrtcpPacket(header, payload, buf.subBuffer(header.sizeBytes + payload.limit()))
        }
    }
}