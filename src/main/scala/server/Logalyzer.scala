package server

import actors.DataProcessor
import actors.DataProcessor.{GetGeneratedDataResponse, GetGeneratedHistogramResponse}
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import models.{DataResponse, HistogramResponse, RequestPayload, Size, Status}
import traits.JsonProtocol
import spray.json.enrichAny
import utils.FileUtil.{getFileSize, getStatus}

import scala.concurrent.duration.DurationInt
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings

import scala.concurrent.Future
import scala.collection.JavaConverters._

object Logalyzer extends App with JsonProtocol with SprayJsonSupport {

  implicit val system = ActorSystem("Logalyer")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val dataReceiver = system.actorOf(Props[DataProcessor], "LogalyzerDataProcessor")
  implicit val defaultTimeout = Timeout(10 seconds)

  val config = ConfigFactory.load()
  val loadedConfig = config.getConfig("logalyzer")
  val host = loadedConfig.getValue("host").render().replaceAll("\"", "")
  val port = System.getenv().asScala.getOrElse("-Dhttp.port", loadedConfig.getValue("port").render().replaceAll("\"", "")).toInt
  val fileFullPath = loadedConfig.getValue("file_location").render().replaceAll("\"", "")

  val corsSettings = CorsSettings.defaultSettings.withAllowGenericHttpRequests(true)
  val corsSettingsWithOrigin = corsSettings.withAllowedOrigins(HttpOriginMatcher.*)


  val route: Route = cors(corsSettings) {

    pathPrefix("api") {
      path("get_status") {
        get {
          val response: Status = getStatus(fileFullPath)
          complete(HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              response.toJson.prettyPrint
            )
          )
          )
        } ~ post {
          complete(StatusCodes.Forbidden)
        } ~ put {
          complete(StatusCodes.Forbidden)
        } ~ delete {
          complete(StatusCodes.Forbidden)
        }
      } ~
      path("get_size") {
        get {
          val response: Size = getFileSize(fileFullPath)
          complete(HttpResponse(
            StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              response.toJson.prettyPrint
            )
          )
          )
        } ~ post {
          complete(StatusCodes.Forbidden)
        } ~ put {
          complete(StatusCodes.Forbidden)
        } ~ delete {
          complete(StatusCodes.Forbidden)
        }
      } ~
      path("data") {
        (post & entity(as[RequestPayload]) & extractLog) { (payload, log) => {
          complete{
            val dataFuture: Future[DataResponse] = (dataReceiver ? GetGeneratedDataResponse(payload.datetimeFrom, payload.datetimeUntil, payload.phrase, fileFullPath)).mapTo[DataResponse]
            dataFuture.map { data =>
              log.info("Data retrieve successful")
              HttpResponse(
                StatusCodes.OK,
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  data.toJson.prettyPrint
                )
              )
            }
          }
        }

        } ~ get {
          complete(StatusCodes.Forbidden)
        } ~ put {
          complete(StatusCodes.Forbidden)
        } ~ delete {
          complete(StatusCodes.Forbidden)
        }
      } ~
      path("histogram") {
        (post & entity(as[RequestPayload]) & extractLog) { (payload, log) => {
          complete{
            val dataFuture: Future[HistogramResponse] = (dataReceiver ? GetGeneratedHistogramResponse(payload.datetimeFrom, payload.datetimeUntil, payload.phrase, fileFullPath)).mapTo[HistogramResponse]
            dataFuture.map { data =>
              log.info("Data retrieve successful")
              HttpResponse(
                StatusCodes.OK,
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  data.toJson.prettyPrint
                )
              )
            }
          }
        }

        } ~ get {
          complete(StatusCodes.Forbidden)
        } ~ put {
          complete(StatusCodes.Forbidden)
        } ~ delete {
          complete(StatusCodes.Forbidden)
        }
      }

    }
  }

  Http().bindAndHandle(route, host, port)
}
