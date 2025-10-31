package com.aletheia.spring;

import org.springframework.context.annotation.Import;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AletheiaAutoConfiguration.class)
public @interface EnableAletheia {
}