package org.joakimhenriksson.patcher;

public class Main {
	public static void main(String[] argv) {
		PatchableObject po = new PatchableObject();
		po.patch("{\"string\":\"quack\"}");
		System.out.println("Duck: " + po.string);
	}
}
