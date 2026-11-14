package com.rms.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.rms.entity.Role;
import com.rms.security.CurrentUserArgumentResolver;
import com.rms.security.RoleInterceptor;

/** Wires the custom JWT auth plumbing: the {@code @CurrentUser} resolver, the per-group role
 * guards and cross-origin policy. Full Spring Security is intentionally not used. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserResolver;
    private final AppProperties props;

    public WebConfig(CurrentUserArgumentResolver currentUserResolver, AppProperties props) {
        this.currentUserResolver = currentUserResolver;
        this.props = props;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RoleInterceptor(Role.ADMIN)).addPathPatterns("/api/admin/**");
        registry.addInterceptor(new RoleInterceptor(Role.WORKER)).addPathPatterns("/api/worker/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(props.getCors().getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "X-Requested-With")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
