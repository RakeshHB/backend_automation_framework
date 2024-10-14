package org.example.responsehandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONString;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.JSONParser;
import org.testng.Assert;

public class JsonResponseValidator extends AbstractResponseValidator {
    @Override
    public void validateResponse(String actualResponseBody, String expectedResponseBody,
                                 int actualResponseCode, int expectedResponseCode) {
        validateHttpCode(actualResponseCode, expectedResponseCode);
        JSONCompareResult result;
        try {
            Object expected = JSONParser.parseJSON(expectedResponseBody);
            Object actual = JSONParser.parseJSON(actualResponseBody);
            if ((expected instanceof JSONObject) && (actual instanceof JSONObject)) {
                result = JSONCompare.compareJSON((JSONObject) expected, (JSONObject) actual, JSONCompareMode.LENIENT);
            } else if ((expected instanceof JSONArray) && (actual instanceof JSONArray)) {
                result = JSONCompare.compareJSON((JSONArray) expected, (JSONArray) actual, JSONCompareMode.LENIENT);
            } else if (expected instanceof JSONString && actual instanceof JSONString) {
                result = JSONCompare.compareJson((JSONString) expected, (JSONString) actual);
            } else if (expected instanceof JSONObject) {
                result =  new JSONCompareResult().fail("", expected, actual);
            } else {
                result =  new JSONCompareResult().fail("", expected, actual);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        if (result.failed()) {
            Assert.fail("actual response body didn't contain expected response body " + result.getMessage());
        }
    }
}
