package com.labreserve.support.annotations;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithMockTeacherSecurityContextFactory.class)
public @interface WithMockTeacher {
    long userId() default 2L;
    String username() default "teacher1";
}
