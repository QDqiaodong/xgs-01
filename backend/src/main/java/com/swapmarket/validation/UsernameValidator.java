package com.swapmarket.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UsernameValidator implements ConstraintValidator<Username, String> {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 20;
    private static final String PATTERN = "^[a-zA-Z0-9_]+$";

    @Override
    public void initialize(Username constraintAnnotation) {
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.isBlank()) {
            return false;
        }
        if (username.length() < MIN_LENGTH || username.length() > MAX_LENGTH) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "用户名长度必须在" + MIN_LENGTH + "到" + MAX_LENGTH + "个字符之间"
            ).addConstraintViolation();
            return false;
        }
        if (!username.matches(PATTERN)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "用户名只能包含字母、数字和下划线"
            ).addConstraintViolation();
            return false;
        }
        return true;
    }
}
