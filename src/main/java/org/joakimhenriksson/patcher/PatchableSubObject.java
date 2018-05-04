package org.joakimhenriksson.patcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class PatchableSubObject {
	@JsonProperty("strong")
	public String strung = "strung";

	@JsonProperty("strong")
	public String getStrung() {
		return strung;
	}

	@JsonProperty("strong")
	private void setStrung(String strung) {
		this.strung = strung;
	}
}
