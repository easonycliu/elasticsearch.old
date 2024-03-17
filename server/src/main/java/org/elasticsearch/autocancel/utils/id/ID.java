package org.elasticsearch.autocancel.utils.id;

public interface ID {
	public String toString();

	public boolean equals(Object o);

	public int hashCode();

	public Boolean isValid();

	public Long toLong();
}
