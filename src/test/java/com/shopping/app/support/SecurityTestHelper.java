package com.shopping.app.support;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.util.Arrays;
import java.util.Collection;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class SecurityTestHelper {

    private SecurityTestHelper() {
    }

    public static JwtRequestPostProcessor userJwt(String email) {
        return jwt().jwt(j -> j.subject(email));
    }

    public static JwtRequestPostProcessor roleJwt(String email, String... roles) {
        Collection<GrantedAuthority> authorities = Arrays.stream(roles)
                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                .toList();
        return jwt()
                .jwt(j -> j.subject(email).claim("roles", Arrays.asList(roles)))
                .authorities(authorities);
    }
}
