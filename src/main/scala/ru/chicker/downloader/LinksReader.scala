package ru.chicker.downloader

import java.io.FileInputStream
import java.util.Scanner

import ru.chicker.downloader.entities.DownloadLinkInfo
import ru.chicker.downloader.exceptions.InvalidFileStructureException
import ru.chicker.downloader.util.use

import scala.collection.immutable.ListSet

object LinksReader {
    def load(linksFileName: String): ListSet[DownloadLinkInfo] = {

        use(new Scanner(new FileInputStream(linksFileName))) { scanner =>
            try {
                parseLinkInfo(scanner, ListSet.empty)
            }
            catch {
                case ex: NoSuchElementException => {
                    throw new InvalidFileStructureException(linksFileName, ex)
                }
            }
        }
    }

    def parseLinkInfo(scanner: Scanner, links: ListSet[DownloadLinkInfo]): ListSet[DownloadLinkInfo] = {
        if (scanner.hasNextLine) {
            val httpLink = scanner.next
            val fileNameToSave = scanner.next

            parseLinkInfo(scanner, links + new DownloadLinkInfo(fileNameToSave, httpLink))
        } else {
            links
        }
    }
}