import Dependencies._

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.slingmedia",
      scalaVersion := "2.11.8",
      version      := "0.1.0"
    )),
    name := "micro-container-matcher",
    libraryDependencies ++= Seq(
  		spark,
  		logback,
  		scalaLogger,
  		kafka,
  		mongoSpark,
  		databricksCSV
	),
	assemblyMergeStrategy in assembly := {
		case PathList("META-INF", xs @ _*) => MergeStrategy.discard
		case x => MergeStrategy.first
	}	
  )