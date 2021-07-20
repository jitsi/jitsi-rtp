/*
 * Copyright @ 2021 - present 8x8, Inc.
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
package org.jitsi.rtp.rtp.header_extensions

import org.jitsi.rtp.rtp.RtpPacket
import org.jitsi.rtp.rtp.header_extensions.HeaderExtensionHelpers.Companion.getDataLengthBytes
import org.jitsi.rtp.util.BufferPool
import java.nio.charset.StandardCharsets

/**
 * https://datatracker.ietf.org/doc/html/rfc7941#section-4.1.1
 * Note: this is only the One-Byte Format, because we don't support Two-Byte yet.
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  ID   |  len  | SDES item text value ...                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
class SdesHeaderExtension {
    companion object {
        const val DATA_OFFSET = 1

        fun getTextValue(ext: RtpPacket.HeaderExtension): String =
            getTextValue(ext.currExtBuffer, ext.currExtOffset)
        fun setTextValue(ext: RtpPacket.HeaderExtension, sdesValue: String) =
            setTextValue(ext.currExtBuffer, ext.currExtOffset, sdesValue)

        private fun getTextValue(buf: ByteArray, offset: Int): String {
            val dataLength = getDataLengthBytes(buf, offset)
            val copy = BufferPool.getArray(dataLength)
            System.arraycopy(buf, offset + SdesHeaderExtension.DATA_OFFSET, copy, 0, dataLength)
            return String(copy, 0, dataLength, StandardCharsets.US_ASCII)
        }

        private fun setTextValue(buf: ByteArray, offset: Int, sdesValue: String) {
            val dataLength = getDataLengthBytes(buf, offset)
            assert(dataLength == sdesValue.length) { "buffer size doesn't match SDES value length" }
            val array = sdesValue.toByteArray(StandardCharsets.US_ASCII)
            System.arraycopy(
                array, 0, buf,
                offset + SdesHeaderExtension.DATA_OFFSET, sdesValue.length
            )
        }
    }
}
