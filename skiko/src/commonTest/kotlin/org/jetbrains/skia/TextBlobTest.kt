package org.jetbrains.skia

import org.jetbrains.skia.tests.assertCloseEnough
import org.jetbrains.skia.tests.assertContentCloseEnough
import org.jetbrains.skia.tests.makeFromResource
import org.jetbrains.skiko.kotlinBackend
import org.jetbrains.skiko.tests.runTest
import kotlin.test.*

class TextBlobTest {

    private val inter36: suspend () -> Font = suspend {
        Font(Typeface.makeFromResource("./fonts/Inter-Hinted-Regular.ttf"), 36f)
    }

    private val eps = 0.001f // only this value works for kotlin/js

    @Test
    fun canMakeFromPos() = runTest {
        val glyphs = shortArrayOf(1983, 830, 1213, 1205, 638, 1231, 1326, 161, 611, 721, 721, 773, 1326)

        val positions = listOf(
            0.0f, 0.0f, 26.0f, 0.0f, 48.0f, 0.0f, 69.0f, 0.0f, 89.0f, 0.0f, 109.28409f, 0.0f, 128.28409f, 0.0f,
            138.28409f, 0.0f, 165.28409f, 0.0f, 186.28409f, 0.0f, 195.28409f, 0.0f, 204.28409f, 0.0f, 225.28409f, 0.0f
        ).chunked(2).map { Point(it[0], it[1]) }.toTypedArray()

        val textBlob = TextBlob.makeFromPos(
            glyphs = glyphs,
            pos = positions,
            font = inter36()
        )!!

        assertNotEquals(
            illegal = 0,
            actual = textBlob.uniqueId,
            message = "uniqueId should return a non-zero value unique among all text blobs."
        )

        assertContentEquals(
            expected = glyphs,
            actual = textBlob.glyphs
        )

        assertContentEquals(
            expected = Point.flattenArray(positions),
            actual = textBlob.positions
        )

        assertCloseEnough(
            expected = Rect(-26.59091f, -39.272827f, 318.27557f, 11.505432f),
            actual = textBlob.bounds,
            epsilon = eps
        )

        assertContentCloseEnough(
            expected = floatArrayOf(
                3.2215908f, 19.585226f, 28.761364f, 40.38493f, 50.761364f, 63.698864f, 71.76136f, 81.017044f, 97.01448f,
                102.99094f, 132.33693f, 134.25397f, 141.45454f, 158.58522f, 173.29857f, 179.27502f, 212.21196f,
                217.8335f, 229.33693f, 231.25397f
            ),
            actual = textBlob.getIntercepts(lowerBound = 0f, upperBound = 1f),
            epsilon = 0.02f // smaller values don't work on k/js :(
        )

        if (kotlinBackend.isNotJs()) {
            assertCloseEnough(
                expected = Rect(2f, -28f, 234.97159f, 9f),
                actual = textBlob.tightBounds,
                epsilon = eps
            )
        } else {
            //TODO(karpovich): can we avoid such a difference between targets?
            assertEquals(
                expected = Rect(3f, -26f, 234f, 7f),
                actual = textBlob.tightBounds,
            )
        }

        assertCloseEnough(
            expected = Rect(0f, -34.875f, 235.30681f, 8.692932f),
            actual = textBlob.blockBounds.also { println(it) },
            epsilon = 0.2f // smaller values don't work on k/js :(
        )

        assertEquals(0f, textBlob.firstBaseline)
        assertEquals(0f, textBlob.lastBaseline)

        val data = textBlob.serializeToData()
        val blobFromData = TextBlob.makeFromData(data)!!
        assertContentEquals(expected = glyphs, actual = blobFromData.glyphs)
        assertContentEquals(expected = Point.flattenArray(positions), actual = blobFromData.positions)

//        assertFailsWith<IllegalArgumentException> {
//            textBlob.clusters
//        }
    }

    @Test
    fun canMakeFromPosH() = runTest {
        val glyphs = shortArrayOf(1983, 830, 1213, 1205, 638, 1231, 1326, 161, 611, 721, 721, 773, 1326)

        val positions = listOf(
            0.0f, 0.0f, 26.0f, 0.0f, 48.0f, 0.0f, 69.0f, 0.0f, 89.0f, 0.0f, 109.28409f, 0.0f, 128.28409f, 0.0f,
            138.28409f, 0.0f, 165.28409f, 0.0f, 186.28409f, 0.0f, 195.28409f, 0.0f, 204.28409f, 0.0f, 225.28409f, 0.0f
        ).filterIndexed {
            // remove y, leave only x (horizontal positions)
                ix, _ ->
            ix % 2 == 0
        }.toTypedArray().toFloatArray()

        val textBlob = TextBlob.makeFromPosH(
            glyphs = glyphs,
            xpos = positions,
            ypos = 1f,
            font = inter36()
        )!!

        assertNotEquals(
            illegal = 0,
            actual = textBlob.uniqueId,
            message = "uniqueId should return a non-zero value unique among all text blobs."
        )

        assertNotEquals(
            illegal = 0,
            actual = textBlob.uniqueId,
            message = "uniqueId should return a non-zero value unique among all text blobs."
        )

        assertContentEquals(
            expected = glyphs,
            actual = textBlob.glyphs
        )

        assertContentEquals(
            expected = positions,
            actual = textBlob.positions
        )

        assertCloseEnough(
            expected = Rect(-26.59091f, -38.272827f, 318.27557f, 12.505432f),
            actual = textBlob.bounds,
            epsilon = eps
        )

        assertContentCloseEnough(
            expected = floatArrayOf(
                3.2215908f, 22.75568f, 28.761364f, 42.330772f, 50.761364f, 66.71591f, 71.76136f, 85.03906f, 94.86898f,
                105.11858f, 117.41477f, 120.431816f, 131.26941f, 135.32149f, 141.45454f, 161.75568f, 171.15308f,
                181.40268f, 189.04546f, 192.0625f, 198.04546f, 201.0625f, 210.14375f, 219.9017f, 228.26941f, 232.32149f
            ),
            actual = textBlob.getIntercepts(lowerBound = 0f, upperBound = 1f)!!,
            epsilon = 0.02f // smaller values don't work on k/js :(
        )

        assertFailsWith<IllegalArgumentException> { textBlob.tightBounds }
        assertFailsWith<IllegalArgumentException> { textBlob.blockBounds }
        assertFailsWith<IllegalArgumentException> { textBlob.firstBaseline }
        assertFailsWith<IllegalArgumentException> { textBlob.lastBaseline }

        val data = textBlob.serializeToData()
        val blobFromData = TextBlob.makeFromData(data)!!
        assertContentEquals(expected = glyphs, actual = blobFromData.glyphs)
        assertContentEquals(expected = positions, actual = blobFromData.positions)
    }
}