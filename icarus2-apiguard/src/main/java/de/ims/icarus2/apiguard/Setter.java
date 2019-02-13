/**
 *
 */
package de.ims.icarus2.apiguard;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author Markus Gärtner
 *
 */
@Inherited
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface Setter {

	/**
	 * Name of the property this method is associated with, never {@code null}.
	 * @return
	 */
	String value();

	/**
	 * Signals that the method should not be called more than once with
	 * valid parameters.
	 * @return
	 */
	boolean restricted() default false;
}