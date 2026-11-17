package io.github.mikeyphw.autonexus.termuxtasker

import org.junit.Assert.*
import org.junit.Test

class EditorDraftBridgeTest {
    private val launch = EditorDraft("old.sh", "--old", "~/old", "old stdin", "1", "3", false, true)

    @Test fun discardEnhancedDraftAndReadNewStockOnReentry() {
        val mode = EditorDraftBridge(launch)
        val unsaved = launch.copy(arguments = "--unsaved")
        mode.stage(unsaved)
        assertTrue(mode.needsDiscardConfirmation(unsaved, launch))
        mode.returnToOriginal(launch)
        assertFalse(mode.enhanced)
        assertEquals(launch, mode.draft)
        val stockAfterOriginal = launch.copy(executable = "edited.sh", arguments = "--new", stdin = "new stdin",
            sessionAction = "%par1", logLevel = "6", inTerminal = true, waitForResult = false)
        mode.reopenEnhanced(stockAfterOriginal)
        assertTrue(mode.enhanced)
        assertEquals(stockAfterOriginal, mode.draft)
        assertNotEquals(unsaved, mode.draft)
    }

    @Test fun switchConfirmationComparesAgainstLatestStockNotActivityLaunch() {
        val mode = EditorDraftBridge(launch)
        val editedOriginal = launch.copy(arguments = "changed in original")
        mode.returnToOriginal(launch)
        mode.reopenEnhanced(editedOriginal)
        assertFalse(mode.needsDiscardConfirmation(editedOriginal, editedOriginal))
        assertTrue(mode.needsDiscardConfirmation(launch, editedOriginal))
    }

    @Test fun discardDoesNotOverwriteCurrentOriginal() {
        val mode = EditorDraftBridge(launch)
        val changedInStock = launch.copy(stdin = "stock", waitForResult = false)
        mode.stage(launch.copy(stdin = "unsaved enhanced"))
        mode.returnToOriginal(changedInStock)
        assertEquals(changedInStock, mode.draft)
        mode.reopenEnhanced(changedInStock)
        assertEquals(changedInStock, mode.draft)
    }
}
