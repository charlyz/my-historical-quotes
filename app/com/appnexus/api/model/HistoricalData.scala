package com.appnexus.api.model

import org.joda.time.DateTime

import com.github.tototoshi.slick.MySQLJodaSupport.datetimeTypeMapper

import slick.driver.MySQLDriver.api._

case class HistoricalData(
  ticker: String,
  date: DateTime,
  open: Float,
  close: Float
)

class HistoricalDataTable(
  tag: Tag
) extends Table[HistoricalData](
  tag, "historical_data"
) {
  
  def ticker = column[String]("ticker", O.Length(10))
  def date = column[DateTime]("date")
  def open = column[Float]("open")
  def close = column[Float]("close")
  
  def * = (
    ticker,
    date, 
    open,
    close
  ) <> (HistoricalData.tupled, HistoricalData.unapply)
  
  def tickerIdx = index("idx_historical_data_ticker", ticker)
  def dateIdx = index("idx_historical_data_date", (ticker, date), unique = true)
  def openIdx = index("idx_historical_data_open", (ticker, open))
  def closeIdx = index("idx_historical_data_close", (ticker, close))
  
}

object HistoricalDataTableQuery extends TableQuery(new HistoricalDataTable(_))
