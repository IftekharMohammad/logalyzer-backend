package actors

import akka.actor.{Actor, ActorLogging}
import utils.FileUtil.{generateDataResponse, generateHistogramResponse, getFileSize, getStatus}

object DataProcessor {
  case class GetStatus(filePath: String)
  case class GetSize(filePath: String)
  case class GetGeneratedDataResponse(dateTimeStart: String, dateTimeEnd: String, phrase: String, filePath: String)
  case class GetGeneratedHistogramResponse(dateTimeStart: String, dateTimeEnd: String, phrase: String, filePath: String)
}


class DataProcessor extends Actor with ActorLogging {
  import DataProcessor._

  override def receive: Receive = {
    case GetStatus(filePath) =>
      log.info(s"Checking If File Exists. File Path: $filePath")
      val response = getStatus(filePath)
      sender() ! response

    case GetSize(filePath) =>
      log.info(s"Getting File Size. File Path: $filePath")
      sender() ! getFileSize(filePath)

    case GetGeneratedDataResponse(dateTimeStart, dateTimeEnd, phrase, filePath) =>
      log.info(s"Generating Response From Requested Data. From: $dateTimeStart, Until: $dateTimeEnd, Phrase: $phrase, File Path: $filePath")
      sender() ! generateDataResponse(dateTimeStart, dateTimeEnd, phrase, filePath)

    case GetGeneratedHistogramResponse(dateTimeStart, dateTimeEnd, phrase, filePath) =>
      log.info(s"Generating Histogram From Requested Data. From: $dateTimeStart, Until: $dateTimeEnd, Phrase: $phrase, File Path: $filePath")
      sender() ! generateHistogramResponse(dateTimeStart, dateTimeEnd, phrase, filePath)

  }
}