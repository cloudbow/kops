#!/usr/bin/env sh
## All the tags for all the images
DOCKER_IMAGE_TYPE=`echo $INPUT_TAG | cut -f2 -d '/' | cut -f1 -d '_'`
TAG=`echo $INPUT_TAG | cut -f2 -d '/' | cut -f1 -d '_'`:`echo $INPUT_TAG | cut -f2 -d '/' | cut -f2 -d '_'`
echo "Going to work on TAG $TAG"

## The real docker id to push to
DOCKER_ID=registry.sports-cloud.com:5000


echo "Using base path $BASE_PATH"


case "$DOCKER_IMAGE_TYPE" in
        sports-cloud-rest)
            echo "Build REST Layer"
    		$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Rest "$TAG" "$DOCKER_ID"
            ;;
        artifact-server)
            echo "Build artifact server"
			$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-ArtifactsServer "$TAG" "$DOCKER_ID"
            ;;
        sc-job-scheduler)
			echo "Building job scheduler"
			$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-ScheduledJob "$TAG" "$DOCKER_ID"
            ;;
        sc-cp-connect)
            echo "Build kafka connect"
			$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-ConfluentConnect "$TAG" "$DOCKER_ID"
            ;;
        zeppelin)
            echo "Building zeppelin"
			$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Spark/zeppelin "$TAG" "$DOCKER_ID"
    		;;
        spark-base)
			echo "Building spark base"
			$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Spark/spark-base "$TAG" "$DOCKER_ID"
			;;
		spark-master)
			echo "Building spark master"
			$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Spark/spark-master "$TAG" "$DOCKER_ID"
			;;
		spark-worker)
			echo "Building spark worker"
			$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Spark/spark-worker "$TAG" "$DOCKER_ID"
			;;
		spark-job)
			echo "Building spark job"
			$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-SparkJob "$TAG" "$DOCKER_ID"
            ;;         
        *)
            echo $"Invalid tag . Pass the right tag!! or configure here"
            exit 1
esac
$BASE_PATH/scripts/docker/push-docker-container.sh $DOCKER_ID/$TAG
