/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.appengine.tools.development.jetty.ee10;

import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.runtime.jetty.EE10AppEngineAuthentication;
import java.io.File;
import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;

/**
 * {@code AppEngineWebAppContext} is a customization of Jetty's {@link WebAppContext} that is aware
 * of the {@link ApiProxy} and can provide custom logging and authentication.
 *
 */
public class AppEngineWebAppContext extends WebAppContext {

  // TODO: This should be some sort of Prometheus-wide
  // constant.  If it's much larger than this we may need to
  // restructure the code a bit.
  private static final int MAX_RESPONSE_SIZE = 32 * 1024 * 1024;

  private final String serverInfo;

  public AppEngineWebAppContext(File appDir, String serverInfo) {
    // We set the contextPath to / for all applications.
    super(appDir.getPath(), "/");
    Resource webApp = null;
    try {
      webApp = ResourceFactory.root().newResource(appDir.getAbsolutePath());

      if (appDir.isDirectory()) {
        setWar(appDir.getPath());
        setBaseResource(webApp);
      } else {
        // Real war file, not exploded , so we explode it in tmp area.
        File extractedWebAppDir = createTempDir();
        extractedWebAppDir.mkdir();
        extractedWebAppDir.deleteOnExit();
        Resource jarWebWpp = ResourceFactory.root().newJarFileResource(webApp.getURI());
        jarWebWpp.copyTo(extractedWebAppDir.toPath());
        setBaseResource(ResourceFactory.root().newResource(extractedWebAppDir.getAbsolutePath()));
        setWar(extractedWebAppDir.getPath());
      }
    } catch (Exception e) {
      throw new IllegalStateException("cannot create AppEngineWebAppContext:", e);
    }

    this.serverInfo = serverInfo;

    // Configure the Jetty SecurityHandler to understand our method of
    // authentication (via the UserService).
    EE10AppEngineAuthentication.configureSecurityHandler(
        (ConstraintSecurityHandler) getSecurityHandler());

    setMaxFormContentSize(MAX_RESPONSE_SIZE);
  }

  @Override
  public ServletScopedContext getContext() {
    // TODO: Override the default HttpServletContext implementation (for logging)?.
    AppEngineServletContext appEngineServletContext = new AppEngineServletContext();
    return super.getContext();
  }

  private static File createTempDir() {
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    String baseName = System.currentTimeMillis() + "-";

    for (int counter = 0; counter < 10; counter++) {
      File tempDir = new File(baseDir, baseName + counter);
      if (tempDir.mkdir()) {
        return tempDir;
      }
    }
    throw new IllegalStateException("Failed to create directory ");
  }

  @Override
  public Class<? extends SecurityHandler> getDefaultSecurityHandlerClass() {
    return AppEngineConstraintSecurityHandler.class;
  }

  /**
   * Override to make sure all RoleInfos do not have security constraints to avoid a Jetty failure
   * when not running with https.
   */
  public static class AppEngineConstraintSecurityHandler extends ConstraintSecurityHandler {
    @Override
    protected Constraint getConstraint(String pathInContext, Request request) {
      Constraint constraint = super.getConstraint(pathInContext, request);

      // Remove constraints so that we can emulate HTTPS locally.
      constraint =
          Constraint.from(
              constraint.getName(),
              Constraint.Transport.ANY,
              constraint.getAuthorization(),
              constraint.getRoles());
      return constraint;
    }
  }

  // N.B.: Yuck.  Jetty hardcodes all of this logic into an
  // inner class of ContextHandler.  We need to subclass WebAppContext
  // (which extends ContextHandler) and then subclass the SContext
  // inner class to modify its behavior.

  /** Context extension that allows logs to be written to the App Engine log APIs. */
  public class AppEngineServletContext extends ServletScopedContext {

    @Override
    public ClassLoader getClassLoader() {
      return AppEngineWebAppContext.this.getClassLoader();
    }

    /*
    TODO fix logging.
    @Override
    public void log(String message) {
      log(message, null);
    }
     */

    /**
     * {@inheritDoc}
     *
     * @param throwable an exception associated with this log message, or {@code null}.
     */
    /*
    @Override
    public void log(String message, Throwable throwable) {
      StringWriter writer = new StringWriter();
      writer.append("javax.servlet.ServletContext log: ");
      writer.append(message);

      if (throwable != null) {
        writer.append("\n");
        throwable.printStackTrace(new PrintWriter(writer));
      }

      LogRecord.Level logLevel = throwable == null ? LogRecord.Level.info : LogRecord.Level.error;
      ApiProxy.log(
          new ApiProxy.LogRecord(logLevel, System.currentTimeMillis() * 1000L, writer.toString()));
    }

    @Override
    public void log(Exception exception, String msg) {
      log(msg, exception);
    }
     */
  }
}