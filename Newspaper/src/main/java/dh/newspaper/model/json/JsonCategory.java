package dh.newspaper.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by hiep on 15/05/2014.
 */
public class JsonCategory {
	private long id;
	private String name;

	public JsonCategory() {
	}

	public JsonCategory(long id, String name) {
		this.id = id;
		this.name = name;
	}

	@JsonProperty(required = true)
	public long getId() {
		return id;
	}

	@JsonProperty(required = true)
	public String getName() {
		return name;
	}

	@JsonProperty(required = true)
	public void setId(long id) {
		this.id = id;
	}

	@JsonProperty(required = true)
	public void setName(String name) {
		this.name = name;
	}
}
