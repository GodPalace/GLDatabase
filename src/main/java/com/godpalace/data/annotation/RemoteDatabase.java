package com.godpalace.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RemoteDatabase {
    long autoSaveTime() default 0;
    TimeUnit autoSaveTimeUnit() default TimeUnit.SECONDS;
}
