/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.certify.config;

import io.mosip.certify.core.config.LocalAuthenticationEntryPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;


import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile(value = {"!test"})
public class SecurityConfig {

    @Autowired
    private LocalAuthenticationEntryPoint localAuthenticationEntryPoint;

    @Value("${server.servlet.path}")
    private String servletPath;

    @Value("#{${mosip.certify.security.auth.post-urls}}")
    private Map<String, List<String>> securePostUrls;

    @Value("#{${mosip.certify.security.auth.put-urls}}")
    private Map<String, List<String>> securePutUrls;

    @Value("#{${mosip.certify.security.auth.get-urls}}")
    private Map<String, List<String>> secureGetUrls;

    @Value("${mosip.certify.authn.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${mosip.certify.security.ignore-auth-urls}")
    private String[] ignoreAuthUrls;

    @Value("${mosip.certify.security.ignore-csrf-urls}")
    private String[] ignoreCsrfCheckUrls;

    @Bean
    public SecurityFilterChain web(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector)
                .servletPath(servletPath);

        RequestMatcher[] csrfIgnoreMatchers = new RequestMatcher[ignoreCsrfCheckUrls.length];

        for (int i = 0; i < ignoreCsrfCheckUrls.length; i++) {
            if (ignoreCsrfCheckUrls[i].startsWith("/services/")) {
                csrfIgnoreMatchers[i] = new AntPathRequestMatcher(ignoreCsrfCheckUrls[i]);
            } else {
                csrfIgnoreMatchers[i] = mvcMatcherBuilder.pattern(ignoreCsrfCheckUrls[i]);
            }
        }

        RequestMatcher[] authIgnoreMatchers = new RequestMatcher[ignoreAuthUrls.length];

        for (int i = 0; i < ignoreAuthUrls.length; i++) {
            if (ignoreAuthUrls[i].startsWith("/services/")) {
                authIgnoreMatchers[i] = new AntPathRequestMatcher(ignoreAuthUrls[i]);
            } else {
                authIgnoreMatchers[i] = mvcMatcherBuilder.pattern(ignoreAuthUrls[i]);
            }
        }

        http.csrf(httpEntry -> httpEntry.ignoringRequestMatchers(csrfIgnoreMatchers)
                .csrfTokenRepository(this.getCsrfTokenRepository()));

        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                .requestMatchers(authIgnoreMatchers).permitAll()
                .anyRequest().authenticated()
        ).oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .jwkSetUri(jwkSetUri)
                )
        );
        http.exceptionHandling(exceptionConfigurer -> exceptionConfigurer.authenticationEntryPoint(localAuthenticationEntryPoint));
        http.sessionManagement(sessionConfigurer -> sessionConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    private CsrfTokenRepository getCsrfTokenRepository() {
        CookieCsrfTokenRepository cookieCsrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        cookieCsrfTokenRepository.setCookiePath("/");
        return cookieCsrfTokenRepository;
    }

}
