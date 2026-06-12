package com.swapmarket.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = UsernameValidator.class)
public @interface Username {
    String message() default "用户名格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
