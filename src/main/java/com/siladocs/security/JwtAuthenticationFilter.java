package com.siladocs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService; // Spring inyectará tu AuthService aquí

    // 🔹 Constructor actualizado
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extraer el token de la cabecera
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            // Si no hay token, simplemente sigue la cadena de filtros
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Quita "Bearer "

        try {
            // 2. Validar el token y extraer el email
            userEmail = jwtUtil.extractEmail(jwt);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token inválido o expirado");
            return;
        }

        // 3. Si el token es válido pero el usuario AÚN NO está autenticado en esta petición
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                // 4. Carga los detalles del usuario desde la BD (usando tu AuthService)
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 5. Crea el objeto de autenticación
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // No se necesitan credenciales (password)
                        userDetails.getAuthorities() // Roles (ej. ROLE_ADMIN)
                );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 6. Establece al usuario como AUTENTICADO en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception e) {
                // Si el usuario del token ya no existe o falla la carga, seguimos sin
                // autenticar en vez de propagar la excepción fuera del filtro (lo que
                // produce un 500 sin pasar por GlobalExceptionHandler en endpoints
                // públicos como /institutions o /access-codes).
                SecurityContextHolder.clearContext();
            }
        }

        // 7. Continúa con el resto de los filtros
        filterChain.doFilter(request, response);
    }
}
