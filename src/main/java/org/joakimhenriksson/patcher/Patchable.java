package org.joakimhenriksson.patcher;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated field is patchable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
public @interface Patchable {
	/**
	 * Indicates wether this field is Patchable, or not.
	 * @return
	 */
	public boolean value() default true;
}
