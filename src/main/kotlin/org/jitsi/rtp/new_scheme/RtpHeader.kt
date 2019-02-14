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

import org.jitsi.rtp.RtpHeader
import org.jitsi.rtp.RtpHeaderExtensions
import org.jitsi.rtp.Serializable
import org.jitsi.rtp.extensions.clone
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

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
class RtpHeaderData(
    var version: Int = 2,
    var hasPadding: Boolean = false,
    var marker: Boolean = false,
    var payloadType: Int = 0,
    var sequenceNumber: Int = 0,
    var timestamp: Long = 0,
    var ssrc: Long = 0,
    var csrcs: MutableList<Long> = mutableListOf(),
    var extensions: RtpHeaderExtensions = RtpHeaderExtensions.NO_EXTENSIONS
) {
    val sizeBytes: Int
        get() = RtpHeader.FIXED_SIZE_BYTES +
                (csrcCount * RtpHeader.CSRC_SIZE_BYTES) +
                extensions.size

    val hasExtension: Boolean
        get() = extensions.isNotEmpty()

    val csrcCount
        get() = csrcs.size

    companion object {
        fun fromBuffer(buf: ByteBuffer): RtpHeaderData {
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
                RtpHeader.getExtensions(buf.subBuffer(RtpHeader.getExtensionsHeaderOffset(csrcCount)))
            } else {
                RtpHeaderExtensions.NO_EXTENSIONS
            }
            return RtpHeaderData(
                    version,
                    hasPadding,
                    marker,
                    payloadType,
                    sequenceNumber,
                    timestamp,
                    ssrc,
                    csrcs,
                    extensions
            )
        }
    }

    fun clone(): RtpHeaderData {
        return RtpHeaderData(
                version,
                hasPadding,
                marker,
                payloadType,
                sequenceNumber,
                timestamp,
                ssrc,
                csrcs.toMutableList(),
                //TODO(brian): add clone method to extensions
                extensions
        )
    }

    override fun toString(): String = with (StringBuffer()) {
        appendln("size: $sizeBytes")
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

class ReadOnlyRtpHeader(
        version: Int = 2,
        hasPadding: Boolean = false,
        marker: Boolean = false,
        payloadType: Int = 0,
        sequenceNumber: Int = 0,
        timestamp: Long = 0,
        ssrc: Long = 0,
        csrcs: MutableList<Long> = mutableListOf(),
        extensions: RtpHeaderExtensions = RtpHeaderExtensions.NO_EXTENSIONS,
        buf: ByteBuffer? = null
) : Serializable, ReadOnly, CanBecomeModifiable<ModifiableRtpHeader> {
    private val dataBuf: ByteBuffer

    private val rtpHeaderData = RtpHeaderData(
        version, hasPadding, marker, payloadType, sequenceNumber, timestamp, ssrc, csrcs, extensions
    )

    val version: Int = rtpHeaderData.version
    val hasPadding: Boolean = rtpHeaderData.hasPadding
    val marker: Boolean = rtpHeaderData.marker
    val payloadType: Int = rtpHeaderData.payloadType
    val sequenceNumber: Int = rtpHeaderData.sequenceNumber
    val timestamp: Long = rtpHeaderData.timestamp
    val ssrc: Long = rtpHeaderData.ssrc
    val csrcs: List<Long> = rtpHeaderData.csrcs
    //TODO(brian): need a readonly RtpheaderExtensions
    val extensions: RtpHeaderExtensions = rtpHeaderData.extensions

    init {
        val b = ByteBufferUtils.ensureCapacity(buf, rtpHeaderData.sizeBytes)
        b.rewind()
        b.limit(rtpHeaderData.sizeBytes)

        RtpHeader.setVersion(b, rtpHeaderData.version)
        RtpHeader.setPadding(b, rtpHeaderData.hasPadding)
        RtpHeader.setExtension(b, rtpHeaderData.hasExtension)
        RtpHeader.setCsrcCount(b, rtpHeaderData.csrcCount)
        RtpHeader.setMarker(b, rtpHeaderData.marker)
        RtpHeader.setPayloadType(b, rtpHeaderData.payloadType)
        RtpHeader.setSequenceNumber(b, rtpHeaderData.sequenceNumber)
        RtpHeader.setTimestamp(b, rtpHeaderData.timestamp)
        RtpHeader.setSsrc(b, rtpHeaderData.ssrc)
        RtpHeader.setCsrcs(b, rtpHeaderData.csrcs)
        if (rtpHeaderData.hasExtension) {
            // Write the generic extension header (the cookie and the length)
            b.position(RtpHeader.getExtensionsHeaderOffset(rtpHeaderData.csrcCount))
            RtpHeader.setExtensions(b, extensions)
        }
        b.rewind()

        dataBuf = b
    }

    companion object {
        fun fromBuffer(buf: ByteBuffer): ReadOnlyRtpHeader {
            val rtpHeaderData = RtpHeaderData.fromBuffer(buf)
            return ReadOnlyRtpHeader(
                    rtpHeaderData.version,
                    rtpHeaderData.hasPadding,
                    rtpHeaderData.marker,
                    rtpHeaderData.payloadType,
                    rtpHeaderData.sequenceNumber,
                    rtpHeaderData.timestamp,
                    rtpHeaderData.ssrc,
                    rtpHeaderData.csrcs,
                    rtpHeaderData.extensions,
                    buf
            )
        }
    }

    val sizeBytes: Int = rtpHeaderData.sizeBytes

    override fun getBuffer(): ByteBuffer = dataBuf.asReadOnlyBuffer()

    override fun modifyInPlace(): ModifiableRtpHeader = ModifiableRtpHeader(rtpHeaderData, dataBuf)

    override fun getModifiableCopy(): ModifiableRtpHeader = ModifiableRtpHeader(rtpHeaderData.clone(), dataBuf.clone())

    override fun toString(): String = rtpHeaderData.toString()
}

open class ModifiableRtpHeader(
        private val headerData: RtpHeaderData = RtpHeaderData(),
        private val dataBuf: ByteBuffer? = null
) : Modifiable, CanBecomeReadOnly<ReadOnlyRtpHeader> {
    val sizeBytes: Int = headerData.sizeBytes

    var version: Int
        get() = headerData.version
        set(version) {
            headerData.version = version
        }
    var hasPadding: Boolean
        get() = headerData.hasPadding
        set(hasPadding) {
            headerData.hasPadding = hasPadding
        }
    var marker: Boolean
        get() = headerData.marker
        set(marker) {
            headerData.marker = marker
        }
    var payloadType: Int
        get() = headerData.payloadType
        set(payloadType) {
            headerData.payloadType = payloadType
        }
    var sequenceNumber: Int
        get() = headerData.sequenceNumber
        set(sequenceNumber) {
            headerData.sequenceNumber = sequenceNumber
        }
    var timestamp: Long
        get() = headerData.timestamp
        set(timestamp) {
            headerData.timestamp = timestamp
        }
    var ssrc: Long
        get() = headerData.ssrc
        set(ssrc) {
            headerData.ssrc = ssrc
        }
    //TODO(brian): for csrcs and extensions, can we make them vals and the data structures
    // themselves are just mutable?
    var csrcs: MutableList<Long>
        get() = headerData.csrcs
        set(csrcs) {
            headerData.csrcs = csrcs
        }
    var extensions: RtpHeaderExtensions
        get() = headerData.extensions
        set(extensions) {
            headerData.extensions = extensions
        }

    companion object {
        fun fromBuffer(buf: ByteBuffer): ModifiableRtpHeader {
            val rtpHeaderData = RtpHeaderData.fromBuffer(buf)
            return ModifiableRtpHeader(
                    rtpHeaderData,
                    buf
            )
        }
    }

    override fun toReadOnly(): ReadOnlyRtpHeader {
        return ReadOnlyRtpHeader(
                headerData.version,
                headerData.hasPadding,
                headerData.marker,
                headerData.payloadType,
                headerData.sequenceNumber,
                headerData.timestamp,
                headerData.ssrc,
                headerData.csrcs,
                headerData.extensions,
                dataBuf
        )
    }

    override fun toString(): String = headerData.toString()
}
