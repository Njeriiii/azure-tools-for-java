package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiDoWhileStatement;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiForeachStatement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiWhileStatement;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.SingleOperationInLoopCheck.SingleOperationInLoopVisitor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class is used to test the SingleOperationInLoopCheck class.
 * The SingleOperationInLoopCheck is an inspection to check if there is a single Azure client operation inside a loop.
 * A single Azure client operation is defined as a method call on a class that is part of the Azure SDK.
 * If a single Azure client operation is found inside a loop, a problem will be registered.
 *
 * THis is an example of a situation where the inspection should register a problem:
 *
 * 1. With a single PsiDeclarationStatement inside a while loop
 * // While loop
 *         int i = 0;
 *         while (i < 10) {
 *
 *             BlobAsyncClient blobAsyncClient = new BlobClientBuilder()
 *                 .endpoint("https://<your-storage-account-name>.blob.core.windows.net")
 *                 .sasToken("<your-sas-token>")
 *                 .containerName("<your-container-name>")
 *                 .blobName("<your-blob-name>")
 *                 .buildAsyncClient();
 *
 *             i++;
 *         }
 *
 * 2. With a single PsiExpressionStatement inside a for loop
 * for (String documentPath : documentPaths) {
 *
 *             blobAsyncClient.uploadFromFile(documentPath)
 *                 .doOnSuccess(response -> System.out.println("Blob uploaded successfully in enhanced for loop."))
 *                 .subscribe();
 *         }
 */
public class SingleOperationInLoopCheckTest {

    // Declare as instance variables
    @Mock
    private ProblemsHolder mockHolder;
    private JavaElementVisitor mockVisitor;

    @BeforeEach
    public void setup() {
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
    }

    /**
     * This test is used to verify a problem is registered when a
     * single PsiExpressionStatement operation is found in a for loop.
     */
    @Test
    public void testSingleOperationInLoopCheckPsiExpressionStatementInForStatement() {
        assertVisitor();

        PsiForStatement statement = mock(PsiForStatement.class);
        String packageName = "com.azure.storage.blob";
        int numberOfInvocations = 1;
        int numOfOperations = 1;
        verifyRegisterProblemWithSinglePsiExpressionStatement(statement,
                packageName, numberOfInvocations, numOfOperations);
    }

    /**
     * This test is used to verify a problem is registered when a
     * single PsiExpressionStatement operation is found in a for each loop.
     */
    @Test
    public void testSingleOperationInLoopCheckPsiExpressionStatementInForEachStatement() {
        assertVisitor();

        PsiForeachStatement statement = mock(PsiForeachStatement.class);
        String packageName = "com.azure.storage.blob";
        int numberOfInvocations = 1;
        int numOfOperations = 1;
        verifyRegisterProblemWithSinglePsiExpressionStatement(statement, packageName, numberOfInvocations, numOfOperations);
    }

    /**
     * This test is used to verify a problem is registered when a
     * single PsiExpressionStatement operation is found in a while loop.
     */
    @Test
    public void testSingleOperationInLoopCheckPsiExpressionStatementInWhileStatement() {
        assertVisitor();

        PsiWhileStatement statement = mock(PsiWhileStatement.class);
        String packageName = "com.azure.storage.blob";
        int numberOfInvocations = 1;
        int numOfOperations = 1;
        verifyRegisterProblemWithSinglePsiExpressionStatement(statement, packageName, numberOfInvocations, numOfOperations);
    }

    /**
     * This test is used to verify a problem is registered when a
     * single PsiExpressionStatement operation is found in a do while loop.
     */
    @Test
    public void testSingleOperationInLoopCheckPsiExpressionStatementInDoWhileStatement() {
        assertVisitor();

        PsiDoWhileStatement statement = mock(PsiDoWhileStatement.class);
        String packageName = "com.azure.storage.blob";
        int numberOfInvocations = 1;
        int numOfOperations = 1;
        verifyRegisterProblemWithSinglePsiExpressionStatement(statement, packageName, numberOfInvocations, numOfOperations);
    }

    /**
     * This test is used to verify a problem is registered when a single
     * PsiExpressionStatement operation is found in a for each loop.
     */
    @Test
    public void testSingleOperationInLoopCheckPsiDeclarationStatementInForStatement() {
        assertVisitor();

        PsiForStatement statement = mock(PsiForStatement.class);
        String packageName = "com.azure.storage.blob";
        int numberOfInvocations = 1;
        int numOfOperations = 1;
        verifyRegisterProblemWithSinglePsiDeclarationStatement(statement, packageName, numberOfInvocations, numOfOperations);
    }

    /**
     * This test is used to verify a problem is registered when a single
     * PsiDeclarationStatement operation is found in a do while loop.
     */
    @Test
    public void testSingleOperationInLoopCheckPsiDeclarationStatementInForEachStatement() {
        assertVisitor();

        PsiForeachStatement statement = mock(PsiForeachStatement.class);
        String packageName = "com.azure.storage.blob";
        int numberOfInvocations = 1;
        int numOfOperations = 1;
        verifyRegisterProblemWithSinglePsiDeclarationStatement(statement, packageName, numberOfInvocations, numOfOperations);
    }

