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

import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.extensions.put
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.new_scheme2.CanBecomeMutable
import org.jitsi.rtp.new_scheme2.ConstructableFromBuffer
import org.jitsi.rtp.new_scheme2.Convertible
import org.jitsi.rtp.new_scheme2.InPlaceModifier
import org.jitsi.rtp.new_scheme2.Mutable
import org.jitsi.rtp.new_scheme2.rtp.ImmutableRtpHeader
import org.jitsi.rtp.new_scheme2.rtp.ImmutableRtpPacket
import org.jitsi.rtp.new_scheme2.rtp.MutableRtpHeader
import org.jitsi.rtp.new_scheme2.rtp.MutableRtpPacket
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
) : ImmutableRtpPacket(header, payload, backingBuffer), Convertible<ImmutableRtpPacket>  {

    fun getAuthTag(tagLength: Int): ByteBuffer =
            dataBuf.subBuffer(dataBuf.limit() - tagLength)

    override fun <NewType : ImmutableRtpPacket> convertTo(factory: ConstructableFromBuffer<NewType>): NewType {
        return factory.fromBuffer(dataBuf)
    }

    override fun toMutable(): MutableSrtpPacket {
        return MutableSrtpPacket(header.toMutable(), payload, null)
    }

    override fun modifyInPlace(block: MutableRtpPacket.() -> Unit) {
        with (MutableSrtpPacket(header.toMutable(), payload)) {
            block()
        }
        TODO()
    }

    fun <T : Mutable>modifyInPlace(immutable: CanBecomeMutable<T>, block: T.() -> Unit) {
        with (immutable.toMutable()) {
            block()
        }
    }

    private val modifier = SrtpPacketInPlaceModifier()
    override fun getInPlaceModifier(): SrtpPacketInPlaceModifier {
        return modifier
    }

    inner class SrtpPacketInPlaceModifier : InPlaceModifier {
        // This method can't be defined in an interface, because the receiver function
        // type is specific to the individual class
        fun modify(block: MutableSrtpPacket.() -> Unit) {
            with (MutableSrtpPacket(header.toMutable(), payload)) {
                block()
            }
        }
    }

    override fun getMutableCopy(): MutableSrtpPacket =
        MutableSrtpPacket(header.getMutableCopy(), payload.clone())

    companion object : ConstructableFromBuffer<ImmutableSrtpPacket> {
        override fun fromBuffer(buf: ByteBuffer): ImmutableSrtpPacket {
            val header = ImmutableRtpHeader.fromBuffer(buf)
            val payload = buf.subBuffer(header.sizeBytes)
            return ImmutableSrtpPacket(header, payload, buf.subBuffer(0, header.sizeBytes + payload.limit()))
        }
    }
}

class MutableSrtpPacket(
    header: MutableRtpHeader = MutableRtpHeader(),
    payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    backingBuffer: ByteBuffer? = null
) : MutableRtpPacket(header, payload, backingBuffer) {

    fun removeAuthTag(tagLength: Int) {
        payload.limit(payload.limit() - tagLength)
    }

    fun addAuthTag(authTag: ByteBuffer) {
        //TODO(brian): by definition we're always going to re-allocate a new buffer here to fit the auth tag
        // because payload's limit when adding the auth tag will always be at the end of the payload
        // itself, and therefore have no extra room.  We don't have a way of tracking the
        // the capacity of the ByteBuffer instance we've allocated for the payload (which won't
        // have an independent capacity value from all the other ByteBuffers created from the same
        // backing array).  We could check the entire backingBuffer's capacity and compare it to the
        // header.sizeBytes + payload.limit().  if that leaves room, then we should be able to grow
        // the payload buffer --> i don't think this will work in the compound RTCP case?
        if (backingBuffer != null) {
            val availableBytes = backingBuffer.capacity() - (header.sizeBytes + payload.limit())
            if (availableBytes >= authTag.limit()) {
                // We assume here that if the backing buffer has room, then we can afford to grow the
                // payload.
                payload.limit(payload.limit() + authTag.limit())
                payload.put(payload.limit() - authTag.limit(), authTag)
            }
        } else {
            val newPayload = ByteBuffer.allocate(payload.limit() + authTag.limit())
            payload.rewind()
            newPayload.put(payload)
            newPayload.put(authTag)
        }
    }
}

fun main() {
    val isrtp = ImmutableSrtpPacket()

    isrtp.getInPlaceModifier().modify {
        removeAuthTag(10)

    }
}
