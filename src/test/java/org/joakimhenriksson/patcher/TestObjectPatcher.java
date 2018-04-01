package org.joakimhenriksson.patcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jcabi.aspects.Loggable;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

@Test
public class TestObjectPatcher {
	@Test
	@Loggable(Loggable.ERROR)
	public void testPatchString() {
		PatcherObject patchableObject = new PatcherObject();
		String json = generateJson(String.class, "quack");
		patchableObject.patch(json);
		Assert.assertEquals(patchableObject.string, "quack");
	}

	@Test
	public void testPatchInt() {
		PatcherObject patchableObject = new PatcherObject();
		String test = generateJson(Integer.TYPE, 0);
		patchableObject.patch(test);
		Assert.assertEquals(patchableObject.intgr, 0);
	}

	@Test
	public void testPatchInteger() {
		PatcherObject patchableObject = new PatcherObject();
		String json = generateJson(Integer.class, 24);
		patchableObject.patch(json);
		Assert.assertEquals(patchableObject.integer, Integer.valueOf(24));
	}

	@Test
	public void testSubObject() {
		PatcherObject patchableObject = new PatcherObject();
		String json = "{\"sub\":{\"strong\":\"Quack!\"}}";
		patchableObject.patch(json);
		Assert.assertEquals(patchableObject.sub.getStrung(), "Quack!");
	}

	private String generateJson(Class<?> cls, String value) {
		Optional<Field> field = Arrays.stream(PatcherObject.class.getDeclaredFields()).filter(f -> cls.isAssignableFrom(f.getType())).findFirst();
		return field.map((f) -> String.format("{\"%s\":\"%s\"}", getFieldName(f), value)).orElse("{}");
	}

	private String generateJson(Class<?> cls, Number value) {
		Optional<Field> field = Arrays.stream(PatcherObject.class.getDeclaredFields()).filter(f -> cls.isAssignableFrom(f.getType())).findFirst();
		return field.map((f) -> String.format("{\"%s\":%s}", getFieldName(f), value)).orElse("{}");
	}

	private String getFieldName(Field field) {
		return ObjectPatcher.getAnnotation(field, JsonProperty.class).map(JsonProperty::value).orElse(field.getName());
	}
}
