package com.promptoven.gateway.router;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import com.promptoven.gateway.filter.JwtAuthorizationFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ServiceRouter {

	@Value("#{'${services.names}'.split(',')}")
	private List<String> serviceNames;

	@Autowired
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		var routes = builder.routes();
		routes = addSwaggerRoutes(routes);
		routes = addRoleBasedRoutes(routes);
		routes = addDefaultProtectedRoutes(routes);
		
		return routes.build();
	}

	private RouteLocatorBuilder.Builder addSwaggerRoutes(RouteLocatorBuilder.Builder routes) {
		for (String serviceName : serviceNames) {
			String serviceId = serviceName.toLowerCase();

			// API docs route with CORS
			routes = routes.route(serviceId + "-api-docs",
				r -> r.path("/" + serviceId + "/v3/api-docs")
					.filters(f -> f
						.rewritePath("/" + serviceId + "/v3/api-docs", "/v3/api-docs")
						.modifyResponseBody(String.class, String.class, (exchange, s) -> {
							if (s != null) {
								// Replace the server URL in the OpenAPI documentation
								return Mono.just(s.replaceAll(
									"\"servers\":\\s*\\[\\s*\\{\\s*\"url\":\\s*\"[^\"]*\"",
									"\"servers\":[{\"url\":\"http://localhost:8000\""
								));
							}
							return Mono.empty();
						})
						.addResponseHeader("Access-Control-Allow-Origin", "*")
						.addResponseHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
						.addResponseHeader("Access-Control-Allow-Headers",
							"Authorization, Refreshtoken, Content-Type, X-Requested-With, X-XSRF-TOKEN"))
					.uri("lb://" + serviceName)
			);
		}

		return routes;
	}


	private RouteLocatorBuilder.Builder addDefaultProtectedRoutes(RouteLocatorBuilder.Builder routes) {
		// Add default protected routes for each service
		for (String serviceName : serviceNames) {
			String serviceId = serviceName.toLowerCase();
			String baseServiceName = serviceId.replace("-service", "");

			// Default protected routes (requires authentication but no specific role)
			routes = routes.route(serviceId + "-default-routes",
				r -> r.path("/v1/" + baseServiceName + "/**")
					.filters(f -> f
						.stripPrefix(0)
						.addResponseHeader("Access-Control-Allow-Origin", "*")
						.addResponseHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
						.addResponseHeader("Access-Control-Allow-Headers",
							"Authorization, Refreshtoken, Content-Type, X-Requested-With, X-XSRF-TOKEN")
					)
					.uri("lb://" + serviceName)
			);
		}
		return routes;
	}

	private RouteLocatorBuilder.Builder addRoleBasedRoutes(RouteLocatorBuilder.Builder routes) {
		// For each service, add admin, seller, and member routes
		for (String serviceName : serviceNames) {
			String serviceId = serviceName.toLowerCase();
			String baseServiceName = serviceId.replace("-service", "");

			// Admin routes for this service
			routes = routes.route(baseServiceName + "-admin-routes",
				r -> r.path("/v1/admin/" + baseServiceName + "/**")
					.filters(f -> f
						.stripPrefix(0)
						.filter(jwtAuthorizationFilter.apply(new JwtAuthorizationFilter.Config()))
						.addResponseHeader("Access-Control-Allow-Origin", "*")
						.addResponseHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
						.addResponseHeader("Access-Control-Allow-Headers",
							"Authorization, Refreshtoken, Content-Type, X-Requested-With, X-XSRF-TOKEN")
					)
					.uri("lb://" + serviceName)
			);

			// Seller routes for this service
			routes = routes.route(baseServiceName + "-seller-routes",
				r -> r.path("/v1/seller/" + baseServiceName + "/**")
					.filters(f -> f
						.stripPrefix(0)
						.filter(jwtAuthorizationFilter.apply(new JwtAuthorizationFilter.Config()))
						.addResponseHeader("Access-Control-Allow-Origin", "*")
						.addResponseHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
						.addResponseHeader("Access-Control-Allow-Headers",
							"Authorization, Refreshtoken, Content-Type, X-Requested-With, X-XSRF-TOKEN")
					)
					.uri("lb://" + serviceName)
			);

			// Member routes for this service
			routes = routes.route(baseServiceName + "-member-routes",
				r -> r.path("/v1/member/" + baseServiceName + "/**")
					.filters(f -> f
						.stripPrefix(0)
						.filter(jwtAuthorizationFilter.apply(new JwtAuthorizationFilter.Config()))
						.addResponseHeader("Access-Control-Allow-Origin", "*")
						.addResponseHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
						.addResponseHeader("Access-Control-Allow-Headers",
							"Authorization, Refreshtoken, Content-Type, X-Requested-With, X-XSRF-TOKEN")
					)
					.uri("lb://" + serviceName)
			);
		}
		return routes;
	}
}
