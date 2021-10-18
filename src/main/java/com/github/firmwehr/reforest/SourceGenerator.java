package com.github.firmwehr.reforest;

import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.reference.CtTypeReference;

public interface SourceGenerator {

    <T> CtClass<T> generateClass();

    CtTypeMember generateClassMember();

    <T> CtField<T> generateField();

    <T> CtMethod<T> generateMethod();

    <T> CtParameter<T> generateParameter();

    <T> CtTypeReference<T> generateType(boolean voidAllowed);

    CtStatement generateStatement();

    <T> CtBlock<T> generateBlock();

    CtStatement generateBlockStatement();

    CtStatement generateLocalVariableDeclarationStatement();

    CtStatement generateEmptyStatement();

    CtStatement generateWhileStatement();

    CtStatement generateIfStatement();

    CtStatement generateExpressionStatement();

    CtStatement generateReturnStatement();

    <T> CtExpression<T> generateExpression();

    <T> CtExpression<T> generateAssignmentExpression();

    <T> CtExpression<T> generateLogicalOrExpression();

    <T> CtExpression<T> generateLogicalAndExpression();

    <T> CtExpression<T> generateEqualityExpression();

    <T> CtExpression<T> generateRelationalExpression();

    <T> CtExpression<T> generateAdditiveExpression();

    <T> CtExpression<T> generateMultiplicativeExpression();

    <T> CtExpression<T> generateUnaryExpression();

    <T> CtExpression<T> generatePostfixExpression();

    <T> CtExpression<T> generatePostfixOp();

    <T> CtInvocation<T> generateMethodInvocation();

    <T> CtFieldAccess<T> generateFieldAccess();

    <T> CtArrayAccess<T, ?> generateArrayAccess();

    <T> CtExpression<T> generateArgument();

    <T> CtExpression<T> generatePrimaryExpression();

    <T> CtExpression<T> generateNewObjectExpression();

    <T> CtExpression<T> generateNewArrayExpression();

}
