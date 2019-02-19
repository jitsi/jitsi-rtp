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
import org.jitsi.rtp.new_scheme2.ImmutablePacket
import org.jitsi.rtp.new_scheme2.rtp.ImmutableRtpHeader
import org.jitsi.rtp.new_scheme2.rtp.ImmutableRtpPacket
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer


/**
 * [UnparsedReadOnlySrtpPacket] is either an SRTP packet or SRTCP packet (but we don't know which)
 * so it basically just distinguishes a packet as encrypted and stores the buffer
 * TODO(brian): technically this should only be converted into either ImmutableSrtpPacketWithAuthTag or
 * ImmutableSrtcpPacket, but the first ancestor those 2 types have in common is ImmutablePacket,
 * so we use that here in Convertible.  It'd be nice if we could have a lower ancestor, but it's
 * tricky currently.
 */
class ImmutableUnparsedSrtpProtocolPacket(
    override val dataBuf: ByteBuffer
) : ImmutablePacket(), Convertible<ImmutablePacket> {
    override val sizeBytes: Int = dataBuf.limit()

    override fun <NewType : ImmutablePacket> convertTo(factory: ConstructableFromBuffer<NewType>): NewType {
        return factory.fromBuffer(dataBuf)
    }

    companion object : ConstructableFromBuffer<ImmutableUnparsedSrtpProtocolPacket> {
        override fun fromBuffer(buf: ByteBuffer): ImmutableUnparsedSrtpProtocolPacket =
                ImmutableUnparsedSrtpProtocolPacket(buf.duplicate())

    }
}

/**
 * An SRTP packet with an unknown auth tag length
 */
class ImmutableSrtpPacketUnknownAuthTag(
    header: ImmutableRtpHeader = ImmutableRtpHeader(),
    payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    backingBuffer: ByteBuffer? = null
) : ImmutableRtpPacket(header, payload, backingBuffer) {

    fun setAuthTagLength(authTagLen: Int): ImmutableSrtpPacketWithAuthTag =
        ImmutableSrtpPacketWithAuthTag(
            header,
            payload.duplicate() as ByteBuffer,
            authTagLen)

    companion object : ConstructableFromBuffer<ImmutableSrtpPacketUnknownAuthTag> {
        override fun fromBuffer(buf: ByteBuffer): ImmutableSrtpPacketUnknownAuthTag {
            val header = ImmutableRtpHeader.fromBuffer(buf)
            val payload = buf.subBuffer(header.sizeBytes)
            return ImmutableSrtpPacketUnknownAuthTag(header, payload, buf.subBuffer(0, header.sizeBytes + payload.limit()))
        }
    }
}

