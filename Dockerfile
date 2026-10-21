
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM tomcat:9.0-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/blood-bank-management.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 10000
CMD ["sh", "-c", "sed -i 's/port=\"8080\"/port=\"10000\"/' /usr/local/tomcat/conf/server.xml && catalina.sh run"]
