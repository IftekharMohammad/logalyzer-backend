package server

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http

import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import com.typesafe.config.ConfigFactory

import spray.json._
import utils.FileUtil.{generateDataResponse, generateHistogramResponse, getFileSize, getStatus}


case class Status(status: String)
case class Error(message: String)

case class Size(size: Long)

case class RequestPayload(datetimeFrom: String, datetimeUntil: String, phrase: String)

case class HighlightPosition(fromPosition: Int, toPosition: Int)
case class Data(datetime: String, message: String, highlightText:List[HighlightPosition])
case class DataResponse(data: List[Data], datetimeFrom: String, datetimeUntil: String, phrase: String)


case class Histogram(datetime: String, count: Int)
case class HistogramResponse(histogram: List[Histogram], datetimeFrom: String, datetimeUntil: String, phrase: String)

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
      println(s"Checking If File Exists. File Path: $filePath")
      log.info(s"Checking If File Exists. File Path: $filePath")
      val response = getStatus(filePath)
      println(response)
      sender() ! response

    case GetSize(filePath) =>
      log.info(s"Getting File Size. File Path: $filePath")
      sender() ! getFileSize(filePath)

    case GetGeneratedDataResponse(dateTimeStart, dateTimeEnd, phrase, filePath) =>
      log.info(s"Generating Response From Requested Data. From: $dateTimeStart, Until: $dateTimeEnd, Phrase: $phrase, File Path: $filePath")
      println("inside ss1")
      val a = generateDataResponse(dateTimeStart, dateTimeEnd, phrase, filePath)
      println(a)
      sender() ! a

    case GetGeneratedHistogramResponse(dateTimeStart, dateTimeEnd, phrase, filePath) =>
      log.info(s"Generating Histogram From Requested Data. From: $dateTimeStart, Until: $dateTimeEnd, Phrase: $phrase, File Path: $filePath")
      println("inside ss")
      sender() ! generateHistogramResponse(dateTimeStart, dateTimeEnd, phrase, filePath)

  }
}

trait JsonProtocol extends DefaultJsonProtocol {
  implicit val statusFormat = jsonFormat1(Status)
  implicit val errorFormat = jsonFormat1(Error)
  implicit val sizeFormat = jsonFormat1(Size)

  implicit val requestFormat = jsonFormat3(RequestPayload)

  implicit val highlightFormat = jsonFormat2(HighlightPosition)
  implicit val dataFormat = jsonFormat3(Data)
  implicit val dataResponseFormat = jsonFormat4(DataResponse)

  implicit val histogramFormat = jsonFormat2(Histogram)
  implicit val histogramResponseFormat = jsonFormat4(HistogramResponse)
}

object Logalyzer extends App with JsonProtocol {

  val config = ConfigFactory.load()
  val loadedConfig = config.getConfig("logalyzer")
  val host = loadedConfig.getValue("host").render().replaceAll("\"", "")
  val port = loadedConfig.getValue("port").render().replaceAll("\"", "").toInt
  val fileFullPath = loadedConfig.getValue("file_location").render().replaceAll("\"", "")


  implicit val system = ActorSystem("Logalyzer")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher



