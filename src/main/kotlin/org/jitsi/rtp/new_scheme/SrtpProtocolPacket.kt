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

package org.jitsi.rtp.new_scheme

import org.jitsi.rtp.SrtpProtocolPacket
import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.extensions.put
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

/**
 * [SrtpProtocolPacket] is either an SRTP packet or SRTCP packet (but we don't know which)
 * so it basically just distinguishes a packet as encrypted and stores the buffer
 */
open class ReadOnlySrtpProtocolPacket(override val dataBuf: ByteBuffer) : ReadOnlyPacket() {
    companion object : ConstructableFromBuffer<ReadOnlySrtpProtocolPacket> {
        override fun fromBuffer(buf: ByteBuffer): ReadOnlySrtpProtocolPacket {
            return ReadOnlySrtpProtocolPacket(buf)
        }
    }
    fun <T : ReadOnlyPacket> convertTo(builder: ConstructableFromBuffer<T>): T {
        return builder.fromBuffer(dataBuf)
    }

    fun getAuthTag(tagLength: Int): ByteBuffer =
            ByteBufferUtils.wrapSubArray(dataBuf.array(), dataBuf.limit() - tagLength, tagLength)
}

class ReadOnlySrtpPacket(buf: ByteBuffer) : ReadOnlySrtpProtocolPacket(buf), CanBecomeModifiable<ModifiableSrtpPacket> {
    companion object : ConstructableFromBuffer<ReadOnlySrtpPacket> {
        override fun fromBuffer(buf: ByteBuffer): ReadOnlySrtpPacket {
            return ReadOnlySrtpPacket(buf)
        }
    }

    val header = ReadOnlyRtpHeader.fromBuffer(dataBuf)
    val payload: ByteBuffer = dataBuf.subBuffer(header.sizeBytes).asReadOnlyBuffer()

    init {

    }

    override fun modifyInPlace(): ModifiableSrtpPacket = ModifiableSrtpPacket(dataBuf)

    override fun getModifiableCopy(): ModifiableSrtpPacket = ModifiableSrtpPacket(dataBuf.clone())
}

class ReadOnlySrtcpPacket(buf: ByteBuffer) : ReadOnlySrtpProtocolPacket(buf), CanBecomeModifiable<ModifiableSrtcpPacket> {
    companion object : ConstructableFromBuffer<ReadOnlySrtpProtocolPacket> {
        override fun fromBuffer(buf: ByteBuffer): ReadOnlySrtpProtocolPacket {
            return ReadOnlySrtpProtocolPacket(buf)
        }
    }

    fun getSrtcpIndex(tagLength: Int): Int {
        return dataBuf.getInt(dataBuf.limit() - (4 + tagLength)) and (0x80000000.inv()).toInt()
    }
    fun isEncrypted(tagLength: Int): Boolean {
        return dataBuf.getInt(dataBuf.limit() - (4 + tagLength)) and 0x80000000.toInt() == 0x80000000.toInt()
    }

    override fun modifyInPlace(): ModifiableSrtcpPacket = ModifiableSrtcpPacket(dataBuf)

    override fun getModifiableCopy(): Modifiable = ModifiableSrtcpPacket(dataBuf.clone())
}

open class ModifiableSrtpProtocolPacket(var buf: ByteBuffer) : ModifiablePacket, CanBecomeReadOnly {
    val sizeBytes: Int
        get() = buf.limit()

    fun addAuthTag(authTag: ByteBuffer) {
        if (buf.capacity() - buf.limit() >= authTag.limit()) {
            buf.limit(buf.limit() + authTag.limit())
            buf.put(buf.limit() - authTag.limit(), authTag)
        } else {
            val newBuf = ByteBuffer.allocate(sizeBytes + authTag.limit())
            buf.rewind()
            newBuf.put(buf)
            newBuf.put(authTag)
            newBuf.flip()
            buf = newBuf
        }
    }

    fun removeAuthTag(tagLength: Int) {
        buf.limit(buf.limit() - tagLength)
    }

    override fun toReadOnly(): ReadOnly = ReadOnlySrtpProtocolPacket(buf)
}

class ModifiableSrtpPacket(buf: ByteBuffer) : ModifiableSrtpProtocolPacket(buf) {
    override fun toReadOnly(): ReadOnlySrtpPacket {
        return ReadOnlySrtpPacket(buf)
    }
}

class ModifiableSrtcpPacket(buf: ByteBuffer) : ModifiableSrtpProtocolPacket(buf) {
    fun setSrtcpIndex(srtcpIndex: Int) {
        if (buf.capacity() - buf.limit() >= 4) {
            buf.limit(buf.limit() + 4)
            buf.putInt(buf.limit() - 4, srtcpIndex)
        } else {
            val newBuf = ByteBuffer.allocate(sizeBytes + 4)
            newBuf.put(buf)
            newBuf.putInt(srtcpIndex)
            newBuf.flip()
            buf = newBuf
        }
    }

    override fun toReadOnly(): ReadOnly = ReadOnlySrtcpPacket(buf)
}

fun main() {
    val unparsed = ReadOnlyUnparsedPacket(ByteBuffer.allocate(100))
    val srtpProtocolPacket = unparsed.convertTo(ReadOnlySrtpProtocolPacket)
    srtpProtocolPacket.getAuthTag(10)
    var srtpPacket = srtpProtocolPacket.convertTo(ReadOnlySrtpPacket.Companion)
    println(srtpPacket.sizeBytes)

    srtpPacket = srtpPacket.modifyInPlace().run {
        removeAuthTag(10)
        toReadOnly()
    }

    println(srtpPacket.sizeBytes)
}