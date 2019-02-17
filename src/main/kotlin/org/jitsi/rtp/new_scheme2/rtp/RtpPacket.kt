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

package org.jitsi.rtp.new_scheme2.rtp

import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.new_scheme2.CanBecomeImmutable
import org.jitsi.rtp.new_scheme2.CanBecomeMutable
import org.jitsi.rtp.new_scheme2.ConstructableFromBuffer
import org.jitsi.rtp.new_scheme2.ImmutablePacket
import org.jitsi.rtp.new_scheme2.Mutable
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

open class ImmutableRtpPacket(
    val header: ImmutableRtpHeader = ImmutableRtpHeader(),
    val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    backingBuffer: ByteBuffer? = null
) : ImmutablePacket(), CanBecomeMutable<MutableRtpPacket> {

    override val sizeBytes: Int = header.sizeBytes + payload.limit()

    final override val dataBuf: ByteBuffer by lazy {
        val b = ByteBufferUtils.ensureCapacity(backingBuffer, sizeBytes)
        b.rewind()
        b.limit(sizeBytes)
        b.put(header.getBuffer())
        payload.rewind()
        b.put(payload)

        b.rewind()

        b
    }

    override fun getMutableCopy(): MutableRtpPacket =
        MutableRtpPacket(header.getMutableCopy(), payload.clone(), dataBuf.clone())

    override fun modifyInPlace(block: MutableRtpPacket.() -> Unit) {
        //TODO(brian): we don't have a good way to get a modifiable version
        // of an immutable class to use here...neither getting a copy nor
        // modifyingInPlace works.  i think we may need a third method
        // (one that modifyInPlace could probably leverage and therefore nott
        // need to be implemented by each class?)
        TODO()
    }

    companion object : ConstructableFromBuffer<ImmutableRtpPacket> {
        override fun fromBuffer(buf: ByteBuffer): ImmutableRtpPacket {
            val header = ImmutableRtpHeader.fromBuffer(buf)
            val payload = buf.subBuffer(header.sizeBytes)
            return ImmutableRtpPacket(header, payload, buf.subBuffer(0, header.sizeBytes + payload.limit()))
        }
    }
}

open class MutableRtpPacket(
    val header: MutableRtpHeader = MutableRtpHeader(),
    val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    backingBuffer: ByteBuffer? = null
) : Mutable, CanBecomeImmutable<ImmutableRtpPacket> {

    override fun toImmutable(): ImmutableRtpPacket =
        ImmutableRtpPacket(header.toImmutable(), payload)
}