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

import com.google.gson.*;

import java.lang.reflect.Type;

public class JsonBooleanDeAndSerializer implements JsonDeserializer<Boolean>, JsonSerializer<Boolean> {

	@Override
	public Boolean deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
		if(json.getAsJsonPrimitive().getAsString().equalsIgnoreCase("true") || json.getAsJsonPrimitive().getAsString().equalsIgnoreCase("\"true\"")){
			return Boolean.valueOf(true);
		}
		return Boolean.valueOf(false);
	}

	@Override
	public JsonElement serialize(Boolean b, Type type, JsonSerializationContext jsonSerializationContext) {
		return jsonSerializationContext.serialize(""+b);
	}
}