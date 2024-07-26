package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiResourceList;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.PsiVariable;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.ClosingCloseableClientsCheck.CloseableClientVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class tests the ClosingCloseableClientsCheck class
 * <p>
 * If a Closeable client is not closed, a warning is issued.
 */
public class ClosingCloseableClientsCheckTest {

    // Create a mock ProblemsHolder to be used in the test
    @Mock
    private ProblemsHolder mockHolder;

    // Create a mock JavaElementVisitor for visiting the PsiVariable
    @Mock
    private CloseableClientVisitor mockVisitor;

    // Create a mock PsiVariable.
    @Mock
    private PsiVariable mockVariable;


    @BeforeEach
    public void setUp() {
        // Set up the test
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
        mockVariable = mock(PsiVariable.class);
    }

    /**
     * This test checks if the Closeable client is declared in a try-with-resources block.
     * If it is, the Closeable client should be closed automatically.
     * So, no warning should be issued.
     */
    @Test
    public void testDeclaredInTryWithResources() {

        String packageName = "com.azure.messaging.servicebus.ServiceBusReceiverClient";
        String closeableTypeName = "java.lang.AutoCloseable";
        boolean declaredInTryWithResources = true;
        boolean resourceClosedInFinally = false;
        boolean isResourceClosedElsewhere = false;
        int numOfInvocations = 0;
        verifyRegisterProblem(packageName, closeableTypeName, declaredInTryWithResources, resourceClosedInFinally, numOfInvocations, isResourceClosedElsewhere);
    }

    /**
     * This test checks if the Closeable client is closed in a finally block.
     * If it is, no warning should be issued.
     */
    @Test
    public void testResourceClosedInFinally() {

        String packageName = "com.azure.messaging.servicebus.ServiceBusReceiverClient";
        String closeableTypeName = "java.lang.AutoCloseable";
        boolean declaredInTryWithResources = false;
        boolean resourceClosedInFinally = true;
        boolean isResourceClosedElsewhere = true;
        int numOfInvocations = 0;
        verifyRegisterProblem(packageName, closeableTypeName, declaredInTryWithResources, resourceClosedInFinally, numOfInvocations, isResourceClosedElsewhere);
    }

    /**
     * This test checks if the Closeable client is closed elsewhere in the source code.
     * If it is, no warning should be issued.
     */
    @Test
    public void testResourceClosedElsewhere() {

        String packageName = "com.azure.messaging.eventhubs.EventHubConsumerAsyncClient";
        String closeableTypeName = "java.io.Closeable";
        boolean declaredInTryWithResources = false;
        boolean resourceClosedInFinally = false;
        boolean isResourceClosedElsewhere = true;
        int numOfInvocations = 0;
        verifyRegisterProblem(packageName, closeableTypeName, declaredInTryWithResources, resourceClosedInFinally, numOfInvocations, isResourceClosedElsewhere);
    }

    /**
     * This test checks if the Closeable client is not closed at all
     * If it is not, a warning should be issued.
     */
    @Test
    public void testResourceNotClosed() {

        String packageName = "com.azure.messaging.servicebus.ServiceBusReceiverClient";
        String closeableTypeName = "java.lang.AutoCloseable";
        boolean declaredInTryWithResources = false;
        boolean resourceClosedInFinally = false;
        boolean isResourceClosedElsewhere = false;
        int numOfInvocations = 1;
        verifyRegisterProblem(packageName, closeableTypeName, declaredInTryWithResources, resourceClosedInFinally, numOfInvocations, isResourceClosedElsewhere);
    }

    /**
     * This test checks if the Closeable client is not a Closeable type
     * If it is not, a warning should not be issued.
     */
    @Test
    public void testNotCloseable() {

        String packageName = "com.azure.messaging.servicebus.ServiceBusReceiverClient";
        String closeableTypeName = "not.AutoCloseable";
        boolean declaredInTryWithResources = false;
        boolean resourceClosedInFinally = false;
        boolean isResourceClosedElsewhere = false;
        int numOfInvocations = 0;
        verifyRegisterProblem(packageName, closeableTypeName, declaredInTryWithResources, resourceClosedInFinally, numOfInvocations, isResourceClosedElsewhere);
    }

    /**
     * This test checks if the Closeable client is not an Azure client
     * If it is not, a warning should not be issued as the rule is only for Azure clients.
     */
    @Test
    public void testNotAzure() {

        String packageName = "com.notazure.messaging.servicebus.ServiceBusReceiverClient";
        String closeableTypeName = "not.AutoCloseable";
        boolean declaredInTryWithResources = true;
        boolean resourceClosedInFinally = false;
        boolean isResourceClosedElsewhere = false;
        int numOfInvocations = 0;
        verifyRegisterProblem(packageName, closeableTypeName, declaredInTryWithResources, resourceClosedInFinally, numOfInvocations, isResourceClosedElsewhere);
    }

