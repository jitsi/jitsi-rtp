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

import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.new_scheme.rtcp.ModifiableRtcpHeader
import org.jitsi.rtp.new_scheme.rtcp.ReadOnlyRtcpHeader
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

/**
 * [UnparsedReadOnlySrtpPacket] is either an SRTP packet or SRTCP packet (but we don't know which)
 * so it basically just distinguishes a packet as encrypted and stores the buffer
 */
open class ReadOnlyUnparsedSrtpProtocolPacket(override val dataBuf: ByteBuffer) : ReadOnlyPacket() {
    companion object : ConstructableFromBuffer<ReadOnlyUnparsedSrtpProtocolPacket> {
        override fun fromBuffer(buf: ByteBuffer): ReadOnlyUnparsedSrtpProtocolPacket {
            return ReadOnlyUnparsedSrtpProtocolPacket(buf)
        }
    }
}

/**
 * [ReadOnlySrtpPacket] can't be parsed until we know the tag length and sometimes we want to access
 * data in the header of an SRTP packet before we know the tag length, so this type allows just parsing
 * the header and payload without worrying about the tag length
 */
class ReadOnlyUnparsedSrtpPacket(
    val header: ReadOnlyRtpHeader = ReadOnlyRtpHeader(),
    val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    buf: ByteBuffer? = null
) : ReadOnlyPacket(), Convertable<ReadOnlyPacket> {
    override val dataBuf: ByteBuffer

    override val sizeBytes: Int = header.sizeBytes + payload.limit()

    companion object : ConstructableFromBuffer<ReadOnlyUnparsedSrtpPacket> {
        override fun fromBuffer(buf: ByteBuffer): ReadOnlyUnparsedSrtpPacket {
            val header = ReadOnlyRtpHeader.fromBuffer(buf)
            val payload = buf.subBuffer(header.sizeBytes)
            return ReadOnlyUnparsedSrtpPacket(header, payload)
        }
    }

    init {
        val b = ByteBufferUtils.ensureCapacity(buf, sizeBytes)
        b.rewind()
        b.limit(sizeBytes)
        b.put(header.getBuffer())
        b.put(payload)
        payload.rewind()
        b.rewind()

        dataBuf = b
    }
}

class ReadOnlyAuthenticatedSrtpPacket(
    val header: ReadOnlyRtpHeader = ReadOnlyRtpHeader(),
    val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    val authTag: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    buf: ByteBuffer? = null
) : ReadOnlyPacket() {
    companion object {
        fun fromBuffer(buf: ByteBuffer, tagLength: Int): ReadOnlyAuthenticatedSrtpPacket {
            val header = ReadOnlyRtpHeader.fromBuffer(buf)
            val payload = buf.subBuffer(header.sizeBytes, buf.limit() - header.sizeBytes - tagLength)
            val authTag = buf.subBuffer(header.sizeBytes + payload.limit(), tagLength)
            return ReadOnlyAuthenticatedSrtpPacket(header, payload, authTag, buf)
        }
    }
    override val sizeBytes: Int = header.sizeBytes + payload.limit() + authTag.limit()

    override val dataBuf: ByteBuffer

    init {
        val b = ByteBufferUtils.ensureCapacity(buf, sizeBytes)
        b.rewind()
        b.limit(sizeBytes)
        b.put(header.getBuffer())
        b.put(payload)
        payload.rewind()
        b.put(authTag)
        authTag.rewind()

        b.rewind()
        dataBuf = b
    }

    fun removeAuthTag(): ReadOnlyUnauthenticatedSrtpPacket =
            ReadOnlyUnauthenticatedSrtpPacket(header, payload, dataBuf)

//    override fun modifyInPlace(): ModifiableAuthenticatedSrtpPacket =
//            ModifiableAuthenticatedSrtpPacket.fromBuffer(dataBuf, authTag.limit())
//
//    override fun getModifiableCopy(): ModifiableAuthenticatedSrtpPacket =
//            ModifiableAuthenticatedSrtpPacket.fromBuffer(dataBuf.clone(), authTag.limit())
}

class ReadOnlyUnauthenticatedSrtpPacket(
    val header: ReadOnlyRtpHeader = ReadOnlyRtpHeader(),
    val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    buf: ByteBuffer? = null
) : ReadOnlyPacket() {
    override val sizeBytes: Int = header.sizeBytes + payload.limit()

    override val dataBuf: ByteBuffer

    init {
        val b = ByteBufferUtils.ensureCapacity(buf, sizeBytes)
        b.rewind()
        b.limit(sizeBytes)
        b.put(header.getBuffer())
        b.put(payload)
        payload.rewind()

        b.rewind()
        dataBuf = b
    }

    fun addAuthTag(authTag: ByteBuffer): ReadOnlyAuthenticatedSrtpPacket =
            ReadOnlyAuthenticatedSrtpPacket(header, payload, authTag, dataBuf)
}

