package com.laeben.corelauncher.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates the method returns null instead of an empty value.
 */
@Target(ElementType.METHOD)
public @interface ReturnsNull {

}
