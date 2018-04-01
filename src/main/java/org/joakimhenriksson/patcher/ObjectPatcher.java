package org.joakimhenriksson.patcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.jcabi.aspects.Loggable;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.String;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;


@Slf4j
public class ObjectPatcher {
	protected static final int LOGLEVEL = Loggable.ERROR;
	private static final String GET = "get";
	private static final String SET = "set";
	protected static final Logger LOGGER = LoggerFactory.getLogger(ObjectPatcher.class);

	protected static final ObjectMapper objectMapper = new ObjectMapper();

	public static Predicate<String> whiteList(Set<String> whiteList) {
		return s -> whiteList.contains(s);
	}

	public static Predicate<String> blackList(Set<String> blackList) {
		return s -> !blackList.contains(s);
	}

	@Loggable(LOGLEVEL)
	protected static <T> void setFieldValue(String name, T patchable, Object value) {
		Class<?> cls = patchable.getClass();
		Optional<Method> method = getMethod(SET, name, cls);
		if (method.isPresent()) {
			try {
				method.get().invoke(patchable, value);
				return;
			} catch (IllegalAccessException | InvocationTargetException ignored) {}
		}
		getField(name, cls).ifPresent((field) -> setFieldValue(field, patchable, value));
	}

	@Loggable(LOGLEVEL)
	protected static <T> Optional<Object> getFieldValue(String name, T patchable) {
		Class<?> cls = patchable.getClass();
		Optional<Method> method = getMethod(GET, name, cls);
		if (method.isPresent()) {
			Optional<Object> methodValue = null;
			try {
				return Optional.ofNullable(method.get().invoke(patchable));
			} catch (IllegalAccessException | InvocationTargetException ignored) {}
		}
		return getField(name, cls).flatMap((f) -> getFieldValue(f, patchable));
	}

	@Loggable(LOGLEVEL)
	protected static void setFieldValue(Field field, Object o, Object value) {
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
	protected static Optional<Object> getFieldValue(Field field, Object o) {
		try {
			return Optional.ofNullable(field.get(o));
		} catch (IllegalAccessException e) {
			return Optional.empty();
		}
	}

	@Loggable(LOGLEVEL)
	protected static Optional<Method> getSetMethod(String name, Class<?> cls) {
		return getMethod(SET, name, cls);
	}

	@Loggable(LOGLEVEL)
	protected static Optional<Method> getGetMethod(String name, Class<?> cls) {
		return getMethod(GET, name, cls);
	}

	@Loggable(LOGLEVEL)
	protected static Optional<Method> getMethod(String prefix, String name, Class<?> cls) {
		return Arrays.stream(cls.getDeclaredMethods())
			       .filter(isAnnotated(JsonProperty.class, (JsonProperty property) -> property.value().equals(name)))
			       .filter(method -> method.getName().startsWith(prefix))
			       .findFirst();
	}

	@Loggable(LOGLEVEL)
	protected static <T extends Annotation> Predicate<AccessibleObject> isAnnotated(Class<T> annotationClass, Predicate<T> annotationPredicate) {
		return accessibleObject -> getAnnotation(accessibleObject, annotationClass).filter(annotationPredicate).map(x -> true).orElse(false);
	}

	@Loggable(LOGLEVEL)
	protected static <T extends Annotation> Optional<T> getAnnotation(AccessibleObject accessibleObject, Class<T> annotationClass) {
		return Optional.ofNullable(accessibleObject.getAnnotation(annotationClass));
	}

	@Loggable(LOGLEVEL)
	protected static Optional<Field> getField(String name, Class<?> cls) {
		Optional<Field> optionalField;
		try {
			optionalField = Optional.of(cls.getField(name));
		} catch (NoSuchFieldException e) {
			optionalField = Optional.empty();
		}
		Optional<Field> annotatedField = Arrays.stream(cls.getDeclaredFields()).filter(isAnnotated(JsonProperty.class, jsonProperty -> jsonProperty.value().equals(name))).findFirst();
		return annotatedField.isPresent() ? annotatedField : optionalField;
	}

	@Loggable(LOGLEVEL)
	protected static boolean isContainer(Object o) {
		Class<?> cls = o.getClass();
		return
			Map.class.isAssignableFrom(cls) ||
			List.class.isAssignableFrom(cls) ||
			Set.class.isAssignableFrom(cls) ||
			Collection.class.isAssignableFrom(cls);
	}

	@Loggable(LOGLEVEL)
	protected static Optional<Object> getOptionalFieldValue(ValueNode value) {
		return Optional.ofNullable(getFieldValue(value));
	}

	@Loggable(LOGLEVEL)
	protected static Object getFieldValue(ValueNode value) {
		if (value.isTextual()) {
			return value.asText();
		}
		if (value.isTextual()) {
			return value.textValue();
		}
		if (value.isBoolean()) {
			return value.asBoolean();
		}
		if (value.isInt()) {
			return value.asInt();
		}
		if (value.isDouble()) {
			return value.asDouble();
		}
		if (value.isFloat() || value.isFloatingPointNumber()) {
			return value.floatValue();
		}
		if (value.isBinary()) {
			try {
				return value.binaryValue();
			} catch (IOException e) {
				throw new PatcherException("Unable to get the binary value from " + value);
			}
		}
		if (value.isLong()) {
			return value.asLong();
		}
		if (value.isShort()) {
			return value.shortValue();
		}
		if (value.isBigInteger()) {
			return value.bigIntegerValue();
		}
		throw new NoSuchElementException("Unsupported Value type" + value);
	}
}
