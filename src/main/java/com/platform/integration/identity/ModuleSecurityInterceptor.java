package com.platform.integration.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class ModuleSecurityInterceptor implements HandlerInterceptor {

    private final IdentityClient identityClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

        String token = authHeader.substring(7);
        return identityClient.validateSession(token)
                .map(context -> {
                    // Propagate the Identity context through the execution thread
                    request.setAttribute("USER_CONTEXT", context);
                    return true;
                }).orElseGet(() -> {
                    response.setStatus(401);
                    return false;
                });
    }
}