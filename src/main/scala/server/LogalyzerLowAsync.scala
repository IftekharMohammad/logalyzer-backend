package server

import actors.DataProcessor
import actors.DataProcessor.{GetGeneratedDataResponse, GetGeneratedHistogramResponse, GetSize, GetStatus}
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import models.{DataResponse, HistogramResponse, RequestPayload, Size, Status}
import spray.json.enrichAny
import traits.JsonProtocol

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import spray.json._


object LogalyzerLowAsync extends JsonProtocol {

  val config = ConfigFactory.load()
  val loadedConfig = config.getConfig("logalyzer")
  val host = loadedConfig.getValue("host").render().replaceAll("\"", "")
  val port = loadedConfig.getValue("port").render().replaceAll("\"", "").toInt
  val fileFullPath = loadedConfig.getValue("file_location").render().replaceAll("\"", "")


  implicit val system = ActorSystem("Logalyzer")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val dataReceiver = system.actorOf(Props[DataProcessor], "LogalyzerDataProcessor")

  implicit val defaultTimeout = Timeout(10 seconds)
  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/api/get_status"), _, _, _) =>
      val statusFuture: Future[Status] = (dataReceiver ? GetStatus(fileFullPath)).mapTo[Status]
      statusFuture.map { status =>
        HttpResponse(
          entity = HttpEntity(
            ContentTypes.`application/json`,
            status.toJson.prettyPrint
          )
        )
      }

    case HttpRequest(HttpMethods.GET, Uri.Path("/api/get_size"), _, _, _) =>
      val sizeFuture: Future[Size] = (dataReceiver ? GetSize(fileFullPath)).mapTo[Size]
      sizeFuture.map { size =>
        HttpResponse(
          entity = HttpEntity(
            ContentTypes.`application/json`,
            size.toJson.prettyPrint
          )
        )
      }

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/data"), _, entity, _) =>
      val strictEntityFuture = entity.toStrict(3 seconds)
      strictEntityFuture.flatMap { strictEntity =>

        val payloadJsonString = strictEntity.data.utf8String
        val payload = payloadJsonString.parseJson.convertTo[RequestPayload]

        val dataFuture: Future[DataResponse] = (dataReceiver ? GetGeneratedDataResponse(payload.datetimeFrom, payload.datetimeUntil, payload.phrase, fileFullPath)).mapTo[DataResponse]
        dataFuture.map { data =>
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              data.toJson.prettyPrint
            )
          )
        }
      }

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/histogram"), _, entity, _) =>
      val strictEntityFuture = entity.toStrict(3 seconds)
      strictEntityFuture.flatMap { strictEntity =>

        val payloadJsonString = strictEntity.data.utf8String
        val payload = payloadJsonString.parseJson.convertTo[RequestPayload]

        val histogramFuture: Future[HistogramResponse] = (dataReceiver ? GetGeneratedHistogramResponse(payload.datetimeFrom, payload.datetimeUntil, payload.phrase, fileFullPath)).mapTo[HistogramResponse]
        histogramFuture.map { histogram =>
          HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              histogram.toJson.prettyPrint
            )
          )
        }
      }

    case request: HttpRequest =>
      request.discardEntityBytes()
      Future {
        HttpResponse(status = StatusCodes.NotFound)
      }
  }

  Http().bindAndHandleAsync(requestHandler, host, port)
}
