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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.collections4.IteratorUtils;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.dam.cfm.ContentElement;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentVariation;
import com.adobe.cq.dam.cfm.VariationTemplate;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.tagging.Tag;

import io.wcm.testing.mock.aem.context.TestAemContext;
import io.wcm.testing.mock.aem.junit.AemContext;

public class MockContentFragmentTest {

  @Rule
  public AemContext context = TestAemContext.newAemContext();

  @Test
  @SuppressWarnings("null")
  public void testContentFragmentStructure() throws Exception {
    final String assetPath = context.uniqueRoot().dam() + "/cfStructure";
    final Tag testTag1 = context.create().tag("TestTag1");
    final ContentFragment cf = context.create().contentFragmentStructured(assetPath,
        "param1", "value1", "param2", 123, "param3", true, "param4", new String[] { "v1", "v2" });
    assertNotNull(cf);

    cf.setTitle("myTitle");
    cf.setDescription("myDesc");
    cf.setMetaData("meta1", "value1");
    cf.setTags(new Tag[] {testTag1});

    assertEquals("cfStructure", cf.getName());
    assertEquals("myTitle", cf.getTitle());
    assertEquals("myDesc", cf.getDescription());
    assertEquals("value1", cf.getMetaData().get("meta1"));

    assertTrue(cf.getElements().hasNext());
    assertTrue(cf.hasElement("param1"));

    assertEquals("value1", cf.getElement("param1").getContent());
    assertEquals("123", cf.getElement("param2").getContent());
    assertEquals("true", cf.getElement("param3").getContent());
    assertEquals("v1\nv2", cf.getElement("param4").getContent());

    // update data
    final ContentElement param1 = cf.getElement("param1");
    param1.setContent("new_value", null);
    assertEquals("new_value", param1.getContent());

    // create variation
    VariationTemplate varTemplate = cf.createVariation("v1", "V1", "desc1");
    ContentVariation variation = param1.createVariation(varTemplate);
    assertEquals("v1", variation.getName());
    assertEquals("V1", variation.getTitle());
    assertEquals("desc1", variation.getDescription());
    variation.setContent("var_value", null);
    assertEquals("var_value", variation.getContent());

    assertEquals(1, IteratorUtils.toList(cf.listAllVariations()).size());
    assertEquals(1, IteratorUtils.toList(param1.getVariations()).size());
    variation = param1.getVariation("v1");
    assertEquals("v1", variation.getName());

    // remove variation
    param1.removeVariation(variation);

    // create variation with only name
    varTemplate = cf.createVariation("v2", null, null);
    variation = param1.createVariation(varTemplate);
    assertEquals("v2", variation.getName());
    assertEquals("v2", variation.getTitle());
    assertEquals("", variation.getDescription());

    // test tagging
    assertNotNull(cf.getTags());
    assertEquals(1, cf.getTags().length);
    assertEquals(testTag1.getTagID(), cf.getTags()[0].getTagID());
  }

  @Test
  @SuppressWarnings("null")
  public void testContentFragmentText() throws Exception {
    final String assetPath = context.uniqueRoot().dam() + "/cfText";
    final ContentFragment cf = context.create().contentFragmentText(assetPath,
        "<p>Text</p>", "text/html");
    assertNotNull(cf);

    cf.setTitle("myTitle");
    cf.setDescription("myDesc");
    cf.setMetaData("meta1", "value1");

    assertEquals("cfText", cf.getName());
    assertEquals("myTitle", cf.getTitle());
    assertEquals("myDesc", cf.getDescription());
    assertEquals("value1", cf.getMetaData().get("meta1"));
    assertEquals("text/html", cf.getMetaData().get(DamConstants.DC_FORMAT));

    assertTrue(cf.getElements().hasNext());
    assertTrue(cf.hasElement("main"));

    //getElement with null param should act as if "main" param was passed
    final ContentElement contentElementEmptyParam = cf.getElement("");
    assertEquals("<p>Text</p>", contentElementEmptyParam.getContent());
    assertEquals("text/html", contentElementEmptyParam.getContentType());

    //getElement with null param should act as if "main" param was passed
    final ContentElement contentElementNullParam = cf.getElement(null);
    assertEquals("<p>Text</p>", contentElementNullParam.getContent());
    assertEquals("text/html", contentElementNullParam.getContentType());

    final ContentElement contentElement = cf.getElement("main");
    assertEquals("<p>Text</p>", contentElement.getContent());
    assertEquals("text/html", contentElement.getContentType());

    // update text
    contentElement.setContent("Text", "text/plain");
    assertEquals("Text", contentElement.getContent());
    assertEquals("text/plain", contentElement.getContentType());

    // create variation
    final VariationTemplate varTemplate = cf.createVariation("v1", "V1", "desc1");
    ContentVariation variation = contentElement.createVariation(varTemplate);
    assertEquals("v1", variation.getName());
    assertEquals("V1", variation.getTitle());
    assertEquals("desc1", variation.getDescription());
    variation.setContent("Var-Text", "text/plain");
    assertEquals("Var-Text", variation.getContent());
    assertEquals("text/plain", variation.getContentType());

    assertEquals(1, IteratorUtils.toList(cf.listAllVariations()).size());
    assertEquals(1, IteratorUtils.toList(contentElement.getVariations()).size());
    variation = contentElement.getVariation("v1");
    assertEquals("v1", variation.getName());

    // remove variation
    contentElement.removeVariation(variation);
  }

}
