package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
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
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.mockito.Mockito.mock;
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
        boolean declaredInTryWithResources = true;
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

        PsiResourceVariable resource = mock(PsiResourceVariable.class);
        PsiResourceList resourceList = mock(PsiResourceList.class);
        PsiResourceVariable[] resourceVariables = {resource};

        PsiIdentifier resourceIdentifier = mock(PsiIdentifier.class);
        PsiIdentifier variableIdentifier = mock(PsiIdentifier.class);
        PsiCodeBlock parentCodeBlock = mock(PsiCodeBlock.class);

        // isResourceClosed
        PsiTryStatement statement = mock(PsiTryStatement.class);
        PsiStatement[] statements = {statement};
        PsiCodeBlock finallyBlock = mock(PsiCodeBlock.class);

        // findClosingMethodCall
        PsiTreeUtil mockTreeUtil = mock(PsiTreeUtil.class);


        // visitVariable
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
                    return scope;
                } else {
                    return parent;
                }
            }
        });

        when(scope.getParent()).thenReturn(context);
        when(resolvedClass.getSuperTypes()).thenReturn(superTypesList);
        when(closeableSuperType.equalsToText(closeableType)).thenReturn(true);

        // checkIfDeclaredInTryWith
        when(parent.getParent()).thenReturn(parentTryStatement);
        when(parentTryStatement.getResourceList()).thenReturn(resourceList);
        when(resourceList.getChildren()).thenReturn(resourceVariables);

        when(resource.getNameIdentifier()).thenReturn(resourceIdentifier);
        when(mockVariable.getNameIdentifier()).thenReturn(variableIdentifier);

        when(resourceIdentifier.getText()).thenReturn(resourceIdentifierText);
        when(variableIdentifier.getText()).thenReturn(variableIdentifierText); // Change this to make them not equal


        // isResourceClosed
        when(parentCodeBlock.getStatements()).thenReturn(statements);
        when(statement.getFinallyBlock()).thenReturn(finallyBlock);

        // findClosingMethodCall
//        when(mockTreeUtil.findchildrenOfType(finallyBlock, PsiMethodCallExpression.class)).thenReturn(null);



        mockVisitor.visitVariable(mockVariable);


    }
}
