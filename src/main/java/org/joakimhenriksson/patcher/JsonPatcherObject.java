package org.joakimhenriksson.patcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.*;

@ToString
@EqualsAndHashCode(callSuper = true)
class JsonPatcherObject extends JsonPatcher {
	@JsonProperty("duck")
	public String string = "Duck";

	@SuppressWarnings("SpellCheckingInspection")
	@JsonProperty("int")
	public int intgr = 21;

	@JsonProperty("integer")
	public Integer integer = 42;

	@JsonProperty("blackListedInteger")
	@BlackListed
	public Integer blackListedInteger;

	@JsonProperty("map")
	public Map<String, String> map = new HashMap<>();

	@JsonProperty("set")
	public Set<String> set = new HashSet<>();

	@JsonProperty("List")
	public List<String> list = new ArrayList<>();

	@JsonProperty("collection")
	public Collection<String> collection = new HashSet<>();

	@JsonProperty("sub")
	public PatchableSubObject sub = new PatchableSubObject();

	@JsonProperty("subNull")
	public PatchableSubObject subNull = null;

	@JsonProperty("set")
	public void setSet(Set<String> set) {
		this.set = set;
	}

	@JsonProperty("set")
	@BlackListed
	public Set<String> getSet() {
		return set;
	}
}
