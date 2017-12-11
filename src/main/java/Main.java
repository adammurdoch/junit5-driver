import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException, ClassNotFoundException {

        // Discover the tests for each test class

        LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request().selectors(
                DiscoverySelectors.selectClass(Test1.class),
                DiscoverySelectors.selectClass(Test2.class)
        ).build();

        TestPlan testPlan = LauncherFactory.create().discover(discoveryRequest);

        // Collect the tests to run somewhere else and serialize the identifiers

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

        System.out.println("Discovered tests:");
        int testCount = 0;
        for (TestIdentifier testIdentifier : testPlan.getRoots()) {
            System.out.println("  " + testIdentifier);
            for (TestIdentifier child : testPlan.getDescendants(testIdentifier)) {
                System.out.println("    " + child);
                // Do some filtering, this would be implemented in a better way
                if (child.getSource().isPresent() && child.getSource().get() instanceof ClassSource) {
                    continue;
                }
                if (child.getSource().isPresent() && child.getSource().get() instanceof MethodSource) {
                    MethodSource methodSource = (MethodSource) child.getSource().get();
                    if (methodSource.getClassName().equals("Test2") && methodSource.getMethodName().equals("broken")) {
                        continue;
                    }
                }
                objectOutputStream.writeObject(child);
                testCount++;
            }
        }

        objectOutputStream.flush();

        // Read in the test identifiers and run the tests

        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
        List<TestIdentifier> ids = new ArrayList<>();
        System.out.println("selecting");
        for (int i = 0; i < testCount; i++) {
            TestIdentifier identifier = (TestIdentifier) objectInputStream.readObject();
            System.out.println(identifier);
            ids.add(identifier);
        }

        LauncherDiscoveryRequest executionRequest = LauncherDiscoveryRequestBuilder.request().selectors(ids.stream().map(id -> DiscoverySelectors.selectUniqueId(id.getUniqueId())).collect(
                Collectors.toList())).build();

        LauncherFactory.create().execute(executionRequest, new TestExecutionListener() {
            @Override
            public void testPlanExecutionStarted(TestPlan testPlan) {
                System.out.println("PLAN STARTED");
            }

            @Override
            public void testPlanExecutionFinished(TestPlan testPlan) {
                System.out.println("PLAN FINISHED");
            }

            @Override
            public void dynamicTestRegistered(TestIdentifier testIdentifier) {
                System.out.println("test discovered " + testIdentifier);
            }

            @Override
            public void executionSkipped(TestIdentifier testIdentifier, String reason) {
                System.out.println("test skipped " + testIdentifier);
            }

            @Override
            public void executionStarted(TestIdentifier testIdentifier) {
                System.out.println("test started " + testIdentifier);
            }

            @Override
            public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                System.out.println("test finished " + testIdentifier);
            }

            @Override
            public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
                System.out.println("test report entry " + testIdentifier + " " + entry);
            }
        });
    }
}
