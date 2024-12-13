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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.testing.mock.sling.builder.ImmutableValueMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.ElementTemplate;
import com.adobe.cq.dam.cfm.FragmentTemplate;
import com.adobe.cq.dam.cfm.VariationDef;
import com.adobe.cq.dam.cfm.VariationTemplate;
import com.adobe.cq.dam.cfm.VersionDef;
import com.adobe.cq.dam.cfm.VersionedContent;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagConstants;
import com.day.cq.tagging.TagManager;

/**
 * Mock implementation of {@link ContentFragment}.
 */
final class MockContentFragment implements ContentFragment {

  private final Resource assetResource;
  private final Asset asset;
  private final Resource contentResource;
  private final ModifiableValueMap contentProps;
  private final ModifiableValueMap metadataProps;
  private final ModifiableValueMap structuredDataProps;
  private final Resource modelElementsResource;

  MockContentFragment(final Resource assetResource) {
    this.assetResource = assetResource;
    asset = assetResource.adaptTo(Asset.class);

    contentResource = assetResource.getChild(JcrConstants.JCR_CONTENT);
    if (contentResource == null) {
      throw new IllegalArgumentException("Missing jcr:content node.");
    }

    contentProps = contentResource.adaptTo(ModifiableValueMap.class);

    final Resource   metadataResource = contentResource.getChild(DamConstants.METADATA_FOLDER);
    if (metadataResource == null) {
      throw new IllegalArgumentException("Missing jcr:content/metadata node.");
    }
    metadataProps = metadataResource.adaptTo(ModifiableValueMap.class);

    final Resource structuredDataResource = contentResource.getChild("data/master");
    if (structuredDataResource != null) {
      structuredDataProps = structuredDataResource.adaptTo(ModifiableValueMap.class);
    }
    else {
      structuredDataProps = null;
    }

    modelElementsResource = contentResource.getChild("model/elements");
  }

  @Override
  public String getName() {
    return assetResource.getName();
  }

  @Override
  public String getTitle() {
    return contentProps.get(JcrConstants.JCR_TITLE, assetResource.getName());
  }

  @Override
  public String getDescription() {
    return contentProps.get(JcrConstants.JCR_DESCRIPTION, "");
  }

  @Override
  public Map<String, Object> getMetaData() {
    return metadataProps;
  }

  @Override
  public void setTitle(final String title) throws ContentFragmentException {
    contentProps.put(JcrConstants.JCR_TITLE, title);
  }

  @Override
  public void setDescription(final String description) throws ContentFragmentException {
    contentProps.put(JcrConstants.JCR_DESCRIPTION, description);
  }

  @Override
  public void setMetaData(final String name, final Object value) throws ContentFragmentException {
    metadataProps.put(name, value);
  }

