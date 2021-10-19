package com.github.firmwehr.reforest;

import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.reference.CtTypeReference;

import java.util.List;

public interface SourceGenerator {

    List<CtClass<?>> generateProgram();

    <T> CtClass<T> generateClass(String name);

    CtTypeMember generateClassMember();

    <T> CtField<T> generateField();

    <T> CtMethod<T> generateMethod();

    <T> CtParameter<T> generateParameter();

    <T> CtTypeReference<T> generateType(boolean voidAllowed);

    CtStatement generateStatement(AccessContext context);

    <T> CtBlock<T> generateBlock(AccessContext context, CtTypeReference<?> returnType, int level);

    CtStatement generateBlockStatement(AccessContext context);

    CtStatement generateLocalVariableDeclarationStatement(AccessContext context);

    CtStatement generateEmptyStatement(AccessContext context);

    CtStatement generateWhileStatement(AccessContext context);

    CtStatement generateIfStatement(AccessContext context);

    CtStatement generateExpressionStatement(AccessContext context);

    CtStatement generateReturnStatement(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generateExpression(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generateAssignmentExpression(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generateLogicalOrExpression(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generateLogicalAndExpression(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generateEqualityExpression(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generateRelationalExpression(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generateAdditiveExpression(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generateMultiplicativeExpression(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generateUnaryExpression(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generatePostfixExpression(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generatePostfixOp(AccessContext context, CtTypeReference<?> type);

    <T> CtInvocation<T> generateMethodInvocation(AccessContext context, CtTypeReference<?> type);

    <T> CtFieldAccess<T> generateFieldAccess(AccessContext context, CtTypeReference<?> type);

    <T> CtArrayAccess<T, ?> generateArrayAccess(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generateArgument(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generatePrimaryExpression(AccessContext context, CtTypeReference<?> type);

    <T> CtExpression<T> generateNewObjectExpression(CtTypeReference<?> type);

    <T> CtExpression<T> generateNewArrayExpression(AccessContext context, CtTypeReference<?> type);

    record AccessContext(
            List<CtLocalVariable<?>> localVariables,
            List<CtParameter<?>> parameters,
            CtClass<?> enclosingClass
    ) {
    }
}
