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

package org.jitsi.rtp.new_scheme3.rtcp.data

import org.jitsi.rtp.extensions.getBitAsBool
import org.jitsi.rtp.extensions.getBits
import org.jitsi.rtp.extensions.putBitAsBoolean
import org.jitsi.rtp.extensions.putBits
import org.jitsi.rtp.extensions.unsigned.allocateByteBuffer
import org.jitsi.rtp.extensions.unsigned.getUByte
import org.jitsi.rtp.extensions.unsigned.getUInt
import org.jitsi.rtp.extensions.unsigned.getUShort
import org.jitsi.rtp.extensions.unsigned.incrementPosition
import org.jitsi.rtp.extensions.unsigned.putUByte
import org.jitsi.rtp.extensions.unsigned.putUInt
import org.jitsi.rtp.extensions.unsigned.putUShort
import org.jitsi.rtp.extensions.unsigned.subBuffer
import org.jitsi.rtp.extensions.unsigned.uposition
import org.jitsi.rtp.new_scheme3.SerializableData
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
@ExperimentalUnsignedTypes
data class RtcpHeaderData(
    var version: Int = 2,
    var hasPadding: Boolean = false,
    var reportCount: Int = 0,
    var packetType: UByte = 0u,
    var length: UShort = 0u,
    var senderSsrc: UInt = 0u
) : SerializableData(), kotlin.Cloneable {
    override val sizeBytes: UInt = SIZE_BYTES

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

    public override fun clone(): RtcpHeaderData {
        return RtcpHeaderData(
            version, hasPadding, reportCount, packetType, length, senderSsrc
        )
    }

    override fun getBuffer(): ByteBuffer {
        val b = allocateByteBuffer(sizeBytes)
        serializeTo(b)
        return b.rewind() as ByteBuffer
    }

    override fun serializeTo(buf: ByteBuffer) {
        // Because of the nature of the fields in the RTCP header, it's easier for us
        // to write the values using absolute positions within the given buffer.  However,
        // those values assume position 0 of the buffer is where the header should start,
        // which isn't necessarily the case (and doesn't match the rest of the implementations
        // of serializeTo), so we create a temporary wrapper around the given buffer whose
        // position 0 is at buf's current position and then we manually increment
        // buf's position to after the header data we just wrote
        val absBuf = buf.subBuffer(buf.uposition(), RtcpHeaderData.SIZE_BYTES)
        setVersion(absBuf, version)
        setPadding(absBuf, hasPadding)
        setReportCount(absBuf, reportCount)
        setPacketType(absBuf, packetType)
        setLength(absBuf, length)
        setSenderSsrc(absBuf, senderSsrc)
        buf.incrementPosition(SIZE_BYTES)
    }

    companion object {
        const val SIZE_BYTES: UInt = 8u
        fun create(buf: ByteBuffer): RtcpHeaderData {
            val version = getVersion(buf)
            val hasPadding = hasPadding(buf)
            val reportCount = getReportCount(buf)
            val packetType = getPacketType(buf)
            val length = getLength(buf)
            val senderSsrc = getSenderSsrc(buf)

            return RtcpHeaderData(version, hasPadding, reportCount, packetType, length, senderSsrc)
        }

        fun getVersion(buf: ByteBuffer): Int = buf.get(0).getBits(0, 2).toInt()
        fun setVersion(buf: ByteBuffer, version: Int) = buf.putBits(0, 0, version.toByte(), 2)

        fun hasPadding(buf: ByteBuffer): Boolean = buf.get(0).getBitAsBool(2)
        fun setPadding(buf: ByteBuffer, hasPadding: Boolean) = buf.putBitAsBoolean(0, 2, hasPadding)

        fun getReportCount(buf: ByteBuffer): Int = buf.get(0).getBits(3, 5).toInt()
        fun setReportCount(buf: ByteBuffer, reportCount: Int) = buf.putBits(0, 3, reportCount.toByte(), 5)

        fun getPacketType(buf: ByteBuffer): UByte = buf.getUByte(1)
        fun setPacketType(buf: ByteBuffer, packetType: UByte) {
            buf.putUByte(1, packetType)
        }

        fun getLength(buf: ByteBuffer): UShort = buf.getUShort(2)
        fun setLength(buf: ByteBuffer, length: UShort) {
            buf.putUShort(2, length)
        }

        fun getSenderSsrc(buf: ByteBuffer): UInt = buf.getUInt(4)
        fun setSenderSsrc(buf: ByteBuffer, senderSsrc: UInt) {
            buf.putUInt(4, senderSsrc)
        }
    }
}
