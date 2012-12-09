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

import com.facebook.tsdb.tsdash.server.data.DataTable;
import com.facebook.tsdb.tsdash.server.data.TsdbDataProvider;
import com.facebook.tsdb.tsdash.server.data.TsdbDataProviderFactory;
import com.facebook.tsdb.tsdash.server.model.Metric;
import com.facebook.tsdb.tsdash.server.model.MetricQuery;
import net.opentsdb.core.DataPoint;
import net.opentsdb.core.DataPoints;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.opentsdb.tsd.TsdApi;
import net.opentsdb.core.Query;
import net.opentsdb.core.Aggregators;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.TimeZone;

public class DataEndpoint extends TsdbServlet {

    private static final long serialVersionUID = 1L;

    @Override
    @SuppressWarnings("unchecked")
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        response.setContentType("application/json");

        TsdApi api = new TsdApi();
        PrintWriter out = response.getWriter();
        try {
            long ts = System.currentTimeMillis();
            // decode parameters
            String jsonParams = request.getParameter("params");
            if (jsonParams == null) {
                throw new Exception("Parameters not specified");
            }

            JSONObject jsonParamsObj = (JSONObject) JSONValue.parse(jsonParams);
            long tsFrom = (Long) jsonParamsObj.get("tsFrom");
            long tsTo = (Long) jsonParamsObj.get("tsTo");
            JSONArray metricsArray = (JSONArray) jsonParamsObj.get("metrics");
            if (metricsArray.size() == 0) {
                throw new Exception("No metrics to fetch");
            }
            MetricQuery[] metricQueries = new MetricQuery[metricsArray.size()];
            for (int i = 0; i < metricsArray.size(); i++) {
                metricQueries[i] = MetricQuery
                        .fromJSONObject((JSONObject) metricsArray.get(i));
            }

            Query[] queries = new Query[1];
            queries[0] = _tsdb.newQuery();
            queries[0].setTimeSeries(metricQueries[0].name, metricQueries[0].tags, net.opentsdb.core.Aggregators.SUM, metricQueries[0].rate);

            net.opentsdb.graph.Plot plot = api.Query(_tsdb, tsFrom, tsTo, queries, TimeZone.getDefault(), false);

            long loadTime = System.currentTimeMillis() - ts;
            JSONObject responseObj = new JSONObject();
            responseObj.put("data", test(plot));
            /*
            for (Metric metric : metrics) {
                encodedMetrics.add(metric.toJSONObject());
            }
            responseObj.put("metrics", encodedMetrics);
            responseObj.put("loadtime", loadTime);
            DataTable dataTable = new DataTable(metrics);
            responseObj.put("series", dataTable.toJSONObject());
              */

            doSendResponse(request, out, responseObj.toJSONString());

            long encodingTime = System.currentTimeMillis() - ts - loadTime;
            logger.info("[Data] time frame: " + (tsTo - tsFrom) + "s, "
                    + "load time: " + loadTime + "ms, " + "encoding time: "
                    + encodingTime + "ms");
        } catch (Exception e) {
            out.println(getErrorResponse(e));
        }
        out.close();
    }

    public JSONArray test(net.opentsdb.graph.Plot plot) {
        int npoints = 0;
        JSONArray arrary = new JSONArray();

        for (DataPoints dataPoints : plot.getDataPoints()) {
            for (DataPoint point : dataPoints) {
                JSONArray values = new JSONArray();
                values.add(point.timestamp() * 1000);
                if (point.isInteger()) {
                    values.add(point.longValue());
                } else {
                    final double value = point.doubleValue();
                    if (value != value || Double.isInfinite(value)) {
                        throw new IllegalStateException("invalid datapoint found");
                    }
                    values.add(value);
                }
                arrary.add(values);
            }

        }
        return arrary;
    }

}
