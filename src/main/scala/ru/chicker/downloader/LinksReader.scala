package ru.chicker.downloader

import java.io.FileInputStream
import java.util.Scanner

import ru.chicker.downloader.entities.DownloadLinkInfo
import ru.chicker.downloader.exceptions.InvalidFileStructureException
import ru.chicker.downloader.util.use

object LinksReader {
    def load(linksFileName: String): java.util.Set[DownloadLinkInfo] = {
        val result = new java.util.HashSet[DownloadLinkInfo]
        use(new Scanner(new FileInputStream(linksFileName))) { scanner =>
            while (scanner.hasNextLine) {
                try {
                    val httpLink = scanner.next
                    val fileNameToSave = scanner.next
                    result.add(new DownloadLinkInfo(fileNameToSave, httpLink))
                }
                catch {
                    case ex: NoSuchElementException => {
                        throw new InvalidFileStructureException(linksFileName, ex)
                    }
                }
            }
        }
        result
    }
}