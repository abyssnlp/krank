FROM openjdk:11-jdk-slim as build

WORKDIR /app

COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
COPY src/ src/
COPY gradlew ./

RUN ./gradlew build --no-daemon

FROM openjdk:11-jre-slim
ARG VERSION

WORKDIR /app

COPY --from=build /app/build/libs/krank-${VERSION}-standalone.jar /app/krank.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/krank.jar"]
