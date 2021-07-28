package server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.`Access-Control-Allow-Origin`
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import spray.json._
import utils.FileUtil.{generateDataResponse, generateHistogramResponse, getFileSize, getStatus}
import models._
import traits.JsonProtocol




object LogalyzerLow extends JsonProtocol{

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
      ).addHeader(`Access-Control-Allow-Origin`.*)

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/data"), _, entity, _) =>

      try {
        //        have to remove after test
        val response = generateDataResponse("Jul 23 23:24:09", "Jul 27 23:24:09", "connection", fileFullPath)
        HttpResponse(
          StatusCodes.OK,
          entity = HttpEntity(
            ContentTypes.`application/json`,
            response.toJson.prettyPrint
          )
        ).addHeader(`Access-Control-Allow-Origin`.*)

      } catch {
        case e: ClassCastException => HttpResponse(
          StatusCodes.InternalServerError,
          entity = HttpEntity(
            ContentTypes.`application/json`,
            Error(message = "Invalid Request Body, Please Provide Proper Body").toJson.prettyPrint
          )
        ).addHeader(`Access-Control-Allow-Origin`.*)
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
        ).addHeader(`Access-Control-Allow-Origin`.*)

      } catch {
        case e: ClassCastException => HttpResponse(
          StatusCodes.InternalServerError, // HTTP 200
          entity = HttpEntity(
            ContentTypes.`application/json`,
            Error(message = "Invalid Request Body, Please Provide Proper Body").toJson.prettyPrint
          )
        ).addHeader(`Access-Control-Allow-Origin`.*)
      }

    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound, // 404
        entity = HttpEntity(
          ContentTypes.`application/json`,
          Error(message = "Invalid route. Possible Routes[/api/get_data, /api/get_size, /api/data, /api/histogram,]").toJson.prettyPrint
        )
      ).addHeader(`Access-Control-Allow-Origin`.*)
  }
  Http().bindAndHandleSync(requestHandler, host, port)
}
