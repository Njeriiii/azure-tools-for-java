package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
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

    @Test
    public void testCloseableClientCheck() {
        // Test the visitDeclarationStatement method of the CloseableClientVisitor class.
        // This test checks if the Closeable client is properly closed.
        // If the Closeable client is not properly closed, a problem is registered with the ProblemsHolder.

        String packageName = "com.azure.messaging.servicebus.ServiceBusReceiverClient";
        String closeableType = "java.lang.AutoCloseable";
        boolean declaredInTryWithResources = false;
        String resourceIdentifierText = "receiver";
        String variableIdentifierText = "receiver";
        boolean resourceClosedInFinally = false;
        boolean isResourceClosed = false;
        int numOfInvocations = 1;
        verifyRegisterProblem(packageName, closeableType, declaredInTryWithResources, resourceClosedInFinally, numOfInvocations, isResourceClosed);
    }


    private CloseableClientVisitor createVisitor() {
        return new CloseableClientVisitor(mockHolder, false);
    }

    private void verifyRegisterProblem(String packageName, String closeableType, boolean declaredInTryWithResources, boolean resourceClosedInFinally, int numOfInvocations, boolean isResourceClosed) {
        // Verify that the registerProblem method is called with the correct parameters

        // visitVariable
        PsiClassType type = mock(PsiClassType.class);
        PsiClass resolvedClass = mock(PsiClass.class);
        PsiDeclarationStatement scope = mock(PsiDeclarationStatement.class);
        PsiCodeBlock context = mock(PsiCodeBlock.class);
        PsiClassType closeableSuperType = mock(PsiClassType.class);
        PsiClassType[] superTypesList = {closeableSuperType};

        // checkIfDeclaredInTryWith
        PsiElement parent = mock(PsiElement.class);
        PsiTryStatement parentTryStatement = mock(PsiTryStatement.class);

        PsiResourceList resourceList = mock(PsiResourceList.class);
        PsiCodeBlock parentCodeBlock = mock(PsiCodeBlock.class);

        // isResourceClosed
        PsiTryStatement statement = mock(PsiTryStatement.class);
        PsiStatement[] statements = {statement};
        PsiCodeBlock finallyBlock = mock(PsiCodeBlock.class);
        PsiFile mockFile = mock(PsiFile.class);

        // findClosingMethodCall
        PsiMethodCallExpression methodCall = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);

        // isClosingMethodCallExpression
        PsiReferenceExpression qualifierExpression = mock(PsiReferenceExpression.class);

        PsiIdentifier variableIdentifier = mock(PsiIdentifier.class);

        // visitVariable
        when(mockVariable.getType()).thenReturn(type);
        when(type.resolve()).thenReturn(resolvedClass);
        when(resolvedClass.getQualifiedName()).thenReturn(packageName);

        // Mocking the behavior for getParent() dynamically
        when(mockVariable.getParent()).thenAnswer(new Answer<PsiElement>() {
            private boolean firstCall = true;
            private boolean secondCall = true;

            @Override
            public PsiElement answer(InvocationOnMock invocation) {
                if (firstCall) {
                    firstCall = false;
                    return scope;
                } else if (secondCall) {
                    secondCall = false;
                    return resourceList;
                }
                return parent;
            }
        });

        when(scope.getParent()).thenReturn(context);
        when(resolvedClass.getSuperTypes()).thenReturn(superTypesList);
        when(closeableSuperType.equalsToText(closeableType)).thenReturn(true);

        // checkIfDeclaredInTryWith
        if (declaredInTryWithResources) {
            when(resourceList.getParent()).thenReturn(parentTryStatement);
        }
        when(parent.getParent()).thenReturn(parentCodeBlock);

        // isResourceClosed
        when(parentCodeBlock.getStatements()).thenReturn(statements);

        if (resourceClosedInFinally) {
            when(statement.getFinallyBlock()).thenReturn(finallyBlock);
        } else {
            when(parentCodeBlock.getContainingFile()).thenReturn(mockFile);
        }

        // findClosing  MethodCall
        when(methodCall.getMethodExpression()).thenReturn(methodExpression);

        if (isResourceClosed) {
            when(methodExpression.getReferenceName()).thenReturn("close");
        }
        else {
            when(methodExpression.getReferenceName()).thenReturn(null);
        }

        // isClosingMethodCallExpression
        when(methodExpression.getQualifierExpression()).thenReturn(qualifierExpression);
        when(qualifierExpression.resolve()).thenReturn(mockVariable);

        PsiElement sourceCode = resourceClosedInFinally ? finallyBlock : mockFile;

        // Stub the accept method on the mock element to trigger the visitor
        doAnswer(invocation -> {
            JavaElementVisitor visitor = invocation.getArgument(0);
            visitor.visitMethodCallExpression(methodCall);
            return null;
        }).when(sourceCode).accept(any(JavaElementVisitor.class));


        when(mockVariable.getNameIdentifier()).thenReturn(variableIdentifier);

        mockVisitor.visitVariable(mockVariable);

        verify(mockHolder, times(numOfInvocations)).registerProblem(Mockito.eq(variableIdentifier), Mockito.contains("Closeable client is not properly closed"));
    }
}
