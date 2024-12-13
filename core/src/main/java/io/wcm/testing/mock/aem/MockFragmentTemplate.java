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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.adobe.cq.dam.cfm.ElementTemplate;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.adobe.cq.dam.cfm.MetaDataDefinition;
import com.adobe.cq.dam.cfm.VariationTemplate;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.wcm.api.NameConstants;

public class MockFragmentTemplate implements FragmentTemplate {

   private final Resource templateResource;
   private final Resource contentResource;
   private final ModifiableValueMap contentProps;
   private final List<ElementTemplate> elementTemplates;

   MockFragmentTemplate(final Resource templateResource) {
      this.templateResource = templateResource;
      contentResource = templateResource.getChild(JcrConstants.JCR_CONTENT);
      if (contentResource == null) {
         throw new IllegalArgumentException("Missing jcr:content node.");
      }

      contentProps = contentResource.adaptTo(ModifiableValueMap.class);

      final Resource modelResource = contentResource.getChild("model");
      if (modelResource == null) {
         throw new IllegalArgumentException("Missing jcr:content/model node.");
      }

      final Resource itemsResource = modelResource.getChild("cq:dialog/content/items");
      if (itemsResource == null) {
         throw new IllegalArgumentException(
               "could not find cq:dialog/content/items on content fragment template '" + templateResource.getPath() + "'");
      }
      elementTemplates = StreamSupport.stream(itemsResource.getChildren().spliterator(), false).map(MockElementTemplate::new)
            .collect(Collectors.toList());
   }

   @SuppressWarnings("unchecked")
   @Override
   public <AdapterType> @Nullable AdapterType adaptTo(@NotNull final Class<AdapterType> type) {
      if (type == Resource.class) {
         return (AdapterType) templateResource;
      }

      return null;
   }

   @Override
   public String getTitle() {
      return contentProps.get(JcrConstants.JCR_TITLE, String.class);
   }

   @Override
   public String getDescription() {
      return contentProps.get(JcrConstants.JCR_DESCRIPTION, String.class);
   }

   @Override
   public String getThumbnailPath() {
      final Resource thumbnailResource = templateResource.getChild(NameConstants.NN_THUMBNAIL_PNG);
      if (thumbnailResource != null) {
         return thumbnailResource.getPath();
      }
      return null;
   }

   @Override
   public ContentFragment createFragment(final Resource parent, final String name, final String title) throws ContentFragmentException {
      final ResourceResolver resourceResolver = templateResource.getResourceResolver();

      if (parent == null) {
         throw new ContentFragmentException("empty parent submitted");
      }

      if (StringUtils.isEmpty(name) && StringUtils.isEmpty(title)) {
         throw new IllegalArgumentException("either name or title must be specified.");
      }

      // derive page name from title if none given
      String childResourceName = name;
      if (StringUtils.isEmpty(childResourceName)) {
         childResourceName = JcrUtil.createValidName(title, JcrUtil.HYPHEN_LABEL_CHAR_MAPPING, "_");
      } else if (!JcrUtil.isValidName(childResourceName)) {
         throw new IllegalArgumentException("Illegal content fragment name name.");
      }

      // use a unique variant of content fragement if a node with the given name already exists
      try {
         childResourceName = ResourceUtil.createUniqueChildName(parent, childResourceName);
      } catch (final PersistenceException ex) {
         throw new ContentFragmentException("Unable to get unique child name.", ex);
      }

      Resource fragmentResource;
      try {
         // content fragement node
         Map<String, Object> props = new HashMap<>();
         props.put(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSET);
         fragmentResource = resourceResolver.create(parent, childResourceName, props);

         // content fragment content node
         props = new HashMap<>();
         props.put(JcrConstants.JCR_PRIMARYTYPE, DamConstants.NT_DAM_ASSETCONTENT);
         props.put(JcrConstants.JCR_TITLE, title);
         final Resource fragmentContentResource = resourceResolver.create(fragmentResource, JcrConstants.JCR_CONTENT, props);

         props = new HashMap<>();
         props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
         props.put("cq:model", templateResource.getPath());
         final Resource dataResource = resourceResolver.create(fragmentContentResource, "data", props);

         resourceResolver.create(fragmentContentResource, DamConstants.METADATA_FOLDER, Map.of());

         resourceResolver.create(dataResource, "master", Map.of());
      } catch (final PersistenceException ex) {
         throw new ContentFragmentException("Creating page failed at :" + parent.getPath() + "/" + childResourceName + " failed.", ex);
      }

      return fragmentResource.adaptTo(ContentFragment.class);

   }

   @Override
   public Iterator<ElementTemplate> getElements() {
      return elementTemplates.iterator();
   }

   @Override
   public ElementTemplate getForElement(final ContentElement element) {
      return elementTemplates.stream().filter(ele -> ele.getName().equals(element.getName())).findFirst().orElse(null);
   }

   @Override
   public Iterator<VariationTemplate> getVariations() {
      return Collections.emptyIterator();
   }

   @Override
   public VariationTemplate getForVariation(final ContentVariation variation) {
      return null;
   }

   @Override
   public Iterator<String> getInitialAssociatedContent() {
      return Collections.emptyIterator();
   }

   @Override
   public MetaDataDefinition getMetaDataDefinition() {
      return null;
   }

}
