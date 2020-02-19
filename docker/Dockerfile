FROM openjdk:latest
#"openjdk:8-jre-alpine"
#http://blog.michaelhamrah.com/2014/03/running-an-akka-cluster-with-docker-containers/
ARG JAR_FILE
ARG WORKDIR=/opt/docker/bin/evvo
ARG JAR_FILE_PATH=${WORKDIR}/service.jar
ENV JAR_FILE_PATH=${JAR_FILE_PATH}
WORKDIR ${WORKDIR}

ADD target/${JAR_FILE} ${JAR_FILE_PATH}
ADD docker/run_service.sh ${WORKDIR}/run_service.sh

RUN chmod +x "/opt/docker/bin/evvo/run_service.sh"
ENTRYPOINT ["/opt/docker/bin/evvo/run_service.sh"]
