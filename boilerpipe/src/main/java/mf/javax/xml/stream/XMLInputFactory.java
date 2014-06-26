/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package mf.javax.xml.stream;

import mf.javax.xml.transform.Source;
import mf.javax.xml.stream.util.XMLEventAllocator;

/**
 * Defines an abstract implementation of a factory for getting streams.
 *
 * The following table defines the standard properties of this specification.
 * Each property varies in the level of support required by each implementation.
 * The level of support required is described in the 'Required' column.
 *
 *   <table border="2" rules="all" cellpadding="4">
 *    <thead>
 *      <tr>
 *        <th align="center" colspan="5">
 *          Configuration parameters
 *        </th>
 *      </tr>
 *    </thead>
 *    <tbody>
 *      <tr>
 *        <th>Property Name</th>
 *        <th>Behavior</th>
 *        <th>Return type</th>
 *        <th>Default Value</th>
 *        <th>Required</th>
 *      </tr>
 * <tr><td>javax.xml.stream.isValidating</td><td>Turns on/off implementation specific DTD validation</td><td>Boolean</td><td>False</td><td>No</td></tr>
 * <tr><td>javax.xml.stream.isNamespaceAware</td><td>Turns on/off namespace processing for XML 1.0 support</td><td>Boolean</td><td>True</td><td>True (required) / False (optional)</td></tr>
 * <tr><td>javax.xml.stream.isCoalescing</td><td>Requires the processor to coalesce adjacent character data</td><td>Boolean</td><td>False</td><td>Yes</td></tr>
 * <tr><td>javax.xml.stream.isReplacingEntityReferences</td><td>replace internal entity references with their replacement text and report them as characters</td><td>Boolean</td><td>True</td><td>Yes</td></tr>
 *<tr><td>javax.xml.stream.isSupportingExternalEntities</td><td>Resolve external parsed entities</td><td>Boolean</td><td>Unspecified</td><td>Yes</td></tr>
 *<tr><td>javax.xml.stream.supportDTD</td><td>Use this property to request processors that do not support DTDs</td><td>Boolean</td><td>True</td><td>Yes</td></tr>
 *<tr><td>javax.xml.stream.reporter</td><td>sets/gets the impl of the XMLReporter </td><td>javax.xml.stream.XMLReporter</td><td>Null</td><td>Yes</td></tr>
 *<tr><td>javax.xml.stream.resolver</td><td>sets/gets the impl of the XMLResolver interface</td><td>javax.xml.stream.XMLResolver</td><td>Null</td><td>Yes</td></tr>
 *<tr><td>javax.xml.stream.allocator</td><td>sets/gets the impl of the XMLEventAllocator interface</td><td>javax.xml.stream.util.XMLEventAllocator</td><td>Null</td><td>Yes</td></tr>
 *    </tbody>
 *  </table>
 *
 *
 * @version 1.2
 * @author Copyright (c) 2009 by Oracle Corporation. All Rights Reserved.
 * @see XMLOutputFactory
 * @see mf.javax.xml.stream.XMLEventReader
 * @see XMLStreamReader
 * @see mf.javax.xml.stream.EventFilter
 * @see mf.javax.xml.stream.XMLReporter
 * @see mf.javax.xml.stream.XMLResolver
 * @see javax.xml.stream.util.XMLEventAllocator
 * @since 1.6
 */

public abstract class XMLInputFactory {
  /**
   * The property used to turn on/off namespace support,
   * this is to support XML 1.0 documents,
   * only the true setting must be supported
   */
  public static final String IS_NAMESPACE_AWARE=
    "javax.xml.stream.isNamespaceAware";

  /**
   * The property used to turn on/off implementation specific validation
   */
  public static final String IS_VALIDATING=
    "javax.xml.stream.isValidating";

  /**
   * The property that requires the parser to coalesce adjacent character data sections
   */
  public static final String IS_COALESCING=
    "javax.xml.stream.isCoalescing";

  /**
   * Requires the parser to replace internal
   * entity references with their replacement
   * text and report them as characters
   */
  public static final String IS_REPLACING_ENTITY_REFERENCES=
    "javax.xml.stream.isReplacingEntityReferences";

  /**
   *  The property that requires the parser to resolve external parsed entities
   */
  public static final String IS_SUPPORTING_EXTERNAL_ENTITIES=
    "javax.xml.stream.isSupportingExternalEntities";

  /**
   *  The property that requires the parser to support DTDs
   */
  public static final String SUPPORT_DTD=
    "javax.xml.stream.supportDTD";

  /**
   * The property used to
   * set/get the implementation of the XMLReporter interface
   */
  public static final String REPORTER=
    "javax.xml.stream.reporter";

  /**
   * The property used to set/get the implementation of the XMLResolver
   */
  public static final String RESOLVER=
    "javax.xml.stream.resolver";

  /**
   * The property used to set/get the implementation of the allocator
   */
  public static final String ALLOCATOR=
    "javax.xml.stream.allocator";

