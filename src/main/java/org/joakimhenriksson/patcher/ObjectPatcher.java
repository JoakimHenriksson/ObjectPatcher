package org.joakimhenriksson.patcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.jcabi.aspects.Loggable;
import lombok.extern.slf4j.Slf4j;

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
	private static final String GET = "get";
	private static final String SET = "set";

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Loggable(Loggable.TRACE)
	public static <T> Optional<T> PATCH(String json, T patchable) throws PatcherException {
		try {
			JsonNode tree = objectMapper.readTree(json);
			return PATCH(tree, patchable);
		} catch (IOException e) {
			throw new PatcherException("Unable to json unmarshall json-string: " + json, e);
		}
	}

	@Loggable(Loggable.TRACE)
	public static <T> Optional<T> PATCH(JsonNode tree, T patchable) {
		Optional<T> result = Optional.ofNullable(patchable);
		if (tree.isContainerNode() && !tree.isArray() && !isContainer(patchable)) {
			tree.fields().forEachRemaining((entry) -> PATCH_FIELD(entry.getKey(), entry.getValue(), patchable));
		}
		return result;
	}

	@Loggable(Loggable.TRACE)
	public static <T> void PATCH_FIELD(String name, JsonNode tree, T patchable) {
		if (tree.isContainerNode()) {
			getFieldValue(name, patchable)
			.flatMap((value) -> PATCH(tree, value));
		} else if (tree.isValueNode()) {
			getOptionalFieldValue((ValueNode) tree)
			.ifPresent((value) -> setFieldValue(name, patchable, value));
		}
	}

	@Loggable(Loggable.TRACE)
	private static <T> void setFieldValue(String name, T patchable, Object value) {
		Class<?> cls = patchable.getClass();
		Optional<Method> method = getMethod(SET, name, cls);
		if (method.isPresent()) {
			try {
				method.get().invoke(patchable, value);
				return;
			} catch (IllegalAccessException | InvocationTargetException ignored) {}
		}
		getField(name, cls).ifPresent((field) -> setValue(field, patchable, value));
	}

	@Loggable(Loggable.TRACE)
	private static <T> Optional<Object> getFieldValue(String name, T patchable) {
		Class<?> cls = patchable.getClass();
		Optional<Method> method = getMethod(GET, name, cls);
		if (method.isPresent()) {
			try {
				return Optional.ofNullable(method.get().invoke(patchable));
			} catch (IllegalAccessException | InvocationTargetException ignored) {}
		}
		return getField(name, cls).flatMap((f) -> getFieldValue(f, patchable));
	}

	@Loggable(Loggable.TRACE)
	private static void setValue(Field field, Object o, Object value) {
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

	@Loggable(Loggable.TRACE)
	private static Optional<Object> getFieldValue(Field field, Object o) {
		try {
			return Optional.ofNullable(field.get(o));
		} catch (IllegalAccessException e) {
			return Optional.empty();
		}
	}

	@Loggable(Loggable.TRACE)
	private static Optional<Method> getSetMethod(String name, Class<?> cls) {
		return getMethod(SET, name, cls);
	}

	@Loggable(Loggable.TRACE)
	private static Optional<Method> getGetMethod(String name, Class<?> cls) {
		return getMethod(GET, name, cls);
	}

	@Loggable(Loggable.TRACE)
	private static Optional<Method> getMethod(String prefix, String name, Class<?> cls) {
		return Arrays.stream(cls.getDeclaredMethods())
			       .filter(isAnnotated(name))
			       .filter(method -> method.getName().startsWith(prefix))
			       .findFirst();
	}

	@Loggable(Loggable.TRACE)
	private static <T extends Annotation> Predicate<AccessibleObject> isAnnotated(String value) {
		return accessibleObject -> getAnnotation(accessibleObject).map((annotation) -> annotation.value().equals(value)).orElse(false);
	}

	@Loggable(Loggable.TRACE)
	public static Optional<JsonProperty> getAnnotation(AccessibleObject accessibleObject) {
		return Optional.ofNullable(accessibleObject.getAnnotation(JsonProperty.class));
	}

	@Loggable(Loggable.TRACE)
	private static Optional<Field> getField(String name, Class<?> cls) {
		Optional<Field> optionalField;
		try {
			optionalField = Optional.of(cls.getField(name));
		} catch (NoSuchFieldException e) {
			optionalField = Optional.empty();
		}
		Optional<Field> annotatedField = Arrays.stream(cls.getDeclaredFields()).filter(isAnnotated(name)).findFirst();
		return annotatedField.isPresent() ? annotatedField : optionalField;
	}

	@Loggable(Loggable.TRACE)
	private static boolean isContainer(Object o) {
		Class<?> cls = o.getClass();
		return
			Map.class.isAssignableFrom(cls) ||
			List.class.isAssignableFrom(cls) ||
			Set.class.isAssignableFrom(cls) ||
			Collection.class.isAssignableFrom(cls);
	}

	@Loggable(Loggable.TRACE)
	private static Optional<Object> getOptionalFieldValue(ValueNode value) {
		return Optional.ofNullable(getFieldValue(value));
	}

	@Loggable(Loggable.TRACE)
	private static Object getFieldValue(ValueNode value) {
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
