package com.ivandelic.prototype.chaincodes;

import java.io.Serializable;

public class Property implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String landRegistry;
	private String property;
	
	public Property(String landRegistry, String property) {
		this.landRegistry = landRegistry;
		this.property = property;
	}
	
	public String getLandRegistry() {
		return landRegistry;
	}
	public void setLandRegistry(String landRegistry) {
		this.landRegistry = landRegistry;
	}
	public String getProperty() {
		return property;
	}
	public void setProperty(String property) {
		this.property = property;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((landRegistry == null) ? 0 : landRegistry.hashCode());
		result = prime * result + ((property == null) ? 0 : property.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Property other = (Property) obj;
		if (landRegistry == null) {
			if (other.landRegistry != null)
				return false;
		} else if (!landRegistry.equals(other.landRegistry))
			return false;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		return true;
	}
	
	
}