//TODO(brian): do we need these modifable types? or would we just transition from one
// read-only type to the next for srtp stuff?
//class ModifiableAuthenticatedSrtpPacket(
//    var header: ModifiableRtpHeader = ModifiableRtpHeader(),
//    var payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
//    val authTag: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
//    private val dataBuf: ByteBuffer? = null
//) : ModifiablePacket, CanBecomeReadOnly<ReadOnlyAuthenticatedSrtpPacket> {
//    companion object {
//        fun fromBuffer(buf: ByteBuffer, tagLength: Int): ModifiableAuthenticatedSrtpPacket {
//            val header = ModifiableRtpHeader.fromBuffer(buf)
//            val payload = buf.subBuffer(header.sizeBytes, buf.limit() - header.sizeBytes - tagLength)
//            val authTag = buf.subBuffer(header.sizeBytes + payload.limit(), tagLength)
//
//            return ModifiableAuthenticatedSrtpPacket(header, payload, authTag, buf)
//        }
//    }
//    override fun toReadOnly(): ReadOnlyAuthenticatedSrtpPacket = ReadOnlyAuthenticatedSrtpPacket(header.toReadOnly(), payload, authTag)
//
//    fun removeAuthTag(): ModifiableUnauthenticatedSrtpPacket = ModifiableUnauthenticatedSrtpPacket(header, payload, dataBuf)
//}

//class ModifiableUnauthenticatedSrtpPacket(
//    var header: ModifiableRtpHeader = ModifiableRtpHeader(),
//    var payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
//    private val dataBuf: ByteBuffer? = null
//) : ModifiablePacket {
//
//    fun addAuthTag(authTag: ByteBuffer): ModifiableAuthenticatedSrtpPacket =
//            ModifiableAuthenticatedSrtpPacket(header, payload, authTag, dataBuf)
//}

class ReadOnlyUnparsedSrtcpPacket(
    val header: ReadOnlyRtcpHeader = ReadOnlyRtcpHeader(),
    val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    buf: ByteBuffer? = null
) : ReadOnlyPacket(), Convertable<ReadOnlyPacket> {

    override val dataBuf: ByteBuffer

    companion object : ConstructableFromBuffer<ReadOnlyUnparsedSrtcpPacket> {
        override fun fromBuffer(buf: ByteBuffer): ReadOnlyUnparsedSrtcpPacket {
            val header = ReadOnlyRtcpHeader.fromBuffer(buf)
            val payload = buf.subBuffer(header.sizeBytes)

            return ReadOnlyUnparsedSrtcpPacket(header, payload, buf)
        }
    }

    init {
        val b = ByteBufferUtils.ensureCapacity(buf, sizeBytes)
        b.rewind()
        b.limit(sizeBytes)
        b.put(header.getBuffer())
        b.put(payload)
        payload.rewind()
        b.rewind()

        dataBuf = b
    }
}

class ReadOnlyAuthenticatedSrtcpPacket(
    val header: ReadOnlyRtcpHeader = ReadOnlyRtcpHeader(),
    val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    val isEncrypted: Boolean = true,
    val srtcpIndex: Int = -1,
    val authTag: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    buf: ByteBuffer? = null
) : ReadOnlyPacket() {
    companion object {
        fun fromBuffer(buf: ByteBuffer, tagLength: Int): ReadOnlyAuthenticatedSrtcpPacket {
            val header= ReadOnlyRtcpHeader.fromBuffer(buf)
            val payload = buf.subBuffer(header.sizeBytes, buf.limit() - header.sizeBytes - tagLength - 4)
            val isEncryptedSrtcpIndexField = buf.subBuffer(header.sizeBytes + payload.limit(), 4)
            val isEncrypted = isEncryptedSrtcpIndexField.getInt(0) and (0x80000000).toInt() == 0x80000000.toInt()
            val srtcpIndex = buf.subBuffer(header.sizeBytes + payload.limit(), 4).int and (0x80000000.inv()).toInt()
            val authTag = buf.subBuffer(header.sizeBytes + payload.limit() + 4, tagLength)
            return ReadOnlyAuthenticatedSrtcpPacket(header, payload, isEncrypted, srtcpIndex, authTag)
        }
    }

    override val dataBuf: ByteBuffer

    init {
        val srtcpIndexSize = 4
        val authTagSize = authTag.limit()
        val sizeBytes = header.sizeBytes + payload.limit() + srtcpIndexSize + authTagSize

        val b = ByteBufferUtils.ensureCapacity(buf, sizeBytes)
        b.rewind()
        b.limit(sizeBytes)
        b.put(header.getBuffer())
        b.put(payload)
        payload.rewind()

        val isEncryptedSrtcpIndexField = ByteBuffer.allocate(4)
        if (isEncrypted) {
            isEncryptedSrtcpIndexField.putInt(0, 0x80000000.toInt())
        }
        isEncryptedSrtcpIndexField.putInt(0, srtcpIndex and (0x7FFFFFFF))

        b.rewind()
        dataBuf = b
    }

    fun removeAuthTagAndSrtcpIndex(): ReadOnlyUnauthenticatedSrtcpPacket =
            ReadOnlyUnauthenticatedSrtcpPacket(header, payload, dataBuf)

//    override fun modifyInPlace(): ModifiableSrtcpPacket =
//            ModifiableSrtcpPacket.fromBuffer(dataBuf, authTag.limit())
//
//    override fun getModifiableCopy(): ModifiableSrtcpPacket =
//            ModifiableSrtcpPacket.fromBuffer(dataBuf.clone(), authTag.limit())
}

