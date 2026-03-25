package com.shopping.app.support;

import com.shopping.app.config.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.jwt.secret=dGhpcyBpcyBhIHZlcnkgc2VjdXJlIHNlY3JldCBrZXkgZm9yIGRldmVsb3BtZW50IG9ubHk=",
        "app.cors.allowed-origins=http://localhost:3000"
})
public @interface SecuredControllerTest {
}
