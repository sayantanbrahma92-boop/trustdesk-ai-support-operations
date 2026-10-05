package com.trustdesk.trustdesk.approval;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DemoTokenWebConfiguration implements WebMvcConfigurer {
    private final DemoTokenInterceptor demoTokenInterceptor;

    public DemoTokenWebConfiguration(
            DemoTokenInterceptor demoTokenInterceptor) {
        this.demoTokenInterceptor = demoTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(demoTokenInterceptor)
                .addPathPatterns(
                        "/api/tickets/*/refund-review",
                        "/api/approvals/**");
    }
}