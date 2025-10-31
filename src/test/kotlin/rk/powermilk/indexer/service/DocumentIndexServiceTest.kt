package rk.powermilk.indexer.service

import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentIndexServiceTest {

    private lateinit var service: DocumentIndexService

    @BeforeTest
    fun setup() {
        service = DocumentIndexService()
    }

    @AfterTest
    fun tearDown() {
        service.cleanAll()
    }

    @Test
    fun `should register and query single document`() {
        service.registerDocument("doc1", "hello world")

        assertEquals(setOf("doc1"), service.query("hello"))
        assertEquals(setOf("doc1"), service.query("world"))
    }

    @Test
    fun `should query multiple documents`() {
        service.registerDocument("doc1", "hello world")
        service.registerDocument("doc2", "hello kotlin")
        service.registerDocument("doc3", "goodbye world")

        assertEquals(setOf("doc1", "doc2"), service.query("hello"))
        assertEquals(setOf("doc1", "doc3"), service.query("world"))
        assertEquals(setOf("doc2"), service.query("kotlin"))
    }

    @Test
    fun `should return empty set for non-existent word`() {
        service.registerDocument("doc1", "hello world")

        assertEquals(emptySet(), service.query("missing"))
        assertEquals(emptySet(), service.query(""))
    }

    // ============ Case insensitivity ============

    @Test
    fun `should be case insensitive for indexing`() {
        service.registerDocument("doc1", "Hello WORLD HeLLo")

        assertEquals(setOf("doc1"), service.query("hello"))
        assertEquals(setOf("doc1"), service.query("HELLO"))
        assertEquals(setOf("doc1"), service.query("Hello"))
    }

    @Test
    fun `should be case insensitive for queries`() {
        service.registerDocument("doc1", "hello world")

        assertEquals(setOf("doc1"), service.query("HELLO"))
        assertEquals(setOf("doc1"), service.query("Hello"))
        assertEquals(setOf("doc1"), service.query("hello"))
        assertEquals(setOf("doc1"), service.query("WORLD"))
    }

    // ============ Punctuation handling ============

    @Test
    fun `should ignore punctuation`() {
        service.registerDocument("doc1", "hello, world! How are you?")

        assertEquals(setOf("doc1"), service.query("hello"))
        assertEquals(setOf("doc1"), service.query("world"))
        assertEquals(setOf("doc1"), service.query("how"))
        assertEquals(setOf("doc1"), service.query("are"))
        assertEquals(setOf("doc1"), service.query("you"))
    }

    @Test
    fun `should handle various punctuation marks`() {
        service.registerDocument("doc1", "hello...world---test@email.com")

        assertEquals(setOf("doc1"), service.query("hello"))
        assertEquals(setOf("doc1"), service.query("world"))
        assertEquals(setOf("doc1"), service.query("test"))
        assertEquals(setOf("doc1"), service.query("email"))
        assertEquals(setOf("doc1"), service.query("com"))
    }

    // ============ Letters and digits ============

    @Test
    fun `should treat digits as part of words`() {
        service.registerDocument("doc1", "COVID19 is a disease")

        assertEquals(setOf("doc1"), service.query("covid19"))
        assertEquals(setOf("doc1"), service.query("COVID19"))
    }

    @Test
    fun `should handle numbers with letters`() {
        service.registerDocument("doc1", "Windows 11 is version11 of Windows11Pro")

        assertEquals(setOf("doc1"), service.query("windows"))
        assertEquals(setOf("doc1"), service.query("11"))
        assertEquals(setOf("doc1"), service.query("version11"))
        assertEquals(setOf("doc1"), service.query("windows11pro"))
    }

    // ============ Empty documents ============

    @Test
    fun `should handle empty document`() {
        service.registerDocument("empty", "")

        // Should not crash
        assertEquals(emptySet(), service.query("anything"))
    }

    @Test
    fun `should handle document with only punctuation`() {
        service.registerDocument("punctuation", "!@#$%^&*()")

        assertEquals(emptySet(), service.query("anything"))
    }

    @Test
    fun `should handle document with only whitespace`() {
        service.registerDocument("whitespace", "   \n\t  ")

        assertEquals(emptySet(), service.query("anything"))
    }

    // ============ Duplicate words within document ============

    @Test
    fun `should not return duplicate document names for duplicate words`() {
        service.registerDocument("doc1", "hello hello hello world world")

        val results = service.query("hello")
        assertEquals(setOf("doc1"), results)
        assertEquals(1, results.size) // Ensure no duplicates
    }

    @Test
    fun `should handle massive duplication`() {
        val content = "repeat ".repeat(1000) + "once"
        service.registerDocument("doc1", content)

        assertEquals(setOf("doc1"), service.query("repeat"))
        assertEquals(setOf("doc1"), service.query("once"))
    }

    // ============ Unicode handling ============

    @Test
    fun `should handle Unicode accented characters`() {
        service.registerDocument("doc1", "naïve café résumé")

        assertEquals(setOf("doc1"), service.query("naïve"))
        assertEquals(setOf("doc1"), service.query("café"))
        assertEquals(setOf("doc1"), service.query("résumé"))
    }

    @Test
    fun `should handle various Unicode scripts`() {
        service.registerDocument("doc1", "Hello мир 世界 مرحبا")

        assertEquals(setOf("doc1"), service.query("hello"))
        assertEquals(setOf("doc1"), service.query("мир"))      // Cyrillic
        assertEquals(setOf("doc1"), service.query("世界"))      // Chinese
        assertEquals(setOf("doc1"), service.query("مرحبا"))    // Arabic
    }

    @Test
    fun `should normalize Unicode for case insensitivity`() {
        service.registerDocument("doc1", "Café CAFÉ café")

        assertEquals(setOf("doc1"), service.query("café"))
        assertEquals(setOf("doc1"), service.query("CAFÉ"))
        assertEquals(setOf("doc1"), service.query("Café"))
    }

    @Test
    fun `should handle emoji and special Unicode`() {
        service.registerDocument("doc1", "hello 😀 world ❤️ test")

        assertEquals(setOf("doc1"), service.query("hello"))
        assertEquals(setOf("doc1"), service.query("world"))
        assertEquals(setOf("doc1"), service.query("test"))
        // Emoji are treated as non-letters (separators)
    }

    // ============ Document removal ============

    @Test
    fun `should remove document and update index`() {
        service.registerDocument("doc1", "hello world")
        service.registerDocument("doc2", "hello kotlin")

        assertEquals(setOf("doc1", "doc2"), service.query("hello"))

        service.removeDocument("doc1")

        assertEquals(setOf("doc2"), service.query("hello"))
        assertEquals(emptySet(), service.query("world"))
    }

    @Test
    fun `should not crash when removing non-existent document`() {
        // Should not throw exception
        assertDoesNotThrow {
            service.removeDocument("nonexistent")
        }
    }

    @Test
    fun `should handle removing document multiple times`() {
        service.registerDocument("doc1", "hello world")

        assertDoesNotThrow {
            service.removeDocument("doc1")
            service.removeDocument("doc1") // Second removal
            service.removeDocument("doc1") // Third removal
        }

        assertEquals(emptySet(), service.query("hello"))
    }

    @Test
    fun `should remove document from empty service`() {
        assertDoesNotThrow {
            service.removeDocument("anything")
        }
    }

    // ============ Document replacement ============

    @Test
    fun `should replace document with same name`() {
        service.registerDocument("doc1", "hello world")
        assertEquals(setOf("doc1"), service.query("hello"))
        assertEquals(setOf("doc1"), service.query("world"))

        service.registerDocument("doc1", "goodbye kotlin")

        assertEquals(emptySet(), service.query("hello"))
        assertEquals(emptySet(), service.query("world"))
        assertEquals(setOf("doc1"), service.query("goodbye"))
        assertEquals(setOf("doc1"), service.query("kotlin"))
    }

    @Test
    fun `should properly clean up index when replacing document`() {
        service.registerDocument("doc1", "unique1 unique2")
        service.registerDocument("doc2", "shared unique3")

        // Replace doc1 with different content
        service.registerDocument("doc1", "shared unique4")

        assertEquals(emptySet(), service.query("unique1"))
        assertEquals(emptySet(), service.query("unique2"))
        assertEquals(setOf("doc2"), service.query("unique3"))
        assertEquals(setOf("doc1"), service.query("unique4"))
        assertEquals(setOf("doc1", "doc2"), service.query("shared"))
    }

    // ============ Complex scenarios ============

    @Test
    fun `should handle real-world text`() {
        service.registerDocument("doc1", """
            Hello, World! This is a test.
            COVID-19 affected everyone in 2020-2021.
            Let's meet at café "Naïve" at 3:30pm.
            Email me at: test@example.com
        """.trimIndent())

        assertEquals(setOf("doc1"), service.query("hello"))
        assertEquals(setOf("doc1"), service.query("covid"))
        assertEquals(setOf("doc1"), service.query("19"))
        assertEquals(setOf("doc1"), service.query("2020"))
        assertEquals(setOf("doc1"), service.query("café"))
        assertEquals(setOf("doc1"), service.query("naïve"))
        assertEquals(setOf("doc1"), service.query("test"))
        assertEquals(setOf("doc1"), service.query("example"))
    }

    @Test
    fun `should handle edge case combinations`() {
        service.registerDocument("empty", "")
        service.registerDocument("punctuation", "!!!")
        service.registerDocument("mixed", "Hello123 WORLD456 test789")
        service.registerDocument("unicode", "Ñoño naïve")

        assertEquals(emptySet(), service.query("hello"))
        assertEquals(setOf("mixed"), service.query("hello123"))
        assertEquals(setOf("mixed"), service.query("world456"))
        assertEquals(setOf("unicode"), service.query("ñoño"))
        assertEquals(setOf("unicode"), service.query("ÑOÑO"))
    }
}