    /**
     * This test is used to verify a problem is registered when a single
     * PsiDeclarationStatement operation is found in a while loop.
     */
    @Test
    public void testSingleOperationInLoopCheckPsiDeclarationStatementInWhileStatement() {
        assertVisitor();

        PsiWhileStatement statement = mock(PsiWhileStatement.class);
        String packageName = "com.azure.storage.blob";
        int numberOfInvocations = 1;
        int numOfOperations = 1;
        verifyRegisterProblemWithSinglePsiDeclarationStatement(statement, packageName, numberOfInvocations, numOfOperations);
    }

    /**
     * This test is used to verify a problem is registered when a single
     * PsiDeclarationStatement operation is found in a do while loop.
     */
    @Test
    public void testSingleOperationInLoopCheckPsiDeclarationStatementInDoWhileStatement() {
        assertVisitor();

        PsiDoWhileStatement statement = mock(PsiDoWhileStatement.class);
        String packageName = "com.azure.storage.blob";
        int numberOfInvocations = 1;
        int numOfOperations = 1;
        verifyRegisterProblemWithSinglePsiDeclarationStatement(statement, packageName, numberOfInvocations, numOfOperations);
    }

    /**
     * This test is used to verify a problem is NOT registered when multiple
     * PsiExpressionStatement operations are found in a for loop.
     */
    @Test
    public void testMultipleOperationsInLoopCheckPsiExpressionStatementInForEachStatement() {
        assertVisitor();

        PsiForeachStatement statement = mock(PsiForeachStatement.class);
        String packageName = "com.azure.storage.blob";
        int numberOfInvocations = 0;
        int numOfOperations = 4;
        verifyRegisterProblemWithSinglePsiExpressionStatement(statement, packageName, numberOfInvocations, numOfOperations);
    }

    /**
     * This test is used to verify a problem is NOT registered when multiple
     * PsiExpressionStatement operations are found in a while loop.
     */
    @Test
    public void testMultipleOperationsInLoopCheckPsiDeclarationStatementInDoWhileStatement() {
        assertVisitor();

        PsiDoWhileStatement statement = mock(PsiDoWhileStatement.class);
        String packageName = "com.azure.storage.blob";
        int numberOfInvocations = 0;
        int numOfOperations = 2;
        verifyRegisterProblemWithSinglePsiDeclarationStatement(statement, packageName, numberOfInvocations, numOfOperations);
    }

    /**
     * This test is used to verify a problem is NOT registered when a different package name
     * is used in the PsiExpressionStatement operation in a for loop.
     */
    @Test
    public void testSingleOperationInLoopCheckWithDifferentPackageName() {
        assertVisitor();

        PsiForStatement statement = mock(PsiForStatement.class);
        String packageName = "com.microsoft.azure.storage.blob";
        int numberOfInvocations = 0;
        int numOfOperations = 1;
        verifyRegisterProblemWithSinglePsiDeclarationStatement(statement, packageName, numberOfInvocations, numOfOperations);
    }

    /**
     * This helper method is used to create a new SingleOperationInLoopVisitor object.
     */
    private JavaElementVisitor createVisitor() {
        boolean isOnTheFly = true;
        SingleOperationInLoopVisitor visitor = new SingleOperationInLoopVisitor(mockHolder, isOnTheFly);
        return visitor;
    }

    /**
     * Assert that the visitor is not null and is an instance of JavaElementVisitor
     * to ensure the visitor is created correctly.
     */
    private void assertVisitor() {
        assertNotNull(mockVisitor);
        assertTrue(mockVisitor instanceof JavaElementVisitor);
    }

