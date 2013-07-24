//   Copyright 2013 Bibliotheca Alexandrina, Wellcome Trust Library
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

/**
 * 
 */
package org.bibalex.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author younos
 * 
 */
public class WebAppConstants {
	/** "Cache" holding all public static fields by it's field name */
	private static HashMap<String, Map> nameToValueMaps = new HashMap<String, Map>();
	
	/**
	 * Puts all public static fields via introspection into the resulting Map.
	 * Uses the name of the field as key to reference it's in the Map.
	 * 
	 * @return a Map of field names to field values of
	 *         all public static fields of this class
	 */
	private static Map createNameToValueMap(Class<?> clazz) {
		Map result = new HashMap();
		Field[] publicFields = clazz.getFields();
		for (int i = 0; i < publicFields.length; i++) {
			Field field = publicFields[i];
			String name = field.getName();
			try {
				result.put(name, field.get(null));
			} catch (Exception e) {
				System.err.println("Initialization of " + clazz.getSimpleName() + "class failed!");
				e.printStackTrace(System.err);
			}
		}
		return result;
	}
	
	/**
	 * Gets the Map of all public static fields.
	 * The field name is used as key for the value of the field itself.
	 * 
	 * @return the Map of all public static fields
	 */
	public static Map getNameToValueMap(Class<?> clazz) {
		Map result = nameToValueMaps.get(clazz.getName());
		
		if (result == null) {
			result = createNameToValueMap(clazz);
		}
		
		return result;
	}
	
}
