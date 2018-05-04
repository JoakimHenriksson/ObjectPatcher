package org.joakimhenriksson.patcher;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.function.Predicate;

public abstract class JsonPatcher {
	public void patch(String json) throws PatcherException {
		patch(json, x -> true);
	}

	public void patch(String json, Predicate<Map.Entry<String, JsonNode>> filter) throws PatcherException {
		RecursivePatcherJson.PATCH(json, this, filter);
	}
}
