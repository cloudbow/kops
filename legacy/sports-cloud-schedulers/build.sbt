import Dependencies._
import ReleaseTransformations._

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.slingmedia",
      scalaVersion := "2.12.2",
      version      := "0.1.0"
    )),
    name := "sports-cloud-schedulers",
    mainClass in assembly := Some("com.slingmedia.sportscloud.schedulers.SportsCloudSchedulers"),
    libraryDependencies ++= Seq(
  		scalaTest % Test,
  		gson,
  		fasterXML,
  		quartzScheduler,
  		jaywayJson,
  		httpcomponents,
  		scalaparser,
  		scalaLogger,
  		logback
	),
	assemblyMergeStrategy in assembly := {
		case PathList("META-INF", xs @ _*) => MergeStrategy.discard
		case x => MergeStrategy.first
	},
	publishTo := Some("<https://svn.slingmedia.com:443> Sling Media Subversion Repository" at "https://svn.slingmedia.com/repos/maven2"),
	credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
	releaseProcess := Seq[ReleaseStep](
		publishArtifacts
	)

  )
