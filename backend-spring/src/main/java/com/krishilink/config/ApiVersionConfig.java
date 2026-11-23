package com.krishilink.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Serves every /api/** endpoint under /api/v1/** as well. The unversioned paths remain for existing
 * clients and are marked deprecated (RFC 9745 Deprecation header plus a successor-version Link).
 */
@Configuration
public class ApiVersionConfig implements WebMvcRegistrations, WebMvcConfigurer {

    public static final String LEGACY_PREFIX = "/api/";
    public static final String V1_PREFIX = "/api/v1/";

    public static String toV1(String legacyPath) {
        return V1_PREFIX + legacyPath.substring(LEGACY_PREFIX.length());
    }

    private static boolean isLegacy(String path) {
        return path.startsWith(LEGACY_PREFIX) && !path.startsWith(V1_PREFIX);
    }

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {
            @Override
            protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
                RequestMappingInfo info = super.getMappingForMethod(method, handlerType);
                if (info == null) {
                    return null;
                }
                Set<String> paths = new LinkedHashSet<>(info.getPatternValues());
                info.getPatternValues().stream().filter(ApiVersionConfig::isLegacy)
                        .forEach(path -> paths.add(toV1(path)));
                return paths.size() == info.getPatternValues().size()
                        ? info
                        : info.mutate().paths(paths.toArray(String[]::new)).build();
            }
        };
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String path = request.getRequestURI().substring(request.getContextPath().length());
                if (isLegacy(path)) {
                    response.setHeader("Deprecation", "true");
                    response.setHeader("Link", "<" + request.getContextPath() + toV1(path) + ">; rel=\"successor-version\"");
                }
                return true;
            }
        });
    }
}
