package utils

import models.{Data, DataResponse, HighlightPosition, Histogram, HistogramResponse, Size, Status}

import java.io.{File, FileNotFoundException, IOException}
import java.text.SimpleDateFormat
import java.time.format.DateTimeParseException
import java.util.Date


case class Log(dateTimeString: String, dateTime: Date, logString: String)

object FileUtil {


  def doesFileExists(filePath: String): String = {
    try {
      val source = scala.io.Source.fromFile(filePath, "utf-8")
      val lines = try source.getLines.mkString finally source.close()
      if (lines.nonEmpty) "OK" else "ERROR"
    } catch {
      case e: FileNotFoundException => "ERROR"
      case e: IOException => "ERROR"
    }

  }

  def retrieveFileSize(filePath: String): Long = {
    try {

      val someFile = new File(filePath)
      val fileSize = someFile.length
      fileSize
    } catch {
      case e: FileNotFoundException => 0
      case e: IOException => 0
    }

  }

  def returnDateRegexFirstMatch(str: String): String = {
    val pattern = "^(\\S{3}\\s+\\d{1,2}\\s+(\\d{2}:){2}\\d+)".r
    val found = pattern findFirstIn str
    found.mkString
  }

  def makeDateTimeFromDateTimeString(logDateTime: String): Date = {
    try {
      val dateFormat = new SimpleDateFormat("MMM d kk:mm:ss")
      dateFormat.parse(logDateTime)

    } catch {
      case e: DateTimeParseException => {
        try {
          val dateFormat = new SimpleDateFormat("MMM  d kk:mm:ss")
          dateFormat.parse(logDateTime)
        } catch {
          case e: DateTimeParseException => {
            try {
              val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
              dateFormat.parse(logDateTime)
            } catch {
              case e: DateTimeParseException => {
                null
              }
            }
          }
        }
      }
    }
  }

  def getHighlightPositions(parentString: String, phrase: String): List[HighlightPosition] = {
    var highLightedPositions = List.empty[HighlightPosition]
    val mainString = parentString.trim.toLowerCase
    val processedPhrase = phrase.trim.toLowerCase

    val words = mainString.split(" ")

    var stringLength = 0

    words.foreach(word => {
      if (word == processedPhrase) {
        val highlight = HighlightPosition(fromPosition = stringLength, toPosition = stringLength + word.length)
        highLightedPositions = highLightedPositions :+ highlight
      }

      stringLength = stringLength + word.length + 1 // 1 for space
    })

    highLightedPositions
  }

  def readFileAndCreateInMemoryDB(filePath: String): List[Log] = {
    var logCollection = List.empty[Log]
    val source = scala.io.Source.fromFile(filePath, "utf-8")

    for (line <- source.getLines()) {
      val dateString = returnDateRegexFirstMatch(line)
      val currentLog: Log = Log(dateTimeString = dateString, dateTime = makeDateTimeFromDateTimeString(dateString), logString = line.replace(dateString, "").trim)
      logCollection = logCollection :+ currentLog
    }
    source.close()
    logCollection
  }

  def filterLog(dateTimeStart: Date, dateTimeEnd: Date, phrase: String, filePath: String): List[Log] = {
    val logCollection = readFileAndCreateInMemoryDB(filePath)
    var filteredLog = List.empty[Log]
    for (log <- logCollection) {
      if ((log.dateTime.after(dateTimeStart) || log.dateTime.equals(dateTimeStart)) && (log.dateTime.before(dateTimeEnd) || log.dateTime.equals(dateTimeEnd))) {
        if (log.logString.toLowerCase.contains(phrase.toLowerCase)) {
          filteredLog = filteredLog :+ log
        }
      }
    }
    filteredLog
  }

  def getStatus(filePath: String): Status ={
    val fileStatus = doesFileExists(filePath)
    val status:Status = Status(fileStatus)
    status
  }

  def getFileSize(filePath: String): Size ={
    val size:Size = Size(size = retrieveFileSize(filePath))
    size
  }

  def generateDataResponse(dateTimeStart: String, dateTimeEnd: String, phrase: String, filePath: String): DataResponse = {
    var dataList = List.empty[Data]
    val processedDateTimeStart = makeDateTimeFromDateTimeString(dateTimeStart)
    val processedDateTimeEnd = makeDateTimeFromDateTimeString(dateTimeEnd)
    val filteredLogs = filterLog(processedDateTimeStart, processedDateTimeEnd, phrase, filePath)
    filteredLogs.foreach(log => {
      val highlightPosition = getHighlightPositions(log.logString, phrase)
      val processedData = Data(log.dateTimeString, log.logString, highlightPosition)
      dataList = dataList :+ processedData
    })
    val dataResponse = DataResponse(dataList, dateTimeStart, dateTimeEnd, phrase)

    dataResponse
  }

  def generateHistogramResponse(dateTimeStart: String, dateTimeEnd: String, phrase: String, filePath: String): HistogramResponse = {
    val histogramMapping = scala.collection.mutable.Map[String, Int]()

    var histogramList = List.empty[Histogram]

    val processedDateTimeStart = makeDateTimeFromDateTimeString(dateTimeStart)
    val processedDateTimeEnd = makeDateTimeFromDateTimeString(dateTimeEnd)
    val filteredLogs = filterLog(processedDateTimeStart, processedDateTimeEnd, phrase, filePath)
    filteredLogs.foreach(log => {
      if (histogramMapping.contains(log.dateTimeString)) {
        histogramMapping(log.dateTimeString) = histogramMapping(log.dateTimeString) + 1
      } else {
        histogramMapping += (log.dateTimeString -> 1)
      }
    })

    for ((k, v) <- histogramMapping) {
      val histogram = Histogram(k, v)
      histogramList = histogramList :+ histogram
    }

    val histogramResponse: HistogramResponse = HistogramResponse(histogramList, dateTimeStart, dateTimeEnd, phrase)
    histogramResponse
  }


//  println(generateDataResponse("Jul 23 23:24:09", "Jul 27 23:24:09", "connection", fileFullPath))
//  println(generateHistogramResponse("Jul 23 23:24:09", "Jul 27 23:24:09", "connection", fileFullPath))
}
