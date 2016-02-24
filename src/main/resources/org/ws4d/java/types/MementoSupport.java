package org.ws4d.java.types;

import java.io.IOException;

public interface MementoSupport {

	public void saveToMemento(Memento m);

	public void readFromMemento(Memento m) throws IOException;
}
