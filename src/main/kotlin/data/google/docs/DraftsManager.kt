package data.google.docs

import GlobalConfig
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.docs.v1.Docs
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import data.models.Draft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Abstraction over Google Docs, Sheets and Drive APIs providing drafts and rewrites management
 */
class DraftsManager {
    companion object {
        private const val APP_NAME = "EvincismBot/1.0"

        private const val DOCS_MIME_TYPE = "application/vnd.google-apps.document"

        private val DRAFTS_QUERY = "mimeType='$DOCS_MIME_TYPE' and '${GlobalConfig.DRAFTS_FOLDER_ID}' in parents"
        private val REWRITES_QUERY = "mimeType='$DOCS_MIME_TYPE' and '${GlobalConfig.REWRITES_FOLDER_ID}' in parents"

        private const val DRAFTS_SHEET_DATA_RANGE = "A2:D"
        private const val REWRITES_SHEET_DATA_RANGE = "A2:D"

        private val AUTHOR_REGEX = "\\[(.*?)]".toRegex()
        private val STATUS_REGEX = "\\[c]".toRegex()

        private const val SHEETS_BOOLEAN_TRUE = "TRUE"
    }

    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

    private val credentials = GoogleCredentials.getApplicationDefault()
    private var requestInitializer: HttpRequestInitializer = HttpCredentialsAdapter(
        credentials.createScoped(
            DriveScopes.DRIVE_READONLY,
            SheetsScopes.SPREADSHEETS,
        )
    )

    private val gsonFactory = GsonFactory.getDefaultInstance()

    private val driveService = Drive.Builder(
        httpTransport,
        gsonFactory,
        requestInitializer
    ).setApplicationName(APP_NAME).build()

    private val docsService = Docs.Builder(
        httpTransport,
        gsonFactory,
        requestInitializer
    ).setApplicationName(APP_NAME).build()

    private val sheetsService = Sheets.Builder(
        httpTransport,
        gsonFactory,
        requestInitializer
    ).setApplicationName(APP_NAME).build()

    /**
     * Fetches list of drafts from the drafts Google sheet
     */
    suspend fun getDraftsFromSpreadsheet(): List<Draft> = coroutineScope {
        val rows = fetchSheetData(GlobalConfig.DRAFTS_SHEET_ID, DRAFTS_SHEET_DATA_RANGE)
        val drafts = rows
            .filter { row -> row.size >= 4 } // skip rows with any empty cells
            .map { row ->
                Draft(
                    docUrl = row[0].toString(),
                    title = row[1].toString(),
                    author = row[2].toString(),
                    isCompleted = row[3] == SHEETS_BOOLEAN_TRUE
                )
            }

        drafts
    }

    /**
     * Fetches list of rewrites from the rewrites Google sheet
     */
    suspend fun getRewritesFromSpreadsheet(): List<Draft> = coroutineScope {
        val rows = fetchSheetData(GlobalConfig.REWRITES_SHEET_ID, REWRITES_SHEET_DATA_RANGE)
        val drafts = rows
            .filter { row -> row.size >= 4 } // skip rows with any empty cells
            .map { row ->
                Draft(
                    docUrl = row[0].toString(),
                    title = row[1].toString(),
                    author = row[2].toString(),
                    isCompleted = row[3] == SHEETS_BOOLEAN_TRUE
                )
            }

        drafts
    }

    /**
     * Finds drafts from Google Drive and maps them to [Draft] objects
     */
    suspend fun scanDraftsFromDrive(): List<Draft> = coroutineScope {
        withContext(Dispatchers.IO) {
            listDraftDocuments().map { file ->
                val title = getDocumentTitle(file.id)
                Draft(
                    docUrl = DocsLinkFormats.googleDocsLink(file.id),
                    title = cleanTitle(title),
                    author = getAuthorNameFromDocumentTitle(title) ?: "Unknown",
                    isCompleted = getCompletedStatusFromDocumentTitle(title),
                )
            }
        }
    }

