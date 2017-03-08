package com.appnexus.api

import javax.inject.Singleton
import slick.driver.MySQLDriver.api.Database

@Singleton
class MHQConfiguration {
  val db = Database.forURL("jdbc:h2:mem:test;MODE=MYSQL;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
}