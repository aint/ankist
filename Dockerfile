FROM openjdk:8-jre-alpine
COPY target/scala-*/Ankist-assembly-*.jar /home/ankist.jar
CMD ["java","-jar","/home/ankist.jar"]
