package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.StopThenStartOnServiceBusProcessorCheck.StopThenStartOnServiceBusProcessorVisitor;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StopThenStartOnServiceBusProcessorCheckTest {


    // Create a mock ProblemsHolder to be used in the test
    @Mock
    private ProblemsHolder mockHolder;

    // Create a mock visitor for visiting the PsiMethodCallExpression
    @Mock
    private StopThenStartOnServiceBusProcessorVisitor mockVisitor;


    @BeforeEach
    public void setUp() {
        // Set up the test
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
    }

    /**
     * If a stop method is called on a ServiceBusProcessorClient object, followed by a start method call on the same object
     * the test should register a problem with the ProblemsHolder
     */
    @Test
    public void testStopThenStart() {

        String stopMethod = "stop";
        boolean isStopMethod = true;

        String startMethod = "start";
        boolean isStartMethod = true;

        String closeMethod = "close";
        boolean isCloseMethod = false;

        String packageName = "com.azure.messaging.servicebus.ServiceBusProcessorClient";
        int numOfInvocations = 1;

        boolean sameVariable = true;

        verifyRegisterProblem(stopMethod, isStopMethod, startMethod, isStartMethod, packageName, closeMethod, isCloseMethod, numOfInvocations, sameVariable);
    }

    /**
     * If a stop method is called on a ServiceBusProcessorClient object, followed by a close method call on the same object
     * Calling a start method on the same object afterward should not register a problem with the ProblemsHolder
     */
    @Test
    public void testStopThenCloseThenStart() {

        String stopMethod = "stop";
        boolean isStopMethod = true;

        String startMethod = "start";
        boolean isStartMethod = true;

        String closeMethod = "close";
        boolean isCloseMethod = true;

        String packageName = "com.azure.messaging.servicebus.ServiceBusProcessorClient";
        int numOfInvocations = 0;

        boolean sameVariable = true;

        verifyRegisterProblem(stopMethod, isStopMethod, startMethod, isStartMethod, packageName, closeMethod, isCloseMethod, numOfInvocations, sameVariable);
    }

    /**
     * If a stop method is called on a ServiceBusProcessorClient object, followed by a start method call on a different object of the same type
     * the test should not register a problem with the ProblemsHolder
     */
    @Test
    public void testStopStartDifferentVariables() {

        String stopMethod = "stop";
        boolean isStopMethod = true;

        String startMethod = "start";
        boolean isStartMethod = true;

        String closeMethod = "close";
        boolean isCloseMethod = false;

        String packageName = "com.azure.messaging.servicebus.ServiceBusProcessorClient";
        int numOfInvocations = 0;

        boolean sameVariable = false;

        verifyRegisterProblem(stopMethod, isStopMethod, startMethod, isStartMethod, packageName, closeMethod, isCloseMethod, numOfInvocations, sameVariable);
    }

    /**
     * If a stop method is called on a different client service, even if the start method is called on the same client service
     * the test should not register a problem with the ProblemsHolder
     */
    @Test
    public void testStopThenStartDifferentService() {

        String stopMethod = "stop";
        boolean isStopMethod = true;

        String startMethod = "start";
        boolean isStartMethod = true;

        String closeMethod = "close";
        boolean isCloseMethod = false;

        String packageName = "com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient";
        int numOfInvocations = 0;

        boolean sameVariable = true;

        verifyRegisterProblem(stopMethod, isStopMethod, startMethod, isStartMethod, packageName, closeMethod, isCloseMethod, numOfInvocations, sameVariable);
    }


    /**
     * This helper method creates a new StopThenStartOnServiceBusProcessorVisitor object
     *
     * @return a new StopThenStartOnServiceBusProcessorVisitor object
     */
    private StopThenStartOnServiceBusProcessorVisitor createVisitor() {
        return new StopThenStartOnServiceBusProcessorVisitor(mockHolder);
    }

    /**
     * This helper method verifies that the ProblemsHolder has been registered with the problem
     *
     * @param stopMethod       the stop method to be called
     * @param isStopMethod     a boolean indicating if the stop method is called
     * @param startMethod      the start method to be called
     * @param isStartMethod    a boolean indicating if the start method is called
     * @param packageName      the package name of the variable
     * @param closeMethod      the close method to be called
     * @param isCloseMethod    a boolean indicating if the close method is called
     * @param numOfInvocations the number of times the problem should be registered
     * @param sameVariable     a boolean indicating if the start method is called on the same variable
     */
    private void verifyRegisterProblem(String stopMethod, boolean isStopMethod, String startMethod, boolean isStartMethod, String packageName, String closeMethod, boolean isCloseMethod, int numOfInvocations, boolean sameVariable) {

        PsiMethodCallExpression stopMethodCallExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression stopMethodExpression = mock(PsiReferenceExpression.class);

        PsiReferenceExpression qualifierExpression = mock(PsiReferenceExpression.class);

        PsiMethodCallExpression startMethodCallExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression startMethodExpression = mock(PsiReferenceExpression.class);

        PsiMethodCallExpression closeMethodCallExpression = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression closeMethodExpression = mock(PsiReferenceExpression.class);

        PsiVariable variable = mock(PsiVariable.class);
        PsiType type = mock(PsiType.class);


        // isServiceBusProcessorClient method
        when(variable.getType()).thenReturn(type);
        when(type.getCanonicalText()).thenReturn(packageName);

        // visitMethodCallExpression method
        when(qualifierExpression.resolve()).thenReturn(variable);

        if (isStopMethod) {
            when(stopMethodCallExpression.getMethodExpression()).thenReturn(stopMethodExpression);
            when(stopMethodExpression.getReferenceName()).thenReturn(stopMethod);
            when(stopMethodExpression.getQualifierExpression()).thenReturn(qualifierExpression);
            mockVisitor.visitMethodCallExpression(stopMethodCallExpression);
        }

        if (isCloseMethod) {
            when(closeMethodCallExpression.getMethodExpression()).thenReturn(closeMethodExpression);
            when(closeMethodExpression.getReferenceName()).thenReturn(closeMethod);
            when(closeMethodExpression.getQualifierExpression()).thenReturn(qualifierExpression);
            mockVisitor.visitMethodCallExpression(closeMethodCallExpression);
        }

        if (isStartMethod) {
            when(startMethodCallExpression.getMethodExpression()).thenReturn(startMethodExpression);
            when(startMethodExpression.getReferenceName()).thenReturn(startMethod);

            if (!sameVariable) {
                PsiReferenceExpression qualifierExpression3 = mock(PsiReferenceExpression.class);
                PsiVariable variable3 = mock(PsiVariable.class);

                when(startMethodExpression.getQualifierExpression()).thenReturn(qualifierExpression3);
                when(qualifierExpression3.resolve()).thenReturn(variable3);
                when(variable3.getType()).thenReturn(type);
            } else {
                when(startMethodExpression.getQualifierExpression()).thenReturn(qualifierExpression);
            }
            mockVisitor.visitMethodCallExpression(startMethodCallExpression);
        }

        // Verify that the ProblemsHolder has been registered with the problem
        verify(mockHolder, times(numOfInvocations)).registerProblem(eq(startMethodCallExpression), contains("Starting Processor that was stopped before is not recommended, and this feature may be deprecated in the future. Please close this processor instance and create a new one to restart processing"));
    }
}
