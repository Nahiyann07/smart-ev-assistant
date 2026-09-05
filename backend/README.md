# Backend

This folder contains the Java 21 Spring Boot application, security configuration, database migrations, API/page controllers, service layer, and automated tests.

The build intentionally imports the Thymeleaf templates and browser assets from `../frontend`, keeping one deployable application while making the frontend/backend ownership clear.

Run from this folder:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The Dockerfile uses the repository root as its build context because it packages both folders.
