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

package org.jitsi.rtp.new_scheme3.srtp

import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.extensions.unsigned.subBuffer
import org.jitsi.rtp.extensions.unsigned.ulimit
import org.jitsi.rtp.new_scheme3.Packet
import org.jitsi.rtp.new_scheme3.rtp.RtpHeader
import org.jitsi.rtp.new_scheme3.rtp.RtpPacket
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

@ExperimentalUnsignedTypes
class SrtpPacket(
    header: RtpHeader = RtpHeader(),
    payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    backingBuffer: ByteBuffer? = null
) : RtpPacket(header, payload, backingBuffer) {

    fun getAuthTag(tagLen: Int): ByteBuffer =
        payload.subBuffer(payload.ulimit() - tagLen.toUInt())

    fun removeAuthTag(tagLen: Int) {
        modifyPayload {
           limit(limit() - tagLen)
        }
    }

    fun addAuthTag(authTag: ByteBuffer) {
        //TODO(brian)
        modifyPayload {

        }
    }

    override fun clone(): Packet {
        return SrtpPacket(_header.clone(), _payload.clone())
    }

    @ExperimentalUnsignedTypes
    companion object {
        fun create(buf: ByteBuffer): SrtpPacket {
            val header = RtpHeader.create(buf)
            val payload = buf.subBuffer(header.sizeBytes)
            return SrtpPacket(header, payload, buf)
        }
    }
}