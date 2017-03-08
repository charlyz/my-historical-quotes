resolvers ++= Seq(
  "AN Releases" at "http://nexus.corp.appnexus.com/nexus/content/repositories/releases/",
  "AN Snapshots" at "http://nexus.corp.appnexus.com/nexus/content/repositories/snapshots/",
  "Typesafe Releases"       at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Maven Releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
  "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases/",
  "GridGrain" at "http://www.gridgainsystems.com/nexus/content/repositories/external/" 
)

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.12")
