/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.testing.mock.aem;

import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.adobe.cq.dam.cfm.DataType;
import com.adobe.cq.dam.cfm.ElementTemplate;

public class MockElementTemplate implements ElementTemplate {

   private final Resource elementTemplateResource;
   private final ModifiableValueMap elementValueMap;

   MockElementTemplate(final Resource elementTemplateResource) {
      this.elementTemplateResource = elementTemplateResource;
      elementValueMap = elementTemplateResource.adaptTo(ModifiableValueMap.class);
   }

   @SuppressWarnings("unchecked")
   @Override
   public <AdapterType> @Nullable AdapterType adaptTo(@NotNull final Class<AdapterType> type) {
      if (type == Resource.class) {
         return (@Nullable AdapterType) elementTemplateResource;
      }
      return null;
   }

   @Override
   public String getName() {
      return elementValueMap.get("name", String.class);
   }

   @Override
   public String getTitle() {
      return elementValueMap.get("fieldLabel", String.class);
   }

   @Override
   public @NotNull DataType getDataType() {
      return new DataType() {

         @Override
         public @NotNull String getTypeString() {
            return elementValueMap.get("valueType", String.class);
         }

         @Override
         public @NotNull String getValueType() {
            return elementValueMap.get("valueType", String.class);
         }

         @Override
         public @Nullable String getSemanticType() {
            return elementValueMap.get("metaType", String.class);
         }

         @Override
         public boolean isMultiValue() {
            return elementValueMap.get("valueType", "").endsWith("[]");
         }

      };
   }

   @Override
   public @Nullable String getInitialContentType() {
      return null;
   }

   @Override
   public @Nullable String getDefaultContent() {
      return elementValueMap.get("value", String.class);
   }

   @Override
   public Map<String, Object> getMetaData() {
      return Map.of();
   }

}
