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

package org.jitsi.rtp.new_scheme3.srtp

import org.jitsi.rtp.new_scheme3.UnparsedPacket
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer

/**
 * Either an SRTP or SRTCP packet, but we don't know which yet
 */
class SrtpProtocolPacket(
    buf: ByteBuffer = ByteBufferUtils.EMPTY_BUFFER
) : UnparsedPacket(buf)
