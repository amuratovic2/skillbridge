FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
WORKDIR /workspace/services/api-gateway
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests dependency:go-offline

COPY services/api-gateway/src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package
RUN mkdir /layers && java -Djarmode=layertools -jar target/*.jar extract --destination /layers

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom" \
    SPRING_OUTPUT_ANSI_ENABLED=never

COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./

USER spring:spring
EXPOSE 3000
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
