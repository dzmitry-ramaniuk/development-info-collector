# Только runtime: компилятора, исходников и Maven в build context нет.
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre
FROM ${RUNTIME_IMAGE}
WORKDIR /app
COPY --chown=10001:10001 app.jar /app/app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
