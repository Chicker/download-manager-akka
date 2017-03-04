package ru.chicker.downloader.exceptions

class InvalidFileStructureException(val fileName: String, val exCause: Throwable)
    extends Exception(String.format("File %s has invalid format!", fileName), exCause) { }
