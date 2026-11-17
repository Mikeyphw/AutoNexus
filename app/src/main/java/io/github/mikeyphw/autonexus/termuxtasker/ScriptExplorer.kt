package io.github.mikeyphw.autonexus.termuxtasker

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/**
 * Read-only, explicit-user-request local explorer. It never escalates access,
 * traverses symlinks, reads file bodies, or scans automatically.
 * A different Android UID may not be able to open Termux private files.
 */
object ScriptExplorer {
    const val TERMUX_SCRIPTS = "/data/data/com.termux/files/home/.termux/tasker"
    const val MAX_ENTRIES = 60
    // Bound *inspected* entries too, including skipped symlinks and odd files.
    const val MAX_INSPECTED = 256
    const val MAX_DEPTH = 5
    enum class Kind { DIRECTORY, REGULAR_FILE }
    data class Entry(val label: String, val path: String, val kind: Kind)
    data class Listing(val available: Boolean, val entries: List<Entry>,
                       val truncated: Boolean, val message: String)

    private fun inside(root: File, candidate: File): Boolean =
        candidate.path == root.path || candidate.path.startsWith(root.path + File.separator)

    fun browse(root: File, folder: File): Listing {
        fun unavailable(message: String) = Listing(false, emptyList(), false, message)
        val canonicalRoot: File
        val canonicalFolder: File
        try {
            canonicalRoot = root.canonicalFile
            canonicalFolder = folder.canonicalFile
        } catch (_: Exception) { return unavailable("Cannot resolve folder safely; use Original editor.") }
        if (!inside(canonicalRoot, canonicalFolder))
            return unavailable("Folder is outside the approved script directory.")
        // Canonical containment alone permits a symlink which points back inside
        // the root; reject such intermediate components instead of following them.
        try {
            val rootPath = root.toPath().toAbsolutePath().normalize()
            val folderPath = folder.toPath().toAbsolutePath().normalize()
            if (!folderPath.startsWith(rootPath))
                return unavailable("Folder is outside the approved script directory.")
            var segment = rootPath
            for (part in rootPath.relativize(folderPath)) {
                segment = segment.resolve(part)
                if (Files.isSymbolicLink(segment))
                    return unavailable("Symlink traversal is not supported; enter the path manually.")
            }
        } catch (_: Exception) { return unavailable("Cannot inspect folder path safely.") }
        val relative = canonicalFolder.path.removePrefix(canonicalRoot.path).trimStart(File.separatorChar)
        if (relative.isNotEmpty() && relative.split(File.separatorChar).size > MAX_DEPTH)
            return unavailable("Maximum browse depth reached; enter the script path manually.")
        if (!canonicalRoot.isDirectory || !canonicalFolder.isDirectory || !canonicalFolder.canRead())
            return unavailable("Termux scripts are not accessible to this plugin process. Use Original or enter the path manually; no root is attempted.")
        val items = ArrayList<Entry>()
        var more = false
        var inspected = 0
        try {
            Files.newDirectoryStream(canonicalFolder.toPath()).use { stream ->
                for (entry in stream) {
                    if (inspected >= MAX_INSPECTED) { more = true; break }
                    inspected++
                    if (Files.isSymbolicLink(entry)) continue
                    val child = try { entry.toFile().canonicalFile } catch (_: Exception) { continue }
                    if (!inside(canonicalRoot, child) || child.name == "." || child.name == "..") continue
                    val kind = when {
                        Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS) && child.canRead() -> Kind.DIRECTORY
                        Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS) && child.canRead() -> Kind.REGULAR_FILE
                        else -> continue
                    }
                    if (items.size >= MAX_ENTRIES) { more = true; break }
                    items.add(Entry(child.name, child.absolutePath, kind))
                }
            }
        } catch (_: Exception) { return unavailable("The directory could not be enumerated with current app permissions.") }
        return Listing(true, items.sortedWith(compareBy<Entry> { it.kind != Kind.DIRECTORY }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }), more,
            if (more) "Limited scan: at most $MAX_INSPECTED entries inspected and $MAX_ENTRIES displayed; no files were opened." else
                "Read-only listing; select explicitly to stage a path.")
    }
}
