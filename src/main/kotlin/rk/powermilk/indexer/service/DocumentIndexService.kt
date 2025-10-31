package rk.powermilk.indexer.service

/**
 * Service for indexing text documents and performing word queries.
 */
class DocumentIndexService {
    // Stores document name -> original content
    private val documents = mutableMapOf<String, String>()

    // Inverted index: normalized word -> set of document names
    private val wordIndex = mutableMapOf<String, MutableSet<String>>()

    /**
     * Registers a document with the given name and content.
     * If a document with the same name exists, it will be replaced.
     */
    fun registerDocument(name: String, content: String) {
        // Remove old document if it exists
        if (documents.containsKey(name)) {
            removeDocument(name)
        }

        // Store the document
        documents[name] = content

        // Extract and index words
        extractWords(content).forEach {
            wordIndex.getOrPut(it) { mutableSetOf() }.add(name)
        }
    }

    /**
     * Removes a document by name.
     * Does nothing if the document doesn't exist.
     */
    fun removeDocument(name: String) {
        val content = documents.remove(name) ?: return

        // Remove document from all word indices
        val words = extractWords(content)
        for (word in words) {
            wordIndex[word]?.remove(name)
            // Clean up empty sets
            if (wordIndex[word]?.isEmpty() == true) {
                wordIndex.remove(word)
            }
        }
    }

    /**
     * Queries for documents containing the given word.
     * Returns a set of document names (case-insensitive matching).
     */
    fun query(word: String): Set<String> {
        val normalizedWord = normalizeWord(word)
        return wordIndex[normalizedWord]?.toSet() ?: emptySet()
    }

    fun cleanAll() {
        documents.clear()
        wordIndex.clear()
    }

    /**
     * Extracts and normalizes all words from the text.
     */
    private fun extractWords(text: String): Set<String> {
        return text
            .split(Regex("[^\\p{L}\\p{N}]+"))  // Split on: NOT (letters OR digits)
            .filter { it.isNotEmpty() }
            .map { normalizeWord(it) }
            .toSet()
    }

    /**
     * Normalizes a word for indexing (lowercase, handles Unicode).
     */
    private fun normalizeWord(word: String): String {
        return word.lowercase()
    }
}
