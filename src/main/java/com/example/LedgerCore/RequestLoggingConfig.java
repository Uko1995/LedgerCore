package com.example.LedgerCore;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@Slf4j
public class RequestLoggingConfig {

    @Bean
    public OncePerRequestFilter logFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String query = request.getQueryString();
                String queryString = query != null ? "?" + query : "";
                String path = request.getRequestURI();
                String method = request.getMethod();

                log.info("{} {}{} from {}", method, path, queryString, request.getRemoteAddr());

                long start = System.currentTimeMillis();
                filterChain.doFilter(request, response);
                long duration = System.currentTimeMillis() - start;

                log.info("{} {}{} => {} ({}ms)", method, path, queryString, response.getStatus(), duration);
            }
        };
    }
}
