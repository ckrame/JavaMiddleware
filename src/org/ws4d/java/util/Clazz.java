package org.ws4d.java.util;

public class Clazz {

	public static ClazzInitializer	instance	= null;

	public static void registerClazzLoader(ClazzInitializer clazzLoader) {
		instance = clazzLoader;
	}

	public static Class forName(String path) throws ClassNotFoundException {
		if (instance != null) {
			return instance.forName(path);
		} else {
			return Class.forName(path);
		}
	}
}