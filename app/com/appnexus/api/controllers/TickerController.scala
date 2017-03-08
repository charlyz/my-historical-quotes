package com.appnexus.api.controllers

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.appnexus.api.MHQConfiguration
import com.appnexus.api.loader.CacheLoader
import com.appnexus.api.model.DividendDateTableQuery
import com.appnexus.api.model.HistoricalDataTableQuery
import com.github.tototoshi.slick.MySQLJodaSupport.datetimeTypeMapper

import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsError
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.Ok
import slick.driver.MySQLDriver.api.booleanColumnExtensionMethods
import slick.driver.MySQLDriver.api.booleanColumnType
import slick.driver.MySQLDriver.api.columnExtensionMethods
import slick.driver.MySQLDriver.api.columnToOrdered
import slick.driver.MySQLDriver.api.floatColumnType
import slick.driver.MySQLDriver.api.streamableQueryActionExtensionMethods
import slick.driver.MySQLDriver.api.stringColumnType
import slick.driver.MySQLDriver.api.valueToConstColumn

class TickerController @Inject() (config: MHQConfiguration, cacheLoader: CacheLoader) {
  
  implicit val payloadReads = Json.reads[Payload]
  implicit val responseWrites = Json.writes[Response]
  implicit val messageWrites = Json.writes[Message]
  
  def getQuotes() = Action.async { request =>
    Try {
      request.body.asJson match {
        case Some(json) => json.validate[Payload] match {
          case JsSuccess(payload, _) => payload
          case JsError(e) => throw new Exception(s"Could not parse payload: $e")
        }
        case None => throw new Exception("No payload")
      }
    } match {
      case Success(payload) => {
        cacheLoader.cachedTickers.contains(payload.ticker) match {
          case true => {
            val joinedQuery = HistoricalDataTableQuery
              .joinLeft(DividendDateTableQuery)
              .on { (historicalData, dividendDate) => 
                historicalData.ticker === dividendDate.ticker &&
                historicalData.date === dividendDate.date
              }
              .filter(_._1.ticker === payload.ticker)
            
            val sortedQuery = payload.order match {
              case "open.asc" => joinedQuery.sortBy(_._1.open.asc)
              case "open.desc" => joinedQuery.sortBy(_._1.open.desc)
              case "close.asc" => joinedQuery.sortBy(_._1.close.asc)
              case "close.desc" => joinedQuery.sortBy(_._1.close.asc)
              case "date.asc" => joinedQuery.sortBy(_._1.date.asc)
              case "date.desc" => joinedQuery.sortBy(_._1.date.desc)
              case _ => joinedQuery.sortBy(_._1.date.asc)
            }
            
            val limitedQuery = sortedQuery.drop(payload.offset).take(payload.limit)
            
            val resultsFuture = config.db.run(limitedQuery.result)
            
            resultsFuture.map { rows => 
              val response = rows.map { case (historicalData, dividendDateOpt) =>
                Response(
                  historicalData.ticker,
                  historicalData.date.toString(),
                  historicalData.open,
                  historicalData.close,
                  is_dividend_day = dividendDateOpt.isDefined
                )
              }
              Ok(Json.toJson(response))
            }
          }
          case false => {
            cacheLoader.cacheTicker(payload.ticker)
            Future.successful(
              Ok(Json.toJson(Message("Quotes and dividend dates are being cached. Try later.")))
            )
          }
        }
      }
      case Failure(e) => Future.successful(
        BadRequest(Json.toJson(Message(e.getMessage)))
      )
    }
  }
  
}

case class Response(
  ticker: String,
  date: String,
  open: Float,
  close: Float,
  is_dividend_day: Boolean
)

case class Message(message: String)

case class Payload(
  ticker: String,
  limit: Int,
  offset: Int,
  order: String
)