package org.joakimhenriksson.patcher;

import java.util.function.Predicate;

/**
 * Patcher interface
 * @param <P>
 */
public interface Patcher<P> {
	/**
	 * Take a patch (@link P)  and a patchable value (@link T)
	 * @param patch
	 * @param patchableValue
	 * @return returns a patched value
	 */
	<T> T patch(P patch, T patchableValue);

	/**
	 * Take a patch (@link P) and a patchable value (@link T) and a Predicate
	 * @param patch
	 * @param patchableValue
	 * @param filter
	 * @return
	 */
	<T> T patch(P patch, T patchableValue, Predicate<P> filter);
}
