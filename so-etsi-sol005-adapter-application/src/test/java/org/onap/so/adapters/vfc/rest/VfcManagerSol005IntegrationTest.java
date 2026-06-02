/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2026 Deutsche Telekom AG
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

package org.onap.so.adapters.vfc.rest;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.so.adapters.vfc.MSOVfcApplication;
import org.onap.so.adapters.vfc.exceptions.ApplicationException;
import org.onap.so.adapters.vfc.model.NSResourceInputParameter;
import org.onap.so.adapters.vfc.model.RestfulResponse;
import org.onap.so.adapters.vfc.util.JsonUtil;
import org.onap.so.db.request.beans.InstanceNfvoMapping;
import org.onap.so.db.request.data.repository.InstanceNfvoMappingRepository;
import org.onap.so.db.request.data.repository.ResourceOperationStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = MSOVfcApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "mso.msb-ip=localhost",
                "mso.msb-port=" + VfcManagerSol005IntegrationTest.WIREMOCK_PORT,
                "mariaDB4j.port=" + VfcManagerSol005IntegrationTest.MARIADB_PORT,
                "mariaDB4j.baseDir=/tmp/MariaDB4j-inttest",
                "spring.datasource.jdbc-url=jdbc:mariadb://localhost:"
                        + VfcManagerSol005IntegrationTest.MARIADB_PORT + "/requestdb",
                "spring.flyway.enabled=false",
                "flyway.enabled=false",
                "test.hibernate.ddl-auto=create"
        })
@ActiveProfiles("test")
public class VfcManagerSol005IntegrationTest {

    static final int WIREMOCK_PORT = 19876;
    static final int MARIADB_PORT = 3308;

    @ClassRule
    public static WireMockClassRule wireMockServer = new WireMockClassRule(WIREMOCK_PORT);

    @Rule
    public WireMockClassRule wireMockRule = wireMockServer;

    private static final String NFVO_ID = "b1bb0ce7-2222-4fa7-95ed-4840d70a1101";
    private static final String NS_INSTANCE_ID = "inttest-ns-instance-id";
    private static final String JOB_ID = "inttest-job-id";

    @Autowired
    private VfcManagerSol005 vfcManagerSol005;

    @Autowired
    private InstanceNfvoMappingRepository instanceNfvoMappingRepository;

    @Autowired
    private ResourceOperationStatusRepository resourceOperationStatusRepository;

    @After
    public void cleanDatabase() {
        instanceNfvoMappingRepository.deleteAll();
        resourceOperationStatusRepository.deleteAll();
    }

    private NSResourceInputParameter loadCreateNsRequest() throws Exception {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        String content = new String(
                Files.readAllBytes(new File(cl.getResource("json/createNsReq.json").getFile()).toPath()))
                        .replace("\n", "");
        NSResourceInputParameter param = JsonUtil.unMarshal(content, NSResourceInputParameter.class);
        param.getNsParameters().getAdditionalParamForNs().put("isSol005Interface", "true");
        return param;
    }

    private String aaiNfvoResponseBody() {
        return "{\"nfvoId\":\"" + NFVO_ID + "\","
                + "\"name\":\"external_nfvo\","
                + "\"api-root\":\"\","
                + "\"vendor\":\"vz\","
                + "\"version\":\"v1.0\","
                + "\"url\":\"http://localhost:" + WIREMOCK_PORT + "\","
                + "\"userName\":\"admin\","
                + "\"password\":\"sacjnasnc\"}";
    }

