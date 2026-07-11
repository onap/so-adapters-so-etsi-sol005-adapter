/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (c) 2019 Samsung. All rights reserved.
 * ================================================================================
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
 * ============LICENSE_END=========================================================
 */

package org.onap.so.adapters.vfc.util;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.so.adapters.vfc.model.RestfulResponse;
import org.springframework.http.HttpStatus;
import jakarta.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RestfulUtilTest {

    @InjectMocks
    @Spy
    private RestfulUtil restfulUtil;

    @Mock
    private HttpClient client;

    @Before
    public void setUp() {
        doReturn("https://testHost").when(restfulUtil).getMsbHost();
    }

    private ClassicHttpResponse buildResponse(String body) {
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(HttpStatus.OK.value());
        response.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        response.setHeader("Content-Type", "application/json");
        response.setHeader("cache-control", "no-cache");
        return response;
    }

    @Test
    public void sendGet() throws Exception {

        when(client.execute(any(HttpGet.class))).thenReturn(buildResponse("GET"));

        RestfulResponse restfulResponse = restfulUtil.send("test", HttpMethod.GET, "some request content");

        assertEquals(HttpStatus.OK.value(), restfulResponse.getStatus());
        assertEquals("GET", restfulResponse.getResponseContent());

        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("a", "b");
        RestfulResponse restfulResponse1 =
                restfulUtil.send("test", HttpMethod.GET, "some request content", requestHeader);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), restfulResponse1.getStatus());

    }

    @Test
    public void sendPost() throws Exception {

        when(client.execute(any(HttpPost.class))).thenReturn(buildResponse("POST"));

        RestfulResponse restfulResponse = restfulUtil.send("test", HttpMethod.POST, "some request content");

        assertEquals(HttpStatus.OK.value(), restfulResponse.getStatus());
        assertEquals("POST", restfulResponse.getResponseContent());

        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("a", "b");
        RestfulResponse restfulResponse1 =
                restfulUtil.send("test", HttpMethod.POST, "some request content", requestHeader);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), restfulResponse1.getStatus());

    }

    @Test
    public void sendPut() throws Exception {

        when(client.execute(any(HttpPut.class))).thenReturn(buildResponse("PUT"));

        RestfulResponse restfulResponse = restfulUtil.send("test", HttpMethod.PUT, "some request content");

        assertEquals(HttpStatus.OK.value(), restfulResponse.getStatus());
        assertEquals("PUT", restfulResponse.getResponseContent());

        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("a", "b");
        RestfulResponse restfulResponse1 =
                restfulUtil.send("test", HttpMethod.PUT, "some request content", requestHeader);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), restfulResponse1.getStatus());

    }

    @Test
    public void sendDelete() throws Exception {

        when(client.execute(any(HttpDelete.class))).thenReturn(buildResponse("DELETE"));

        RestfulResponse restfulResponse = restfulUtil.send("test", HttpMethod.DELETE, "some request content");

        assertEquals(HttpStatus.OK.value(), restfulResponse.getStatus());
        assertEquals("DELETE", restfulResponse.getResponseContent());

        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("a", "b");
        RestfulResponse restfulResponse1 =
                restfulUtil.send("test", HttpMethod.DELETE, "some request content", requestHeader);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), restfulResponse1.getStatus());

    }

    @Test
    public void sendOptions() throws Exception {

        RestfulResponse restfulResponse = restfulUtil.send("test", HttpMethod.OPTIONS, "some request content");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), restfulResponse.getStatus());
        assertEquals("Error processing request to VFC", restfulResponse.getResponseContent());

        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("a", "b");
        RestfulResponse restfulResponse1 =
                restfulUtil.send("test", HttpMethod.OPTIONS, "some request content", requestHeader);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), restfulResponse1.getStatus());
    }

    @Test
    public void getNfvoFromAAITest() throws Exception {

        RestfulResponse restfulResponse = restfulUtil.getNfvoFromAAI("test");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), restfulResponse.getStatus());
    }
}
