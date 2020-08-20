package org.jitsi.rtp.rtp

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeTypeOf

import org.jitsi.rtp.extensions.bytearray.byteArrayOf

class RedRtpPacketTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        context("Parsing block headers") {
            context("Primary") {
                val block = BlockHeader.parse(byteArrayOf(0x05), 0)
                block.shouldBeTypeOf<PrimaryBlockHeader>()
                block.pt shouldBe 5.toByte()
            }
            context("Redundancy") {
                val block = BlockHeader.parse(byteArrayOf(0x85, 0xff, 0xff, 0xff), 0)
                block.shouldBeTypeOf<RedundancyBlockHeader>()
                block as RedundancyBlockHeader
                block.pt shouldBe 5.toByte()
                block.timestampOffset shouldBe 0x3fff
                block.length shouldBe 0x3ff
            }
        }
        context("Parsing a RED packet with a single block") {
            context("Without reading the redundancy blocks") {
                val packet = RtpRedPacket(redPacketBytesSingleBlock.clone(), 0, redPacketBytesSingleBlock.size)
                packet.payloadType shouldBe 112
                packet.sequenceNumber shouldBe 0x7b2b
                packet.timestamp shouldBe 0x44b2836e
                packet.ssrc shouldBe 0x16493f2d
                packet.hasExtensions shouldBe true
                packet.getHeaderExtension(1).shouldHaveId1AndLen1()
                packet.payloadLength shouldBe 66

                packet.parse(false)
                packet.payloadType shouldBe 111
                packet.sequenceNumber shouldBe 0x7b2b
                packet.timestamp shouldBe 0x44b2836e
                packet.ssrc shouldBe 0x16493f2d
                packet.hasExtensions shouldBe true
                packet.getHeaderExtension(1).shouldHaveId1AndLen1()
                packet.payloadLength shouldBe 65
            }
            context("And reading the redundancy blocks") {
                val packet = RtpRedPacket(redPacketBytesSingleBlock.clone(), 0, redPacketBytesSingleBlock.size)
                packet.payloadType shouldBe 112
                packet.sequenceNumber shouldBe 0x7b2b
                packet.timestamp shouldBe 0x44b2836e
                packet.ssrc shouldBe 0x16493f2d
                packet.hasExtensions shouldBe true
                packet.getHeaderExtension(1).shouldHaveId1AndLen1()
                packet.payloadLength shouldBe 66

                val redundantPackets = packet.parse(true)
                redundantPackets.shouldBeEmpty()
                packet.payloadType shouldBe 111
                packet.sequenceNumber shouldBe 0x7b2b
                packet.timestamp shouldBe 0x44b2836e
                packet.ssrc shouldBe 0x16493f2d
                packet.hasExtensions shouldBe true
                packet.getHeaderExtension(1).shouldHaveId1AndLen1()
                packet.payloadLength shouldBe 65
            }
        }
        context("Parsing a RED packet with multiple blocks") {
            context("Without reading the redundancy blocks") {
                val packet = RtpRedPacket(redPacketBytes.clone(), 0, redPacketBytes.size)
                packet.payloadType shouldBe 112
                packet.sequenceNumber shouldBe 0x7b2b
                packet.timestamp shouldBe 0x44b2836e
                packet.ssrc shouldBe 0x16493f2d
                packet.hasExtensions shouldBe true
                packet.getHeaderExtension(1).shouldHaveId1AndLen1()
                packet.payloadLength shouldBe 4 + 4 + 1 + 65 + 68 + 62 // block headers + block lengths

                packet.parse(false)
                packet.payloadType shouldBe 111
                packet.sequenceNumber shouldBe 0x7b2b
                packet.timestamp shouldBe 0x44b2836e
                packet.ssrc shouldBe 0x16493f2d
                packet.hasExtensions shouldBe true
                packet.getHeaderExtension(1).shouldHaveId1AndLen1()
                packet.payloadLength shouldBe 62
            }
            context("And reading the redundancy block") {
                val packet = RtpRedPacket(redPacketBytes.clone(), 0, redPacketBytes.size)
                packet.payloadType shouldBe 112
                packet.sequenceNumber shouldBe 0x7b2b
                // Make sure we test the case when the sequence numbers wrap
                packet.sequenceNumber = 1
                packet.timestamp shouldBe 0x44b2836e
                packet.ssrc shouldBe 0x16493f2d
                packet.hasExtensions shouldBe true
                packet.getHeaderExtension(1).shouldHaveId1AndLen1()
                packet.payloadLength shouldBe 4 + 4 + 1 + 65 + 68 + 62 // block headers + block lengths

                val redundancyPackets = packet.parse(true)
                redundancyPackets.size shouldBe 2

                redundancyPackets.forEachIndexed { i, p ->
                    // Offset relative to the sequence number of the primary. The first redundancy packet has
                    // offset=-2, the second has previous has offset=-1
                    val offset = i - 2

                    p.payloadType shouldBe 111
                    p.sequenceNumber shouldBe ((1 + offset) and 0xffff)
                    p.timestamp shouldBe (0x44b2836e + offset * 960)
                    p.ssrc shouldBe packet.ssrc
                    p.hasExtensions shouldBe false
                }

                packet.payloadType shouldBe 111
                packet.sequenceNumber shouldBe 1
                packet.timestamp shouldBe 0x44b2836e
                packet.ssrc shouldBe 0x16493f2d
                packet.hasExtensions shouldBe true
                packet.getHeaderExtension(1).shouldHaveId1AndLen1()
                packet.payloadLength shouldBe 62
            }
        }
    }
}

