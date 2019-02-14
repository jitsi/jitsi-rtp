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
import org.jitsi.rtp.extensions.getBitAsBool
import org.jitsi.rtp.extensions.getBits
import org.jitsi.rtp.extensions.putBitAsBoolean
import org.jitsi.rtp.extensions.putBits
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.util.ByteBufferUtils
import toUInt
import unsigned.toUInt
import unsigned.toULong
import unsigned.toUShort
import java.nio.ByteBuffer


//open class ReadOnlyRtpHeader(
//    open val version: Int = 2,
//    open val hasPadding: Boolean = false,
//    open val csrcCount: Int = 0,
//    open val marker: Boolean = false,
//    open val payloadType: Int = 0,
//    open val sequenceNumber: Int = 0,
//    open val timestamp: Long = 0,
//    open val ssrc: Long = 0,
//    open val csrcs: MutableList<Long> = mutableListOf(),
//    open val extensions: RtpHeaderExtensions = RtpHeaderExtensions.NO_EXTENSIONS,
//    buf: ByteBuffer? = null
//) : Serializable {
//
//    private lateinit var buffer: ByteBuffer
//
//    init {
//        finalize(buf)
//    }
//
//    val size: Int
//        get() = RtpHeader.FIXED_SIZE_BYTES +
//                (csrcCount * RtpHeader.CSRC_SIZE_BYTES) +
//                extensions.size
//
//    val hasExtension: Boolean
//        get() = extensions.isNotEmpty()
//
//    override fun getBuffer(): ByteBuffer = buffer
//
//    protected fun finalize(optionalBuffer: ByteBuffer?) {
//        val b = ByteBufferUtils.ensureCapacity(optionalBuffer, size)
//        b.rewind()
//        b.limit(size)
//
//        RtpHeader.setVersion(b, version)
//        RtpHeader.setPadding(b, hasPadding)
//        RtpHeader.setExtension(b, hasExtension)
//        RtpHeader.setCsrcCount(b, csrcCount)
//        RtpHeader.setMarker(b, marker)
//        RtpHeader.setPayloadType(b, payloadType)
//        RtpHeader.setSequenceNumber(b, sequenceNumber)
//        RtpHeader.setTimestamp(b, timestamp)
//        RtpHeader.setSsrc(b, ssrc)
//        RtpHeader.setCsrcs(b, csrcs)
//        if (hasExtension) {
//            // Write the generic extension header (the cookie and the length)
//            //TODO
////            b.position(getExtensionsHeaderOffset(csrcCount))
////            setExtensions(b, extensions)
//        }
//        b.rewind()
//        buffer = b
//    }
//
//    //TODO(brian): we could return 'this' if this one has already come
//    // from a modifiable header...but maybe we also want to give the option
//    // to definitely return a new copy...basically giving us copy-on-write
//    fun modify(): ModifiableRtpHeader {
//        return if (this is ModifiableRtpHeader) {
//            this
//        } else {
//            ModifiableRtpHeader(version, hasPadding, csrcCount, marker, payloadType, sequenceNumber, timestamp, ssrc, csrcs, extensions)
//        }
//    }
//
//    override fun toString(): String = with (StringBuffer()) {
//        appendln("size: $size")
//        appendln("version: $version")
//        appendln("hasPadding: $hasPadding")
//        appendln("hasExtension: $hasExtension")
//        appendln("csrcCount: $csrcCount")
//        appendln("marker: $marker")
//        appendln("payloadType: $payloadType")
//        appendln("sequenceNumber: $sequenceNumber")
//        appendln("timestamp: $timestamp")
//        appendln("ssrc: $ssrc")
//        appendln("csrcs: $csrcs")
//        appendln("Extensions: $extensions")
//        toString()
//    }
//}

//open class ModifiableRtpHeader(
//    version: Int = 2,
//    hasPadding: Boolean = false,
//    csrcCount: Int = 0,
//    marker: Boolean = false,
//    payloadType: Int = 0,
//    sequenceNumber: Int = 0,
//    timestamp: Long = 0,
//    ssrc: Long = 0,
//    csrcs: MutableList<Long> = mutableListOf(),
//    extensions: RtpHeaderExtensions = RtpHeaderExtensions.NO_EXTENSIONS,
//    buf: ByteBuffer? = null
//) : ReadOnlyRtpHeader(version, hasPadding, csrcCount, marker, payloadType, sequenceNumber, timestamp, ssrc, csrcs, extensions, buf) {
//    override var version: Int = super.version
//    override var ssrc: Long = super.ssrc
//
//    //TODO(brian): we need to reflect the changes into the read-only class' buffer
//    fun finalize(): ReadOnlyRtpHeader {
//        super.finalize(super.getBuffer())
//        return this
//    }
//
//    // Getting the buffer from a ModifiableRtpHeader isn't allowed, as it is
//    // not in a finalized state
//    override fun getBuffer(): ByteBuffer = throw Exception()
//}

