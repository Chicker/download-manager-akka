package ru.chicker.downloader.actors

import java.io.{ByteArrayOutputStream, FileOutputStream, IOException, InputStream}
import java.nio.file.FileSystems

import akka.actor.{Actor, ActorLogging, Props}
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import ru.chicker.downloader.actors.DownloaderActor.{MsgGetTask, MsgTaskFinished}
import ru.chicker.downloader.entities.DownloadTask
import ru.chicker.downloader.models.{DownloadError, DownloadResult, DownloadSuccess}

class DownloaderActor(val speedLimit: Long) extends Actor with ActorLogging {
    private val BUFFER_SIZE: Int = 64 * 1024
    private val ONE_SECOND: Int = 1000

    override def receive: Receive = {
        case DownloadManager.MsgNewTask(downloadTask) => {
            val processResult = processTask(downloadTask)

            sender() ! MsgTaskFinished(processResult)
        }
    }

    private def processTask(task: DownloadTask): DownloadResult = {
        log.info("New task has been received! {}", task.toString)
        downloadLink(task) match {
            case Right(bytesCount) => DownloadSuccess(task.getLinkInfo,
                bytesCount)
            case Left(thr) => DownloadError(task.getLinkInfo, thr)
        }
    }

    private def downloadLink(downloadTask: DownloadTask): Either[Throwable, Long] = {
        val httpclient = HttpClients.createDefault()
        val httpget = new HttpGet(downloadTask.getLinkInfo.getHttpLink)
        var bytesRead = 0
        log.info("Starting downloading file: {}", downloadTask.getLinkInfo.getFileName)

        bytesRead = 0
        try {
            val response = httpclient.execute(httpget)
            try {
                val statusCode = response.getStatusLine.getStatusCode
                if (statusCode == 200) {
                    val entity = response.getEntity
                    if (entity != null) {
                        val instream = entity.getContent
                        try {
                            val byteStream = readWithLimit(this.speedLimit.toInt, instream)
                            val outputFilePath = FileSystems.getDefault.getPath(
                                downloadTask.getOutputFolder,
                                downloadTask.getLinkInfo.getFileName)

                            log.debug("Downloader is trying to write file to the folder: {}",
                                outputFilePath)
                            val fo = new FileOutputStream(outputFilePath.toFile)
                            try {
                                fo.write(byteStream.toByteArray)
                                bytesRead = byteStream.size()
                            } finally {
                                if (fo != null) fo.close()
                            }

                        } finally {
                            if (instream != null) instream.close()
                        }
                    } else {
                        log.error("downloadLink error, httpEntity is null")
                        return Left(new IOException("httpEntity is null"))
                    }
                }
            } finally {
                if (response != null) response.close()
            }
        } catch {
            case ex: Throwable =>
                log.error(ex, "downloadLink error")
                return Left(ex)
        }

        log.info("Download {} finished", downloadTask.getLinkInfo.getFileName)

        Right(bytesRead)
    }

    def readWithLimit(speedLimit: Int,
                      inputStream: InputStream): ByteArrayOutputStream = {
        import util.control.Breaks._

        var bytesRead = 0
        // скачиваем за раз сколько позволено или размер буфера
        val bytesToReadOnce = if (speedLimit <= BUFFER_SIZE) speedLimit else BUFFER_SIZE
        val buffer = new Array[Byte](BUFFER_SIZE)
        val outputStream = new ByteArrayOutputStream(BUFFER_SIZE)
        do {
            val timeStart = System.currentTimeMillis
            var sumBytesReadToLimit = 0
            breakable {
                do {
                    log.debug("Downloader started download the next portion of data")
                    bytesRead = inputStream.read(buffer, 0, bytesToReadOnce)
                    if (bytesRead > 0) {
                        log.debug("Downloader read {} bytes", bytesRead)
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    else break
                    sumBytesReadToLimit += bytesRead
                    // Если поток успел скачать отведенное ему кол-во байт меньше, чем за
                    // 1 с, то на оставщееся время, он засыпает, чтобы не превысить 
                    // заданное ограничение скорости
                    val timeEnd = System.currentTimeMillis
                    val diff = timeEnd - timeStart
                    // если время еще не вышло (1 с)
                    if (diff < ONE_SECOND) {
                        // и время на закачку еще есть, то идем дальше
                        if (sumBytesReadToLimit < speedLimit) {
                            // ничего не делаем, идем качать следующую порцию данных
                        }
                        else {
                            // мы достигли лимита скачанных байт за отведенное время
                            log.debug("Downloader reached limit of the downloaded bytes in {} ms" +
                                ". It will sleep remaining time", diff)
                            Thread.sleep(ONE_SECOND - diff)
                        }
                    }
                    else {
                        // limit time reached.  
                        break
                    }
                } while (true)
            }

        } while (bytesRead != -1)
        outputStream
    }


    @scala.throws[Exception](classOf[Exception])
    override def preStart(): Unit = {
        super.preStart()
        log.debug("starting...")

        context.parent ! MsgGetTask
    }

    @scala.throws[Exception](classOf[Exception])
    override def postStop(): Unit = {
        super.postStop()
        log.debug("stopping")
    }
}

object DownloaderActor {

    case object MsgGetTask

    case class MsgTaskFinished(downloadResult: DownloadResult)

    def props(speedLimit: Long): Props = Props(classOf[DownloaderActor], speedLimit)
}
