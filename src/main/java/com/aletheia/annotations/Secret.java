package com.aletheia.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Secret {
    String value();
    boolean required() default true;
    String defaultValue() default "";
}