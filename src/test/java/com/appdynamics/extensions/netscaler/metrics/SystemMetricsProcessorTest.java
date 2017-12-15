package com.appdynamics.extensions.netscaler.metrics;

import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Lists;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

/**
 * Created by aditya.jagtiani on 12/6/17.
 */
public class SystemMetricsProcessorTest {
    private MonitorConfiguration configuration = mock(MonitorConfiguration.class);
    private CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    private CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    private MetricWriteHelper metricWriteHelper = mock(MetricWriteHelper.class);
    private Map<String, ?> conf;
    private BasicHttpEntity entity;
    private StatusLine statusLine = mock(StatusLine.class);

    @Before
    public void setup() throws IOException {
        conf = YmlReader.readFromFile(new File
                ("src/test/resources/conf/config.yml"));
        entity = new BasicHttpEntity();
        entity.setContent(new FileInputStream("src/test/resources/json/system.json"));
    }

    @Test
    public void getSystemMetricsTest() throws IOException {
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        CountDownLatch latch = new CountDownLatch(1);
        when(configuration.getHttpClient()).thenReturn(httpClient);
        when(configuration.getMetricPrefix()).thenReturn("Metric Prefix|");
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(entity);

        Map allMetrics = (Map) conf.get("metrics");
        List<Map> systemMetricsFromCfg = (List) allMetrics.get("System");
        SystemMetricsProcessor systemMetricsProcessor = new SystemMetricsProcessor(configuration, metricWriteHelper,
                "serverURL", "server1", systemMetricsFromCfg, latch);
        systemMetricsProcessor.run();
        verify(metricWriteHelper, times(1)).transformAndPrintMetrics(captor.capture());

        List<Metric> resultList = captor.getValue();
        for (Metric metric : resultList) {
            Assert.assertTrue(metricNamesFromAPI().contains(metric.getMetricName()));
        }
    }

    private List<String> metricNamesFromAPI() {
        List<String> systemMetricsFromAPI = Lists.newArrayList();
        systemMetricsFromAPI.add("memsizemb");
        systemMetricsFromAPI.add("cpuusagepcnt");
        systemMetricsFromAPI.add("memusagepcnt");
        return systemMetricsFromAPI;
    }
}
