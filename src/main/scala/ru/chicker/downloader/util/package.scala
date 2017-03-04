package ru.chicker.downloader

package object util {
    def use[A <: { def close(): Unit }, B](resource: A)(code: A ⇒ B): B =
        try
            code(resource)
        finally
            resource.close()
}
