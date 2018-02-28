package com.ims.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author demo
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
    String value() default "";
}
