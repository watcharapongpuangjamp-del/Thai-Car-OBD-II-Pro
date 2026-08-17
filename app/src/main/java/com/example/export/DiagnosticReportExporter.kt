package com.example.export

import com.example.model.DiagnosticSession
import java.io.File
import java.io.OutputStream

/**
 * Interface defining the contract for exporting vehicle diagnostic sessions
 * into various document formats (e.g. PDF, CSV, JSON).
 */
interface DiagnosticReportExporter {

    /**
     * Generates a formatted report for the given [session] and writes the byte stream to [outputStream].
     * @param session The diagnostic session snapshot including DTCs, telemetry, and rule engine report.
     * @param outputStream The stream to receive the document bytes.
     * @return true if generation and writing succeeded, false otherwise.
     */
    suspend fun exportToStream(session: DiagnosticSession, outputStream: OutputStream): Boolean

    /**
     * Generates a formatted report file for the given [session] and saves it directly to [destinationFile].
     * @param session The diagnostic session snapshot.
     * @param destinationFile The target output file.
     * @return [Result] containing the exported [File] on success, or an exception on failure.
     */
    suspend fun exportToFile(session: DiagnosticSession, destinationFile: File): Result<File>
}
