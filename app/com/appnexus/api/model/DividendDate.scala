package com.appnexus.api.model

import org.joda.time.DateTime

import com.github.tototoshi.slick.MySQLJodaSupport.datetimeTypeMapper

import slick.driver.MySQLDriver.api._

case class DividendDate(
  ticker: String,
  date: DateTime
)

class DividendDateTable(
  tag: Tag
) extends Table[DividendDate](
  tag, "dividend_date"
) {
  
  def ticker = column[String]("ticker", O.Length(10))
  def date = column[DateTime]("date")
  
  def * = (
    ticker,
    date
  ) <> (DividendDate.tupled, DividendDate.unapply)
  
  def tickerIdx = index("idx_dividend_date_ticker", ticker)
  def dateIdx = index("idx_dividend_date_date", (ticker, date), unique = true)
  
}

object DividendDateTableQuery extends TableQuery(new DividendDateTable(_))
