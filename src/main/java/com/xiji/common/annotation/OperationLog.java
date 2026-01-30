package com.xiji.common.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解：操作日志
 * @author liberty
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    String description() default "";
}

