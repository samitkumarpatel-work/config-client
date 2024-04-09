# config-client

To reload the configuration from config server, we need to send a POST request to the /actuator/refresh endpoint. This endpoint is exposed by Spring Boot Actuator.

```shell
curl -X POST http://localhost:8080/actuator/refresh
```