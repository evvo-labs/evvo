FROM openjdk:latest
ARG JAR_FILE
ARG WORKDIR=/opt/docker/bin/evvo
ARG JAR_FILE_PATH=${WORKDIR}/service.jar
ENV JAR_FILE_PATH=${JAR_FILE_PATH}
WORKDIR ${WORKDIR}
ADD target/${JAR_FILE} ${JAR_FILE_PATH}
RUN chmod +x "/opt/docker/bin/evvo/service.jar"
ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/docker/bin/evvo/service.jar"]