  //  val dataReceiver = system.actorOf(Props[DataProcessor], "LogalyzerDataProcessor")
  //
  //  implicit val defaultTimeout = Timeout(60 seconds)
  //  val requestHandler: HttpRequest => Future[HttpResponse] = {
  //    case HttpRequest(HttpMethods.GET, Uri.Path("/api/get_status"), _, _, _) =>
  //      val guitarsFuture: Future[Status] = (dataReceiver ? GetStatus(fileFullPath)).mapTo[Status]
  //      guitarsFuture.map { guitars =>
  //        HttpResponse(
  //          entity = HttpEntity(
  //            ContentTypes.`application/json`,
  //            guitars.toJson.prettyPrint
  //          )
  //        )
  //      }
  //
  //    case HttpRequest(HttpMethods.GET, Uri.Path("/api/get_size"), _, _, _) =>
  //      val guitarsFuture: Future[Size] = (dataReceiver ? GetSize(fileFullPath)).mapTo[Size]
  //      guitarsFuture.map { guitars =>
  //        HttpResponse(
  //          entity = HttpEntity(
  //            ContentTypes.`application/json`,
  //            guitars.toJson.prettyPrint
  //          )
  //        )
  //      }
  //
  //    case HttpRequest(HttpMethods.POST, Uri.Path("/api/data"), _, entity, _) =>
  //      // entities are a Source[ByteString]
  //      val strictEntityFuture = entity.toStrict(3 seconds)
  //      strictEntityFuture.flatMap { strictEntity =>
  //
  //        val guitarJsonString = strictEntity.data.utf8String
  //        val guitar = guitarJsonString.parseJson.convertTo[RequestPayload]
  //        println(guitar)
  //        val guitarCreatedFuture: Future[Data] = (dataReceiver ? GetGeneratedDataResponse(guitar.datetimeFrom, guitar.datetimeUntil, guitar.phrase, fileFullPath)).mapTo[Data]
  //        guitarCreatedFuture.onComplete {
  //          case Success(data) => {
  //            println("inside Data")
  //            HttpResponse(
  //              StatusCodes.OK,
  //              entity = HttpEntity(
  //                ContentTypes.`application/json`,
  //                data.toJson.prettyPrint
  //              )
  //            )
  //          }
  //          case Failure(exception)  =>  {
  //            println("inside Data")
  //            HttpResponse(
  //              StatusCodes.OK,
  //              entity = HttpEntity(
  //                ContentTypes.`application/json`,
  //                Error(message = "Invalid Request Body, Please Provide Proper Body").toJson.prettyPrint
  //              )
  //            )
  //          }
  //
  //        }
  //      }
  //
  //    case HttpRequest(HttpMethods.POST, Uri.Path("/api/histogram"), _, entity, _) =>
  //      // entities are a Source[ByteString]
  //      val strictEntityFuture = entity.toStrict(3 seconds)
  //      strictEntityFuture.flatMap { strictEntity =>
  //
  //        val guitarJsonString = strictEntity.data.utf8String
  //        val guitar = guitarJsonString.parseJson.convertTo[RequestPayload]
  //
  //        val guitarCreatedFuture: Future[Data] = (dataReceiver ? GetGeneratedDataResponse(guitar.datetimeFrom, guitar.datetimeUntil, guitar.phrase, fileFullPath)).mapTo[Data]
  //        guitarCreatedFuture.map { data =>
  //          HttpResponse(
  //            StatusCodes.OK,
  //            entity = HttpEntity(
  //              ContentTypes.`application/json`,
  //              data.toJson.prettyPrint
  //            )
  //          )
  //        }
  //      }
  //
  //    case request: HttpRequest =>
  //      request.discardEntityBytes()
  //      Future {
  //        HttpResponse(status = StatusCodes.NotFound)
  //      }
  //  }
  //
  //  Http().bindAndHandleAsync(requestHandler, "localhost", 8000)

  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/api/get_status"), _, _, _) =>
      val response: Status = getStatus(fileFullPath)

      HttpResponse(
        StatusCodes.OK, // HTTP 200
        entity = HttpEntity(
          ContentTypes.`application/json`,
          response.toJson.prettyPrint
        )
      )

    case HttpRequest(HttpMethods.GET, Uri.Path("/api/get_size"), _, _, _) =>
      val response: Size = getFileSize(fileFullPath)

      HttpResponse(
        StatusCodes.OK, // HTTP 200
        entity = HttpEntity(
          ContentTypes.`application/json`,
          response.toJson.prettyPrint
        )
      )

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/data"), _, entity, _) =>



      try {
        val response = generateDataResponse("Jul 23 23:24:09", "Jul 27 23:24:09", "connection", fileFullPath)
        HttpResponse(
          StatusCodes.InternalServerError, // HTTP 200
          entity = HttpEntity(
            ContentTypes.`application/json`,
            response.toJson.prettyPrint
          )
        )

      } catch {
        case e: ClassCastException => HttpResponse(
          StatusCodes.InternalServerError, // HTTP 200
          entity = HttpEntity(
            ContentTypes.`application/json`,
            Error(message = "Invalid Request Body, Please Provide Proper Body").toJson.prettyPrint
          )
        )
      }




    case HttpRequest(HttpMethods.POST, Uri.Path("/api/histogram"), _, entity, _) =>
      try {
        val response = generateHistogramResponse("Jul 23 23:24:09", "Jul 27 23:24:09", "connection", fileFullPath)
        HttpResponse(
          StatusCodes.InternalServerError, // HTTP 200
          entity = HttpEntity(
            ContentTypes.`application/json`,
            response.toJson.prettyPrint
          )
        )

      } catch {
        case e: ClassCastException => HttpResponse(
          StatusCodes.InternalServerError, // HTTP 200
          entity = HttpEntity(
            ContentTypes.`application/json`,
            Error(message = "Invalid Request Body, Please Provide Proper Body").toJson.prettyPrint
          )
        )
      }

    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound, // 404
        entity = HttpEntity(
          ContentTypes.`application/json`,
          Error(message = "Invalid route. Possible Routes[/api/get_data, /api/get_size, /api/data, /api/histogram,]").toJson.prettyPrint
        )
      )
  }
  Http().bindAndHandleSync(requestHandler, host, port)
}
