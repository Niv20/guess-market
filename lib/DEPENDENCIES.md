# Runtime and build dependencies

The repository includes dependencies so the grader does not need a dependency manager or network access.

| Dependency | Version | Use | Source |
| --- | --- | --- | --- |
| Gson | 2.13.2 | JSON in client and server | https://github.com/google/gson |
| Jakarta Servlet API | 6.0.0 | Compile server only; Tomcat supplies runtime | https://jakarta.ee/specifications/servlet/6.0/ |
| Error Prone annotations | 2.41.0 | Gson compile-time annotations only | https://github.com/google/error-prone |
| JAXB API and runtime | 4.0.2 / 4.0.5 | XML on server | https://eclipse-ee4j.github.io/jaxb-ri/ |
| JavaFX | 25 | Client controls, FXML and graphics | https://openjfx.io/ |

`provided/jakarta.servlet-api-6.0.0.jar` is deliberately excluded from the WAR.
JavaFX is deliberately excluded from the WAR. The client contains no engine or JAXB jars.
The bundled libraries retain their original license metadata under `META-INF`.
The application targets Tomcat 10.1 (Jakarta Servlet 6.0) and Java 25.
