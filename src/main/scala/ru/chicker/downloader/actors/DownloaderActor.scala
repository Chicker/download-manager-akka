package ru.chicker.downloader.actors

import java.io.{ByteArrayOutputStream, FileOutputStream, IOException, InputStream}
import java.nio.file.{FileSystems, Path}

import akka.actor.{Actor, ActorLogging, Props}
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import ru.chicker.downloader.actors.DownloaderActor.{MsgGetTask, MsgTaskFinished}
import ru.chicker.downloader.entities.DownloadTask
import ru.chicker.downloader.models.{DownloadError, DownloadResult, DownloadSuccess}
import ru.chicker.downloader.util.{use}

class DownloaderActor(val speedLimit: Long) extends Actor with ActorLogging {
    private val BUFFER_SIZE: Int = 64 * 1024
    private val ONE_SECOND: Int = 1000

    override def receive: Receive = {
        case DownloadManager.MsgNewTask(downloadTask) =>
            val processResult = processTask(downloadTask)

            sender ! MsgTaskFinished(processResult)
        case msg =>
            log.error(s"Undefined message ${msg.toString} has been received")
    }

    private def processTask(task: DownloadTask): DownloadResult = {
        log.debug("New task has been received! {}", task.toString)
        
        try {
            DownloadSuccess(task.getLinkInfo, downloadLink(task))
        } catch {
            case ex: Throwable => DownloadError(task.getLinkInfo, ex)
        }
    }

    private def httpGet[B](url: String)(op: InputStream => B): B = {
        val httpclient = HttpClients.createDefault()
        val httpget = new HttpGet(url)

        val response = httpclient.execute(httpget)
        
        use(response) { response => 
            val statusCode = response.getStatusLine.getStatusCode
            if (statusCode == 200) {
                val entity = response.getEntity
                if (entity != null) {
                    op(entity.getContent)
                } else {
                    throw new IOException("No http content exists!")
                }
            } else {
                throw new IOException(s"The server response code <> 200, is equal $statusCode")
            }
        }
    }

    private def saveToFile(outputFile: Path, byteStream: ByteArrayOutputStream) = {
        log.debug("Downloader is trying to write file to the folder: {}", outputFile)
        
        val fo = new FileOutputStream(outputFile.toFile)

        use(fo) { _.write(byteStream.toByteArray) }
    }

    private def downloadLink(downloadTask: DownloadTask): Int = {

        def read(in: InputStream) = readWithLimit(this.speedLimit.toInt, in)
        
        log.info("Starting downloading file: {}", downloadTask.getLinkInfo.getFileName)
        val byteStream = httpGet(downloadTask.getLinkInfo.getHttpLink)(read)
        val bytesRead = byteStream.size()

        val outputFilePath = FileSystems.getDefault.getPath(
            downloadTask.getOutputFolder,
            downloadTask.getLinkInfo.getFileName)

        saveToFile(outputFilePath, byteStream)

        log.info("{} is successfully downloaded and saved to file",
            downloadTask.getLinkInfo.getFileName)

        bytesRead
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


    override def preStart(): Unit = {
        super.preStart()
        log.debug("starting...")

        context.parent ! MsgGetTask
    }
}

object DownloaderActor {

    case object MsgGetTask

    case class MsgTaskFinished(downloadResult: DownloadResult)

    def props(speedLimit: Long): Props = Props(classOf[DownloaderActor], speedLimit)
}
