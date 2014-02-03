/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

package org.sonatype.nexus.web.internal;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.servlet.FilterPipeline;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.Mediator;
import org.eclipse.sisu.inject.BeanLocator;
import org.eclipse.sisu.wire.EntryListAdapter;

/**
 * {@link FilterPipeline} that can dynamically re-write the underlying sequence of filters and servlets.
 */
public final class DynamicFilterPipeline
    implements FilterPipeline
{
  /**
   * Reflection constants used to peek into the original pipelines.
   */

  private static final ClassLoader loader = FilterPipeline.class.getClassLoader();

  private static final Class<FilterPipeline> managedFilterPipelineClass = loadClass("com.google.inject.servlet.ManagedFilterPipeline");

  private static final Class<?> managedServletPipelineClass = loadClass("com.google.inject.servlet.ManagedServletPipeline");
  private static final Class<?> filterDefinitionClass = loadClass("com.google.inject.servlet.FilterDefinition");
  private static final Class<?> servletDefinitionClass = loadClass("com.google.inject.servlet.ServletDefinition");

  private static final Field servletPipelineField = accessField(managedFilterPipelineClass, "servletPipeline");
  private static final Field filterDefinitionsField = accessField(managedFilterPipelineClass, "filterDefinitions");
  private static final Field servletDefinitionsField = accessField(managedServletPipelineClass, "servletDefinitions");

  private static final Object[] filterDefinitionArray = (Object[]) Array.newInstance(filterDefinitionClass, 0);
  private static final Object[] servletDefinitionArray = (Object[]) Array.newInstance(servletDefinitionClass, 0);

  /**
   * Original filter and servlet pipelines for the root application.
   */

  private final FilterPipeline managedFilterPipeline;
  private final Object managedServletPipeline;

  private final BeanLocator locator;

  /**
   * Dynamic prioritized sequences of filters and servlet definitions.
   */

  private final List<?> filterDefinitions;
  private final List<?> servletDefinitions;

  private boolean initialized;

  public DynamicFilterPipeline(Injector injector) throws Exception {

    // extract the original pipeline bound in the root application
    managedFilterPipeline = injector.getInstance(managedFilterPipelineClass);
    managedServletPipeline = servletPipelineField.get(managedFilterPipeline);

    locator = injector.getInstance(BeanLocator.class);

    // the locator will return dynamic prioritized collections based on the bound definitions
    filterDefinitions = new EntryListAdapter<>(locator.locate(Key.get(filterDefinitionClass)));
    servletDefinitions = new EntryListAdapter<>(locator.locate(Key.get(servletDefinitionClass)));
  }

  public synchronized void initPipeline(final ServletContext context) throws ServletException {
    if (!initialized) {
      initialized = true; // avoid multiple/recursive initialization

      // make sure we initialize before rewriting
      managedFilterPipeline.initPipeline(context);

      // watch for pipeline updates - this tells us that filter/servlet definitions have come/gone
      locator.watch(Key.get(FilterPipeline.class), new Mediator<Annotation, FilterPipeline, Object>()
      {
        public void add(BeanEntry<Annotation, FilterPipeline> entry, Object watcher) throws Exception {
          // initialize before updating the list
          entry.getValue().initPipeline(context);

          refreshPipeline();
        }

        public void remove(BeanEntry<Annotation, FilterPipeline> entry, Object watcher) throws Exception {
          refreshPipeline();

          // destroy after list was updated
          entry.getValue().destroyPipeline();
        }
      }, this);
    }
  }

  /**
   * Updates the original pipeline to contain the latest aggregate sequence of filters and servlets.
   * 
   * Note: uses reflection to rewrite internal fields (ideally guice-servlet would be more adaptable)
   */
  public void refreshPipeline() throws Exception {
    filterDefinitionsField.set(managedFilterPipeline, filterDefinitions.toArray(filterDefinitionArray));
    servletDefinitionsField.set(managedServletPipeline, servletDefinitions.toArray(servletDefinitionArray));
  }

  public void dispatch(ServletRequest request, ServletResponse response, FilterChain defaultFilterChain)
      throws IOException, ServletException
  {
    // delegate to original; will now have the latest filters and servlets
    managedFilterPipeline.dispatch(request, response, defaultFilterChain);
  }

  public void destroyPipeline() {
    // cleanup any remaining filters/servlets
    managedFilterPipeline.destroyPipeline();
  }

  /**
   * Attempts to load the class using Guice's classloader.
   * 
   * @throws TypeNotPresentException if class doesn't exist
   */
  @SuppressWarnings("unchecked")
  private static <T> Class<T> loadClass(String name) {
    try {
      return (Class<T>) loader.loadClass(name);
    }
    catch (ClassNotFoundException e) {
      throw new TypeNotPresentException(name, e);
    }
  }

  /**
   * Finds the declared method and makes it accessible.
   * 
   * @throws TypeNotPresentException if method doesn't exist
   */
  private static Field accessField(Class<?> clazz, String name) {
    try {
      Field field = clazz.getDeclaredField(name);
      field.setAccessible(true);
      return field;
    }
    catch (NoSuchFieldException e) {
      throw new TypeNotPresentException(name, e);
    }
  }
}
