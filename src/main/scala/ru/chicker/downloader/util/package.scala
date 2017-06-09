package ru.chicker.downloader

import java.io.{File, FileNotFoundException}

import scala.util.{Failure, Success, Try}

package object util {
    def use[A <: { def close(): Unit }, B](resource: A)(code: A ⇒ B): B =
        try
            code(resource)
        finally
            resource.close()

    def ignore[T](expr: => T): Unit = {
        expr
    }

    def tryToEither[T,L](t: => T)(left: Throwable => L): Either[L, T] = {
        Try.apply(t) match {
            case Success(v) => Right(v)
            case Failure(thr) => Left(left(thr))
        }
    }

    def checkFileIsExist(fileName: String): Unit = {
        val fileHandler = new File(fileName)

        if (!fileHandler.exists() || !fileHandler.isFile) {
            throw new FileNotFoundException(
                s"The file [$fileName] is not exist or not accessible!")
        }
    }

    def checkDirIsExist(dirName: String): Unit = {
        val fileHandler = new File(dirName)

        if (!fileHandler.exists() || !fileHandler.isDirectory) {
            throw new FileNotFoundException(
                s"The directory [$dirName] is not exist or not accessible!")
        }
    }
}