  static final String DEFAULIMPL = "com.sun.xml.internal.stream.XMLInputFactoryImpl";

  protected XMLInputFactory(){}

  /**
   * Create a new instance of the factory.
   * @throws FactoryConfigurationError if an instance of this factory cannot be loaded
   */
  public static XMLInputFactory newInstance()
    throws FactoryConfigurationError
  {
    return (XMLInputFactory) FactoryFinder.find(
      "javax.xml.stream.XMLInputFactory",
      DEFAULIMPL);
  }

  /**
   * Create a new instance of the factory.
   * This static method creates a new factory instance.
   * This method uses the following ordered lookup procedure to determine
   * the XMLInputFactory implementation class to load:
   *   Use the javax.xml.stream.XMLInputFactory system property.
   *   Use the properties file "lib/stax.properties" in the JRE directory.
   *     This configuration file is in standard java.util.Properties format
   *     and contains the fully qualified name of the implementation class
   *     with the key being the system property defined above.
   *   Use the Services API (as detailed in the JAR specification), if available,
   *     to determine the classname. The Services API will look for a classname
   *     in the file META-INF/services/javax.xml.stream.XMLInputFactory in jars
   *     available to the runtime.
   *   Platform default XMLInputFactory instance.
   *
   *   Once an application has obtained a reference to a XMLInputFactory it
   *   can use the factory to configure and obtain stream instances.
   *
   *   Note that this is a new method that replaces the deprecated newInstance() method.
   *     No changes in behavior are defined by this replacement method relative to
   *     the deprecated method.
   *
   * @throws FactoryConfigurationError if an instance of this factory cannot be loaded
   */
  public static XMLInputFactory newFactory()
    throws FactoryConfigurationError
  {
    return (XMLInputFactory) FactoryFinder.find(
      "javax.xml.stream.XMLInputFactory",
      DEFAULIMPL);
  }

  /**
   * Create a new instance of the factory
   *
   * @param factoryId             Name of the factory to find, same as
   *                              a property name
   * @param classLoader           classLoader to use
   * @return the factory implementation
   * @throws FactoryConfigurationError if an instance of this factory cannot be loaded
   *
   * @deprecated  This method has been deprecated to maintain API consistency.
   *              All newInstance methods have been replaced with corresponding
   *              newFactory methods. The replacement {@link
   *              #newFactory(String, ClassLoader)} method
   *              defines no changes in behavior.
   */
  public static XMLInputFactory newInstance(String factoryId,
          ClassLoader classLoader)
          throws FactoryConfigurationError {
      try {
          //do not fallback if given classloader can't find the class, throw exception
          return (XMLInputFactory) FactoryFinder.find(factoryId, classLoader, null);
      } catch (FactoryFinder.ConfigurationError e) {
          throw new FactoryConfigurationError(e.getException(),
                  e.getMessage());
      }
  }

  /**
   * Create a new instance of the factory.
   * If the classLoader argument is null, then the ContextClassLoader is used.
   *
   * Note that this is a new method that replaces the deprecated
   *   newInstance(String factoryId, ClassLoader classLoader) method.
   * No changes in behavior are defined by this replacement method relative
   * to the deprecated method.
   *
   * @param factoryId             Name of the factory to find, same as
   *                              a property name
   * @param classLoader           classLoader to use
   * @return the factory implementation
   * @throws FactoryConfigurationError if an instance of this factory cannot be loaded
   */
  public static XMLInputFactory newFactory(String factoryId,
          ClassLoader classLoader)
          throws FactoryConfigurationError {
      try {
          //do not fallback if given classloader can't find the class, throw exception
          return (XMLInputFactory) FactoryFinder.find(factoryId, classLoader, null);
      } catch (FactoryFinder.ConfigurationError e) {
          throw new FactoryConfigurationError(e.getException(),
                  e.getMessage());
      }
  }

  /**
   * Create a new XMLStreamReader from a reader
   * @param reader the XML data to read from
   * @throws XMLStreamException
   */
  public abstract XMLStreamReader createXMLStreamReader(java.io.Reader reader)
    throws XMLStreamException;

  /**
   * Create a new XMLStreamReader from a JAXP source.  This method is optional.
   * @param source the source to read from
   * @throws UnsupportedOperationException if this method is not
   * supported by this XMLInputFactory
   * @throws XMLStreamException
   */
  public abstract XMLStreamReader createXMLStreamReader(Source source)
    throws XMLStreamException;

  /**
   * Create a new XMLStreamReader from a java.io.InputStream
   * @param stream the InputStream to read from
   * @throws XMLStreamException
   */
  public abstract XMLStreamReader createXMLStreamReader(java.io.InputStream stream)
    throws XMLStreamException;

