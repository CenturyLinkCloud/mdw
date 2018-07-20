/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
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
 */
package com.centurylink.mdw.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import junit.framework.Assert;

import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.impl.common.QNameHelper;


/**
 * Provides JUnit assertion capabilities for XmlBeans.
 */
public class XmlBeanAssert extends Assert
{

  /**
   * Asserts that two XmlBeans are equivalent based on a deep compare (ignoring
   * whitespace and ordering of element attributes). If the two XmlBeans are
   * not equivalent, an AssertionFailedError is thrown, failing the test case.
   *
   * Note: we can't provide line numbers since XmlBeans only makes these available
   * if we're using the Picollo parser.  We'll get line numbers when we upgrade to
   * XmlBeans 2.0.
   *
   * @param expected
   * @param actual
   */
  public static void assertEquals(XmlObject expected, XmlObject actual)
  {
    assertEquals(null, expected.newCursor(), actual.newCursor());
  }

  /**
   * Asserts that two XmlBeans are equivalent based on a deep compare (ignoring
   * whitespace and ordering of element attributes). If the two XmlBeans are
   * not equivalent, an AssertionFailedError is thrown, failing the test case.
   *
   * Note: we can't provide line numbers since XmlBeans only makes these available
   * if we're using the Picollo parser.  We'll get line numbers when we upgrade to
   * XmlBeans 2.0.
   *
   * @param message to display on test failure (may be null)
   * @param expected
   * @param actual
   */
  public static void assertEquals(String message, XmlObject expected, XmlObject actual)
  {
    assertEquals(message, expected.newCursor(), actual.newCursor());
  }

  /**
   * Uses cursors to compare two XML documents, ignoring whitespace and
   * ordering of attributes.  Fails the JUnit test if they're different.
   *
   * @param message to display on test failure (may be null)
   * @param expected
   * @param actual
   */
  public static void assertEquals(String message, XmlCursor expected, XmlCursor actual)
  {
    for (int child = 0; true; child++)
    {
      boolean child1 = expected.toChild(child);
      boolean child2 = actual.toChild(child);
      if (child1 != child2)
      {
        fail(message, "Different XML structure near " + QNameHelper.pretty(expected.getName()));
      }
      else if (expected.getName() != null && !expected.getName().equals(actual.getName()))
      {
        fail(message, "Expected element: '" + expected.getName()
          + "' differs from actual element: '" + actual.getName() + "'");
      }
      else if (child == 0 && !child1)
      {
        if (!(expected.getTextValue().equals(actual.getTextValue())))
        {
          fail(message, "Expected value for element " + QNameHelper.pretty(expected.getName()) + " -> '"
            + expected.getTextValue() + "' differs from actual value '" + actual.getTextValue() + "'");
        }
        break;
      }
      else if (child1)
      {
        assertEquals(message, expected, actual);
        expected.toParent();
        actual.toParent();
      }
      else
      {
        break;
      }
    }

    assertAttributesEqual(message, expected, actual);
  }

  /**
   * Compares the attributes of the elements at the current position of two XmlCursors.
   * The ordering of the attributes is ignored in the comparison.  Fails the JUnit test
   * case if the attributes or their values are different.
   *
   * @param message to display on test failure (may be null)
   * @param expected
   * @param actual
   */
  private static void assertAttributesEqual(String message, XmlCursor expected, XmlCursor actual)
  {
    Map<QName,String> map1 = new HashMap<QName,String>();
    Map<QName,String> map2 = new HashMap<QName,String>();

    boolean attr1 = expected.toFirstAttribute();
    boolean attr2 = actual.toFirstAttribute();
    if (attr1 != attr2)
    {
      if (expected.isAttr() || actual.isAttr())
      {
        expected.toParent();
      }
      fail(message, "Differing number of attributes for element: '" + QNameHelper.pretty(expected.getName()) + "'");
    }
    else if (!attr1)
    {
      return;
    }
    else
    {
      map1.put(expected.getName(), expected.getTextValue());
      map2.put(actual.getName(), actual.getTextValue());
    }
    while (true)
    {
      attr1 = expected.toNextAttribute();
      attr2 = actual.toNextAttribute();
      if (attr1 != attr2)
      {
        if (expected.isAttr() || actual.isAttr())
        {
          expected.toParent();
        }
        fail(message, "Differing number of attributes for element: '" + QNameHelper.pretty(expected.getName()) + "'");
      }
      else if (!attr1)
      {
        break;
      }
      else
      {
        map1.put(expected.getName(), expected.getTextValue());
        map2.put(actual.getName(), actual.getTextValue());
      }
    }

    expected.toParent();
    actual.toParent();

    // check that attribute maps match, neglecting order
    Iterator<QName> iter = map1.keySet().iterator();
    while (iter.hasNext())
    {
      QName name = iter.next();
      String value1 = map1.get(name);
      String value2 = map2.get(name);
      if (value2 == null)
      {
        fail(message, "Expected attribute value missing for element: "
            + QNameHelper.pretty(expected.getName()) + "--> '" + name + "'");
      }
      else if (!value2.equals(value1))
      {
        fail(message, "Attribute values for element '" + QNameHelper.pretty(expected.getName())
            + "':  Expected '" + value1 + "', Actual '" + value2 + "'");
      }
    }
  }

  /**
   * Fails the test (by throwing an AssertionFailedError).
   *
   * @param message to display on test failure (may be null)
   * @param reason
   */
  public static void fail(String message, String reason)
  {
    if (message != null && message.length() > 0)
      fail(message + ": " + reason);
    else
      fail(reason);
  }
}