/**
 *
 * https://tools.ietf.org/html/rfc3550#section-5.1
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P|X|  CC   |M|     PT      |       sequence number         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           timestamp                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           synchronization source (SSRC) identifier            |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |            contributing source (CSRC) identifiers             |
 * |                             ....                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
open class RtpHeader(
    var version: Int = 2,
    var hasPadding: Boolean = false,
    var csrcCount: Int = 0,
    var marker: Boolean = false,
    var payloadType: Int = 0,
    var sequenceNumber: Int = 0,
    var timestamp: Long = 0,
    var ssrc: Long = 0,
    var csrcs: MutableList<Long> = mutableListOf(),
    var extensions: RtpHeaderExtensions = RtpHeaderExtensions.NO_EXTENSIONS,
    buf: ByteBuffer? = null
) : Serializable {
    private var buf: ByteBuffer? = null

    init {
        if (buf != null) {
            this.buf = buf.subBuffer(0, this.size)
        }
    }
    val size: Int
        get() = RtpHeader.FIXED_SIZE_BYTES +
                (csrcCount * RtpHeader.CSRC_SIZE_BYTES) +
                extensions.size

    val hasExtension: Boolean
        get() = extensions.isNotEmpty()

    companion object {
        const val FIXED_SIZE_BYTES = 12
        const val CSRC_SIZE_BYTES = 4

        fun getVersion(buf: ByteBuffer): Int = buf.get(0).getBits(0, 2).toUInt()
        fun setVersion(buf: ByteBuffer, version: Int) = buf.putBits(0, 0, version.toByte(), 2)

        fun hasPadding(buf: ByteBuffer): Boolean = buf.get(0).getBitAsBool(2)
        fun setPadding(buf: ByteBuffer, hasPadding: Boolean) = buf.putBitAsBoolean(0, 3, hasPadding)

        fun getExtension(buf: ByteBuffer): Boolean = buf.get(0).getBitAsBool(3)
        fun setExtension(buf: ByteBuffer, hasExtension: Boolean) = buf.putBitAsBoolean(0, 3, hasExtension)

        fun getCsrcCount(buf: ByteBuffer): Int = buf.get(0).getBits(4, 4).toUInt()
        fun setCsrcCount(buf: ByteBuffer, csrcCount: Int) {
            buf.putBits(0, 4, csrcCount.toByte(), 4)
        }

        fun getMarker(buf: ByteBuffer): Boolean = buf.get(1).getBitAsBool(0)
        fun setMarker(buf: ByteBuffer, isSet: Boolean) {
            buf.putBitAsBoolean(1, 0, isSet)
        }

        fun getPayloadType(buf: ByteBuffer): Int = buf.get(1).getBits(1, 7).toUInt()
        fun setPayloadType(buf: ByteBuffer, payloadType: Int) {
            buf.putBits(1, 1, payloadType.toByte(), 7)
        }

        fun getSequenceNumber(buf: ByteBuffer): Int = buf.getShort(2).toUInt()
        fun setSequenceNumber(buf: ByteBuffer, sequenceNumber: Int) {
            buf.putShort(2, sequenceNumber.toUShort())
        }

        fun getTimestamp(buf: ByteBuffer): Long = buf.getInt(4).toULong()
        fun setTimestamp(buf: ByteBuffer, timestamp: Long) {
            buf.putInt(4, timestamp.toUInt())
        }

        fun getSsrc(buf: ByteBuffer): Long = buf.getInt(8).toULong()
        fun setSsrc(buf: ByteBuffer, ssrc: Long) {
            buf.putInt(8, ssrc.toUInt())
        }

        fun getCsrcs(buf: ByteBuffer, csrcCount: Int): MutableList<Long> {
            return (0 until csrcCount).map {
                buf.getInt(12 + (it * RtpHeader.CSRC_SIZE_BYTES)).toULong()
            }.toMutableList()
        }
        fun setCsrcs(buf: ByteBuffer, csrcs: List<Long>) {
            csrcs.forEachIndexed { index, csrc ->
                buf.putInt(12 + (index * RtpHeader.CSRC_SIZE_BYTES), csrc.toUInt())
            }
        }

        /**
         * The offset at which the generic extension header should be placed
         */
        fun getExtensionsHeaderOffset(csrcCount: Int): Int = RtpHeader.FIXED_SIZE_BYTES + (csrcCount * RtpHeader.CSRC_SIZE_BYTES)

        /**
         * Note that the buffer passed to these two methods, unlike in most other helpers, must already
         * begin at the start of the extensions portion of the header.  This method also
         * assumes that the caller has already verified that there *are* extensions present
         * (i.e. the extension bit is set) in the case of 'getExtensions' or that there is space
         * for the extensions in the passed buffer (in the case of 'setExtensionsAndPadding')
         */
        /**
         * The buffer passed here should point to the start of the generic extension header
         */
        fun getExtensions(extensionsBuf: ByteBuffer): RtpHeaderExtensions = RtpHeaderExtensions(extensionsBuf)

        /**
         * [buf] should point to the start of the generic extension header
         */
        fun setExtensions(buf: ByteBuffer, extensions: RtpHeaderExtensions) {
            buf.put(extensions.getBuffer())
        }

        fun fromBuffer(buf: ByteBuffer): RtpHeader {
            val version = RtpHeader.getVersion(buf)
            val hasPadding = RtpHeader.hasPadding(buf)
            val hasExtension = RtpHeader.getExtension(buf)
            val csrcCount = RtpHeader.getCsrcCount(buf)
            val marker = RtpHeader.getMarker(buf)
            val payloadType = RtpHeader.getPayloadType(buf)
            val sequenceNumber = RtpHeader.getSequenceNumber(buf)
            val timestamp = RtpHeader.getTimestamp(buf)
            val ssrc = RtpHeader.getSsrc(buf)
            val csrcs = RtpHeader.getCsrcs(buf, csrcCount)

            val extensions = if (hasExtension) {
                RtpHeader.getExtensions(buf.subBuffer(getExtensionsHeaderOffset(csrcCount)))
            } else {
                RtpHeaderExtensions.NO_EXTENSIONS
            }

            return RtpHeader(
                version, hasPadding, csrcCount, marker, payloadType, sequenceNumber,
                    timestamp, ssrc, csrcs, extensions, buf
            )
        }
    }

    fun getExtension(id: Int): RtpHeaderExtension? = extensions.getExtension(id)

    fun addExtension(id: Int, ext: RtpHeaderExtension) = extensions.addExtension(id, ext)

    override fun getBuffer(): ByteBuffer {
        val b = ByteBufferUtils.ensureCapacity(buf, size)
        b.rewind()
        b.limit(size)

        RtpHeader.setVersion(b, version)
        RtpHeader.setPadding(b, hasPadding)
        RtpHeader.setExtension(b, hasExtension)
        RtpHeader.setCsrcCount(b, csrcCount)
        RtpHeader.setMarker(b, marker)
        RtpHeader.setPayloadType(b, payloadType)
        RtpHeader.setSequenceNumber(b, sequenceNumber)
        RtpHeader.setTimestamp(b, timestamp)
        RtpHeader.setSsrc(b, ssrc)
        RtpHeader.setCsrcs(b, csrcs)
        if (hasExtension) {
            // Write the generic extension header (the cookie and the length)
            b.position(getExtensionsHeaderOffset(csrcCount))
            setExtensions(b, extensions)
        }
        b.rewind()
        buf = b
        return b
    }

    override fun toString(): String {
        return with (StringBuffer()) {
            appendln("size: $size")
            appendln("version: $version")
            appendln("hasPadding: $hasPadding")
            appendln("hasExtension: $hasExtension")
            appendln("csrcCount: $csrcCount")
            appendln("marker: $marker")
            appendln("payloadType: $payloadType")
            appendln("sequenceNumber: $sequenceNumber")
            appendln("timestamp: $timestamp")
            appendln("ssrc: $ssrc")
            appendln("csrcs: $csrcs")
            appendln("Extensions: $extensions")
            toString()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other?.javaClass != javaClass) {
            return false
        }
        other as RtpHeader
        return (size == other.size &&
                version == other.version &&
                hasPadding == other.hasPadding &&
                hasExtension == other.hasExtension &&
                csrcCount == other.csrcCount &&
                marker == other.marker &&
                payloadType == other.payloadType &&
                sequenceNumber == other.sequenceNumber &&
                timestamp == other.timestamp &&
                ssrc == other.ssrc &&
                csrcs.equals(other.csrcs) &&
                extensions.equals(other.extensions))
    }

    override fun hashCode(): Int {
        return size.hashCode() + version.hashCode() + hasPadding.hashCode() +
                hasExtension.hashCode() + csrcCount.hashCode() + marker.hashCode() +
                payloadType.hashCode() + sequenceNumber.hashCode() + timestamp.hashCode() +
                ssrc.hashCode() + csrcs.hashCode() + extensions.hashCode()
    }
}
