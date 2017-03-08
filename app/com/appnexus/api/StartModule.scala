package com.appnexus.api

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

import com.appnexus.api.loader.CacheLoader
import com.appnexus.api.model.DividendDateTableQuery
import com.appnexus.api.model.HistoricalDataTableQuery
import com.google.inject.AbstractModule

import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.Environment
import play.api.Logger
import slick.driver.MySQLDriver.api.DBIO
import slick.driver.MySQLDriver.api.actionBasedSQLInterpolation
import slick.driver.MySQLDriver.api.schemaActionExtensionMethods
import slick.driver.MySQLDriver.api.tableQueryToTableQueryExtensionMethods

class StartModule(
  environment: Environment, configuration: Configuration
) extends AbstractModule {
  
  override def configure() = {
    bind(classOf[MHQConfiguration])  
    bind(classOf[CacheLoader])
    bind(classOf[StartHook]).asEagerSingleton()
  }
  
}

@Singleton
class StartHook @Inject() (config: MHQConfiguration) {
  config.db.run {
    DBIO.seq(
      sqlu"DROP TABLE IF EXISTS #${HistoricalDataTableQuery.baseTableRow.tableName} CASCADE",
      HistoricalDataTableQuery.schema.create,
      sqlu"DROP TABLE IF EXISTS #${DividendDateTableQuery.baseTableRow.tableName} CASCADE",
      DividendDateTableQuery.schema.create
    )
  }.andThen {
    case Success(_) => Logger.info("DB ready")
    case Failure(e) => Logger.error("DB not ready", e)
  }
}


