/**
 * Original work by Ola Aronsson 2020
 * Courtesy of nollettnoll AB &copy; 2012 - 2020
 * <p>
 * Licensed under the Creative Commons Attribution 4.0 International (the "License")
 * you may not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * <p>
 * https://creativecommons.org/licenses/by/4.0/
 * <p>
 * The software is provided “as is”, without warranty of any kind, express or
 * implied, including but not limited to the warranties of merchantability,
 * fitness for a particular purpose and noninfringement. In no event shall the
 * authors or copyright holders be liable for any claim, damages or other liability,
 * whether in an action of contract, tort or otherwise, arising from, out of or
 * in connection with the software or the use or other dealings in the software.
 */
package com.github.catchitcozucan.supervision.config;

import com.github.catchitcozucan.supervision.utils.json.GsonWrapper;
import com.github.catchitcozucan.supervision.utils.json.adapt.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@AutoConfiguration
@ConditionalOnClass({Gson.class})
@Configuration
public class GsonConfig {

    private GsonBuilder gsonBuilder = new GsonBuilder().disableHtmlEscaping()
            .registerTypeAdapter(Boolean.class, new JsonBooleanDeAndSerializer())
            .registerTypeAdapter(Date.class, new JsonDateDeAndSerializer())
            .registerTypeAdapter(DateWithTimestamp.class, new JsonDateWithTimestampDeAndSerializer())
            .addDeserializationExclusionStrategy(new SuperclassExclusionStrategy())
            .addSerializationExclusionStrategy(new SuperclassExclusionStrategy());

    @ConditionalOnMissingBean
    public GsonBuilder gsonBuilder(List<GsonBuilderCustomizer> customizers) {
        customizers.forEach((c) -> {
            c.customize(gsonBuilder);
        });
        return gsonBuilder;
    }

    @Bean
    @ConditionalOnMissingBean
    public Gson gson(GsonBuilder gsonBuilder) {
        return gsonBuilder.create();
    }

    @Bean
    public GsonWrapper gsonWrapper() {
        return new GsonWrapperImpl();
    }

    public class GsonWrapperImpl implements GsonWrapper {

        private Gson gson;

        public GsonWrapperImpl() {
            this.gson = gsonBuilder.create();
        }

        @Override
        public String toJson(Object object) {
            return gson.toJson(object);
        }

        @Override
        public <T> T toObject(String json, Class<T> clazz) {
            return gson.fromJson(json, clazz);
        }

        @Override
        public <T> List<T> toObjects(String json, Class<T> clazz) {
            Object[] array = (Object[]) java.lang.reflect.Array.newInstance(clazz, 1);
            array = gson.fromJson(json, array.getClass());
            List<T> list = new ArrayList<>();
            for (int i = 0; i < array.length; i++)
                list.add((T) array[i]);
            return list;
        }
    }
}