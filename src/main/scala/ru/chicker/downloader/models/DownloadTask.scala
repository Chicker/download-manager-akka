package ru.chicker.downloader.models

case class DownloadTask(linkInfo: DownloadLinkInfo, outputFolder: String) {
    def getLinkInfo = linkInfo
    def getOutputFolder = outputFolder
}
