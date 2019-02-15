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

package org.jitsi.rtp.new_scheme2.rtcp

import org.jitsi.rtp.Serializable
import org.jitsi.rtp.new_scheme2.ConstructableFromBuffer
import org.jitsi.rtp.new_scheme2.ImmutableSerializableData
import org.jitsi.rtp.rtcp.RtcpHeader
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

/**
 * Models the RTCP header as defined in https://tools.ietf.org/html/rfc3550#section-6.1
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P|    RC   |   PT=SR=200   |             length            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         SSRC of sender                        |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 */
private class RtcpHeaderData(
    var version: Int = 2,
    var hasPadding: Boolean = false,
    var reportCount: Int = 0,
    var packetType: Int = 0,
    var length: Int = 0,
    var senderSsrc: Long = 0
) : Serializable {
    val sizeBytes: Int = RtcpHeader.SIZE_BYTES

    companion object : ConstructableFromBuffer<RtcpHeaderData> {
        override fun fromBuffer(buf: ByteBuffer): RtcpHeaderData {
            val version = RtcpHeader.getVersion(buf)
            val hasPadding = RtcpHeader.hasPadding(buf)
            val reportCount = RtcpHeader.getReportCount(buf)
            val packetType = RtcpHeader.getPacketType(buf)
            val length = RtcpHeader.getLength(buf)
            val senderSsrc = RtcpHeader.getSenderSsrc(buf)

            return RtcpHeaderData(version, hasPadding, reportCount, packetType, length, senderSsrc)
        }
    }

    override fun toString(): String {
        return with(StringBuffer()) {
            appendln("version: $version")
            appendln("hasPadding: $hasPadding")
            appendln("reportCount: $reportCount")
            appendln("packetType: $packetType")
            appendln("length: ${this@RtcpHeaderData.length}")
            appendln("senderSsrc: $senderSsrc")
            this.toString()
        }
    }

    fun clone(): RtcpHeaderData {
        return RtcpHeaderData(
            version, hasPadding, reportCount, packetType, length, senderSsrc
        )
    }

    override fun getBuffer(): ByteBuffer {
        val b = ByteBuffer.allocate(sizeBytes)
        serializeTo(b)
        return b.rewind() as ByteBuffer
    }

    override fun serializeTo(buf: ByteBuffer) {
        RtcpHeader.setVersion(buf, version)
        RtcpHeader.setPadding(buf, hasPadding)
        RtcpHeader.setReportCount(buf, reportCount)
        RtcpHeader.setPacketType(buf, packetType)
        RtcpHeader.setLength(buf, length)
        RtcpHeader.setSenderSsrc(buf, senderSsrc)
    }
}

class ImmutableRtcpHeader private constructor(
    private val headerData: RtcpHeaderData = RtcpHeaderData(),
    backingBuffer: ByteBuffer? = null
) : ImmutableSerializableData() {

    constructor(
        version: Int = 2,
        hasPadding: Boolean = false,
        reportCount: Int = 0,
        packetType: Int = 0,
        length: Int = 0,
        senderSsrc: Long = 0,
        backingBuffer: ByteBuffer? = null
    ) : this(RtcpHeaderData(
            version, hasPadding, reportCount,
            packetType, length, senderSsrc), backingBuffer)

    override val dataBuf: ByteBuffer by lazy {
        val b = ByteBufferUtils.ensureCapacity(backingBuffer, headerData.sizeBytes)
        b.rewind()
        b.limit(headerData.sizeBytes)
        headerData.serializeTo(b)
        b.rewind() as ByteBuffer
    }

    val sizeBytes: Int = headerData.sizeBytes

    val version: Int = headerData.version
    val hasPadding: Boolean = headerData.hasPadding
    val reportCount: Int = headerData.reportCount
    val packetType: Int = headerData.packetType
    val length: Int = headerData.length
    val senderSsrc: Long = headerData.senderSsrc

    companion object : ConstructableFromBuffer<ImmutableRtcpHeader> {
        override fun fromBuffer(buf: ByteBuffer): ImmutableRtcpHeader {
            val headerData = RtcpHeaderData.fromBuffer(buf)
            return ImmutableRtcpHeader(headerData, buf)
        }
    }
}
