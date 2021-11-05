FROM openjdk:11
COPY lib/* /api/lib/
COPY lingua-api.jar /api
WORKDIR /api
CMD ["java", "-jar", "lingua-api.jar"]