package com.google.apphosting.runtime.jetty;

import com.google.apphosting.runtime.AppVersion;
import com.google.apphosting.runtime.jetty.ee8.EE8AppVersionHandlerFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;

import javax.servlet.ServletException;

public interface AppVersionHandlerFactory {
  static AppVersionHandlerFactory newInstance(Server server, String serverInfo)
  {
    return new EE8AppVersionHandlerFactory(server, serverInfo);
  }

  Handler createHandler(AppVersion appVersion) throws ServletException;
}