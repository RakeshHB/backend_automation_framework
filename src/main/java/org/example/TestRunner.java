package org.example;

import lombok.extern.log4j.Log4j2;
import org.example.databean.TestData;
import org.example.dataprovider.RestRequestDataProviderXlsx;
import org.example.responsehandler.AbstractResponseValidator;
import org.example.responsehandler.ResponseValidatorFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

import static org.example.Constants.SCHEMA;

@Log4j2
public class TestRunner
{
    @Test(dataProvider = "restRequestDataProvider", dataProviderClass = RestRequestDataProviderXlsx.class)
    public void runTest(String id, TestData testData) {
        try {
            log.info("*********************************** Starting Test : " + id + "***********************************");
            HttpMethod httpMethod = testData.getHttpMethod();
            log.info("http method is: {}", httpMethod);
            HttpHeaders httpHeaders = testData.getHttpHeaders();
            log.info("headers is: {}", httpHeaders);
            Map<String, String> params = testData.getParams();
            log.info("params is: {}", params);
            String url = computeAndLogComputedUrl(testData.getEndPoint(), testData.getApiPath(), params);
            String requestBody = testData.getRequestBody();
            log.info("request body is: {}", requestBody);
            HttpEntity<String> request = buildRequest(httpMethod, httpHeaders, requestBody);
            ResponseEntity<String> response = fire(url, httpMethod, request);
            int expectedResponseCode = testData.getExpectedResponseCode();
            log.info("expected response code is: {}", expectedResponseCode);
            int actualResponseCode = response.getStatusCodeValue();
            log.info("actual response code is: {}", actualResponseCode);
            httpHeaders = response.getHeaders();
            log.info("headers in actual response is: {}", httpHeaders);
            String expectedResponseBody = testData.getExpectedResponseBody();
            log.info("expected response body is: {}", expectedResponseBody);
            String actualResponseBody = validateAndGetActualResponseBody(response);
            log.info("actual response body is: {}", actualResponseBody);
            boolean isSchemaValidation = testData.getTestCaseId().equalsIgnoreCase(SCHEMA);
            AbstractResponseValidator responseValidator = ResponseValidatorFactory.getInstance(isSchemaValidation);
            responseValidator.validateResponse(actualResponseBody, expectedResponseBody,
                    actualResponseCode, expectedResponseCode);
        } catch (Exception e) {
            String msg = "Exception occurred when running test";
            logAndThrowRunTimeException(msg, e);
        }
    }

    private String validateAndGetActualResponseBody(ResponseEntity<String> response) {
        if (!response.hasBody()) {
            Assert.fail("Actual response doesn't have a body");
        }
        return response.getBody();
    }


    private ResponseEntity<String> fire(String url, HttpMethod httpMethod, HttpEntity<String> request) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.exchange(url, httpMethod, request, String.class);
        } catch (RestClientException e) {
            String msg = "Exception occurred when firing request";
            logAndThrowRunTimeException(msg, e);
        }
        // This line will never be reached
        throw new IllegalStateException("Unexpected state: no response returned");
    }

    private void logAndThrowRunTimeException(String msg, Exception e) {
        log.error(msg);
        throw new RuntimeException(e);
    }

    private HttpEntity<String> buildRequest(HttpMethod httpMethod, HttpHeaders httpHeaders, String requestBody) {
        return (httpMethod == HttpMethod.GET)
                ? new HttpEntity<>(httpHeaders)
                : new HttpEntity<>(requestBody, httpHeaders);
    }

    private String computeAndLogComputedUrl(String endPoint, String apiPath, Map<String, String> params) {
        String url = endPoint + apiPath;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }
        url = builder.toUriString();
        log.info("url is: {}", url);
        return url;
    }
}
