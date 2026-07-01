package com.labreserve.support.annotations;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockTeacherSecurityContextFactory implements WithSecurityContextFactory<WithMockTeacher> {

    @Override
    public SecurityContext createSecurityContext(WithMockTeacher annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                annotation.userId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        ));
        return context;
    }
}
