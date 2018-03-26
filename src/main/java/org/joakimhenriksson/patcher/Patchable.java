package org.joakimhenriksson.patcher;

public abstract class Patchable {
	public void patch(String json) throws PatcherException {
		ObjectPatcher.PATCH(json, this);
	}
}
