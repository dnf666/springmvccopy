package com.ims.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author demo
 */
@Documented
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    String value() default "";
}
