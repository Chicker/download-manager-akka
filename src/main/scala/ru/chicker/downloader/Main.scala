package ru.chicker.downloader

import akka.actor.ActorSystem
import ru.chicker.downloader.actors.DownloadManager

object Main {
    
    def main(args: Array[String]): Unit = {
        Config.readConfig(args)
          .foreach(downloadProcess)
    }

    private def downloadProcess(config: Config) = {
        val actorSystem = ActorSystem.create("MySystem")

        try {
            val links = LinksReader.load(config.pathToLinksFile)

            val downloadManager = actorSystem.actorOf(
                DownloadManager.props(
                    links,
                    config.pathToOutputFolder,
                    config.limit,
                    config.workersCount), "DownloadManager")
            
            actorSystem.awaitTermination()
        } catch {
            case ex: Throwable => ex.printStackTrace()
        }
    }
}
