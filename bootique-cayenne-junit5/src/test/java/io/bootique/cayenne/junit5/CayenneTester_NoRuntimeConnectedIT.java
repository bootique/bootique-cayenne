package io.bootique.cayenne.junit5;

import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestTool;
import io.bootique.junit5.handler.testtool.BQTestToolHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@BQTest
public class CayenneTester_NoRuntimeConnectedIT {

    @ExtendWith(BQTestToolHandler.class)
    static class TesterProvider{
        @BQTestTool
        CayenneTester ct = CayenneTester.create();

        public CayenneTester getCt() {
            return ct;
        }

        @Test
        void testMethod(){}
    }

    @Test
    public void testCayenneTesterInitialization(){
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(TesterProvider.class))
                .build();

        final org.junit.platform.launcher.Launcher launcher = LauncherFactory.create();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();
        final ThrowableCatchingListener throwableCatchingListener = new ThrowableCatchingListener();

        launcher.registerTestExecutionListeners(listener, throwableCatchingListener);
        launcher.execute(request);
        TestExecutionSummary summary = listener.getSummary();
        Assertions.assertEquals(1, summary.getTestsFailedCount());
        List<Throwable> throwableList = throwableCatchingListener.getThrowableList();
        for(Throwable throwable:throwableList){
            Assertions.assertEquals("Cayenne tester not linked to any BQRuntime!", throwable.getMessage());
        }
    }


    private class ThrowableCatchingListener implements TestExecutionListener{
        private final List<Throwable> throwableList = new ArrayList<>();

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            TestExecutionListener.super.executionFinished(testIdentifier, testExecutionResult);
            Optional<Throwable> throwable = testExecutionResult.getThrowable();
            throwable.ifPresent(throwableList::add);
        }

        public List<Throwable> getThrowableList() {
            return throwableList;
        }
    }
}
