package ru.chicker.downloader.models

import ru.chicker.downloader.entities.DownloadLinkInfo

sealed trait DownloadResult

case class DownloadSuccess(linkInfo: DownloadLinkInfo,
                           bytesCount: Int) extends DownloadResult {
    override def toString: String =
        f"Downloading file [${linkInfo.getFileName}%s] with total size [$bytesCount%d] bytes " +
            f"successfully complete"
}

case class DownloadError(linkInfo: DownloadLinkInfo,
                         error: Throwable) extends DownloadResult {
    override def toString: String = {
        f"The file [${linkInfo.getFileName}%s] has been downloaded with error: [${error.getLocalizedMessage}%s]"
    }
}
