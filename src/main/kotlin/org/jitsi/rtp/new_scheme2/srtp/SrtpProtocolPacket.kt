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

import org.jitsi.rtp.new_scheme2.ConstructableFromBuffer
import org.jitsi.rtp.new_scheme2.ImmutablePacket
import java.nio.ByteBuffer


/**
 * [UnparsedReadOnlySrtpPacket] is either an SRTP packet or SRTCP packet (but we don't know which)
 * so it basically just distinguishes a packet as encrypted and stores the buffer
 */
open class ImmutableUnparsedSrtpProtocolPacket(
    final override val dataBuf: ByteBuffer
) : ImmutablePacket() {
    override val sizeBytes: Int = dataBuf.limit()

    companion object : ConstructableFromBuffer<ImmutableUnparsedSrtpProtocolPacket> {
        override fun fromBuffer(buf: ByteBuffer): ImmutableUnparsedSrtpProtocolPacket =
                ImmutableUnparsedSrtpProtocolPacket(buf)

    }
}

