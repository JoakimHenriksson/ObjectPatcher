package org.joakimhenriksson.patcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.jcabi.aspects.Loggable;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.String;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class JsonObjectPatcher {
	protected static final int LOGLEVEL = Loggable.ERROR;
	private static final String GET = "get";
	private static final String SET = "set";
	protected static final Logger LOGGER = LoggerFactory.getLogger(JsonObjectPatcher.class);

	protected static final ObjectMapper objectMapper = new ObjectMapper();

	@Loggable(LOGLEVEL)
	protected static Optional<Object> getOptionalFieldValue(ValueNode value) {
		return Optional.of(getFieldValue(value));
	}

	@Loggable(LOGLEVEL)
	@NotNull protected static Object getFieldValue(@NotNull ValueNode value) {
		if (value.isTextual()) {
			return value.asText();
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