private fun RtpPacket.HeaderExtension?.shouldHaveId1AndLen1() {
    this shouldNotBe null
    this as RtpPacket.HeaderExtension
    id shouldBe 1
    dataLengthBytes shouldBe 1
}

private val redPacketBytes = byteArrayOf(
    // RTP Header. PT=112, seq=0x7b2b
    0x90, 0x70, 0x7b, 0x2b,
    // TS
    0x44, 0xb2, 0x83, 0x6e,
    // SSRC
    0x16, 0x49, 0x3f, 0x2d,
    // Extension
    0xbe, 0xde, 0x00, 0x01,
    // ID=1, value=0x3d
    0x10, 0x3d, 0x00, 0x00,

    // RTP Payload
    // RED Header Block 1: follow=true, pt=111, ts_offset=1920, length=65
    0xef, 0x1e, 0x00, 0x41,
    // RED Header Block 2: follow=true, pt=111, ts_offset=960, length=68
    0xef, 0x0f, 0x00, 0x44,
    // RED Header Block 3: follow=false, pt=111 (remaining length=62)
    0x6f,
    // RED Block 1 (length=65):
    0x68, 0x2f, 0x3b, 0x25,
    0xab, 0x1a, 0x72, 0xfb,
    0x75, 0x2d, 0xf0, 0x9b,
    0xa2, 0xa3, 0xfc, 0x20,
    0x51, 0xf7, 0x9c, 0xe8,
    0x75, 0xe9, 0x02, 0xd6,
    0xc1, 0xe4, 0xa1, 0x51,
    0x02, 0x00, 0x59, 0x55,
    0x04, 0x56, 0x5e, 0xed,
    0x31, 0x55, 0xb5, 0x04,
    0x9d, 0xf6, 0x1c, 0x40,
    0x7b, 0xb7, 0x00, 0x0c,
    0xd9, 0x7b, 0x5d, 0x13,
    0x4c, 0xeb, 0x7d, 0xf1,
    0x74, 0xf8, 0xd5, 0xb9,
    0x07, 0xda, 0x18, 0x19,
    0x92,
    // RED Block 2 (length=68):
    0x68, 0x2f, 0x3b, 0x08,
    0xa5, 0x08, 0xb1, 0x93,
    0x5b, 0x65, 0x41, 0x30,
    0x55, 0xcd, 0xb7, 0xca,
    0xd2, 0x66, 0xc8, 0x53,
    0x18, 0x95, 0x9d, 0x49,
    0x81, 0x0a, 0xba, 0x67,
    0xf2, 0x42, 0xe3, 0xad,
    0x26, 0x73, 0x14, 0x52,
    0x62, 0x03, 0x3d, 0x1e,
    0xdd, 0x58, 0x44, 0x4e,
    0xc9, 0x56, 0x2f, 0x77,
    0xf9, 0x64, 0xf2, 0x6e,
    0x8c, 0x39, 0x35, 0x1a,
    0xba, 0x10, 0xe2, 0x85,
    0x1f, 0x28, 0x87, 0xb9,
    0x02, 0xa4, 0x68, 0x6b,
    // RED Block 3 (length is the remainder, 62):
    0x68, 0x2f, 0x3b, 0x2f,
    0xb9, 0x5f, 0xc4, 0x65,
    0x67, 0x7f, 0x5e, 0xa0,
    0x46, 0xb0, 0x93, 0xf5,
    0xc3, 0x5c, 0x69, 0xd7,
    0x1f, 0x75, 0xe9, 0xef,
    0xd1, 0x94, 0xdc, 0x47,
    0x24, 0x13, 0xf8, 0x6e,
    0xda, 0x18, 0xf3, 0x72,
    0x5b, 0x73, 0x03, 0x8c,
    0xc6, 0x2c, 0x7a, 0xab,
    0x38, 0xad, 0x87, 0x6c,
    0x9b, 0x08, 0x85, 0x08,
    0x8f, 0xa3, 0x72, 0xf1,
    0xf4, 0x7d, 0x88, 0xc3,
    0x13, 0x84
)

private val redPacketBytesSingleBlock = byteArrayOf(
    // RTP Header. PT=112, seq=0x7b2b
    0x90, 0x70, 0x7b, 0x2b,
    // TS
    0x44, 0xb2, 0x83, 0x6e,
    // SSRC
    0x16, 0x49, 0x3f, 0x2d,
    // Extension
    0xbe, 0xde, 0x00, 0x01,
    // ID=1, value=0x3d
    0x10, 0x3d, 0x00, 0x00,

    // RTP Payload
    // RED Header Block 1: follow=false, pt=111
    0x6f,
    // RED Block 1 (length=65):
    0x68, 0x2f, 0x3b, 0x25,
    0xab, 0x1a, 0x72, 0xfb,
    0x75, 0x2d, 0xf0, 0x9b,
    0xa2, 0xa3, 0xfc, 0x20,
    0x51, 0xf7, 0x9c, 0xe8,
    0x75, 0xe9, 0x02, 0xd6,
    0xc1, 0xe4, 0xa1, 0x51,
    0x02, 0x00, 0x59, 0x55,
    0x04, 0x56, 0x5e, 0xed,
    0x31, 0x55, 0xb5, 0x04,
    0x9d, 0xf6, 0x1c, 0x40,
    0x7b, 0xb7, 0x00, 0x0c,
    0xd9, 0x7b, 0x5d, 0x13,
    0x4c, 0xeb, 0x7d, 0xf1,
    0x74, 0xf8, 0xd5, 0xb9,
    0x07, 0xda, 0x18, 0x19,
    0x92
)
