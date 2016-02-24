package de.i2ar.ctrlbox.util;

import java.util.Random;

public class Message {
 
	private static Random random = new Random();
	
	private Long id;
	
	public Message() {
		this.id = random.nextLong();
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof Message)) return false;
		else return this.id.equals(((Message)other).getID());
	}
	
	Long getID() {
		return this.id;
	}
}
