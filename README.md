# ChatServer
This is an https server I created for a university course.  

Compiling:
```bash
mvn package
```

Running the server needs 3 arguments:  
- Path and file name to a database
- Https certificate path and file name
- Password of the certificate

For example with database.db and keystore.jks files in the "ChatServer-main" folder:
```bash
java -cp target/chatserver-1.0-SNAPSHOT-jar-with-dependencies.jar
```

This UML interaction diagram gives an overall view of the communication between the server and a client:
![UML](umldiagram.png)


