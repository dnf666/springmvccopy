package com.ims.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author demo
 */
@Target(ElementType.TYPE)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
    String value() default "";
}
