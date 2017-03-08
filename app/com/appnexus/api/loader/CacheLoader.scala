package com.appnexus.api.loader

import java.util.concurrent.atomic.AtomicBoolean

import scala.collection.mutable.{ Set => MSet }
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import org.joda.time.DateTime

import com.appnexus.api.MHQConfiguration
import com.appnexus.api.model.DividendDate
import com.appnexus.api.model.DividendDateTableQuery
import com.appnexus.api.model.HistoricalData
import com.appnexus.api.model.HistoricalDataTableQuery

import javax.inject.Inject
import javax.inject.Singleton
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsError
import play.api.libs.json.JsLookupResult.jsLookupResultToJsLookup
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsUndefined
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json.__
import play.api.libs.ws.WSClient
import slick.driver.MySQLDriver.api.DBIO
import slick.driver.MySQLDriver.api.queryInsertActionExtensionMethods

@Singleton
class CacheLoader @Inject() (config: MHQConfiguration, ws: WSClient)  {
    
  val baseUrl = "https://query.yahooapis.com/v1/public/yql" 
  val timeout = 10.seconds
  val cachedTickers = MSet[String]()
  val isCaching = new AtomicBoolean(false)

  implicit val historicalDataReads = (
    (__ \ "Symbol").read[String] and
    (__ \ "Date").read[DateTime] and
    (__ \ "Open").read[String].map(_.toFloat) and
    (__ \ "Close").read[String].map(_.toFloat)
  )(HistoricalData.apply _)
  
  implicit val dividenDateReads = (
    (__ \ "Symbol").read[String] and
    (__ \ "Date").read[DateTime]
  )(DividendDate.apply _)
  
  def cacheTicker(ticker: String) = if (isCaching.compareAndSet(false, true)) {
    cachedTickers.contains(ticker) match {
      case true => Future.failed(new Exception(s"$ticker is already cached"))
      case false => {
        getHistoricalData(ticker).flatMap { historicalData =>
          getDividendDates(ticker).flatMap { dividendDates => 
            val insertHistoricalDataActions = historicalData.map { historicalData =>
              HistoricalDataTableQuery += historicalData
            }
            
            val insertDividendDatesActions = dividendDates.map { dividendDate =>
              DividendDateTableQuery += dividendDate
            }
            
            config.db.run {
              DBIO.seq((insertHistoricalDataActions ++ insertDividendDatesActions): _*)
            }.map { _ =>
              cachedTickers.add(ticker)
            }.andThen { case _ =>
              isCaching.set(false)
            }
          }
        }
      }
    }
  } else {
    Future.failed(new Exception("Already caching, try later"))
  }

  def getHistoricalData(ticker: String) = {
    val queryString = Map(
      "q" -> {
        s"""select * from yahoo.finance.historicaldata where symbol = "$ticker" """ + 
        """and startDate = "2011-01-01" and endDate = "2012-01-01""""
      },
      "diagnostic" -> "false",
      "env" -> "store://datatables.org/alltableswithkeys",
      "format" -> "json"
    )
    
    val requestFuture = ws
      .url(baseUrl)
      .withQueryString(queryString.toSeq: _*)
      .withRequestTimeout(timeout)
      .get()
      
    requestFuture.map { response =>
      response.json \ "query" \ "results" \ "quote" match {
        case _: JsUndefined => Seq()
        case quoteJson => {
          quoteJson.validate[Seq[HistoricalData]] match {
            case JsSuccess(result, _) => result
            case JsError(e) => throw new Exception(s"Error happened while validating an object: ${e}}") 
          }
        }
      }
    }
  }
  
  def getDividendDates(ticker: String) = {
    val queryString = Map(
      "q" -> {
        s"""select * from yahoo.finance.dividendhistory where symbol = "$ticker" """ + 
        """and startDate = "2011-01-01" and endDate = "2012-01-01""""
      },
      "diagnostic" -> "false",
      "env" -> "store://datatables.org/alltableswithkeys",
      "format" -> "json"
    )
    
    val requestFuture = ws
      .url(baseUrl)
      .withQueryString(queryString.toSeq: _*)
      .withRequestTimeout(timeout)
      .get()
      
    requestFuture.map { response =>
      response.json \ "query" \ "results" \ "quote" match {
        case _: JsUndefined => Seq()
        case quoteJson => {
          quoteJson.validate[Seq[DividendDate]] match {
            case JsSuccess(result, _) => result
            case JsError(e) => throw new Exception(s"Error happened while validating an object: ${e}}") 
          }
        }
      }
    }
  }

}


