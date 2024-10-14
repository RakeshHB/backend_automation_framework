package org.example.responsehandler;

import org.testng.Assert;

public abstract class AbstractResponseValidator {

    public abstract void validateResponse(String actualResponseBody, String expectedResponseBody,
                                          int actualResponseCode, int expectedResponseCode);

    public void validateHttpCode(int actualResponseCode, int expectedResponseCode) {

        Assert.assertEquals(actualResponseCode, expectedResponseCode, "actual response code is not equal to " +
                "expected response code");


    }

}