    private void stubAaiNfvoLookup() {
        stubFor(get(urlPathEqualTo("/api/aai-esr-server/v1/nfvos/" + NFVO_ID))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(aaiNfvoResponseBody())));
    }

    private InstanceNfvoMapping seedNfvoMapping(String instanceId) {
        InstanceNfvoMapping mapping = new InstanceNfvoMapping();
        mapping.setInstanceId(instanceId);
        mapping.setEndpoint("http://localhost:" + WIREMOCK_PORT);
        mapping.setApiRoot("");
        mapping.setNfvoName(NFVO_ID);
        mapping.setUsername("admin");
        mapping.setPassword("sacjnasnc");
        return instanceNfvoMappingRepository.save(mapping);
    }

    private InstanceNfvoMapping seedNfvoMappingWithJob(String instanceId, String jobId) {
        InstanceNfvoMapping mapping = seedNfvoMapping(instanceId);
        mapping.setJobId(jobId);
        return instanceNfvoMappingRepository.save(mapping);
    }

    @Test
    public void createNs_happyPath_sendsCorrectRequestsToNfvoAndAai() throws Exception {
        stubAaiNfvoLookup();
        stubFor(post(urlPathEqualTo("/api/nslcm/v1/ns_instances"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"" + NS_INSTANCE_ID + "\"}")));

        NSResourceInputParameter request = loadCreateNsRequest();
        RestfulResponse response = vfcManagerSol005.createNs(request);

        assertEquals(201, response.getStatus());

        String expectedAuth =
                "Basic " + Base64.getEncoder().encodeToString("AAI:AAI".getBytes());
        verify(getRequestedFor(urlPathEqualTo("/api/aai-esr-server/v1/nfvos/" + NFVO_ID))
                .withHeader("Authorization", equalTo(expectedAuth)));

        verify(postRequestedFor(urlPathEqualTo("/api/nslcm/v1/ns_instances"))
                .withHeader("Content-Type", containing("application/json")));
    }

    @Test
    public void instantiateNs_happyPath_sendsInstantiateRequestToNfvo() throws Exception {
        seedNfvoMapping(NS_INSTANCE_ID);
        String locationHeader = "http://localhost:" + WIREMOCK_PORT + "/ns_lcm_op_occs/" + JOB_ID;

        stubFor(post(urlPathEqualTo(
                "/api/nslcm/v1/ns_instances/" + NS_INSTANCE_ID + "/instantiate"))
                        .willReturn(aResponse().withStatus(202)
                                .withHeader("Location", locationHeader)));

        NSResourceInputParameter request = loadCreateNsRequest();
        RestfulResponse response = vfcManagerSol005.instantiateNs(NS_INSTANCE_ID, request);

        assertEquals(202, response.getStatus());
        verify(postRequestedFor(urlPathEqualTo(
                "/api/nslcm/v1/ns_instances/" + NS_INSTANCE_ID + "/instantiate"))
                        .withHeader("Content-Type", containing("application/json")));
    }

    @Test
    public void terminateNs_happyPath_sendsTerminateRequestToNfvo() throws Exception {
        seedNfvoMapping(NS_INSTANCE_ID);
        String locationHeader = "http://localhost:" + WIREMOCK_PORT + "/ns_lcm_op_occs/" + JOB_ID;

        stubFor(post(urlPathEqualTo(
                "/api/nslcm/v1/ns_instances/" + NS_INSTANCE_ID + "/terminate"))
                        .willReturn(aResponse().withStatus(202)
                                .withHeader("Location", locationHeader)));

        NSResourceInputParameter request = loadCreateNsRequest();
        RestfulResponse response =
                vfcManagerSol005.terminateNs(request.getNsOperationKey(), NS_INSTANCE_ID);

        assertEquals(202, response.getStatus());
        verify(postRequestedFor(urlPathEqualTo(
                "/api/nslcm/v1/ns_instances/" + NS_INSTANCE_ID + "/terminate")));
    }

    @Test
    public void deleteNs_happyPath_sendsDeleteRequestToNfvo() throws Exception {
        stubFor(delete(urlPathEqualTo("/api/nslcm/v1/ns_instances/" + NS_INSTANCE_ID))
                .willReturn(aResponse().withStatus(204)));

        NSResourceInputParameter request = loadCreateNsRequest();
        RestfulResponse response =
                vfcManagerSol005.deleteNs(request.getNsOperationKey(), NS_INSTANCE_ID);

        assertEquals(204, response.getStatus());
        verify(deleteRequestedFor(
                urlPathEqualTo("/api/nslcm/v1/ns_instances/" + NS_INSTANCE_ID)));
    }

    @Test
    public void getNsProgress_happyPath_sendsQueryRequestToNfvo() throws Exception {
        seedNfvoMappingWithJob(NS_INSTANCE_ID, JOB_ID);
        String lcmResponse = "{\"id\":\"" + JOB_ID + "\","
                + "\"lcmOperationType\":\"INSTANTIATE\","
                + "\"nsInstanceId\":\"" + NS_INSTANCE_ID + "\","
                + "\"operationState\":\"PROCESSING\","
                + "\"statusEnteredTime\":\"2026-04-10T10:00:00\"}";

        stubFor(get(urlPathEqualTo("/api/nslcm/v1/ns_lcm_op_occs/" + JOB_ID))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(lcmResponse)));

        NSResourceInputParameter request = loadCreateNsRequest();
        RestfulResponse response =
                vfcManagerSol005.getNsProgress(request.getNsOperationKey(), JOB_ID);

        assertEquals(200, response.getStatus());
        verify(getRequestedFor(urlPathEqualTo("/api/nslcm/v1/ns_lcm_op_occs/" + JOB_ID)));
    }

    @Test(expected = ApplicationException.class)
    public void createNs_whenNfvoReturns500_throwsApplicationException() throws Exception {
        stubAaiNfvoLookup();
        stubFor(post(urlPathEqualTo("/api/nslcm/v1/ns_instances"))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        vfcManagerSol005.createNs(loadCreateNsRequest());
    }

    @Test(expected = ApplicationException.class)
    public void createNs_whenAaiReturns503_throwsApplicationException() throws Exception {
        stubFor(get(urlPathEqualTo("/api/aai-esr-server/v1/nfvos/" + NFVO_ID))
                .willReturn(aResponse().withStatus(503)));

        vfcManagerSol005.createNs(loadCreateNsRequest());
    }

    @Test(expected = ApplicationException.class)
    public void deleteNs_whenNfvoReturns500_throwsApplicationException() throws Exception {
        stubFor(delete(urlPathEqualTo("/api/nslcm/v1/ns_instances/" + NS_INSTANCE_ID))
                .willReturn(aResponse().withStatus(500)));

        NSResourceInputParameter request = loadCreateNsRequest();
        vfcManagerSol005.deleteNs(request.getNsOperationKey(), NS_INSTANCE_ID);
    }

    @Test(expected = ApplicationException.class)
    public void instantiateNs_whenNfvoReturns409_throwsApplicationException() throws Exception {
        seedNfvoMapping(NS_INSTANCE_ID);
        stubFor(post(urlPathEqualTo(
                "/api/nslcm/v1/ns_instances/" + NS_INSTANCE_ID + "/instantiate"))
                        .willReturn(aResponse().withStatus(409)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"detail\":\"NS already instantiated\"}")));

        NSResourceInputParameter request = loadCreateNsRequest();
        vfcManagerSol005.instantiateNs(NS_INSTANCE_ID, request);
    }
}
