/*
 * Copyright 2011 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.tsdb.tsdash.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.simple.JSONObject;

import com.facebook.tsdb.tsdash.server.data.hbase.HBaseConnection;

public class TsdbServlet extends HttpServlet {

    protected static Logger logger = Logger.getLogger("com.facebook.tsdb.services");

    private static final long serialVersionUID = 1L;
    public static final String PROPERTIES_FILE = "/etc/tsdash/tsdash.properties";
    public static final String LOG4J_PROPERTIES_FILE = "/etc/tsdash/log4j.properties";

    private String hostname = null;

    private static void loadConfiguration() {
        Properties tsdbConf = new Properties();
        try {
            PropertyConfigurator.configure(LOG4J_PROPERTIES_FILE);
            tsdbConf.load(new FileInputStream(PROPERTIES_FILE));
            HBaseConnection.configure(tsdbConf);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find "  + PROPERTIES_FILE);
        } catch (IOException e) {
            System.err.println("Cannot find "  + PROPERTIES_FILE);
        }
    }

    static {
        loadConfiguration();
    }

    @SuppressWarnings("unchecked")
    protected String getErrorResponse(Throwable e) {
        JSONObject errObj = new JSONObject();
        errObj.put("error", e.getMessage());
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        errObj.put("stacktrace", stackTrace.toString());
        return errObj.toJSONString();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    protected void doSendResponse(HttpServletRequest request, PrintWriter out, String jsonString) {
        // jsonp support
        String jsonCallback = request.getParameter("jsoncallback");
        if((jsonCallback != null) && (!jsonCallback.isEmpty())) {
            out.print(jsonCallback + "('");
            out.println(jsonString);
            out.println("');");
        }
        else {
            out.println(jsonString);
        }
    }

}
