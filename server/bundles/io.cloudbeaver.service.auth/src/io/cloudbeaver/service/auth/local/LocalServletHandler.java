/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cloudbeaver.service.auth.local;

import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.server.CBApplication;
import io.cloudbeaver.server.CBPlatform;
import io.cloudbeaver.server.actions.AbstractActionServletHandler;
import io.cloudbeaver.utils.ServletAppUtils;
import io.cloudbeaver.server.actions.CBServerAction;
import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class LocalServletHandler extends AbstractActionServletHandler {

    public static final String URI_PREFIX = "open";
    public static final String PARAM_PROJECT_ID = "project_id";
    public static final String PARAM_CONNECTION_ID = "id";
    public static final String PARAM_CONNECTION_NAME = "name";
    public static final String PARAM_CONNECTION_URL = "url";

    private static final Log log = Log.getLog(LocalServletHandler.class);

    @Override
    public boolean handleRequest(Servlet servlet, HttpServletRequest request, HttpServletResponse response) throws DBException, IOException {
        if (URI_PREFIX.equals(ServletAppUtils.removeSideSlashes(request.getServletPath()))) {
            try {
                WebSession webSession = CBApplication.getInstance().getSessionManager().getWebSession(request, response, true);

                Map<String, Object> parameters = new HashMap<>();
                for (Enumeration<String> ne = request.getParameterNames(); ne.hasMoreElements(); ) {
                    String paramName = ne.nextElement();
                    parameters.put(paramName, request.getParameter(paramName));
                }
                CBServerAction action = new CBServerAction(getActionConsole(), parameters);
                action.saveInSession(webSession);

                // Redirect to home
                String rootURI = ServletAppUtils.getServletApplication().getServerConfiguration().getRootURI();

                // **Vulnerability Mitigation Check**
                if ("/".equals(rootURI)) {
                    log.info("[LocalServletHandler] Redirecting to safe path: /");
                    // Only redirect to the safe, hardcoded, relative path
                    response.sendRedirect("/"); 
                } else {
                    // Log the attempted unsafe redirect for auditing
                    log.error("[LocalServletHandler] Configuration Error: Attempted to redirect to non-root URI: " + rootURI);
                    
                    // **Handle the error gracefully:**
                    // 1. Send an error response to the user.
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid application configuration for root redirection.");
                    
                    // OR 2. Redirect to a known safe path as a fallback (less secure than 1, but avoids a failure)
                    // response.sendRedirect("/"); 
                    
                    // It's generally better to fail securely (option 1) if the configuration is unexpectedly unsafe.
                }

                return true;
            } catch (Exception e) {
                log.error("Error saving open DB action in session", e);
            }
        }
        return false;
    }

    @Override
    protected String getActionConsole() {
        return LocalSessionHandler.ACTION_LOCAL_CONSOLE;
    }
}
