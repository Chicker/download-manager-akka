package ru.chicker.downloader

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
}
