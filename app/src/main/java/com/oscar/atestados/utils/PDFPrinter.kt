package com.oscar.atestados.utils

import java.io.File

interface PDFPrinter {
    fun generarDocumento(htmlContent: String, outputFile: File)
}