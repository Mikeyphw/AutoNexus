package io.github.mikeyphw.autonexus.termuxtasker

/**
 * One editor session has exactly one draft owner. Switching to the original
 * UI abandons enhanced-only edits; reopening Enhanced snapshots CURRENT stock
 * widgets, not the activity-launch snapshot. Never writes a Tasker bundle.
 */
internal class EditorDraftBridge(initial: EditorDraft) {
    var draft: EditorDraft = initial
        private set
    var enhanced: Boolean = true
        private set

    fun stage(edited: EditorDraft) { draft = edited }

    fun needsDiscardConfirmation(edited: EditorDraft, stockNow: EditorDraft): Boolean =
        edited != stockNow

    fun returnToOriginal(stockNow: EditorDraft) {
        draft = stockNow
        enhanced = false
    }

    fun reopenEnhanced(stockNow: EditorDraft) {
        // Explicitly re-snapshot all fields including stdin, flags, and raw mode values.
        draft = stockNow
        enhanced = true
    }
}
