package dev.net.configclient;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@SpringBootApplication
@RequiredArgsConstructor
public class ConfigClientApplication {



	public static void main(String[] args) {
		SpringApplication.run(ConfigClientApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> routerFunction(Handlers handlers, Config config) {
		System.out.println(config);
		return RouterFunctions
				.route()
				.GET("/api/static", request -> ServerResponse.ok().bodyValue(config))
				.GET("/api/dynamic", handlers::dynamicProperties)
				.build();
	}
}

@Component
@RefreshScope
@RequiredArgsConstructor
class Handlers {
	final Config config;
	final JavaRecord javaRecord;
	final Environment environment;

	@Value("${localProperties:}")
	private String localProperties;

	@Value("${hello:defaultHello}")
	//@Value("#{ environment['hello'] }")
	//@Value("#{ config.hello }")
	private String hello;

	public Mono<ServerResponse> dynamicProperties(ServerRequest request) {
		var valueFromEnv = Objects.requireNonNullElseGet(environment.getProperty("hello", String.class), () -> "NULL");
		System.out.println(valueFromEnv);
		System.out.println(Objects.requireNonNullElseGet(environment.getProperty("legacyCountryCodes", List.class), () -> List.of("key","value")));

		return ServerResponse
				.ok()
				.bodyValue(
						Map.of(
								"@Value.hello", hello,
								"ConfigurationProperties.hello", config.getHello(),
								"Environment.hello", valueFromEnv,
								"Record.hello", javaRecord.hello(),
								"localProperties", localProperties
						)
				);
	}
}

/*@Component
@RefreshScope //Caused by: java.lang.IllegalArgumentException: Cannot subclass final class dev.net.configclient.JavaRecord
record JavaRecord(@Value("${hello}") String hello) {}*/

@Component
@RefreshScope
class JavaRecord {
	private final String hello;

	public String hello() {
		return hello;
	}
	public JavaRecord(@Value("${hello}") String hello) {
		this.hello = hello;
	}
}


@ConfigurationProperties()
//OR
// @ConfigurationProperties(prefix = "")
@Component
@Data
class Config {
	String[] supportedCountriesCode;
	String[] supportedCountriesCodePrefix;
	String[] legacyCountryCodes;
	String serverName;
	String hello;
}

@Configuration
@EnableScheduling
@RequiredArgsConstructor
class Scheduler {

	final WebClient.Builder builder;
	final RefreshEndpoint refreshEndpoint;

	@Scheduled(fixedDelay = 5000)
	void scheduler() {
		System.out.println("######### REFRESHING CONFIGURATION ##########");

		/* builder
				 .baseUrl("http://localhost:8080")
				 .build()
				 .post()
				 .uri(uriBuilder -> uriBuilder.path("/actuator/refresh").build())
				 .retrieve()
				 .bodyToMono(Object.class)
				 .subscribe();*/

		// OR

		refreshEndpoint.refresh().forEach(System.out::println);
	}
}