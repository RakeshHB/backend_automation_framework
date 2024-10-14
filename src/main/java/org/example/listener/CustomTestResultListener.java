package org.example.listener;

import lombok.extern.log4j.Log4j2;
import org.testng.ITestListener;
import org.testng.ITestResult;

@Log4j2
public class CustomTestResultListener implements ITestListener {

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("*********************************** Test Case Result: PASS ***********************************");
        log.info("*********************************** Ending Test ***********************************");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("************************************ Failure reason: ************************************");
        log.error(result.getThrowable().getMessage());
        log.error("*********************************** Test Case Result: FAIL ***********************************");
        log.error("*********************************** Ending Test ***********************************");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.error("*********************************** Test Case Result: SKIPPED ***********************************");
        log.error("*********************************** Ending Test ***********************************");
    }

}