  /**
   * Create a new XMLStreamReader from a java.io.InputStream
   * @param stream the InputStream to read from
   * @param encoding the character encoding of the stream
   * @throws XMLStreamException
   */
  public abstract XMLStreamReader createXMLStreamReader(java.io.InputStream stream, String encoding)
    throws XMLStreamException;

  /**
   * Create a new XMLStreamReader from a java.io.InputStream
   * @param systemId the system ID of the stream
   * @param stream the InputStream to read from
   */
  public abstract XMLStreamReader createXMLStreamReader(String systemId, java.io.InputStream stream)
    throws XMLStreamException;

  /**
   * Create a new XMLStreamReader from a java.io.InputStream
   * @param systemId the system ID of the stream
   * @param reader the InputStream to read from
   */
  public abstract XMLStreamReader createXMLStreamReader(String systemId, java.io.Reader reader)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from a reader
   * @param reader the XML data to read from
   * @throws XMLStreamException
   */
  public abstract XMLEventReader createXMLEventReader(java.io.Reader reader)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from a reader
   * @param systemId the system ID of the input
   * @param reader the XML data to read from
   * @throws XMLStreamException
   */
  public abstract XMLEventReader createXMLEventReader(String systemId, java.io.Reader reader)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from an XMLStreamReader.  After being used
   * to construct the XMLEventReader instance returned from this method
   * the XMLStreamReader must not be used.
   * @param reader the XMLStreamReader to read from (may not be modified)
   * @return a new XMLEventReader
   * @throws XMLStreamException
   */
  public abstract XMLEventReader createXMLEventReader(XMLStreamReader reader)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from a JAXP source.
   * Support of this method is optional.
   * @param source the source to read from
   * @throws UnsupportedOperationException if this method is not
   * supported by this XMLInputFactory
   */
  public abstract XMLEventReader createXMLEventReader(Source source)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from a java.io.InputStream
   * @param stream the InputStream to read from
   * @throws XMLStreamException
   */
  public abstract XMLEventReader createXMLEventReader(java.io.InputStream stream)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from a java.io.InputStream
   * @param stream the InputStream to read from
   * @param encoding the character encoding of the stream
   * @throws XMLStreamException
   */
  public abstract XMLEventReader createXMLEventReader(java.io.InputStream stream, String encoding)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from a java.io.InputStream
   * @param systemId the system ID of the stream
   * @param stream the InputStream to read from
   * @throws XMLStreamException
   */
  public abstract XMLEventReader createXMLEventReader(String systemId, java.io.InputStream stream)
    throws XMLStreamException;

  /**
   * Create a filtered reader that wraps the filter around the reader
   * @param reader the reader to filter
   * @param filter the filter to apply to the reader
   * @throws XMLStreamException
   */
  public abstract XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter)
    throws XMLStreamException;

  /**
   * Create a filtered event reader that wraps the filter around the event reader
   * @param reader the event reader to wrap
   * @param filter the filter to apply to the event reader
   * @throws XMLStreamException
   */
  public abstract XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter)
    throws XMLStreamException;

  /**
   * The resolver that will be set on any XMLStreamReader or XMLEventReader created
   * by this factory instance.
   */
  public abstract XMLResolver getXMLResolver();

  /**
   * The resolver that will be set on any XMLStreamReader or XMLEventReader created
   * by this factory instance.
   * @param resolver the resolver to use to resolve references
   */
  public abstract void  setXMLResolver(XMLResolver resolver);

  /**
   * The reporter that will be set on any XMLStreamReader or XMLEventReader created
   * by this factory instance.
   */
  public abstract XMLReporter getXMLReporter();

  /**
   * The reporter that will be set on any XMLStreamReader or XMLEventReader created
   * by this factory instance.
   * @param reporter the resolver to use to report non fatal errors
   */
  public abstract void setXMLReporter(XMLReporter reporter);

  /**
   * Allows the user to set specific feature/property on the underlying implementation. The underlying implementation
   * is not required to support every setting of every property in the specification and may use IllegalArgumentException
   * to signal that an unsupported property may not be set with the specified value.
   * @param name The name of the property (may not be null)
   * @param value The value of the property
   * @throws IllegalArgumentException if the property is not supported
   */
  public abstract void setProperty(String name, Object value)
    throws IllegalArgumentException;

  /**
   * Get the value of a feature/property from the underlying implementation
   * @param name The name of the property (may not be null)
   * @return The value of the property
   * @throws IllegalArgumentException if the property is not supported
   */
  public abstract Object getProperty(String name)
    throws IllegalArgumentException;


  /**
   * Query the set of properties that this factory supports.
   *
   * @param name The name of the property (may not be null)
   * @return true if the property is supported and false otherwise
   */
  public abstract boolean isPropertySupported(String name);

  /**
   * Set a user defined event allocator for events
   * @param allocator the user defined allocator
   */
  public abstract void setEventAllocator(XMLEventAllocator allocator);

  /**
   * Gets the allocator used by streams created with this factory
   */
  public abstract XMLEventAllocator getEventAllocator();

}