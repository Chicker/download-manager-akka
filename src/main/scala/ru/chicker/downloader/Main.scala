package ru.chicker.downloader

import akka.actor.ActorSystem
import ru.chicker.downloader.actors.DownloadManager
import ru.chicker.downloader.util.{checkDirIsExist, checkFileIsExist}

object Main {

    def main(args: Array[String]): Unit = {
        try {
            // if the config will not be properly readed then `scopt` will show help usages 
            // and then the program will close.
            val config = Config.readConfig(args)

            config foreach { cfg =>
                checkFileIsExist(cfg.pathToLinksFile)
                checkDirIsExist(cfg.pathToOutputFolder)
                downloadingProcess(cfg)
            }
        } catch {
            case ex: Throwable =>
                println("While starting program an error has been occurred: \n" +
                    s"\t${ex.getLocalizedMessage}")
        }
    }

    private def downloadingProcess(config: Config) = {
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
