package io.github.mikeyphw.autonexus.termuxtasker

import java.io.File

/** The original Termux:Tasker editor still owns actual path acceptance.
 * This bounded, display-only model never executes, canonicalizes a staged
 * field, follows symlinks, or uses root privileges. */
object EditorPathSuggestions {
    const val MAX_OPTIONS = 40
    const val MAX_QUERY_CHARS = 1024

    fun folderForQuery(root: File, query: String): File? {
        if (query.length > MAX_QUERY_CHARS || query.contains('%') || query.contains('\u0000')) return null
        if (query.isBlank()) return root
        val entered = File(query)
        val path = if (entered.isAbsolute) entered else File(root, query)
        return if (query.endsWith(File.separator)) path else (path.parentFile ?: root)
    }

    fun displayOptions(root: File, folder: File, query: String,
                       listing: ScriptExplorer.Listing, directoryOnly: Boolean): List<String> {
        if (!listing.available) return emptyList()
        if (query.length > MAX_QUERY_CHARS || query.contains('%')) return emptyList()
        val isAbsolute = File(query).isAbsolute
        val prefix = query.substringAfterLast(File.separatorChar)
        val rootPath = root.canonicalFile.toPath()
        val folderPath = try { folder.canonicalFile.toPath() } catch (_: Exception) { return emptyList() }
        if (!folderPath.startsWith(rootPath)) return emptyList()
        val relativeFolder = rootPath.relativize(folderPath).toString()
        return listing.entries.asSequence()
            .filter { !directoryOnly || it.kind == ScriptExplorer.Kind.DIRECTORY }
            .filter { it.label.startsWith(prefix, ignoreCase = true) }
            .take(MAX_OPTIONS)
            .map { item ->
                if (isAbsolute) item.path
                else if (relativeFolder.isBlank()) item.label
                else relativeFolder + File.separator + item.label
            }
            .toList()
    }
}
