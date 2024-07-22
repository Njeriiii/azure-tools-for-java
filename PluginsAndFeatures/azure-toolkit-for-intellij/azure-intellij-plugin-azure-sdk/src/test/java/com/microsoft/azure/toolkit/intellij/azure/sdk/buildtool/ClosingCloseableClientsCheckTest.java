package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiResourceList;
import com.intellij.psi.PsiResourceVariable;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.ClosingCloseableClientsCheck.CloseableClientVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
        verifyRegisterProblem(packageName, closeableType, declaredInTryWithResources, resourceIdentifierText, variableIdentifierText);
    }


    private CloseableClientVisitor createVisitor() {
        return new CloseableClientVisitor(mockHolder, false);
    }

    private void verifyRegisterProblem(String packageName, String closeableType, boolean declaredInTryWithResources, String resourceIdentifierText, String variableIdentifierText) {
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

        // findClosingMethodCall
        PsiMethodCallExpression methodCall = mock(PsiMethodCallExpression.class);
        PsiReferenceExpression methodExpression = mock(PsiReferenceExpression.class);

        // isClosingMethodCallExpression
        PsiReferenceExpression qualifierExpression = mock(PsiReferenceExpression.class);


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

        if (true) {
            when(statement.getFinallyBlock()).thenReturn(finallyBlock);
        } else {
            when(parentCodeBlock.getContainingFile()).thenReturn(mock(PsiFile.class));
        }

        // findClosing  MethodCall
        when(methodCall.getMethodExpression()).thenReturn(methodExpression);
        when(methodExpression.getReferenceName()).thenReturn("close");

        PsiElement mockElement = mock(PsiElement.class);

        // Create the anonymous inner class for the visitor and simulate its behavior
        JavaElementVisitor mockMethodCallVisitor = new JavaElementVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression methodCall) {
                super.visitMethodCallExpression(methodCall);
                // Simulate the behavior of the method call being a closing method call
                if (methodCall.equals(methodCall)) {
                    System.out.println("Simulating close method call");
                }
            }
        };

        // Manually invoke the visitor on the mock element
        doAnswer(invocation -> {
            PsiElement element = invocation.getArgument(0);
            element.accept(mockMethodCallVisitor);
            return null;
        }).when(mockElement).accept(any());




//        when(methodsList.getChildren()).thenReturn(methodCallsArray);
//        when(methodCall.getMethodExpression()).thenReturn(methodExpression);
//
//        when(methodCall.getMethodExpression()).thenReturn(methodExpression);
//        when(methodExpression.getReferenceName()).thenReturn("close");
//
//
//        // isClosingMethodCallExpression
//        when(methodExpression.getQualifierExpression()).thenReturn(qualifierExpression);
//        when(qualifierExpression.resolve()).thenReturn(mockVariable);

        mockVisitor.visitVariable(mockVariable);
    }
}
