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

import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.new_scheme2.ConstructableFromBuffer
import org.jitsi.rtp.new_scheme2.ImmutableSerializableData
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

open class ImmutableRtpData(
    val header: ImmutableRtpHeader = ImmutableRtpHeader(),
    val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    backingBuffer: ByteBuffer? = null
) : ImmutableSerializableData() {

    val sizeBytes: Int = header.sizeBytes + payload.limit()

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

    companion object : ConstructableFromBuffer<ImmutableRtpData> {
        override fun fromBuffer(buf: ByteBuffer): ImmutableRtpData {
            val header = ImmutableRtpHeader.fromBuffer(buf)
            val payload = buf.subBuffer(header.sizeBytes)
            return ImmutableRtpData(header, payload, buf)
        }
    }
}