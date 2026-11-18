package com.example.tenisclubreservations.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

  @Bean
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokens) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> {
          response.setHeader("WWW-Authenticate", "Bearer");
          response.setStatus(401);
        }).accessDeniedHandler((request, response, ex) -> response.setStatus(403)))
        .authorizeHttpRequests(a -> a
            .requestMatchers("/api/openapi", "/api/openapi/**", "/api/swagger-ui.html", "/swagger-ui/**", "/api/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
            .requestMatchers("/api/auth/refresh").authenticated()
            .requestMatchers("/api/accounts", "/api/accounts/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/courts/**", "/api/courts", "/api/surfaces/**", "/api/surfaces", "/api/reservations/**", "/api/reservations").hasAnyRole("USER", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/reservations").hasAnyRole("USER", "ADMIN")
            .requestMatchers("/api/courts/**", "/api/courts", "/api/surfaces/**", "/api/surfaces", "/api/reservations/**", "/api/reservations").hasRole("ADMIN")
            .anyRequest().denyAll())
        .addFilterBefore(new BearerFilter(tokens), UsernamePasswordAuthenticationFilter.class).build();
  }
}
