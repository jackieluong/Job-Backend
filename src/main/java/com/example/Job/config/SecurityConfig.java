package com.example.Job.config;

import com.example.Job.security.oauth2.CustomOAuth2UserService;
import com.example.Job.security.JwtAuthenticationEntryPoint;
import com.example.Job.security.oauth2.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomOAuth2UserService customOAuth2UserService;
//    private final static String oAuth2RedirectUri = "/api/v1/auth/oauth2/success";
    private final static String oAuth2RedirectUri = "http://localhost:3000/auth/callback";

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Autowired
    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint, CustomOAuth2UserService customOAuth2UserService, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String whiteList[] = { "/api/v1/auth/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/ws/**",
                "/api/v1/test-no-auth/**", "/storage/**", "/api/v1/jobs/**",
                "api/v1/companies/**", "api/v1/email/**" };

        http.authorizeHttpRequests(configurer -> configurer
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(whiteList).permitAll()
                .anyRequest().authenticated());

        // http.cors(Customizer.withDefaults());
        // disable Cors
        http.cors(cors -> cors.disable());

        // Spring use OIDC service, not OAuth2 service
        http.oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
        );

        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        // use HTTP Basic authentication

        http.exceptionHandling(exception -> exception
                // .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()) //401
                .authenticationEntryPoint(jwtAuthenticationEntryPoint) // handle Jwt exception
                .accessDeniedHandler(new BearerTokenAccessDeniedHandler()) // 403

        );

        http.httpBasic(Customizer.withDefaults());

        http.csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

}