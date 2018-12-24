Introduction
============
Arcus Service broker for cloudfoundry.

How To Build and Run
====================
To build the project
./gradlew build


The build command creates jar file with embedded tomcat container.
java -jar build/libs/openpaas-service-java-broker-arcus.jar


Configuration
=============
By default,
* the tomcat server is listening at port `8080`
* requires local arcus server 
* configurations can be changed by modifying the file 'resources/application-mvc.properties'


Routes
======
|Routes|Method|Description|
|------|------|-----------|
|/v2/catalog|GET|Service and its plan details by this broker|
|/v2/service_instances/:id|PUT|create a arcus cache for this service|
|/v2/service_instances/:id|DELETE|delete previously created arcus for this service|
|/v2/service_instances/:id/service_bindings/:id|PUT|create user and grant privilege for the arcus associated with service.|
|/v2/service_instances/:id/service_bindings/:id|DELETE|delete the user created previously for this binding.|

