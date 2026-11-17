package io.github.mikeyphw.autonexus.termuxtasker

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class EditorPathSuggestionsTest {
    @Test fun blankAndRelativeQueriesStayInApprovedRoot() {
        val root = File("/data/data/com.termux/files/home/.termux/tasker")
        assertEquals(root, EditorPathSuggestions.folderForQuery(root, ""))
        assertEquals(root, EditorPathSuggestions.folderForQuery(root, "prefix"))
        assertEquals(File(root,"sub"), EditorPathSuggestions.folderForQuery(root,"sub/hi"))
    }
    @Test fun variablesAndHugeQueriesNeverEnumerate() {
        val root = File("/tmp/tasker")
        assertNull(EditorPathSuggestions.folderForQuery(root,"%var/script"))
        assertNull(EditorPathSuggestions.folderForQuery(root,"x".repeat(1025)))
        assertNull(EditorPathSuggestions.folderForQuery(root,"a\u0000b"))
    }
    @Test fun typedFilterIsBoundedAndDirectorySelectionDistinct() {
        val root = Files.createTempDirectory("autocomplete-").toFile()
        try {
            val contents = (0 until 70).map {
                ScriptExplorer.Entry("script$it", File(root,"script$it").path, ScriptExplorer.Kind.REGULAR_FILE)
            } + ScriptExplorer.Entry("subfolder", File(root,"subfolder").path, ScriptExplorer.Kind.DIRECTORY)
            val listing = ScriptExplorer.Listing(true, contents, false, "read only")
            assertEquals(40, EditorPathSuggestions.displayOptions(root,root,"script",listing,false).size)
            assertEquals(listOf("subfolder"), EditorPathSuggestions.displayOptions(root,root,"s",listing,true))
            assertTrue(EditorPathSuggestions.displayOptions(root,root,"%variable",listing,false).isEmpty())
        } finally { root.deleteRecursively() }
    }
    @Test fun inaccessibleAndOutsideRootCannotSuggest() {
        val root = Files.createTempDirectory("autocomplete-").toFile()
        val outside = Files.createTempDirectory("autocomplete-outside-").toFile()
        try {
            val listing = ScriptExplorer.Listing(true, listOf(
                ScriptExplorer.Entry("hidden", File(outside,"hidden").path, ScriptExplorer.Kind.REGULAR_FILE)), false, "")
            assertTrue(EditorPathSuggestions.displayOptions(root,outside,"",listing,false).isEmpty())
            assertTrue(EditorPathSuggestions.displayOptions(root,root,"",
                listing.copy(available=false),false).isEmpty())
        } finally { root.deleteRecursively();outside.deleteRecursively() }
    }
}
