package org.joakimhenriksson.patcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.jcabi.aspects.Loggable;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ObjectPatcher {
	protected static final int LOGLEVEL = Loggable.ERROR;
	private static final String GET = "get";
	private static final String SET = "set";

	public static Predicate<Map.Entry<String, JsonNode>> whiteList(@NotNull Set<String> whiteList) {
		return e -> whiteList.contains(e.getKey());
	}

	public static Predicate<Map.Entry<String, JsonNode>> blackList(@NotNull Set<String> blackList) {
		return whiteList(blackList).negate();
	}

	@Loggable(LOGLEVEL)
	public static <T> void setFieldValue(@NotNull String name, @NotNull T patchable, Object value, Predicate<AccessibleObject> predicate) {
		Class<?> cls = patchable.getClass();
		Optional<Method> method = getMethod(SET, name, cls, predicate);
		try {
			invoke(method, patchable, value);
			return;
		} catch (PatcherException ignored) {}
		getField(name, cls, predicate).ifPresent(field -> setFieldValue(field, patchable, value));
	}

	@Loggable(LOGLEVEL)
	public static <T> Optional<Object> getFieldValue(@NotNull String name, @NotNull T patchable, Predicate<AccessibleObject> predicate) {
		Class<?> cls = patchable.getClass();
		Optional<Method> method = getMethod(GET, name, cls, predicate);
		try {
			return Optional.ofNullable(invoke(method, patchable));
		} catch (PatcherException ignored) {}
		return getField(name, cls, predicate).flatMap((f) -> getFieldValue(f, patchable));
	}

	@Loggable(LOGLEVEL)
	public static <T extends Annotation> Predicate<AccessibleObject> withAnnotation(@NotNull Class<T> annotationClass) {
		return withAnnotation(annotationClass, x -> true);
	}

	@Loggable(LOGLEVEL)
	public static <T extends Annotation> Predicate<AccessibleObject> withoutAnnotation(@NotNull Class<T> annotationClass) {
		return withAnnotation(annotationClass).negate();
	}

	@Loggable(LOGLEVEL)
	public static <T extends Annotation> Predicate<AccessibleObject> withAnnotation(@NotNull Class<T> annotationClass, @NotNull Predicate<T> annotationPredicate) {
		return accessibleObject -> getAnnotation(accessibleObject, annotationClass).filter(annotationPredicate).map(x -> true).orElse(false);
	}

	@Loggable(LOGLEVEL)
	public static <T extends Annotation> Predicate<AccessibleObject> withoutAnnotation(@NotNull Class<T> annotationClass, @NotNull Predicate<T> annotationPredicate) {
		return withAnnotation(annotationClass, annotationPredicate).negate();
	}

	@Loggable(LOGLEVEL)
	private static void setFieldValue(@NotNull Field field, @NotNull Object o, Object value) {
		boolean accessible = field.isAccessible();
		field.setAccessible(true);
		try {
			field.set(o, value);
		} catch (IllegalAccessException e) {
			throw new PatcherException("Unable to set value to field " + field.getName(), e);
		} finally {
			field.setAccessible(accessible);
		}
	}

	@Loggable(LOGLEVEL)
	private static Optional<Object> getFieldValue(@NotNull Field field, @NotNull Object o) {
		try {
			return Optional.ofNullable(field.get(o));
		} catch (IllegalAccessException e) {
			return Optional.empty();
		}
	}

	@Loggable(LOGLEVEL)
	protected static Object invoke(@NotNull Optional<Method> method, @NotNull Object invokable, Object ...args) throws PatcherException {
		Optional<Method> invokingMethod = method.filter(withoutAnnotation(BlackListed.class));
		if (invokingMethod.isPresent()) {
			return invoke(invokingMethod.get(), invokable, args);
		} else {
			throw new PatcherException("No method present to invoke for object: " + invokable.getClass().getSimpleName());
		}
	}

	@Loggable(LOGLEVEL)
	protected static Object invoke(@NotNull Method method, @NotNull Object invokable, Object ...args) throws PatcherException {
		try {
			return method.invoke(invokable,args);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new PatcherException(e);
		}
	}


	@Loggable(LOGLEVEL)
	protected static Optional<Method> getMethod(@NotNull String prefix, @NotNull String name, @NotNull Class<?> cls, Predicate<AccessibleObject> predicate) {
		return Arrays.stream(cls.getDeclaredMethods())
			       .filter(predicate)
			       .filter(method -> method.getName().startsWith(prefix))
			       .findFirst();
	}
	@Loggable(LOGLEVEL)
	public static <T extends Annotation> Optional<T> getAnnotation(@NotNull AccessibleObject accessibleObject, @NotNull Class<T> annotationClass) {
		return Optional.ofNullable(accessibleObject.getAnnotation(annotationClass));
	}

	@Loggable(LOGLEVEL)
	@NotNull private static Optional<Field> getField(@NotNull String name, @NotNull Class<?> cls) {
		return getField(name, cls, withAnnotation(JsonProperty.class, jsonProperty -> jsonProperty.value().equals(name)));
	}

	@Loggable(LOGLEVEL)
	@NotNull private static Optional<Field> getField(@NotNull String name, @NotNull Class<?> cls, Predicate<AccessibleObject> fieldAnnotationPredicate) {
		Optional<Field> optionalField;
		try {
			optionalField = Optional.of(cls.getField(name));
		} catch (NoSuchFieldException e) {
			optionalField = Optional.empty();
		}
		Optional<Field> annotatedField = Arrays.stream(cls.getDeclaredFields()).filter(fieldAnnotationPredicate).findFirst();
		return annotatedField.isPresent() ? annotatedField : optionalField;
	}

	@Loggable(LOGLEVEL)
	@NotNull public static <R> Stream<R> stream(Iterator<R> iterator) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
	}

	@Loggable(LOGLEVEL)
	@NotNull protected static boolean isContainer(Object o) {
		if (o != null) {
			Class<?> cls = o.getClass();
			return
				Map.class.isAssignableFrom(cls) ||
					List.class.isAssignableFrom(cls) ||
					Set.class.isAssignableFrom(cls) ||
					Collection.class.isAssignableFrom(cls);
		}
		return false;
	}
}
