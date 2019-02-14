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

package org.jitsi.rtp.new_scheme.rtcp

import org.jitsi.rtp.Serializable
import org.jitsi.rtp.new_scheme.CanBecomeModifiable
import org.jitsi.rtp.new_scheme.CanBecomeReadOnly
import org.jitsi.rtp.new_scheme.Modifiable
import org.jitsi.rtp.new_scheme.ReadOnly
import org.jitsi.rtp.rtcp.RtcpHeader
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

class RtcpHeaderData(
    var version: Int = 2,
    var hasPadding: Boolean = false,
    var reportCount: Int = 0,
    var packetType: Int = 0,
    var length: Int = 0,
    var senderSsrc: Long = 0
) {
    val sizeBytes: Int = RtcpHeader.SIZE_BYTES

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
}

class ReadOnlyRtcpHeader(
    version: Int = 2,
    hasPadding: Boolean = false,
    reportCount: Int = 0,
    packetType: Int = 0,
    length: Int = 0,
    senderSsrc: Long = 0,
    buf: ByteBuffer? = null
) : Serializable, ReadOnly, CanBecomeModifiable<ModifiableRtcpHeader> {
    private val dataBuf: ByteBuffer

    private val rtcpHeaderData = RtcpHeaderData(
        version, hasPadding, reportCount, packetType, length, senderSsrc
    )

    val version: Int = rtcpHeaderData.version
    val hasPadding: Boolean = rtcpHeaderData.hasPadding
    val reportCount: Int = rtcpHeaderData.reportCount
    val packetType: Int = rtcpHeaderData.packetType
    val length: Int = rtcpHeaderData.length
    val senderSsrc: Long = rtcpHeaderData.senderSsrc

    val sizeBytes: Int = rtcpHeaderData.sizeBytes

    init {
        val b = ByteBufferUtils.ensureCapacity(buf, sizeBytes)
        b.rewind()
        b.limit(sizeBytes)

        RtcpHeader.setVersion(b, version)
        RtcpHeader.setPadding(b, hasPadding)
        RtcpHeader.setReportCount(b, reportCount)
        RtcpHeader.setPacketType(b, packetType)
        RtcpHeader.setLength(b, length)
        RtcpHeader.setSenderSsrc(b, senderSsrc)

        b.rewind()
        dataBuf = b
    }

    companion object {
        fun fromBuffer(buf: ByteBuffer): ReadOnlyRtcpHeader {
            val version = RtcpHeader.getVersion(buf)
            val hasPadding = RtcpHeader.hasPadding(buf)
            val reportCount = RtcpHeader.getReportCount(buf)
            val packetType = RtcpHeader.getPacketType(buf)
            val length = RtcpHeader.getLength(buf)
            val senderSsrc = RtcpHeader.getSenderSsrc(buf)
            return ReadOnlyRtcpHeader(
                version, hasPadding, reportCount, packetType, length, senderSsrc, buf
            )
        }
    }

    override fun getBuffer(): ByteBuffer = dataBuf.asReadOnlyBuffer()

    override fun modifyInPlace(): ModifiableRtcpHeader = ModifiableRtcpHeader(rtcpHeaderData, dataBuf)

    override fun getModifiableCopy(): ModifiableRtcpHeader = ModifiableRtcpHeader(rtcpHeaderData.clone(), dataBuf)
}

class ModifiableRtcpHeader(
    private val headerData: RtcpHeaderData = RtcpHeaderData(),
    private val dataBuf: ByteBuffer? = null
) : Modifiable, CanBecomeReadOnly<ReadOnlyRtcpHeader> {

    override fun toReadOnly(): ReadOnlyRtcpHeader {
        return ReadOnlyRtcpHeader(
            headerData.version,
            headerData.hasPadding,
            headerData.reportCount,
            headerData.packetType,
            headerData.length,
            headerData.senderSsrc,
            dataBuf
        )
    }
}