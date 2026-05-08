package app.paralino.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CipherAlgTest {

    // test data is hardcoded to mimic actual payload and format CipherAlg expects and outputs

    @Test
    fun decode_withoutPrefix_returnsDefaultCipherAlg() {
        val decoded = CipherAlg.decode("rawpayload")
        assertEquals(CipherAlgType.DEFAULT, decoded.algorithm)
        assertEquals(EncodingType.BASE64_URL, decoded.encodingType)
        assertNull(decoded.payload)
        assertEquals("rawpayload", decoded.data)
    }

    @Test
    fun encode_skipTagsIfDefault_stripsTagsForDefaultWithoutPayload() {
        val cipher = CipherAlg(
            encodingType = EncodingType.BASE64_URL,
            algType = CipherAlgType.DEFAULT,
            payload = null,
            data = "data"
        )

        assertEquals("data", cipher.encode(skipTagsIfDefault = true))
        assertEquals("@:${EncodingType.BASE64_URL.key}:${CipherAlgType.DEFAULT.keys.first()}:@data", cipher.encode(skipTagsIfDefault = false))
    }

    @Test
    fun encode_withPayload_alwaysKeepsTags() {
        val cipher = CipherAlg(
            encodingType = EncodingType.BASE64_URL,
            algType = CipherAlgType.DEFAULT_KEY_DERIVE,
            payload = "payload",
            data = "data"
        )

        assertEquals("@:${EncodingType.BASE64_URL.key}:${CipherAlgType.DEFAULT_KEY_DERIVE.keys.first()}:payload:@data", cipher.encode(skipTagsIfDefault = true))
        assertEquals("@:${EncodingType.BASE64_URL.key}:${CipherAlgType.DEFAULT_KEY_DERIVE.keys.first()}:payload:@data", cipher.encode(skipTagsIfDefault = false))
    }

    @Test
    fun decode_withTags_parsesEncodingAlgAndOptionalPayload() {
        val encodedNoPayload = "@:${EncodingType.HEX.key}:${CipherAlgType.DEFAULT.keys.first()}:@ABC"
        val decodedNoPayload = CipherAlg.decode(encodedNoPayload)
        assertEquals(EncodingType.HEX, decodedNoPayload.encodingType)
        assertEquals(CipherAlgType.DEFAULT, decodedNoPayload.algorithm)
        assertNull(decodedNoPayload.payload)
        assertEquals("ABC", decodedNoPayload.data)

        val encodedPayload = "@:${EncodingType.BASE64_URL.key}:${CipherAlgType.DEFAULT_KEY_DERIVE.keys.first()}:p:@XYZ"
        val decodedPayload = CipherAlg.decode(encodedPayload)
        assertEquals(EncodingType.BASE64_URL, decodedPayload.encodingType)
        assertEquals(CipherAlgType.DEFAULT_KEY_DERIVE, decodedPayload.algorithm)
        assertEquals("p", decodedPayload.payload)
        assertEquals("XYZ", decodedPayload.data)
    }

    @Test
    fun decode_malformedPrefix_fallsBackToDefault() {
        // Missing :@ marker
        val decoded1 = CipherAlg.decode("@:b64u:xcp:data")
        assertEquals(CipherAlgType.DEFAULT, decoded1.algorithm)
        assertEquals(EncodingType.BASE64_URL, decoded1.encodingType)
        assertEquals("@:b64u:xcp:data", decoded1.data)

        // Too few metadata parts
        val decoded2 = CipherAlg.decode("@:onlyone:@data")
        assertEquals(CipherAlgType.DEFAULT, decoded2.algorithm)
        assertEquals(EncodingType.BASE64_URL, decoded2.encodingType)
        assertEquals("@:onlyone:@data", decoded2.data)
    }
}

