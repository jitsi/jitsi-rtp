/*
 * Copyright @ 2018 Atlassian Pty Ltd
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
import org.jitsi.rtp.extensions.put
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.rtcp.RtcpHeader
import toUInt
import java.nio.ByteBuffer
import java.util.function.Predicate

abstract class Packet : Serializable {
    abstract val size: Int
    abstract fun clone(): Packet

    //deprecated
    val tags = mutableMapOf<String, Any>()
}

/**
 * Basically just a wrapper around a buffer that inherits from [Packet]
 * so that it can be passed to logic which will further handle it.
 * [buf] must be sized to matched the data within (i.e. [buf.limit()]
 * should return the length of the data in the buffer).
 */
class UnparsedPacket(private val buf: ByteBuffer) : Packet() {
    override val size: Int = buf.limit()

    override fun getBuffer(): ByteBuffer {
        buf.rewind()
        return buf
    }
    override fun clone(): Packet {
        return UnparsedPacket(buf.clone())
    }
}

/**
 * [SrtpProtocolPacket] is either an SRTP packet or SRTCP packet (but we don't know which)
 * so it basically just distinguishes a packet as encrypted and stores the buffer
 */
open class SrtpProtocolPacket(protected var buf: ByteBuffer) : Packet() {
    override val size: Int
        get() = buf.limit()

    override fun getBuffer(): ByteBuffer {
        buf.rewind()
        return buf
    }
    override fun clone(): Packet {
        return SrtpProtocolPacket(buf.clone())
    }
    fun getAuthTag(tagLength: Int): ByteBuffer {
        return buf.subBuffer(buf.limit() - tagLength, tagLength)
    }
    fun removeAuthTag(tagLength: Int) {
        buf.limit(buf.limit() - tagLength)
    }
    fun addAuthTag(authTag: ByteBuffer) {
        if (buf.capacity() - buf.limit() >= authTag.limit()) {
            buf.limit(buf.limit() + authTag.limit())
            buf.put(buf.limit() - authTag.limit(), authTag)
        } else {
            val newBuf = ByteBuffer.allocate(size + authTag.limit())
            buf.rewind()
            newBuf.put(buf)
            newBuf.put(authTag)
            newBuf.flip()
            buf = newBuf
        }
    }
}

/**
 * [SrtpPacket] is a known SRTP (as opposed to SRTCP) packet
 * https://tools.ietf.org/html/rfc3711#section-3.1
 */
class SrtpPacket(buf: ByteBuffer) : SrtpProtocolPacket(buf) {
    val header = RtpHeader.fromBuffer(buf)
    // The size of the payload may change depending on whether or not the auth tag has been
    //  removed, but we know it always occupies the space between the end of the header
    //  and the end of the buffer.
    val payload: ByteBuffer
        get() = buf.subBuffer(header.size, buf.limit() - header.size)
    override val size: Int
        get() = header.size + payload.limit()

//    fun getAuthTag(tagLength: Int): ByteBuffer {
//        return buf.subBuffer(buf.limit() - tagLength, tagLength)
//    }
//    fun removeAuthTag(tagLength: Int) {
//        buf.limit(buf.limit() - tagLength)
//    }
//    fun addAuthTag(authTag: ByteBuffer) {
//        if (buf.capacity() - buf.limit() >= authTag.limit()) {
//            buf.limit(buf.limit() + authTag.limit())
//            buf.put(buf.limit() - authTag.limit(), authTag)
//        } else {
//            val newBuf = ByteBuffer.allocate(size + authTag.limit())
//            newBuf.put(buf)
//            newBuf.put(authTag)
//            newBuf.flip()
//            buf = newBuf
//        }
//    }
    //TODO: override clone
}

/**
 * [SrtcpPacket] is a known SRTCP (as opposed to SRTP) packet
 * https://tools.ietf.org/html/rfc3711#section-3.4
 */
class SrtcpPacket(buf: ByteBuffer) : SrtpProtocolPacket(buf) {
    val header = RtcpHeader(buf)
    val payload: ByteBuffer
        get() = getBuffer().subBuffer(header.size)
    val ssrc: Int = header.senderSsrc.toUInt()
//    fun getAuthTag(tagLength: Int): ByteBuffer {
//        return buf.subBuffer(buf.limit() - tagLength, tagLength)
//    }
    fun getSrtcpIndex(tagLength: Int): Int {
        return buf.getInt(buf.limit() - (4 + tagLength)) and (0x80000000.inv()).toInt()
    }
    fun addSrtcpIndex(srtcpIndex: Int) {
        if (buf.capacity() - buf.limit() >= 4) {
            buf.limit(buf.limit() + 4)
            buf.putInt(buf.limit() - 4, srtcpIndex)
        } else {
            val newBuf = ByteBuffer.allocate(size + 4)
            newBuf.put(buf)
            newBuf.putInt(srtcpIndex)
            newBuf.flip()
            buf = newBuf
        }
    }
    fun isEncrypted(tagLength: Int): Boolean {
        return buf.getInt(buf.limit() - (4 + tagLength)) and 0x80000000.toInt() == 0x80000000.toInt()
    }
    //TODO: override clone
}

class DtlsProtocolPacket(private val buf: ByteBuffer) : Packet() {
    override val size: Int = buf.limit()

    override fun getBuffer(): ByteBuffer {
        buf.rewind()
        return buf
    }

    override fun clone(): Packet {
        return DtlsProtocolPacket(buf.clone())
    }
}

typealias PacketPredicate = Predicate<Packet>
