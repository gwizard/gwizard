package org.gwizard.test.util;

import java.util.Random;

/**
 * Some utility methods for making fake data
 */
public class Fake {

	/** @return a random unique string */
	public static String unique() {
		return Long.toString(Math.abs(new Random().nextLong()));
	}

	/** @return a random unique string with the prefix and a dash */
	public static String unique(final String prefix) {
		return prefix + '-' + unique();
	}

	/** */
	public static String email() {
		return unique("email") + "@example.com";
	}
}
