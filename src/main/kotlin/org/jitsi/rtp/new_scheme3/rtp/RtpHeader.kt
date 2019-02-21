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

package org.jitsi.rtp.new_scheme3.rtp

import org.jitsi.rtp.RtpHeaderExtension
import org.jitsi.rtp.RtpHeaderExtensions
import org.jitsi.rtp.new_scheme3.ImmutableAlias
import org.jitsi.rtp.new_scheme3.SerializableData
import org.jitsi.rtp.new_scheme3.rtp.data.RtpHeaderData
import java.nio.ByteBuffer

abstract class ImmutableRtpHeader internal constructor(
    protected val headerData: RtpHeaderData = RtpHeaderData()
) : SerializableData(), kotlin.Cloneable {
    override val sizeBytes: UInt
        get() = headerData.sizeBytes

    val version: Int by ImmutableAlias(headerData::version)
    val hasPadding: Boolean by ImmutableAlias(headerData::hasPadding)
    val marker: Boolean by ImmutableAlias(headerData::marker)
    val payloadType: Int by ImmutableAlias(headerData::payloadType)
    val sequenceNumber: Int by ImmutableAlias(headerData::sequenceNumber)
    val timestamp: Long by ImmutableAlias(headerData::timestamp)
    val ssrc: Long by ImmutableAlias(headerData::ssrc)
    val csrcs: List<Long> by ImmutableAlias(headerData::csrcs)
    //TODO(brian): need a readonly RtpheaderExtensions, as it can still
    // be modified here since it's an object
    val extensions: RtpHeaderExtensions by ImmutableAlias(headerData::extensions)

    val hasExtension: Boolean get() = extensions.isNotEmpty()

    val csrcCount: Int get() = csrcs.size

    constructor(
        version: Int = 2,
        hasPadding: Boolean = false,
        marker: Boolean = false,
        payloadType: Int = 0,
        sequenceNumber: Int = 0,
        timestamp: Long = 0,
        ssrc: Long = 0,
        csrcs: List<Long> = listOf(),
        extensions: RtpHeaderExtensions = RtpHeaderExtensions.NO_EXTENSIONS
    ) : this(RtpHeaderData(
        version, hasPadding, marker, payloadType, sequenceNumber,
        timestamp, ssrc, csrcs.toMutableList(), extensions))

    fun getExtension(id: Int): RtpHeaderExtension? = extensions.getExtension(id)

    protected fun doModify(block: RtpHeaderData.() -> Unit) {
        with (headerData) {
            block()
        }
    }

    override fun serializeTo(buf: ByteBuffer) {
        headerData.serializeTo(buf)
    }
}

class RtpHeader(
    headerData: RtpHeaderData = RtpHeaderData()
) : ImmutableRtpHeader(headerData) {

    fun modify(block: RtpHeaderData.() -> Unit) {
        doModify(block)
    }

    public override fun clone(): RtpHeader =
        RtpHeader(headerData.clone())

    companion object {
        const val FIXED_HEADER_SIZE_BYTES = RtpHeaderData.FIXED_HEADER_SIZE_BYTES
        fun create(buf: ByteBuffer): RtpHeader =
            RtpHeader(RtpHeaderData.create(buf))

        fun fromValues(
            version: Int = 2,
            hasPadding: Boolean = false,
            marker: Boolean = false,
            payloadType: Int = 0,
            sequenceNumber: Int = 0,
            timestamp: Long = 0,
            ssrc: Long = 0,
            csrcs: List<Long> = listOf(),
            extensions: RtpHeaderExtensions = RtpHeaderExtensions.NO_EXTENSIONS
        ): RtpHeader = RtpHeader(RtpHeaderData(
                version, hasPadding, marker, payloadType, sequenceNumber,
                timestamp, ssrc, csrcs.toMutableList(), extensions))
    }
}

