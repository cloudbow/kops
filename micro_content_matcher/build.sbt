import Dependencies._

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.slingmedia",
      scalaVersion := "2.12.1",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "micro_container_matcher",
    libraryDependencies ++= Seq(
  		scalaTest % Test,
  		scalaMongo,
  		httpcomponents,
  		scalaparser
	)

  )
