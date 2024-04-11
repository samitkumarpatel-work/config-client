package dev.net.configclient;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

	final Config config;

	public static void main(String[] args) {
		SpringApplication.run(ConfigClientApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> routerFunction() {
		System.out.println(config);
		return RouterFunctions
				.route()
				.GET("/api/static", serverRequest -> ServerResponse.ok().bodyValue(config))
				.GET("/api/dynamic", this::dynamicProperties)
				.build();
	}

	@Value("${localProperties:}")
	private String localProperties;

	@Value("${hello:defaultHello}")
	private String hello;

	final Environment environment;

	private Mono<ServerResponse> dynamicProperties(ServerRequest request) {
		var valueFromEnv = Objects.requireNonNullElseGet(environment.getProperty("hello", String.class), () -> "NULL");
		System.out.println(valueFromEnv);

		System.out.println(Objects.requireNonNullElseGet(environment.getProperty("legacyCountryCodes", List.class), () -> List.of("key","value")));

		return ServerResponse
				.ok()
				.bodyValue(
						Map.of(
								"hello", hello,
								"helloFromEnv", valueFromEnv,
								"localProperties", localProperties
						)
				);
	}

}

@ConfigurationProperties(prefix = "")
@Component
@Data
class Config {
	String[] supportedCountriesCode;
	String[] supportedCountriesCodePrefix;
	String[] legacyCountryCodes;
	String serverName;
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

		refreshEndpoint.refresh();

	}
}