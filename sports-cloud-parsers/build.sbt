import Dependencies._

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.slingmedia.slingtv.kafka",
      scalaVersion := "2.11.8",
      version      := "0.1.0"
    )),
    name := "kafka-schedule-parser",
    resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
    assemblyMergeStrategy in assembly := {
		case PathList("META-INF", xs @ _*) => MergeStrategy.discard
		case x => MergeStrategy.first
	},
    libraryDependencies ++= Seq(
  		scalaTest % Test,
  		kafkalib,
  		ftpKafka,
  		scalaXML,
  		scalaparser
	)

  )
