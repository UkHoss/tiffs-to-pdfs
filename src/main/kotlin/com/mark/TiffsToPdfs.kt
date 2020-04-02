package com.mark

import com.itextpdf.text.Document
import com.itextpdf.text.io.FileChannelRandomAccessSource
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.RandomAccessFileOrArray
import com.itextpdf.text.pdf.codec.TiffImage
import java.io.File

fun main(args: Array<String>) {

    val searchDir = File(args[0])

    val allTiffFiles = getTiffGroups(searchDir)

    val tiffCount = allTiffFiles.values.flatten().size

    println("loaded $tiffCount tiffs in ${allTiffFiles.size} dirs from under $searchDir")

    allTiffFiles.forEach { (parentDir, tiffs) ->
        val newFile = parentDir.path + "/" + tiffs.first().nameWithoutExtension + ".pdf"

        val pdfFile = File(newFile)
        println("processing tiffs in dir: [$parentDir], tiff count: [${tiffs.size}], outputting to: [$pdfFile]")

        val document = Document()

        try {
            PdfWriter.getInstance(document, pdfFile.outputStream())
            document.open()

            tiffs.forEach { tiff ->
                tiff.inputStream().use { fileInputStream ->
                    try {
                        val fileChannelRandomAccessSource = FileChannelRandomAccessSource(fileInputStream.channel)

                        val randomAccessFileOrArray = RandomAccessFileOrArray(fileChannelRandomAccessSource)

                        val numberOfPages = TiffImage.getNumberOfPages(randomAccessFileOrArray)

                        for (i in 1..numberOfPages) {
                            val tiffImage = TiffImage.getTiffImage(randomAccessFileOrArray, i)

                            document.pageSize = tiffImage
                            document.newPage()

                            tiffImage.setAbsolutePosition(0F, 0F)
                            document.add(tiffImage)
                        }
                    } catch (e: Exception) {
                        println("failed to process tiff file $tiff")
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            println("Failed to process tiffs in dir $parentDir")
            e.printStackTrace()
        } finally {
            document.close()
        }
    }
}

/**
 *  Gets all *.tiff *.tif files recursively that exist in a specific directory and groups them by parent file
 *
 *  @return a map - the key is the parent dir, and the list of files are the files immediately present in that dir
 */
fun getTiffGroups(dir: File): Map<File, List<File>> {
    return dir.walk()
        .filter { it.isFile && (it.extension.equals("tiff", true) || it.extension.equals("tif", true)) }
        .groupBy { it.parentFile }
        .toMap()
}
