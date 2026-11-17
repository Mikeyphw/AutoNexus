package io.github.mikeyphw.autonexus.termuxtasker

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class ScriptExplorerTest {
    @Test fun missingFolderIsUnavailableNotInvented() {
        val root = Files.createTempDirectory("tt-e2-missing-")
        try {
            val listing = ScriptExplorer.browse(root.toFile(), root.resolve("missing").toFile())
            assertFalse(listing.available)
            assertTrue(listing.entries.isEmpty())
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun listsOnlyBoundedRegularFilesAndDirectories() {
        val root = Files.createTempDirectory("tt-e2-list-")
        try {
            Files.createDirectory(root.resolve("subdir"))
            Files.writeString(root.resolve("example.sh"), "SECRET FILE BODY NOT READ")
            val listing = ScriptExplorer.browse(root.toFile(), root.toFile())
            assertTrue(listing.available)
            assertEquals(listOf("subdir", "example.sh"), listing.entries.map { it.label })
            assertEquals(ScriptExplorer.Kind.DIRECTORY,listing.entries[0].kind)
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun refusesParentTraversal() {
        val root = Files.createTempDirectory("tt-e2-root-")
        val elsewhere = Files.createTempDirectory("tt-e2-other-")
        try {
            val listing = ScriptExplorer.browse(root.toFile(), elsewhere.toFile())
            assertFalse(listing.available)
            assertTrue(listing.message.contains("outside"))
        } finally { root.toFile().deleteRecursively(); elsewhere.toFile().deleteRecursively() }
    }
    @Test fun refusesSymlinkEscapeAndDoesNotListSymlink() {
        val root = Files.createTempDirectory("tt-e2-root-")
        val elsewhere = Files.createTempDirectory("tt-e2-other-")
        try {
            Files.writeString(elsewhere.resolve("secret.sh"), "KEEP PRIVATE")
            Files.createSymbolicLink(root.resolve("escape"), elsewhere)
            val listing = ScriptExplorer.browse(root.toFile(), root.toFile())
            assertTrue(listing.available)
            assertTrue(listing.entries.isEmpty())
            assertFalse(ScriptExplorer.browse(root.toFile(),root.resolve("escape").toFile()).available)
        } finally { root.toFile().deleteRecursively(); elsewhere.toFile().deleteRecursively() }
    }
    @Test fun limitsDirectoryOutput() {
        val root = Files.createTempDirectory("tt-e2-many-")
        try {
            repeat(ScriptExplorer.MAX_ENTRIES + 8) { Files.writeString(root.resolve("f$it"), "") }
            val list = ScriptExplorer.browse(root.toFile(), root.toFile())
            assertTrue(list.available)
            assertEquals(ScriptExplorer.MAX_ENTRIES,list.entries.size)
            assertTrue(list.truncated)
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun symlinkWithinRootStillCannotBeTraversed() {
        val root = Files.createTempDirectory("tt-e2-internal-link-")
        try {
            val child = Files.createDirectory(root.resolve("real"))
            Files.writeString(child.resolve("safe.sh"), "not executed")
            val link = Files.createSymbolicLink(root.resolve("alias"), child)
            val listing = ScriptExplorer.browse(root.toFile(), link.toFile())
            assertFalse(listing.available)
            assertTrue(listing.message.contains("Symlink"))
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun skippedSymlinksCannotBypassTotalInspectionBound() {
        val root = Files.createTempDirectory("tt-e2-many-skips-")
        try {
            // Valid file comes after many skipped entries; do not scan unlimited
            // directory entries trying to fill all 60 display slots.
            val target = Files.writeString(root.resolve("real.sh"), "")
            repeat(ScriptExplorer.MAX_INSPECTED + 40) {
                Files.createSymbolicLink(root.resolve("skip${it.toString().padStart(5,'0')}"), target)
            }
            val listing = ScriptExplorer.browse(root.toFile(), root.toFile())
            assertTrue(listing.available)
            assertTrue(listing.entries.size <= ScriptExplorer.MAX_ENTRIES)
            assertTrue(listing.truncated)
            assertTrue(listing.message.contains("256"))
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun deepTraversalRefuses() {
        val root = Files.createTempDirectory("tt-e2-deep-")
        try {
            var child = root
            repeat(ScriptExplorer.MAX_DEPTH + 1) { child = Files.createDirectory(child.resolve("d$it")) }
            assertFalse(ScriptExplorer.browse(root.toFile(), child.toFile()).available)
        } finally { root.toFile().deleteRecursively() }
    }
}