class ReadOnlyUnauthenticatedSrtcpPacket(
    val header: ReadOnlyRtcpHeader = ReadOnlyRtcpHeader(),
    val payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
    buf: ByteBuffer? = null
) : ReadOnlyPacket() {
    override val sizeBytes: Int = header.sizeBytes + 4

    override val dataBuf: ByteBuffer

    init {
        val b = ByteBufferUtils.ensureCapacity(buf, sizeBytes)
        b.rewind()
        b.limit(sizeBytes)
        b.put(header.getBuffer())
        b.put(payload)
        payload.rewind()

        b.rewind()
        dataBuf = b
    }

    fun addAuthTagAndSrtcpIndex(authTag: ByteBuffer, srtcpIndex: Int): ReadOnlyAuthenticatedSrtcpPacket =
            ReadOnlyAuthenticatedSrtcpPacket(header, payload, true, srtcpIndex, authTag)

}

/*TODO(brian): i wonder if we even need this? --> it gives us the common auth tag code
abstract class ModifiableSrtpProtocolPacket(
    var authTag: ByteBuffer? = null
) : ModifiablePacket, CanBecomeReadOnly<ReadOnlySrtpProtocolPacket> {
//    abstract var dataBuf: ByteBuffer?

    fun addAuthTag(authTag: ByteBuffer) {
        this.authTag = authTag
//        if (buf.capacity() - buf.limit() >= authTag.limit()) {
//            buf.limit(buf.limit() + authTag.limit())
//            buf.put(buf.limit() - authTag.limit(), authTag)
//        } else {
//            val newBuf = ByteBuffer.allocate(sizeBytes + authTag.limit())
//            buf.rewind()
//            newBuf.put(buf)
//            newBuf.put(authTag)
//            newBuf.flip()
//            buf = newBuf
//        }
    }

    fun removeAuthTag() {
        authTag = null
//        buf.limit(buf.limit() - tagLength)
    }

//    override fun toReadOnly(): ReadOnlySrtpProtocolPacket = ReadOnlySrtpProtocolPacket.fromBuffer(buf)
}*/


//class ModifiableSrtcpPacket(
//    var header: ModifiableRtcpHeader = ModifiableRtcpHeader(),
//    var payload: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER,
//    var isEncrypted: Boolean = true,
//    var srtcpIndex: Int? = null,
//    var authTag: ByteBuffer? = null,
//    buf: ByteBuffer? = null
//) : ModifiablePacket, CanBecomeReadOnly<ReadOnlyAuthenticatedSrtcpPacket> {
//    companion object {
//        fun fromBuffer(buf: ByteBuffer, tagLength: Int): ModifiableSrtcpPacket {
//            val header= ModifiableRtcpHeader.fromBuffer(buf)
//            val payload = buf.subBuffer(header.sizeBytes, buf.limit() - header.sizeBytes - tagLength - 4)
//            val isEncryptedSrtcpIndexField = buf.subBuffer(header.sizeBytes + payload.limit(), 4)
//            val isEncrypted = isEncryptedSrtcpIndexField.getInt(0) and (0x80000000).toInt() == 0x80000000.toInt()
//            val srtcpIndex = buf.subBuffer(header.sizeBytes + payload.limit(), 4).int and (0x80000000.inv()).toInt()
//            val authTag = buf.subBuffer(header.sizeBytes + payload.limit() + 4, tagLength)
//            return ModifiableSrtcpPacket(header, payload, isEncrypted, srtcpIndex, authTag, buf)
//        }
//    }
//
//    override fun toReadOnly(): ReadOnlyAuthenticatedSrtcpPacket =
//            ReadOnlyAuthenticatedSrtcpPacket(header.toReadOnly(), payload, isEncrypted, srtcpIndex, authTag)
//}
