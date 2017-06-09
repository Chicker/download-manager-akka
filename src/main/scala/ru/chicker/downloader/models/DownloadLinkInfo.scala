package ru.chicker.downloader.models

case class DownloadLinkInfo(fileName: String, link: String) {
    def getFileName = fileName
    def getHttpLink = link
}
