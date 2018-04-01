package org.joakimhenriksson.patcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.jcabi.aspects.Loggable;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

public class SimplePatcher extends ObjectPatcher {
	public static <T> Optional<T> PATCH(String json, T patchable) throws PatcherException {
		return PATCH(json, patchable, s -> true);
	}

	@Loggable(LOGLEVEL)
	public static <T> Optional<T> PATCH(String json, T patchable, Predicate<String> filter) throws PatcherException {
		try {
			JsonNode tree = objectMapper.reader().readTree(json);
			return PATCH(tree, patchable, filter);
		} catch (IOException e) {
			throw new PatcherException("Unable to json unmarshall json-string: " + json, e);
		}
	}

	@Loggable(LOGLEVEL)
	public static <T> Optional<T> PATCH(JsonNode tree, T patchable, Predicate<String> filter) {
		Optional<T> result = Optional.ofNullable(patchable);
		if (tree.isContainerNode() && !tree.isArray() && !isContainer(patchable)) {
			tree.fields().forEachRemaining(entry -> PATCH_FIELD(entry.getKey(), entry.getValue(), patchable, filter));
		}
		return result;
	}

	@Loggable(LOGLEVEL)
	public static <T> void PATCH_FIELD(String name, JsonNode tree, T patchable, Predicate<String> filter) {
		if (tree.isValueNode()) {
			getOptionalFieldValue((ValueNode) tree)
				.ifPresent((value) -> setFieldValue(name, patchable, value));
		}
	}
}
