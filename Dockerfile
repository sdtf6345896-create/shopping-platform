# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# 先只複製 pom.xml 下載依賴,原始碼變動時可以沿用這層快取
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --create-home appuser && mkdir -p /app/uploads && chown appuser /app/uploads
COPY --from=build /workspace/target/shopping-platform-*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
