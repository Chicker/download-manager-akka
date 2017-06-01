package ru.chicker.downloader.actors

import ru.chicker.downloader.actors.DownloadManager.MsgNewTask
import akka.actor.{Actor, ActorLogging, PoisonPill, Props, Terminated}
import ru.chicker.downloader.actors.DownloaderActor.MsgGetTask
import ru.chicker.downloader.models.{DownloadResult, DownloadSuccess}
import ru.chicker.downloader.entities.{DownloadLinkInfo, DownloadTask}

import scala.collection.immutable.ListSet
import scala.collection.mutable.{ArrayBuffer, LinkedHashSet}

class DownloadManager(val links: ListSet[DownloadLinkInfo],
                      val pathToOutputPathFolder: String,
                      val totalSpeedLimit: Long,
                      val workersCount: Int)
    extends Actor with ActorLogging {
    
    private val ONE_KILOBYTE: Int = 1024
    private val ONE_MEGABYTE: Int = ONE_KILOBYTE * ONE_KILOBYTE
    private val resultList = new ArrayBuffer[DownloadResult]()
    private val taskList = LinkedHashSet.empty ++= links
    private var numberOfTerminatedChilds = 0
    
    override def receive: Receive = {
        case DownloaderActor.MsgGetTask =>
            prepareAndSendNewTask()

        case DownloaderActor.MsgTaskFinished(downloadResult) =>
            processWorkResult(downloadResult)
            prepareAndSendNewTask()

        case Terminated(child) =>
            log.debug(s"The worker $child has been terminated\n")

            numberOfTerminatedChilds = numberOfTerminatedChilds + 1

            if (numberOfTerminatedChilds == workersCount) context stop self
    }

    private def prepareAndSendNewTask() = {
        val link = taskList.headOption

        if (link.isDefined) {
            val task = new DownloadTask(link.get, pathToOutputPathFolder)
            sender ! MsgNewTask(task)

            taskList -= link.get
        } else {
            // no tasks anymore, stop workers
            context stop sender
        }
    }

    private def processWorkResult(downloadResult: DownloadResult) = {
        resultList += downloadResult
    }

    private def printFooter(): Unit = {
        println("----------------------")
        println("Program statistic:")

        val sizeOfAllDownloads =
            resultList
            .collect {case DownloadSuccess(_, bytesCount) => bytesCount }
            .sum
        
        resultList.foreach(println)
        
        println(f"[${resultList.size}%d] links are processed. Total size of the downloaded files " +
            f"is [${formatBytes(sizeOfAllDownloads)}%s]") 
            
        println("----------------------")
    }

    private def init() {
        val speedLimitToThread = totalSpeedLimit / workersCount
        
        startWorkers(speedLimitToThread)

        log.info(s"$workersCount downloaders are been created")
    }

    private def startWorkers(speedLimitToThread: Long) = {
        (1 to workersCount).foreach { i =>
            val child = context.actorOf(DownloaderActor.props(speedLimitToThread),
                s"Downloader_${i}_")

            context.watch(child)
        }
    }

    private def formatBytes(bytes: Long) = {
        if (bytes < ONE_MEGABYTE)
            f"${bytes.toFloat / ONE_KILOBYTE}%.2f Kb"
        else
            f"${bytes.toFloat / ONE_MEGABYTE}%.2f Mb"
    }
    
    override def preStart(): Unit = {
        super.preStart()
        init()
    }

    override def postStop(): Unit = {
        super.postStop()
        log.info("Shutting down actorsystem")
        context.system.shutdown()

        printFooter()
    }
}

object DownloadManager {

    case class MsgNewTask(downloadTask: DownloadTask)

    def props(links: ListSet[DownloadLinkInfo], pathToOutputPathFolder: String,
              totalSpeedLimit: Long, numThreads: Int): Props = {
        Props(classOf[DownloadManager], links,
            pathToOutputPathFolder, totalSpeedLimit, numThreads)
    }

}
