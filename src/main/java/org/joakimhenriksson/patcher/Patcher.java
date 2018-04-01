package org.joakimhenriksson.patcher;

public abstract class Patcher {
	public void patch(String json) throws PatcherException {
		RecursivePatcher.PATCH(json, this);
	}
}
