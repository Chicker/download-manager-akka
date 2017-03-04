package ru.chicker.downloader.actors

import java.util

import ru.chicker.downloader.actors.DownloadManager.MsgNewTask
import akka.actor.{Actor, ActorLogging, PoisonPill, Props, Terminated}
import ru.chicker.downloader.models.DownloadResult
import ru.chicker.downloader.entities.{DownloadLinkInfo, DownloadTask}

import scala.collection.mutable

class DownloadManager(val links: util.Collection[DownloadLinkInfo],
                      val pathToOutputPathFolder: String,
                      val totalSpeedLimit: Long,
                      val numThreads: Int)
    extends Actor with ActorLogging {

    import context.become
    
    private val ONE_KILOBYTE: Int = 1024
    private val ONE_MEGABYTE: Int = ONE_KILOBYTE * ONE_KILOBYTE
    //  private val downloaders: util.List[ActorRef] = new util.ArrayList[ActorRef]
    //  private val resultList: util.Collection[Either[DownloadError, DownloadSuccess]] = new util.ArrayList[Either[DownloadError, DownloadSuccess]]
    private var taskListIsEmpty = false
    private val resultList = new mutable.ArrayBuffer[DownloadResult]()

    private val taskList = new util.LinkedList[DownloadLinkInfo](links)
    
    
    
    def finishing: Receive = {
        case DownloaderActor.MsgGetTask => sender().tell(PoisonPill, self)
        case DownloaderActor.MsgTaskFinished(downloadResult) => {
            processWorkResult(downloadResult)
        }
        case Terminated(child) => {
            printf(s"This child ${child} has been terminated\n")
        }
    }

    override def receive: Receive = {
        case DownloaderActor.MsgGetTask => {
            val link = taskList.poll() 
            
            if (null == link ) {
                sender().tell(PoisonPill, self)
                become(finishing)
                // TODO установить самому себе timeout на ожидание завершения своих 
                // дочерних акторов
            } else {
                val task = new DownloadTask(link, pathToOutputPathFolder)
                sender().tell(MsgNewTask(task), self)
            }
        }

        case DownloaderActor.MsgTaskFinished(downloadResult) => {
            processWorkResult(downloadResult)
        }
    }

    private def processWorkResult(downloadResult: DownloadResult) = {
        resultList.+=(downloadResult)
    }

    private def printFooter(): Unit = {
        //    downloadResult match {
        //      case DownloadSuccess(linkInfo, bytesCount) => {
        //
        //      }
        //      case DownloadError(linkInfo, error) => {
        //
        //      }
        //    }
    }

    private def init() {
        val speedLimitToThread = totalSpeedLimit / numThreads
        
        
        
        startWorkers(speedLimitToThread)

        log.debug(s"${numThreads} downloaders are been created")
    }

    private def startWorkers(speedLimitToThread: Long) = {
        (1 to numThreads).foreach { i =>
            val child = context.actorOf(DownloaderActor.props(speedLimitToThread),
                s"Downloader_${i}_")

            context.watch(child)
        }
    }

    @scala.throws[Exception](classOf[Exception])
    override def preStart(): Unit = {
        super.preStart()
        log.debug("DownloadManager preStart")
        init()
    }
}

object DownloadManager {

    case class MsgNewTask(downloadTask: DownloadTask)

    def props(links: util.Collection[DownloadLinkInfo], pathToOutputPathFolder: String,
              totalSpeedLimit: Long, numThreads: Int): Props = {
        Props(classOf[DownloadManager], links,
            pathToOutputPathFolder, totalSpeedLimit, numThreads)
    }

}
