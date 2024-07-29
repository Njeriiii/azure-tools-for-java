package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiModifier;
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
     * This case is for a standalone concrete class that does not implement any interfaces or extend any classes (except java.lang.Object).
     * This is flagged because it is a standalone concrete class.
     */
    @Test
    public void testStandaloneConcreteClass() {

        Boolean isInterfaceBoolean = false;  // It's not an interface
        Boolean isAbstractBoolean = false;   // It's not abstract
        String classQualifiedName = "com.example.StandaloneClass";  // Arbitrary class name

        // No interfaces implemented
        PsiClass[] interfaces = new PsiClass[]{};
        String interfaceQualifiedName = null;

        // Extends Object (implicitly)
        PsiClass superClass = mock(PsiClass.class);
        String superClassQualifiedName = "java.lang.Object";  // Extends java.lang.Object

        int numOfInvocations = 1;

        verifyRegisterProblem(isInterfaceBoolean, isAbstractBoolean, classQualifiedName, interfaces, superClass, superClassQualifiedName, interfaceQualifiedName, numOfInvocations);
    }

    /**
     * Test case for an interface class that a concrete class implements.
     * This is not flagged because it is correctly used in the variable declaration.
     */
    @Test
    public void testNotConcreteClass() {

        Boolean isInterfaceBoolean = true;  // It's not an interface
        Boolean isAbstractBoolean = false;   // It's not abstract
        String classQualifiedName = "com.example.StandaloneClass";  // Arbitrary class name

        // No interfaces implemented
        PsiClass[] interfaces = new PsiClass[]{};
        String interfaceQualifiedName = null;

        // Extends Object (implicitly)
        PsiClass superClass = mock(PsiClass.class);
        String superClassQualifiedName = "java.lang.Object";  // Extends java.lang.Object

        int numOfInvocations = 0;

        verifyRegisterProblem(isInterfaceBoolean, isAbstractBoolean, classQualifiedName, interfaces, superClass, superClassQualifiedName, interfaceQualifiedName, numOfInvocations);
    }


    /**
     * Test case for a concrete class that implements a custom interface.
     * This is flagged because, even though the class is part of a hierarchy, the concrete type is used directly.
     */
    @Test
    public void testConcreteClassImplementingCustomInterfaceWithVariable() {

        Boolean isInterfaceBoolean = false;  // It's not an interface
        Boolean isAbstractBoolean = false;   // It's not abstract
        String classQualifiedName = "com.example.CustomClass";  // Arbitrary class name

        // Implements a custom interface
        PsiClass interfaceClass = mock(PsiClass.class);
        String interfaceQualifiedName = "com.example.CustomInterface";
        PsiClass[] interfaces = new PsiClass[]{interfaceClass};

        // Does not extend any class (implicitly extends Object)
        PsiClass superClass = mock(PsiClass.class);
        String superClassQualifiedName = "java.lang.Object";  // Extends java.lang.Object

        int numOfInvocations = 1;

        verifyRegisterProblem(isInterfaceBoolean, isAbstractBoolean, classQualifiedName, interfaces, superClass, superClassQualifiedName, interfaceQualifiedName, numOfInvocations);
    }

    /**
     * Test case for a concrete class that implements a Java utility interface.
     * This is not flagged because it is considered part of the standard Java hierarchy.
     */
    @Test
    public void testConcreteClassImplementingJavaUtilityInterface() {

        Boolean isInterfaceBoolean = false;  // It's not an interface
        Boolean isAbstractBoolean = false;   // It's not abstract
        String classQualifiedName = "com.example.SerializableClass";  // Arbitrary class name

        // Implements a Java utility interface
        PsiClass interfaceClass = mock(PsiClass.class);
        String interfaceQualifiedName = "java.io.Serializable";
        PsiClass[] interfaces = new PsiClass[]{interfaceClass};

        // Does not extend any class (implicitly extends Object)
        PsiClass superClass = mock(PsiClass.class);
        String superClassQualifiedName = "java.lang.Object";  // Extends java.lang.Object

        int numOfInvocations = 0;

        verifyRegisterProblem(isInterfaceBoolean, isAbstractBoolean, classQualifiedName, interfaces, superClass, superClassQualifiedName, interfaceQualifiedName, numOfInvocations);
    }

    /**
     * Test case for a concrete class that extends a Java utility class.
     * This is not flagged because it is considered part of the standard Java hierarchy.
     */
    @Test
    public void testConcreteClassExtendingJavaUtil() {

        Boolean isInterfaceBoolean = false;  // It's not an interface
        Boolean isAbstractBoolean = false;   // It's not abstract
        String classQualifiedName = "com.example.ArrayListSubclass";  // Arbitrary class name

        // No interfaces implemented
        PsiClass[] interfaces = new PsiClass[]{};
        String interfaceQualifiedName = null;

        // Extends a Java utility class
        PsiClass superClass = mock(PsiClass.class);
        String superClassQualifiedName = "java.util.ArrayList";  // Extends java.util.ArrayList

        int numOfInvocations = 0;

        verifyRegisterProblem(isInterfaceBoolean, isAbstractBoolean, classQualifiedName, interfaces, superClass, superClassQualifiedName, interfaceQualifiedName, numOfInvocations);
    }

    /**
     * Test case for a concrete class that implements a custom interface and extends a Java utility class.
     * This is flagged because it is a concrete class that is part of a hierarchy.
     */
    @Test
    public void testConcreteClassImplementingCustomInterfaceAndExtendingJavaUtilityClassFlagged() {

        Boolean isInterfaceBoolean = false;  // It's not an interface
        Boolean isAbstractBoolean = false;   // It's not abstract
        String classQualifiedName = "com.example.CustomExtendedClass";  // Arbitrary class name

        // Implements a custom interface
        PsiClass interfaceClass = mock(PsiClass.class);
        String interfaceQualifiedName = "com.example.CustomInterface";
        PsiClass[] interfaces = new PsiClass[]{interfaceClass};

        // Extends a Java utility class
        PsiClass superClass = mock(PsiClass.class);
        String superClassQualifiedName = "java.util.ArrayList";  // Extends java.util.ArrayList
        when(superClass.getQualifiedName()).thenReturn("java.util.ArrayList");

        int numOfInvocations = 1;

        verifyRegisterProblem(isInterfaceBoolean, isAbstractBoolean, classQualifiedName, interfaces, superClass, superClassQualifiedName, interfaceQualifiedName, numOfInvocations);
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
     * @param isInterfaceBoolean      True if the class is an interface, false otherwise
     * @param isAbstractBoolean       True if the class is abstract, false otherwise
     * @param classQualifiedName      Qualified name of the class
     * @param interfaces              Array of interfaces implemented by the class
     * @param superClass              Super class of the class - This is the class that the class extends
     * @param superClassQualifiedName Qualified name of the super class
     * @param interfaceQualifiedName  Qualified name of the interface - This is the interface that the class implements
     * @param numOfInvocations        Number of times registerProblem method is called
     */
    private void verifyRegisterProblem(Boolean isInterfaceBoolean, Boolean isAbstractBoolean, String classQualifiedName, PsiClass[] interfaces, PsiClass superClass, String superClassQualifiedName, String interfaceQualifiedName, int numOfInvocations) {

        PsiClassType type = mock(PsiClassType.class);
        PsiClass psiClass = mock(PsiClass.class);

        when(mockVariable.getType()).thenReturn(type);

        // isConcreteClass method
        when(type.resolve()).thenReturn(psiClass);
        when(psiClass.isInterface()).thenReturn(isInterfaceBoolean);
        when(psiClass.hasModifierProperty(PsiModifier.ABSTRACT)).thenReturn(isAbstractBoolean);

        // hasImplementedInterfaces
        when(psiClass.getQualifiedName()).thenReturn(classQualifiedName);

        when(psiClass.getInterfaces()).thenReturn(interfaces);
        when(psiClass.getSuperClass()).thenReturn(superClass);

        when(superClass.getQualifiedName()).thenReturn(superClassQualifiedName);

        if (interfaces.length > 0) {
            when(interfaces[0].getQualifiedName()).thenReturn(interfaceQualifiedName);
        }

        mockVisitor.visitVariable(mockVariable);

        verify(mockHolder, times(numOfInvocations)).registerProblem(eq(mockVariable), Mockito.contains("Detected usage of a concrete implementation type. Consider using an interface or an abstract class to promote flexibility and testability."));
    }
}
