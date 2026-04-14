package com.jeffery.libraryweb;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    private final BannedUserInterceptor bannedUserInterceptor;

    public WebMvcConfiguration(BannedUserInterceptor bannedUserInterceptor) {
        this.bannedUserInterceptor = bannedUserInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(bannedUserInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",
                        "/register",
                        "/login",
                        "/logout",
                        "/css/**",
                        "/error"
                );
    }
}
