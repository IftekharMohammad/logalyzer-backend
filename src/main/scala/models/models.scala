package models

case class Status(status: String)
case class Error(message: String)

case class Size(size: Long)

case class RequestPayload(datetimeFrom: String, datetimeUntil: String, phrase: String)

case class HighlightPosition(fromPosition: Int, toPosition: Int)
case class Data(datetime: String, message: String, highlightText:List[HighlightPosition])
case class DataResponse(data: List[Data], datetimeFrom: String, datetimeUntil: String, phrase: String)


case class Histogram(datetime: String, count: Int)
case class HistogramResponse(histogram: List[Histogram], datetimeFrom: String, datetimeUntil: String, phrase: String)
