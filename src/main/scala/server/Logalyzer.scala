package server

import akka.actor.{ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Origin`
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

object Logalyzer extends App with JsonProtocol{

  val config = ConfigFactory.load()
  val loadedConfig = config.getConfig("logalyzer")
  val host = loadedConfig.getValue("host").render().replaceAll("\"", "")
  val port = loadedConfig.getValue("port").render().replaceAll("\"", "").toInt
  val fileFullPath = loadedConfig.getValue("file_location").render().replaceAll("\"", "")


  implicit val system = ActorSystem("Logalyzer")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/api/get_status"), _, _, _) =>
      val response: Status = getStatus(fileFullPath)
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`application/json`,
          response.toJson.prettyPrint
        )
      ).addHeader(`Access-Control-Allow-Origin`.*)

    case HttpRequest(HttpMethods.GET, Uri.Path("/api/get_size"), _, _, _) =>
      val response: Size = getFileSize(fileFullPath)

      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`application/json`,
          response.toJson.prettyPrint
        )
      )

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/data"), _, entity, _) =>
      val payload= entity
      println(payload)
      try {
        //        have to remove after test
        val response = generateDataResponse("Jul 23 23:24:09", "Jul 27 23:24:09", "connection", fileFullPath)
        HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentTypes.`application/json`,
            response.toJson.prettyPrint
          )
        )

      } catch {
        case e: ClassCastException => HttpResponse(
          StatusCodes.InternalServerError,
          entity = HttpEntity(
            ContentTypes.`application/json`,
            Error(message = "Invalid Request Body, Please Provide Proper Body").toJson.prettyPrint
          )
        )
      }




    case HttpRequest(HttpMethods.POST, Uri.Path("/api/histogram"), _, entity, _) =>
      try {
//        have to remove after test
        val response = generateHistogramResponse("Jul 23 23:24:09", "Jul 27 23:24:09", "connection", fileFullPath)
        HttpResponse(
          StatusCodes.OK,
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