  @Override
  @SuppressWarnings({ "null", "unchecked" })
  public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
    if (type == Resource.class) {
      return (AdapterType)assetResource;
    }
   if (type == Asset.class) {
      return (AdapterType)assetResource.adaptTo(Asset.class);
    }
    return null;
  }

  @Override
  public Iterator<ContentElement> getElements() {
    if (structuredDataProps != null) {
      return structuredDataProps.keySet().stream()
          .map(key -> (ContentElement)new MockContentFragment_ContentElement_Structured(this, key, structuredDataProps))
          .iterator();
    }
   if (modelElementsResource != null) {
      return StreamSupport.stream(modelElementsResource.getChildren().spliterator(), false)
          .map(resource -> (ContentElement)new MockContentFragment_ContentElement_Text(this, resource))
          .iterator();
    }
   return Collections.emptyIterator();
  }

  @Override
  public ContentElement getElement(final String elementName) {
    if (structuredDataProps != null) {
      if (structuredDataProps.containsKey(elementName)) {
        return new MockContentFragment_ContentElement_Structured(this, elementName, structuredDataProps);
      }
    }
    else if (modelElementsResource != null) {
      Resource resource = null;
      if (StringUtils.isEmpty(elementName)) {
        // if parameter is null or empty lookup "main" and "master" following the contract from the javadocs
        resource = modelElementsResource.getChild("main");
        if (resource == null) {
          resource = modelElementsResource.getChild("master");
        }
      }
      else {
        resource = modelElementsResource.getChild(elementName);
      }
      if (resource != null) {
        return new MockContentFragment_ContentElement_Text(this, resource);
      }
    }
    return null;
  }

  @Override
  public boolean hasElement(final String elementName) {
    if (structuredDataProps != null) {
      return structuredDataProps.containsKey(elementName);
    }
   if (modelElementsResource != null) {
      return modelElementsResource.getChild(elementName) != null;
    }
    return false;
  }

  Asset getAsset() {
    return asset;
  }

  Resource getContentResource() {
    return contentResource;
  }

  @Override
  public VariationTemplate createVariation(final String name, final String title, final String description) throws ContentFragmentException {
    final ResourceResolver resourceResolver = contentResource.getResourceResolver();
    try {
      final Resource variations = ResourceUtil.getOrCreateResource(resourceResolver, contentResource.getPath() + "/model/variations",
          JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, false);
      if (variations.getChild(name) != null) {
        throw new ContentFragmentException("Variation " + name + " already exists.");
      }
      final Resource child = resourceResolver.create(variations, name, ImmutableValueMap.of(
          "name", name,
          JcrConstants.JCR_TITLE, StringUtils.defaultString(title, name),
          JcrConstants.JCR_DESCRIPTION, StringUtils.defaultString(description)));
      return new MockContentFragment_VariationDef(child);
    }
    catch (final PersistenceException ex) {
      throw new ContentFragmentException("Unable to create variation: " + name, ex);
    }
  }

  @Override
  public Iterator<VariationDef> listAllVariations() {
    final Resource variations = contentResource.getChild("model/variations");
    if (variations == null) {
      return Collections.emptyIterator();
    }
    return StreamSupport.stream(variations.getChildren().spliterator(), false)
        .map(resource -> (VariationDef)new MockContentFragment_VariationDef(resource))
        .iterator();
  }


  // --- unsupported operations ---

  @Override
  public FragmentTemplate getTemplate() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ContentElement createElement(final ElementTemplate template) throws ContentFragmentException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<Resource> getAssociatedContent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addAssociatedContent(final Resource content) throws ContentFragmentException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeAssociatedContent(final Resource content) throws ContentFragmentException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeVariation(final String variation) throws ContentFragmentException {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nullable Calendar getLastModifiedDate() {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NotNull Calendar getLastModifiedDeep() throws ContentFragmentException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setTags(@NotNull final Tag[] tags) throws ContentFragmentException {
    metadataProps.put(TagConstants.PN_TAGS, Arrays.stream(tags == null ? new Tag[0] : tags).map(Tag::getTagID).toArray(String[]::new));
  }

  @Override
  public @NotNull Tag[] getTags() throws ContentFragmentException {
    final TagManager tagManager = assetResource.getResourceResolver().adaptTo(TagManager.class);
    return Arrays.stream(metadataProps.get(TagConstants.PN_TAGS, new String[] {})).map(tagId -> tagManager.resolve(tagId)).filter(Objects::nonNull).toArray(Tag[]::new);
  }

  @Override
  public void setVariationTags(@NotNull final Tag[] tags, @NotNull final String variationName) throws ContentFragmentException {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NotNull Tag[] getVariationTags(@NotNull final String variationName) throws ContentFragmentException {
    throw new UnsupportedOperationException();
  }

  @Override
  public VersionDef createVersion(final String label, final String comment) throws ContentFragmentException {
    throw new UnsupportedOperationException();
  }

  @Override
  public VersionedContent getVersionedContent(final VersionDef version) throws ContentFragmentException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<VersionDef> listVersions() throws ContentFragmentException {
    throw new UnsupportedOperationException();
  }

  // latest AEM Cloud API
  @SuppressWarnings("unused")
  public ContentFragment getVersion(final VersionDef versionDef) throws ContentFragmentException {
    throw new UnsupportedOperationException();
  }

}
