package org.example.dataprovider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.databean.TestData;
import org.example.exception.ValidationException;
import org.example.util.ConfigReaderUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.testng.annotations.DataProvider;

import java.util.HashMap;
import java.util.Set;

@Log4j2
public class RestRequestDataProviderXlsx {

    @DataProvider(name = "restRequestDataProvider")
    public Object[][] provideData() {
        try (Workbook workbook = new XSSFWorkbook(ConfigReaderUtil.getConfig("dataProvider"))) {
            Sheet sheet = workbook.getSheet(ConfigReaderUtil.getConfig("sheet_name"));
            int size = sheet.getPhysicalNumberOfRows() - 1;
            Object[][] objects = new Object[size][2];

            for (int i = 1; i <= size; i++) {
                Row row = sheet.getRow(i);
                TestData testData = createTestData(row);
                objects[i - 1] = new Object[]{testData.getId(), testData};
            }
            return objects;
        } catch (Exception e) {
            log.error("Exception occurred in data provider");
            throw new RuntimeException(e);
        }
    }

    private TestData createTestData(Row row) {
        TestData testData = new TestData();
        try {
            testData.setId(validateInput(row, "col_id", "id", false, true));
            testData.setTestCaseId(validateInput(row,
                    "col_test_case_id", "test case id", true, false));
            testData.setTestScenario(validateInput(row,
                    "col_test_scenario", "test scenario", false, false));
            testData.setTestSteps(validateInput(row,
                    "col_test_steps", "test steps", false, false));
            testData.setHttpMethod(computeHttpMethod(row));
            testData.setEndPoint(enrich(validateInput(row,
                    "col_endpoint", "endpoint", false, true)));
            testData.setApiPath(validateInput(row,
                    "col_api_path", "api path", false, true));
            testData.setRequestBody(validateInput(row,
                    "col_request_body", "request body", false, true));
            testData.setExpectedResponseBody(validateInput(row, "col_expected_response_body",
                    "expected response body", false, true));
            testData.setExpectedResponseCode(computeResponseCode(validateInput(row, "col_expected_response_code",
                    "expected response code", false, true)));
            testData.setParams(new ObjectMapper().readValue(validateInput(row, "col_params", "params",
                    false, true), new TypeReference<HashMap<String, String>>() {
            }));
            testData.setHttpHeaders(computeHttpHeaders(validateInput(row, "col_headers",
                    "headers", false, true)));
        } catch (JsonProcessingException e) {
            logAndThrowValidationException("params in excel is invalid");
        }
        return testData;
    }


    private HttpHeaders computeHttpHeaders(String rawHeaders) {
        HttpHeaders httpHeaders = new HttpHeaders();;
        try {
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(rawHeaders);
            if (!jsonObject.isEmpty()) {
                Set<String> strings = jsonObject.keySet();
                for (String string : strings) {
                    httpHeaders.add(string, jsonObject.get(string).toString());
                }
            }
        } catch (ParseException e) {
            logAndThrowValidationException("headers in excel is invalid");
        }
        return httpHeaders;
    }

    private int computeResponseCode(String s) {
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            String msg = String.format("Unable to parse '%s' as expected response code in excel", s);
            logAndThrowValidationException(msg);
            return -1; // This line will never be reached, but included for completeness
        }
    }


    private String enrich(String endPoint) {
        return ConfigReaderUtil.getConfig(endPoint);
    }

    private HttpMethod computeHttpMethod(Row row) {
        String requestType = validateInput(row,
                "col_http_method", "http method", false, true).toUpperCase();

        switch (requestType) {
            case "GET":
                return HttpMethod.GET;
            case "POST":
                return HttpMethod.POST;
            default:
                String msg = String.format("%s in excel is invalid", requestType);
                log.error(msg);
                throw new ValidationException(msg);
        }
    }


    private String validateInput(Row row, String key, String data, boolean initialiseEmpty, boolean mandatory) {
        String stringCellValue = null;
        try {
            Cell cell = row.getCell(Integer.parseInt(ConfigReaderUtil.getConfig(key)));
            cell.setCellType(CellType.STRING);
            stringCellValue = cell.getStringCellValue();
            if (StringUtils.isBlank(stringCellValue)) {
                String msg = data + " in excel is blank";
                if (mandatory) {
                    log.error(msg);
                    throw new ValidationException(msg);
                }
                if (initialiseEmpty)
                    stringCellValue = "";
            } else if (!initialiseEmpty) {
                stringCellValue = stringCellValue.trim();
            }
        } catch (NumberFormatException e) {
            String msg = String.format("Unable to parse int for %s in config", key);
            logAndThrowValidationException(msg);
        }
        return stringCellValue;
    }

    private void logAndThrowValidationException(String msg) {
        log.error(msg);
        throw new ValidationException(msg);
    }

}
