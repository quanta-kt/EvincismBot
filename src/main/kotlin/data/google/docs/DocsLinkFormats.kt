package data.google.docs

object DocsLinkFormats {

    private const val DOC_LINK_FORMAT = "https://docs.google.com/document/d/%s/"
    private val DOC_LINK_REGEX = "https://docs\\.google\\.com/document/d/(.*)/?".toRegex()

    /**
     * Get a Google doc URL from it's document ID
     */
    fun googleDocsLink(docId: String): String = DOC_LINK_FORMAT.format(docId)

    /**
     * Get Google doc document ID from it's URL
     */
    fun googleDocsId(docLink: String) = DOC_LINK_REGEX.find(docLink)?.groupValues?.first()
}