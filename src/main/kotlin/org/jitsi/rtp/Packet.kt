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

package org.jitsi.rtp

import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer
import java.util.function.Predicate

abstract class Serializable {
    abstract val sizeBytes: Int

    open fun getBuffer(): ByteBuffer {
        val b = ByteBuffer.allocate(sizeBytes)
        serializeTo(b)

        return b.rewind() as ByteBuffer
    }

    abstract fun serializeTo(buf: ByteBuffer)
}

abstract class Packet : Serializable(), kotlin.Cloneable {
    public abstract override fun clone(): Packet
}

open class UnparsedPacket(
    private val buf: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER
) : Packet() {

    override val sizeBytes: Int = buf.limit()

    override fun clone(): Packet = UnparsedPacket(buf.clone())

    //TODO: expose as readonly?
    override fun getBuffer(): ByteBuffer = buf

    override fun serializeTo(buf: ByteBuffer) {
        this.buf.rewind()
        buf.put(this.buf)
    }
}

typealias PacketPredicate = Predicate<Packet>

class DtlsProtocolPacket(
    buf: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER
) : UnparsedPacket(buf)
