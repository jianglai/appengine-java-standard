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

package com.google.apphosting.runtime.jetty.ee8;

import com.google.apphosting.runtime.AppVersion;

/** This factory creates {@link AppEngineWebAppContext}. */
public class AppEngineWebAppContextFactory implements WebAppContextFactory {

  @Override
  public AppEngineWebAppContext createContext(AppVersion appVersion, String serverInfo) {
    return new AppEngineWebAppContext(appVersion.getRootDirectory(), serverInfo);
  }
}