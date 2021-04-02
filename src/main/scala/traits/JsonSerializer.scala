package traits

import models.{Data, DataResponse, Error, HighlightPosition, Histogram, HistogramResponse, RequestPayload, Size, Status}
import spray.json.DefaultJsonProtocol

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