    /**
     * Finds rewrites from Google Drive and maps them to [Draft] objects
     */
    suspend fun scanRewritesFromDrive(): List<Draft> = coroutineScope {
        withContext(Dispatchers.IO) {
            listRewriteDocuments().map { file ->
                val title = getDocumentTitle(file.id)
                Draft(
                    docUrl = DocsLinkFormats.googleDocsLink(file.id),
                    title = cleanTitle(title),
                    author = getAuthorNameFromDocumentTitle(title) ?: "Unknown",
                    isCompleted = getCompletedStatusFromDocumentTitle(title),
                )
            }
        }
    }

    /**
     * Update drafts Google sheet with given data
     */
    suspend fun updateDraftData(data: List<Draft>): Unit = coroutineScope {
        writeToSheet(
            GlobalConfig.DRAFTS_SHEET_ID,
            DRAFTS_SHEET_DATA_RANGE,
            data.map { listOf(it.docUrl, it.title, it.author, it.isCompleted) }
        )
    }

    /**
     * Update rewrites Google sheet with given data
     */
    suspend fun updateRewriteData(data: List<Draft>): Unit = coroutineScope {
        writeToSheet(
            GlobalConfig.REWRITES_SHEET_ID,
            REWRITES_SHEET_DATA_RANGE,
            data.map { listOf(it.docUrl, it.title, it.author, it.isCompleted) }
        )
    }

    /**
     * Fetches a Google docs document title
     */
    private suspend fun getDocumentTitle(documentId: String): String = coroutineScope {
        withContext(Dispatchers.IO) {
            docsService.documents().get(documentId).execute().title
        }
    }

    /**
     * Fetches data from a Google sheet at given range
     */
    private suspend fun fetchSheetData(sheetId: String, range: String): List<List<Any>> = coroutineScope {
        withContext(Dispatchers.IO) {
            sheetsService
                .spreadsheets()
                .values()
                .get(sheetId, range)
                .execute()
        }.getValues()
    }

    /**
     * Writes data to a Google sheet at specified range.
     * Any existing data at the specified rage is cleared before writing.
     */
    private suspend fun writeToSheet(
        sheetId: String,
        range: String,
        data: List<List<Any>>
    ) {
        withContext(Dispatchers.IO) {
            val content = ValueRange()
            content.setValues(data)

            // Clear existing data from sheet
            sheetsService.spreadsheets().values().clear(
                sheetId,
                range,
                ClearValuesRequest(),
            ).execute()

            // Set new data
            sheetsService.spreadsheets()
                .values()
                .update(
                    sheetId,
                    range,
                    content,
                )
                .setValueInputOption("RAW")
                .execute()
        }
    }

    /**
     * Query Google Drive and return the resultant files
     */
    private suspend fun queryDrive(q: String): List<File> = coroutineScope {
        val result = async(Dispatchers.IO) {
            driveService.files()
                .list()
                .setQ(q)
                .execute()
        }

        result.await().files
    }

    /**
     * Fetches a list of Google doc files in drive in the drafts folder
     */
    private suspend fun listDraftDocuments(): List<File> {
        return queryDrive(DRAFTS_QUERY)
    }

    /**
     * Fetches a list of Google doc files in drive in the rewrites folder
     */
    private suspend fun listRewriteDocuments(): List<File> {
        return queryDrive(REWRITES_QUERY)
    }

    /**
     * Removes author and status information from the title
     */
    private fun cleanTitle(title: String): String =
        title.split("\\[.*?]".toRegex()).joinToString(separator = "").trim()

    /**
     * Attempts to find author name in title.
     * Author's name is enclosed in square brackets and must be the first such enclosing in the title
     */
    private fun getAuthorNameFromDocumentTitle(title: String): String? =
        AUTHOR_REGEX.find(title)?.groupValues?.get(1)

    private fun getCompletedStatusFromDocumentTitle(title: String): Boolean =
        STATUS_REGEX.containsMatchIn(title)
}