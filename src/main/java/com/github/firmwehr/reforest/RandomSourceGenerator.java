package com.github.firmwehr.reforest;

import spoon.Launcher;
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
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;


public class RandomSourceGenerator implements SourceGenerator {
    private final RandomGenerator random;
    private final RandomSourceGeneratorSettings settings;
    private final Launcher launcher;
    private final Factory factory;

    private final List<CtTypeReference<?>> validFieldTypes;
    private final List<CtTypeReference<?>> validMethodReturnTypes;

    public RandomSourceGenerator(RandomGenerator random, RandomSourceGeneratorSettings settings) {
        this.random = random;
        this.settings = settings;
        this.launcher = new Launcher();
        this.factory = this.launcher.getFactory();

        this.validFieldTypes = new ArrayList<>();
        this.validFieldTypes.addAll(List.of(
                this.factory.Type().INTEGER_PRIMITIVE,
                this.factory.Type().BOOLEAN_PRIMITIVE
        ));
        // contains all valid field types + void
        this.validMethodReturnTypes = new ArrayList<>(this.validFieldTypes);
        this.validMethodReturnTypes.add(this.factory.Type().VOID_PRIMITIVE);
    }

    @Override
    public <T> CtClass<T> generateClass() {
        CtClass<T> ctClass = this.factory.Class().create(randomUpperCamelCase());
        this.validFieldTypes.add(ctClass.getReference());
        int typeMembers = this.random.nextInt(this.settings.maxTypeMembers());
        for (int i = 0; i < typeMembers; i++) {
            CtTypeMember member = generateClassMember();
            if (member instanceof CtField && ctClass.getField(member.getSimpleName()) != null
                    || member instanceof CtMethod && !ctClass.getMethodsByName(member.getSimpleName()).isEmpty()) {
                i--;
                continue;
            }
            ctClass.addTypeMember(member);
        }
        return ctClass;
    }

    @Override
    public CtTypeMember generateClassMember() {
        if (this.settings.fieldToMethodRatio() < this.random.nextDouble()) {
            return generateField();
        }
        return generateMethod();
    }

    @Override
    public <T> CtField<T> generateField() {
        CtField<T> ctField = this.factory.Core().createField();
        ctField.setSimpleName(randomLowerCamelCase());
        ctField.setType(generateType(false));
        ctField.addModifier(ModifierKind.PUBLIC);
        return ctField;
    }

    @Override
    public <T> CtMethod<T> generateMethod() {
        CtMethod<T> ctMethod = this.factory.Core().createMethod();
        ctMethod.setSimpleName(randomLowerCamelCase());
        ctMethod.setType(generateType(true));
        // TODO parameters, throws, body
        ctMethod.addModifier(ModifierKind.PUBLIC);
        return ctMethod;
    }

    @Override
    public <T> CtParameter<T> generateParameter() {
        return null;
    }

    @Override
    public <T> CtTypeReference<T> generateType(boolean voidAllowed) {
        //noinspection unchecked
        return (CtTypeReference<T>) randomFromList(voidAllowed ? this.validMethodReturnTypes : this.validFieldTypes);
    }

    @Override
    public CtStatement generateStatement() {
        return null;
    }

    @Override
    public <T> CtBlock<T> generateBlock() {
        return null;
    }

    @Override
    public CtStatement generateBlockStatement() {
        return null;
    }

    @Override
    public CtStatement generateLocalVariableDeclarationStatement() {
        return null;
    }

    @Override
    public CtStatement generateEmptyStatement() {
        return null;
    }

    @Override
    public CtStatement generateWhileStatement() {
        return null;
    }

    @Override
    public CtStatement generateIfStatement() {
        return null;
    }

    @Override
    public CtStatement generateExpressionStatement() {
        return null;
    }

    @Override
    public CtStatement generateReturnStatement() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateExpression() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateAssignmentExpression() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateLogicalOrExpression() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateLogicalAndExpression() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateEqualityExpression() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateRelationalExpression() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateAdditiveExpression() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateMultiplicativeExpression() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateUnaryExpression() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generatePostfixExpression() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generatePostfixOp() {
        return null;
    }

    @Override
    public <T> CtInvocation<T> generateMethodInvocation() {
        return null;
    }

    @Override
    public <T> CtFieldAccess<T> generateFieldAccess() {
        return null;
    }

    @Override
    public <T> CtArrayAccess<T, ?> generateArrayAccess() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateArgument() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generatePrimaryExpression() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateNewObjectExpression() {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateNewArrayExpression() {
        return null;
    }

    private String randomUpperCamelCase() {
        int approxLength = this.random.nextInt(this.settings.approximateNameLength());
        String result = "";
        while (result.length() < approxLength) {
            //noinspection StringConcatenationInLoop
            result += uppercaseFirst(randomName());
        }
        return result;
    }

    private String randomLowerCamelCase() {
        int approxLength = this.random.nextInt(this.settings.approximateNameLength());
        String result = randomName();
        while (result.length() < approxLength) {
            //noinspection StringConcatenationInLoop
            result += uppercaseFirst(randomName());
        }
        return result;
    }

    private String uppercaseFirst(String s) {
        if (s.length() == 1) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String randomName() {
        return randomFromList(this.settings.identList());
    }

    private <T> T randomFromList(List<T> elements) {
        int index = this.random.nextInt(elements.size());
        return elements.get(index);
    }

}
