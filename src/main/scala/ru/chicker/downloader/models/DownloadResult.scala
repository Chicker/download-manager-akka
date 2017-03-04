package ru.chicker.downloader.models

import ru.chicker.downloader.entities.DownloadLinkInfo

sealed abstract class DownloadResult()

case class DownloadSuccess(linkInfo: DownloadLinkInfo,
                           bytesCount: Long) extends DownloadResult

case class DownloadError(linkInfo: DownloadLinkInfo,
                         error: Throwable) extends DownloadResult
