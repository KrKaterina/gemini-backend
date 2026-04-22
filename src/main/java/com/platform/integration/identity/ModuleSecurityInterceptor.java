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
        String path = request.getRequestURI();
        System.out.println("[DEBUG INTERCEPTOR] Checking request: " + path);

        // ΑΠΟΔΕΙΞΗ ΓΙΑ ΤΗ ΔΙΠΛΩΜΑΤΙΚΗ: Έλεγχος αν το path εξαιρείται
        if (path.contains("/auth/login")) {
            System.out.println("[DEBUG INTERCEPTOR] Public Path Allowed: " + path);
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.err.println("[DEBUG INTERCEPTOR] DENIED: Missing or invalid Header for path: " + path);
            response.setStatus(401);
            return false;
        }

        String token = authHeader.substring(7);
        return identityClient.validateSession(token)
                .map(context -> {
                    System.out.println("[DEBUG INTERCEPTOR] SUCCESS: User authorized: " + context.username());
                    request.setAttribute("USER_CONTEXT", context);
                    return true;
                }).orElseGet(() -> {
                    System.err.println("[DEBUG INTERCEPTOR] DENIED: Session validation failed (Token expired or revoked)");
                    response.setStatus(401);
                    return false;
                });
    }
}