import ru.chicker.downloader.actors.DownloadManager
import akka.actor.ActorSystem
import ru.chicker.downloader.LinksReader

object Main {
    def main(args: Array[String]): Unit = {
        val actorSystem = ActorSystem.create("MySystem")

        try {
            val links = LinksReader.load("/Users/dns/Documents/temp/links.txt")

            val downloadManager = actorSystem.actorOf(
                DownloadManager.props(links,
                    "/Users/dns/Documents/temp/output", 64000, 2), "DownloadManager")
        } catch {
            case ex: Throwable => ex.printStackTrace()
        }
        
    }
}

