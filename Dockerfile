FROM eclipse-temurin:25-jdk
ARG ARTIFACT_NAME
ARG IMAGE_VERSION
ENV JAVA_HOME=/opt/java/openjdk
EXPOSE 8080
ADD target/${ARTIFACT_NAME}*.jar ${ARTIFACT_NAME}.jar
ADD java.security ${JAVA_HOME}/conf/security/
RUN printf "IMAGE_VERSION=${IMAGE_VERSION}" > version.properties
COPY entrypoint.sh ./entrypoint.sh
RUN chmod +x ./entrypoint.sh
ENTRYPOINT ["/bin/bash", "./entrypoint.sh"]
