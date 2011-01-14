package com.markusherzog.sendy.api;


/**
 * <site> <id>1974445</id> <name>Sendy, posterous app for android</name>
 * <hostname>sendy-app</hostname> <url>http://sendy-app.posterous.com</url>
 * <private>false</private> <primary>false</primary>
 * <commentsenabled>true</commentsenabled> <num_posts>0</num_posts> </site>
 * 
 * @author markus
 * 
 */
public class Site  {
	private int id;
	private String name;
	private boolean primary;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setId(String id) {
		this.id = Integer.parseInt(id);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public void setPrimary(String primary) {
		this.primary = Boolean.parseBoolean(primary);
	}

	public Site copy() {
		Site copy = new Site();
		copy.setId(id);
		copy.setName(name);
		copy.setPrimary(primary);
		this.id = -1;
		this.name = null;
		this.primary = false;
		return copy;
	}

}
