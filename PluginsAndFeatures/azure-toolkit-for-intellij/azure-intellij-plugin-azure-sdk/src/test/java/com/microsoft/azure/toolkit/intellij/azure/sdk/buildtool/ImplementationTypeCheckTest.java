package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.ImplementationTypeCheck.ImplementationTypeVisitor;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class tests the ImplementationTypeVisitor class.
 */
public class ImplementationTypeCheckTest {

    // Declare as instance variables
    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private JavaElementVisitor mockVisitor;

    @Mock
    private PsiVariable mockVariable;

    @BeforeEach
    public void setup() {
        mockHolder = mock(ProblemsHolder.class);
        mockVisitor = createVisitor();
        mockVariable = mock(PsiVariable.class);
    }

    /**
     * Test cases for the ImplementationTypeVisitor class.
     * This case is for a class that is an implementation type.
     * The class is in the implementation package.
     * The registerProblem method should be called.
     */
    @Test
    public void testDirectUseOfImplementationType() {
        String classQualifiedName = "com.azure.data.appconfiguration.implementation.models";

        // No interfaces implemented
        PsiClass[] interfaces = new PsiClass[]{};
        String interfaceQualifiedName = null;

        // Extends Object (implicitly)
        PsiClass superClass = mock(PsiClass.class);
        String superClassQualifiedName = "java.lang.Object";  // Extends java.lang.Object

        int numOfInvocations = 1;
        verifyRegisterProblem(classQualifiedName, interfaces, superClass, superClassQualifiedName, interfaceQualifiedName, numOfInvocations);
    }

    /**
     * Test cases for the ImplementationTypeVisitor class.
     * This case is for a class that is not an implementation type.
     * The registerProblem method should not be called.
     */
    @Test
    public void testUseOfNonImplementationAzurePackage() {
        String classQualifiedName = "com.azure.data.appconfiguration.models";

        // No interfaces implemented
        PsiClass[] interfaces = new PsiClass[]{};
        String interfaceQualifiedName = null;

        // Extends Object (implicitly)
        PsiClass superClass = mock(PsiClass.class);
        String superClassQualifiedName = "java.lang.Object";  // Extends java.lang.Object

        int numOfInvocations = 0;
        verifyRegisterProblem(classQualifiedName, interfaces, superClass, superClassQualifiedName, interfaceQualifiedName, numOfInvocations);
    }

    /**
     * Test cases for the ImplementationTypeVisitor class.
     * This case is for a class that implements an implementation type interface.
     * The registerProblem method should be called.
     */
    @Test
    public void testUseOfImplementationTypeInterface() {
        String classQualifiedName = "com.azure.data.appconfiguration.models";

        // Implements an implementation type interface
        PsiClass interfaceClass = mock(PsiClass.class);
        String interfaceQualifiedName = "com.azure.data.appconfiguration.implementation.models";
        PsiClass[] interfaces = new PsiClass[]{interfaceClass};

        // Extends Object (implicitly)
        PsiClass superClass = mock(PsiClass.class);
        String superClassQualifiedName = "java.lang.Object";  // Extends java.lang.Object

        int numOfInvocations = 1;
        verifyRegisterProblem(classQualifiedName, interfaces, superClass, superClassQualifiedName, interfaceQualifiedName, numOfInvocations);
    }

    /**
     * Test cases for the ImplementationTypeVisitor class.
     * This case is for a class that extends an implementation type abstract class.
     * The registerProblem method should be called.
     */
    @Test
    public void testUseOfImplementationTypeAbstractClass() {
        String classQualifiedName = "com.azure.data.appconfiguration.models";

        // Does not implement any interface
        String interfaceQualifiedName = null;
        PsiClass[] interfaces = new PsiClass[]{};

        // Extends Object (implicitly)
        PsiClass superClass = mock(PsiClass.class);
        String superClassQualifiedName = "com.azure.data.appconfiguration.implementation.models";  // Extends java.lang.Object

        int numOfInvocations = 1;
        verifyRegisterProblem(classQualifiedName, interfaces, superClass, superClassQualifiedName, interfaceQualifiedName, numOfInvocations);
    }

    /**
     * Test cases for the ImplementationTypeVisitor class.
     * This case is for a non-Azure class.
     * The registerProblem method should not be called.
     */
    @Test
    public void testUseOfNonAzurePackage() {
        String classQualifiedName = "com.nonazure.data.appconfiguration.implementation.models";

        // No interfaces implemented
        PsiClass[] interfaces = new PsiClass[]{};
        String interfaceQualifiedName = null;

        // Extends Object (implicitly)
        PsiClass superClass = mock(PsiClass.class);
        String superClassQualifiedName = "java.lang.Object";  // Extends java.lang.Object

        int numOfInvocations = 0;
        verifyRegisterProblem(classQualifiedName, interfaces, superClass, superClassQualifiedName, interfaceQualifiedName, numOfInvocations);
    }

    /**
     * Helper method to create visitor.
     *
     * @return ImplementationTypeVisitor
     */
    JavaElementVisitor createVisitor() {
        return new ImplementationTypeVisitor(mockHolder);
    }

    /**
     * Helper method to verify registerProblem method.
     *
     * @param classQualifiedName      Qualified name of the class
     * @param interfaces              Array of interfaces implemented by the class
     * @param superClass              Super class of the class - This is the class that the class extends
     * @param superClassQualifiedName Qualified name of the super class
     * @param interfaceQualifiedName  Qualified name of the interface - This is the interface that the class implements
     * @param numOfInvocations        Number of times registerProblem method is called
     */
    private void verifyRegisterProblem(String classQualifiedName, PsiClass[] interfaces, PsiClass superClass, String superClassQualifiedName, String interfaceQualifiedName, int numOfInvocations) {

        PsiClassType type = mock(PsiClassType.class);
        PsiClass psiClass = mock(PsiClass.class);
        PsiIdentifier mockIdentifier = mock(PsiIdentifier.class);

        when(mockVariable.getType()).thenReturn(type);

        // isImplementationType method
        when(type.resolve()).thenReturn(psiClass);
        when(psiClass.getQualifiedName()).thenReturn(classQualifiedName);

        // ExtendsOrImplementsImplementationType method
        when(psiClass.getInterfaces()).thenReturn(interfaces);
        when(psiClass.getSuperClass()).thenReturn(superClass);

        when(superClass.getQualifiedName()).thenReturn(superClassQualifiedName);

        if (interfaces.length > 0) {
            when(interfaces[0].getQualifiedName()).thenReturn(interfaceQualifiedName);
        }

        when(mockVariable.getNameIdentifier()).thenReturn(mockIdentifier);
        mockVisitor.visitVariable(mockVariable);
        verify(mockHolder, times(numOfInvocations)).registerProblem(eq(mockIdentifier), Mockito.contains("Detected usage of an implementation type. Implementation types are not intended for public use. Use the publicly available Azure classes instead."));
    }
}
