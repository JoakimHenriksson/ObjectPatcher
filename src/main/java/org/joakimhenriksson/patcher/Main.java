package org.joakimhenriksson.patcher;

public class Main {
	public static void main(String[] argv) {
		PatcherObject patchableObject = new PatcherObject();
		String json = "{\"sub\":{\"strong\":\"Quack!\"}}";
		patchableObject.patch(json);
		System.out.println("Duck: " + patchableObject);
	}
}