    /**
     * This helper method is used to verify a problem is registered when a
     * PsiExpressionStatement operation is found in a loop.
     */
    private void verifyRegisterProblemWithSinglePsiExpressionStatement(PsiStatement loopStatement, String packageName, int numberOfInvocations, int numOfOperations) {

        // Arrange
        PsiBlockStatement loopBody = mock(PsiBlockStatement.class);
        PsiCodeBlock codeBlock = mock(PsiCodeBlock.class);
        PsiMethodCallExpression expression = mock(PsiMethodCallExpression.class);
        PsiTreeUtil treeUtil = mock(PsiTreeUtil.class);
        PsiClass containingClass = mock(PsiClass.class);

        // Create a list to dynamically add PsiStatement objects
        List<PsiStatement> statementList = new ArrayList<>();

        // Mock PsiStatement and add to the list x times
        for (int i = 0; i < numOfOperations; i++) {
            PsiExpressionStatement mockStatement = mock(PsiExpressionStatement.class);
            statementList.add(mockStatement);
            when(mockStatement.getExpression()).thenReturn(expression);
        }

        // Convert the list to an array of PsiStatement objects
        PsiStatement[] statements = statementList.toArray(new PsiStatement[0]);

        when(loopBody.getCodeBlock()).thenReturn(codeBlock);
        when(codeBlock.getStatements()).thenReturn(statements);
        when(treeUtil.getParentOfType(expression, PsiClass.class)).thenReturn(containingClass);
        when(containingClass.getQualifiedName()).thenReturn(packageName);

        // Visitor invocation based on the type of loopStatement
        if (loopStatement instanceof PsiForStatement) {

            // .getBody() is specific to objects of PsiForStatement, PsiForeachStatement, PsiWhileStatement, and PsiDoWhileStatement
            when(((PsiForStatement) loopStatement).getBody()).thenReturn(loopBody);
            mockVisitor.visitForStatement((PsiForStatement) loopStatement);
        } else if (loopStatement instanceof PsiForeachStatement) {
            when(((PsiForeachStatement) loopStatement).getBody()).thenReturn(loopBody);
            mockVisitor.visitForeachStatement((PsiForeachStatement) loopStatement);
        } else if (loopStatement instanceof PsiWhileStatement) {
            when(((PsiWhileStatement) loopStatement).getBody()).thenReturn(loopBody);
            mockVisitor.visitWhileStatement((PsiWhileStatement) loopStatement);
        } else if (loopStatement instanceof PsiDoWhileStatement) {
            when(((PsiDoWhileStatement) loopStatement).getBody()).thenReturn(loopBody);
            mockVisitor.visitDoWhileStatement((PsiDoWhileStatement) loopStatement);
        }

        //  Verify problem is registered
        verify(mockHolder,
                times(numberOfInvocations)).registerProblem(Mockito.eq(loopStatement),
                Mockito.contains(
                        "Single operation found in loop. If the SDK provides a batch operation API, use it to perform multiple actions in a single request."));
    }

    /**
     * This helper method is used to verify a problem is registered when a
     * PsiDeclarationStatement operation is found in a loop.
     */
    private void verifyRegisterProblemWithSinglePsiDeclarationStatement(PsiStatement loopStatement, String packageName, int numberOfInvocations, int numOfOperations) {

        // Arrange
        PsiBlockStatement loopBody = mock(PsiBlockStatement.class);
        PsiCodeBlock codeBlock = mock(PsiCodeBlock.class);
        PsiVariable element = mock(PsiVariable.class);
        PsiElement[] elements = new PsiElement[] {element};
        PsiMethodCallExpression initializer = mock(PsiMethodCallExpression.class);
        PsiTreeUtil treeUtil = mock(PsiTreeUtil.class);
        PsiClass containingClass = mock(PsiClass.class);

        // Create a list to dynamically add PsiStatement objects
        List<PsiStatement> statementList = new ArrayList<>();

        // Mock PsiStatement and add to the list x times
        for (int i = 0; i < numOfOperations; i++) {
            PsiDeclarationStatement mockStatement = mock(PsiDeclarationStatement.class);
            statementList.add(mockStatement);
            when(mockStatement.getDeclaredElements()).thenReturn(elements);
        }

        // Convert the list to an array of PsiStatement objects
        PsiStatement[] statements = statementList.toArray(new PsiStatement[0]);

        when(loopBody.getCodeBlock()).thenReturn(codeBlock);
        when(codeBlock.getStatements()).thenReturn(statements);
        when(element.getInitializer()).thenReturn(initializer);
        when(treeUtil.getParentOfType(initializer, PsiClass.class)).thenReturn(containingClass);
        when(containingClass.getQualifiedName()).thenReturn(packageName);

        // Visitor invocation based on the type of loopStatement
        if (loopStatement instanceof PsiForStatement) {
            when(((PsiForStatement) loopStatement).getBody()).thenReturn(loopBody);
            mockVisitor.visitForStatement((PsiForStatement) loopStatement);
        } else if (loopStatement instanceof PsiForeachStatement) {
            when(((PsiForeachStatement) loopStatement).getBody()).thenReturn(loopBody);
            mockVisitor.visitForeachStatement((PsiForeachStatement) loopStatement);
        } else if (loopStatement instanceof PsiWhileStatement) {
            when(((PsiWhileStatement) loopStatement).getBody()).thenReturn(loopBody);
            mockVisitor.visitWhileStatement((PsiWhileStatement) loopStatement);
        } else if (loopStatement instanceof PsiDoWhileStatement) {
            when(((PsiDoWhileStatement) loopStatement).getBody()).thenReturn(loopBody);
            mockVisitor.visitDoWhileStatement((PsiDoWhileStatement) loopStatement);
        }

        //  Verify problem is registered
        verify(mockHolder,
                times(numberOfInvocations)).registerProblem(Mockito.eq(loopStatement),
                Mockito.contains(
                        "Single operation found in loop. If the SDK provides a batch operation API, use it to perform multiple actions in a single request."));
    }
}