/**
 *    Original work by Ola Aronsson 2020
 *    Courtesy of nollettnoll AB &copy; 2012 - 2020
 *
 *    Licensed under the Creative Commons Attribution 4.0 International (the "License")
 *    you may not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *                https://creativecommons.org/licenses/by/4.0/
 *
 *    The software is provided “as is”, without warranty of any kind, express or
 *    implied, including but not limited to the warranties of merchantability,
 *    fitness for a particular purpose and noninfringement. In no event shall the
 *    authors or copyright holders be liable for any claim, damages or other liability,
 *    whether in an action of contract, tort or otherwise, arising from, out of or
 *    in connection with the software or the use or other dealings in the software.
 */
package com.github.catchitcozucan.supervision.utils.json.adapt;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import java.lang.reflect.Field;

/**
 * In case class A extends class B while having a property WITH THW SAME NAME
 * Json will throw
 *
 * java.lang.IllegalArgumentException: ..declares multiple JSON fields named [name]
 *
 * for instances where super has the same named property as the base class
 *
 * In these cases. So - be aware - we are telling JSON som simply SKIP the second property!
 *
 * See https://stackoverflow.com/questions/16476513/class-a-declares-multiple-json-fields
 */
public class SuperclassExclusionStrategy implements ExclusionStrategy {
	public boolean shouldSkipClass(Class<?> arg0) {
		return false;
	}

	public boolean shouldSkipField(FieldAttributes fieldAttributes) {
		String fieldName = fieldAttributes.getName();
		Class<?> theClass = fieldAttributes.getDeclaringClass();

		return isFieldInSuperclass(theClass, fieldName);
	}

	private boolean isFieldInSuperclass(Class<?> subclass, String fieldName) {
		Class<?> superclass = subclass.getSuperclass();
		Field field;

		while (superclass != null) {
			field = getField(superclass, fieldName);

			if (field != null)
				return true;

			superclass = superclass.getSuperclass();
		}

		return false;
	}

	private Field getField(Class<?> theClass, String fieldName) {
		try {
			return theClass.getDeclaredField(fieldName);
		} catch (Exception e) {
			return null;
		}
	}
}