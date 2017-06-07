package ru.chicker.downloader

import ru.chicker.downloader.util.{ignore, tryToEither}

case class Config(
                   workersCount: Int = 2, // 2 workers by default
                   limit: Int = 64 * 1024, // 64 kilobytes per second by default
                   pathToLinksFile: String = "",
                   pathToOutputFolder: String = "")

object Config{
  private val APP_NAME = "downloader-akka"
  
  def readConfig(args: Array[String]): Option[Config] = {
    val cliParser = new scopt.OptionParser[Config](APP_NAME) {
      head(APP_NAME)

      opt[Int]('n', "threads number")
        .validate(n => if (n > 0) success
                         else failure("The number of threads must be > 0"))
        .action( (p,c) => c.copy(workersCount = p))
        .text("number of downloading workers (threads)")

      opt[String]('l', "limit")
        .validate(validateLimit)
        .action( (p,c) => c.copy(limit = Config.parseLimit(p)))
        .text("total limit on downloading speed (e.g. 8192 or 8k or 1M")

      opt[String]('f', "links")
        .required()
        .action( (p,c) => c.copy(pathToLinksFile = p))
        .text("path to a links file")

      opt[String]('o', "output")
        .action( (p,c) => c.copy(pathToOutputFolder = p))
        .text("path to an output folder")
    }

    cliParser.parse(args, Config())
  }
  
  def validateLimit(limitAsStr: String): Either[String, Unit] = {
    val limit = limitAsStr.toLowerCase
    limit match {
      case asKb if limit.endsWith("k") =>
        val value = asKb.dropRight(1)
        tryToEither(ignore(value.toInt))(_.getLocalizedMessage)
      case asMb if limit.endsWith("m") =>
        val value = asMb.dropRight(1)
        tryToEither(ignore(value.toInt))(_.getLocalizedMessage)
      case asBytes => tryToEither(ignore(asBytes.toInt))(_.getLocalizedMessage)
    }
  }
  
  def parseLimit(limitAsStr: String): Int = {
    val limit = limitAsStr.toLowerCase
    limit match {
      case asKb if limit.endsWith("k") =>
        val value = asKb.dropRight(1)
        value.toInt * 1024
      case asMb if limit.endsWith("m") =>
        val value = asMb.dropRight(1) 
        value.toInt * 1024 * 1024
      case asBytes => asBytes.toInt
    }
  }
}
