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

package org.jitsi.rtp.new_scheme3

import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.extensions.unsigned.allocateByteBuffer
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

interface Serializable {
    /**
     * Get the contents of this object serialized into a buffer.  The
     * returned buffer MUST:
     * 1) Have its current position set to 0
     * 2) Have its limit set to the size of the valid data contained within
     * the buffer
     */
    fun getBuffer(): ByteBuffer

    //TODO(brian): eventually this should be the required one and getBuffer can
    // have a default implementation which leverages this method
    fun serializeTo(buf: ByteBuffer)
}

abstract class SerializableData : Serializable {
    abstract val sizeBytes: Int

    override fun getBuffer(): ByteBuffer {
        val b = ByteBuffer.allocate(sizeBytes)
        serializeTo(b)

        return b.rewind() as ByteBuffer
    }
}

//TODO(brian): i don't think cloning RTCP makes much sense.  can we get away
// without it?
abstract class Packet : SerializableData(), kotlin.Cloneable {
    public abstract override fun clone(): Packet
}

open class UnparsedPacket(
    private val buf: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER
) : Packet() {

    override val sizeBytes: Int = buf.limit()

    override fun clone(): Packet = UnparsedPacket(buf.clone())

    override fun getBuffer(): ByteBuffer = buf

    override fun serializeTo(buf: ByteBuffer) {
        this.buf.rewind()
        buf.put(this.buf)
    }
}

class DtlsProtocolPacket(
    buf: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER
) : UnparsedPacket(buf)
