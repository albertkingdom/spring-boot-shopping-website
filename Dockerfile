FROM openjdk:17
#ENV CLEARDB_DATABASE_URL=jdbc:mysql://b2df1be427cd80:049cc270@us-cdbr-east-05.cleardb.net/heroku_5b08af386fbd4e1?reconnect=true \
#     CLEARDB_DATABASE_USERNAME=b2df1be427cd80 \
#     CLEARDB_DATABASE_PWD=049cc270
COPY target/shopping-website-0.0.1-SNAPSHOT.jar shopping-website-backend-docker.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","shopping-website-backend-docker.jar"]
