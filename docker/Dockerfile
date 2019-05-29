FROM openjdk:latest
#"openjdk:8-jre-alpine"
#http://blog.michaelhamrah.com/2014/03/running-an-akka-cluster-with-docker-containers/
ARG JAR_FILE
ARG WORKDIR=/opt/docker/bin/evvo
ARG JAR_FILE_PATH=${WORKDIR}/service.jar
ENV JAR_FILE_PATH=${JAR_FILE_PATH}
WORKDIR ${WORKDIR}
RUN yum install net-tools -y

ADD target/${JAR_FILE} ${JAR_FILE_PATH}
ADD docker/run_service.sh ${WORKDIR}/run_service.sh
ADD src/main/resources/application.conf ${WORKDIR}/src/main/resources/application.conf

RUN chmod +x "/opt/docker/bin/evvo/service.jar"
RUN chmod +x "/opt/docker/bin/evvo/run_service.sh"

EXPOSE 3449

ENTRYPOINT ["/opt/docker/bin/evvo/run_service.sh > ./ras.log"]
