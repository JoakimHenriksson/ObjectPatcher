package org.joakimhenriksson.patcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.jcabi.aspects.Loggable;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;

@Test
public class TestObjectJsonPatcher {
	final static String PREFIX = "****-****-****-";

	@Test
	@Loggable(Loggable.ERROR)
	public void testPatchString() {
		JsonPatcherObject patchableObject = new JsonPatcherObject();
		String json = generateJson(String.class, "quack");
		patchableObject.patch(json);
		Assert.assertEquals(patchableObject.string, "quack");
	}

	@Test
	public void testWhiteList() {
		Predicate<Map.Entry<String, JsonNode>> predicate = JsonObjectPatcher.whiteList(Sets.newHashSet("duck"));
		AbstractMap.SimpleEntry<String, JsonNode> entry = new AbstractMap.SimpleEntry<String, JsonNode>("duck", null);
		Assert.assertTrue(predicate.test(entry));
	}

	@Test
	public void testBlackList() {
		Predicate<Map.Entry<String, JsonNode>> predicate = JsonObjectPatcher.blackList(Sets.newHashSet("duck"));
		AbstractMap.SimpleEntry<String, JsonNode> entry = new AbstractMap.SimpleEntry<String, JsonNode>("duck", null);
		Assert.assertFalse(predicate.test(entry));
	}

	@Test
	public void testPatchInt() {
		JsonPatcherObject patchableObject = new JsonPatcherObject();
		String test = generateJson(Integer.TYPE, 0);
		patchableObject.patch(test);
		Assert.assertEquals(patchableObject.intgr, 0);
	}

	@Test
	public void testPatchInteger() {
		JsonPatcherObject patchableObject = new JsonPatcherObject();
		String json = generateJson(Integer.class, 24);
		patchableObject.patch(json);
		Assert.assertEquals(patchableObject.integer, Integer.valueOf(24));
	}

	@Test
	public void testPatchWithPositiveFilter() {
		JsonPatcherObject patchableObject = new JsonPatcherObject();
		String json = generateJson(Integer.class, 24);
		patchableObject.patch(json, JsonObjectPatcher.whiteList(Sets.newHashSet("integer")));
		Assert.assertEquals(patchableObject.integer, Integer.valueOf(24));

		json = generateJson(Integer.class, 42);
		patchableObject.patch(json, JsonObjectPatcher.whiteList(Sets.newHashSet("intgr")));
		Assert.assertEquals(patchableObject.integer, Integer.valueOf(24));
	}

	@Test
	public void testPatchWithNegativeFilter() {
		JsonPatcherObject patchableObject = new JsonPatcherObject();
		String json = generateJson(Integer.class, 24);
		patchableObject.patch(json, JsonObjectPatcher.blackList(Sets.newHashSet("integer")));
		Assert.assertEquals(patchableObject.integer, Integer.valueOf(42));

		patchableObject.patch(json, JsonObjectPatcher.blackList(Sets.newHashSet("intgr")));
		Assert.assertEquals(patchableObject.integer, Integer.valueOf(24));
	}

	@Test
	public void testSubObject() {
		JsonPatcherObject patchableObject = new JsonPatcherObject();
		String json = "{\"sub\":{\"strong\":\"Quack!\"}}";
		patchableObject.patch(json);
		Assert.assertEquals(patchableObject.sub.getStrung(), "Quack!");
	}

	@Test
	public void testIsAnnotated() throws NoSuchFieldException {
		Predicate<AccessibleObject> blackListPredicate = JsonObjectPatcher.withoutAnnotation(BlackListed.class);
		Predicate<AccessibleObject> integerPredicate = JsonObjectPatcher.withAnnotation(JsonProperty.class, x -> x.value().contains("integer"));

		Field field = JsonPatcherObject.class.getDeclaredField("blackListedInteger");
		Assert.assertFalse(blackListPredicate.test(field));
		Assert.assertFalse(integerPredicate.test(field));

		field = JsonPatcherObject.class.getDeclaredField("integer");
		Assert.assertTrue(blackListPredicate.test(field));
		Assert.assertTrue(integerPredicate.test(field));
	}

	@Test
	public void testInvoke() {
		JsonPatcherObject po = new JsonPatcherObject();
		Optional<Method> method = JsonObjectPatcher.getMethod("set", "set", JsonPatcherObject.class);
		Assert.assertTrue(method.isPresent());
		JsonObjectPatcher.invoke(method, po, Sets.newHashSet("Duck"));
		Assert.assertTrue(po.set.contains("Duck"));
	}

	private String generateJson(Class<?> cls, String value) {
		Optional<Field> field = Arrays.stream(JsonPatcherObject.class.getDeclaredFields()).filter(f -> cls.isAssignableFrom(f.getType())).findFirst();
		return field.map((f) -> String.format("{\"%s\":\"%s\"}", getFieldName(f), value)).orElse("{}");
	}

	private String generateJson(Class<?> cls, Number value) {
		Optional<Field> field = Arrays.stream(JsonPatcherObject.class.getDeclaredFields()).filter(f -> cls.isAssignableFrom(f.getType())).findFirst();
		return field.map((f) -> String.format("{\"%s\":%s}", getFieldName(f), value)).orElse("{}");
	}

	private String getFieldName(Field field) {
		return JsonObjectPatcher.getAnnotation(field, JsonProperty.class).map(JsonProperty::value).orElse(field.getName());
	}
}
