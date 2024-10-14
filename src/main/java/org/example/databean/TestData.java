package org.example.databean;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Getter
@Setter
public class TestData {

    private String id;
    private String testCaseId;
    private String testScenario;
    private String testSteps;
    private HttpMethod httpMethod;
    private String endPoint;
    private String apiPath;
    private HttpHeaders httpHeaders;
    private Map<String, String> params;
    private String requestBody;
    private int expectedResponseCode;
    private String expectedResponseBody;
}
