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

package org.jitsi.rtp

import io.kotlintest.specs.ShouldSpec
import org.jitsi.rtp.new_scheme.ReadOnlyRtpHeader

internal class ReadOnlyRtpHeaderTest : ShouldSpec() {
    init {
        val readOnlyHeader = ReadOnlyRtpHeader(payloadType = 42, ssrc = 123L)

        val copyModify = readOnlyHeader.getModifiableCopy()
        val inPlaceModify = readOnlyHeader.modifyInPlace().run {
            payloadType = 44
            ssrc = 456L
            toReadOnly()
        }

        println(readOnlyHeader)
        println(inPlaceModify)
        println(copyModify)


    }
}