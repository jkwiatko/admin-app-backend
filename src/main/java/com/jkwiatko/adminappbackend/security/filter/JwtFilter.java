package com.jkwiatko.adminappbackend.security.filter;

import com.jkwiatko.adminappbackend.security.auth.UserDetailsServiceImpl;
import com.jkwiatko.adminappbackend.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
@RequiredArgsConstructor

public class JwtFilter extends OncePerRequestFilter {

    private static final Pattern TOKEN_REGEX = Pattern.compile("Bearer (?<token>.+)");

    private final JwtProvider tokenProvider;
    private final UserDetailsServiceImpl userPrincipalDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest httpServletRequest,
                                    @NonNull HttpServletResponse httpServletResponse,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            var jwt = getJwtFromRequest(httpServletRequest);
            if (tokenProvider.validateToken(jwt)) {
                String userEmail = tokenProvider.getUserIdFromJWT(jwt);
                UserDetails userDetails = userPrincipalDetailsService.loadUserByUsername(userEmail);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception exc) {
            logger.error("Could not set user authentication in security context", exc);
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    public static String getJwtFromRequest(HttpServletRequest request) {
        var authHeader = request.getHeader(AUTHORIZATION);
        var tokenMatcher = TOKEN_REGEX.matcher(authHeader);
        if (tokenMatcher.matches()) {
            return tokenMatcher.group("token");
        }
        return null;
    }
}
