#!/usr/bin/env sh
DOCKER_IMAGE_TYPE=`echo $INPUT_TAG | cut -f2 -d '/' | cut -f1 -d '_'`
echo "Going to work on Docker image type $DOCKER_IMAGE_TYPE"

case "$DOCKER_IMAGE_TYPE" in
        sports-cloud-rest)
        	cd /tmp
        	rm -rf /tmp/sports-cloud-rest-server
			cp -rf $BASE_PATH/../../sports-cloud-rest-server .
			cd sports-cloud-rest-server
			mvn clean package
			echo "Creating the jars directory"
			mkdir -p $BASE_PATH/docker/containers/Docker-Rest/jars
			echo "Copying to jars directory"
			cp /tmp/sports-cloud-rest-server/target/sports-cloud-rest-server*.jar $BASE_PATH/docker/containers/Docker-Rest/jars/sports-cloud-rest-server.jar
            ;;
        sc-cp-connect)
			cd /tmp
			rm -rf /tmp/sports-cloud-parsers
			cp -rf $BASE_PATH/../../sports-cloud-parsers  .
			cd sports-cloud-parsers
			### Add the eneco ftp jar to maven
			$MAVEN_HOME/bin/mvn install:install-file -DgroupId=com.eneco  \
			-DartifactId=kafka-connect-ftp  \
			-Dversion=0.0.0-unspecified  \
			-Dfile=/tmp/sports-cloud-parsers/libs/kafka-connect-ftp-0.1.7-8-kafka-0.10.2.0.jar  \
			-Dpackaging=jar \
			-DgeneratePom=true

			### Build and generate the jar and copy to the image
			sbt clean assembly
			mkdir -p $BASE_PATH/docker/containers/Docker-ConfluentConnect/jars
			cp target/scala-2.11/kafka-schedule-parser-assembly-*.jar $BASE_PATH/docker/containers/Docker-ConfluentConnect/jars/kafka-schedule-parser-assembly.jar
			;;
		spark-worker)
			cd /tmp
			rm -rf /tmp/micro-content-matcher
			cp -rf $BASE_PATH/../../micro-content-matcher  .
			cd micro-content-matcher
			sbt clean assembly
			mkdir -p $BASE_PATH/docker/containers/Docker-Spark/spark-worker/jars
			cp /tmp/micro-content-matcher/target/scala-*/micro-container-*.jar $BASE_PATH/docker/containers/Docker-Spark/spark-worker/jars/all-spark-jobs.jar
			;;
		spark-job)
			cd /tmp
			rm -rf /tmp/micro-content-matcher
			cp -rf $BASE_PATH/../../micro-content-matcher  .
			cd micro-content-matcher
			sbt clean assembly
			mkdir -p $BASE_PATH/docker/containers/Docker-SparkJob/jars
			cp /tmp/micro-content-matcher/target/scala-*/micro-container-*.jar $BASE_PATH/docker/containers/Docker-SparkJob/jars/all-spark-jobs.jar
			;;
		sc-job-scheduler)
			cd /tmp
			rm -rf /tmp/sports-cloud-k8s-schedulers
			cp -rf $BASE_PATH/../../sports-cloud-k8s-schedulers  .
			cd sports-cloud-k8s-schedulers
			sbt clean assembly
			
			### Add the jar to the docker image
			mkdir -p $BASE_PATH/docker/containers/Docker-ScheduledJob/deploy-scheduled-jobs/libs
			cp target/scala-*/sports-cloud-*-schedulers-assembly-*.jar $BASE_PATH/docker/containers/Docker-ScheduledJob/deploy-scheduled-jobs/libs/sports-cloud-schedulers-assembly.jar
			mkdir -p $BASE_PATH/docker/containers/Docker-ScheduledJob/deploy-scheduled-jobs/scripts/kafka/connect
			cp -rf $CONFIG_PATH/worker-config $BASE_PATH/docker/containers/Docker-ScheduledJob/deploy-scheduled-jobs/scripts/kafka/connect

			#### rest-proxy-parser
			cd /tmp
			rm -rf /tmp/sports-cloud-rest-parsers
			cp -rf $BASE_PATH/../../sports-cloud-rest-parsers  .
			cd sports-cloud-rest-parsers
			sbt clean assembly
			cp target/scala-*/kafka-schedule-rest-parsers-assembly-*.jar $BASE_PATH/docker/containers/Docker-ScheduledJob/deploy-scheduled-jobs/libs/kafka-schedule-rest-parsers-assembly.jar
			;;
        *)
            echo $"Nothing special to do"
            ;;
esac