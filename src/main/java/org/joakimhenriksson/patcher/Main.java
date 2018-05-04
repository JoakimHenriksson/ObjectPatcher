package org.joakimhenriksson.patcher;

public class Main {
	public static void main(String[] argv) {
		JsonPatcherObject patchableObject = new JsonPatcherObject();
		String json = "{\"sub\":{\"strong\":\"Quack!\"}}";
		patchableObject.patch(json);
		System.out.println("Duck: " + patchableObject);
	}
}
