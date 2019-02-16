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
import org.jitsi.rtp.new_scheme2.Convertible
import org.jitsi.rtp.new_scheme2.rtp.ImmutableRtpHeader
import org.jitsi.rtp.new_scheme2.rtp.ImmutableRtpPacket
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

/**
 * An SRTP packet
 * TODO(brian): i wish we could know the auth tag length
 * when creating this, but we need some form of SRTP packet
 * (distinct from SRTCP) before we know it.  we could do something
 * like 'ImmutableSrtpPacketWithUnknownTag' (or something less
 * ridiculous) and only transition to a type which parsed the
 * tag once we know its length?
 */
class ImmutableSrtpPacket(
    header: ImmutableRtpHeader = ImmutableRtpHeader(),
    payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    backingBuffer: ByteBuffer? = null
) : ImmutableRtpPacket(header, payload, backingBuffer), Convertible<ImmutableRtpPacket> {

    fun getAuthTag(tagLength: Int): ByteBuffer =
            dataBuf.subBuffer(dataBuf.limit() - tagLength)

    override fun <NewType : ImmutableRtpPacket> convertTo(factory: ConstructableFromBuffer<NewType>): NewType {
        return factory.fromBuffer(dataBuf)
    }

    companion object : ConstructableFromBuffer<ImmutableSrtpPacket> {
        override fun fromBuffer(buf: ByteBuffer): ImmutableSrtpPacket {
            val header = ImmutableRtpHeader.fromBuffer(buf)
            val payload = buf.subBuffer(header.sizeBytes)
            return ImmutableSrtpPacket(header, payload, buf.subBuffer(0, header.sizeBytes + payload.limit()))
        }
    }
}