    /**
     * This method creates a CloseableClientVisitor object for testing the ClosingCloseableClientsCheck class.
     */
    private CloseableClientVisitor createVisitor() {
        return new CloseableClientVisitor(mockHolder);
    }

    /**
     * This method verifies if the registerProblem method is called the expected number of times.
     * A variety of parameters are passed to this method to test different scenarios.
     */
    private void verifyRegisterProblem(String packageName, String closeableTypeName, boolean declaredInTryWithResources, boolean resourceClosedInFinally, int numOfInvocations, boolean isResourceClosedElsewhere) {

        // visitVariable method
        PsiClassType type = mock(PsiClassType.class);
        PsiClass resolvedClass = mock(PsiClass.class);
        PsiClassType closeableSuperType = mock(PsiClassType.class);
        PsiClassType[] superTypesList = {closeableSuperType};

        // checkIfDeclaredInTryWith method
        PsiElement parent = mock(PsiElement.class);
        PsiTryStatement parentTryStatement = mock(PsiTryStatement.class);

        PsiResourceList resourceList = mock(PsiResourceList.class);
        PsiCodeBlock parentCodeBlock = mock(PsiCodeBlock.class);

        // isResourceClosedElsewhere method
        PsiTryStatement statement = mock(PsiTryStatement.class);
        PsiStatement[] statements = {statement};
        PsiCodeBlock finallyBlock = mock(PsiCodeBlock.class);
        PsiFile mockFile = mock(PsiFile.class);

        // findClosingMethodCall method
        PsiMethodCallExpression methodCall = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);

        // isClosingMethodCallExpression method
        PsiReferenceExpression qualifierExpression = mock(PsiReferenceExpression.class);

        PsiIdentifier variableIdentifier = mock(PsiIdentifier.class);

        // visitVariable method
        when(mockVariable.getType()).thenReturn(type);
        when(type.resolve()).thenReturn(resolvedClass);
        when(resolvedClass.getQualifiedName()).thenReturn(packageName);

        // Mocking the behavior for getParent() dynamically
        when(mockVariable.getParent()).thenAnswer(new Answer<PsiElement>() {
            private boolean firstCall = true;

            @Override
            public PsiElement answer(InvocationOnMock invocation) {
                if (firstCall) {
                    firstCall = false;
                    return resourceList;  // at checkIfDeclaredInTryWith method
                }
                return parent; // at checkIfDeclaredInTryWith method
            }
        });

        when(resolvedClass.getSuperTypes()).thenReturn(superTypesList);
        when(closeableSuperType.getCanonicalText()).thenReturn(closeableTypeName);

        // checkIfDeclaredInTryWith method
        if (declaredInTryWithResources) {
            when(resourceList.getParent()).thenReturn(parentTryStatement);
        }
        when(parent.getParent()).thenReturn(parentCodeBlock);

        // isResourceClosedElsewhere method
        when(parentCodeBlock.getStatements()).thenReturn(statements);

        if (resourceClosedInFinally) {
            when(statement.getFinallyBlock()).thenReturn(finallyBlock);
        } else {
            when(parentCodeBlock.getContainingFile()).thenReturn(mockFile);
        }

        // findClosingMethodCall method
        when(methodCall.getMethodExpression()).thenReturn(methodExpression);

        if (isResourceClosedElsewhere) {
            when(methodExpression.getReferenceName()).thenReturn("close");
        } else {
            when(methodExpression.getReferenceName()).thenReturn(null);
        }

        when(methodExpression.getQualifierExpression()).thenReturn(qualifierExpression);
        when(qualifierExpression.resolve()).thenReturn(mockVariable);

        // Mock source code for the JavaElementVisitor to identify 'close' method call
        PsiElement sourceCode = resourceClosedInFinally ? finallyBlock : mockFile;

        // Stub the accept method on the mock element to trigger the visitor
        doAnswer(invocation -> {
            JavaRecursiveElementVisitor visitor = invocation.getArgument(0);
            visitor.visitMethodCallExpression(methodCall);
            return null;
        }).when(sourceCode).accept(any(JavaRecursiveElementVisitor.class));

        when(mockVariable.getNameIdentifier()).thenReturn(variableIdentifier);
        mockVisitor.visitVariable(mockVariable);
        verify(mockHolder, times(numOfInvocations)).registerProblem(Mockito.eq(variableIdentifier), Mockito.contains("Closeable client that is not closed detected. Close the client after use to release resources."));
    }
}
