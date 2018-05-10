package org.joakimhenriksson.patcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.jcabi.aspects.Loggable;

import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class RecursivePatcherJson extends JsonObjectPatcher {
	public static <T> Optional<T> PATCH(String json, T patchable) throws PatcherException {
		return PATCH(json, patchable, s -> true);
	}

	@Loggable(LOGLEVEL)
	public static <T> Optional<T> PATCH(String json, T patchable, Predicate<Map.Entry<String, JsonNode>> predicate) throws PatcherException {
		try {
			JsonNode tree = objectMapper.reader().readTree(json);
			return PATCH(tree, patchable, predicate);
		} catch (IOException e) {
			throw new PatcherException("Unable to json unmarshall json-string: " + json, e);
		}
	}

	@Loggable(LOGLEVEL)
	public static <T> Optional<T> PATCH(JsonNode tree, T patchable, Predicate<Map.Entry<String, JsonNode>> predicate) {
		Optional<T> result = Optional.ofNullable(patchable);
		if (tree.isContainerNode() && !tree.isArray() && !ObjectPatcher.isContainer(patchable)) {
			ObjectPatcher.stream(tree.fields())
				.filter(predicate)
				.forEach(entry -> PATCH_FIELD(entry.getKey(), entry.getValue(), patchable, predicate));
		}
		return result;
	}

	@Loggable(LOGLEVEL)
	public static <T> void PATCH_FIELD(String name, JsonNode tree, T patchable, Predicate<Map.Entry<String, JsonNode>> predicate) {
		if (tree.isContainerNode()) {
			Predicate<AccessibleObject> annotationPredicate = ObjectPatcher.withAnnotation(JsonProperty.class, (JsonProperty property) -> property.value().equals(name));
			ObjectPatcher.getFieldValue(name, patchable, annotationPredicate)
				.flatMap((value) -> PATCH(tree, value, predicate));
		} else if (tree.isValueNode()) {
			Predicate<AccessibleObject> annotationPredicate = ObjectPatcher.withAnnotation(JsonProperty.class, (JsonProperty property) -> property.value().equals(name));
			getOptionalFieldValue((ValueNode) tree)
				.ifPresent((value) -> ObjectPatcher.setFieldValue(name, patchable, value, annotationPredicate));
		}
	}
}
