package com.ims.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author demo
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Auwired {
    String value() default "";

}
