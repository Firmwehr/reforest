package com.github.firmwehr.reforest;

import spoon.Launcher;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtWhile;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class RandomSourceGenerator implements SourceGenerator {
    private final RandomGenerator random;
    private final RandomSourceGeneratorSettings settings;
    private final NavigableMap<Double, StatementType> statementTypes;
    private final double statementBound;
    private final Launcher launcher;
    private final Factory factory;

    private final CtTypeReference<?> intType;
    private final CtTypeReference<?> booleanType;

    private final List<CtTypeReference<?>> validFieldTypes;
    private final List<CtTypeReference<?>> validMethodReturnTypes;

    public RandomSourceGenerator(RandomGenerator random, RandomSourceGeneratorSettings settings) {
        this.random = random;
        this.settings = settings;
        this.statementTypes = new TreeMap<>();
        double sum = 0;
        for (RandomSourceGeneratorSettings.WeightedStatementType type : this.settings.statementWeights()) {
            statementTypes.put(sum, type.type());
            sum += type.weight();
        }
        this.statementBound = sum;
        this.launcher = new Launcher();
        this.factory = this.launcher.getFactory();

        this.intType = this.factory.Type().INTEGER_PRIMITIVE;
        this.booleanType = this.factory.Type().BOOLEAN_PRIMITIVE;

        this.validFieldTypes = new ArrayList<>();
        this.validFieldTypes.addAll(List.of(
                this.intType,
                this.booleanType
        ));
        // contains all valid field types + void
        this.validMethodReturnTypes = new ArrayList<>(this.validFieldTypes);
        this.validMethodReturnTypes.add(this.factory.Type().VOID_PRIMITIVE);
    }

    @Override
    public List<CtClass<?>> generateProgram() {
        List<String> names = IntStream.range(0, this.random.nextInt(this.settings.maxTypes()))
                .mapToObj(i -> randomUpperCamelCase())
                .toList();
        List<CtTypeReference<Object>> references = names.stream()
                .map(n -> this.factory.Type().createReference(n))
                .toList();
        this.validFieldTypes.addAll(references);
        this.validMethodReturnTypes.addAll(references);
        List<CtClass<?>> classes = names.stream().map(this::generateClass).collect(Collectors.toList());
        for (CtClass<?> aClass : classes) {
            for (CtMethod<?> method : aClass.getMethods()) {
                method.setBody(generateBlock(
                        new AccessContext(new ArrayList<>(), method.getParameters(), aClass),
                        method.getType(),
                        0
                ));
            }
        }
        return classes;
    }

    @Override
    public <T> CtClass<T> generateClass(String name) {
        CtClass<T> ctClass = this.factory.Class().create(name);
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
        if (this.settings.fieldToMethodRatio() > this.random.nextDouble()) {
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
        // TODO throws
        List<CtParameter<?>> parameters = new ArrayList<>();
        int parameterCount = this.random.nextInt(this.settings.maxParameters());
        for (int i = 0; i < parameterCount; i++) {
            parameters.add(generateParameter());
        }
        ctMethod.setParameters(parameters);
        ctMethod.addModifier(ModifierKind.PUBLIC);
        // body is added later, when method/field references of other classes are available
        return ctMethod;
    }

    @Override
    public <T> CtParameter<T> generateParameter() {
        CtParameter<T> ctParameter = this.factory.Core().createParameter();
        ctParameter.setType(generateType(false));
        ctParameter.setSimpleName(randomLowerCamelCase());
        return ctParameter;
    }

    @Override
    public <T> CtTypeReference<T> generateType(boolean voidAllowed) {
        @SuppressWarnings("unchecked")
        CtTypeReference<T> typeReference = (CtTypeReference<T>) randomFromList(
                voidAllowed
                        ? this.validMethodReturnTypes
                        : this.validFieldTypes
        );
        if (!typeReference.equals(this.factory.Type().VOID_PRIMITIVE)
                && this.random.nextDouble() < this.settings.arrayTypePercentage()) {
            typeReference = toArrayTypeRef(typeReference);
        }
        return typeReference;
    }

    @Override
    public CtStatement generateStatement(AccessContext context) {
        return switch (randomStatementType()) {
            case LOCAL_VARIABLE_DECLARATION -> generateStatement(context); // not allowed here, try again
            case EMPTY -> generateEmptyStatement(context);
            case WHILE -> generateWhileStatement(context);
            case IF -> generateIfStatement(context);
            case EXPRESSION -> generateExpressionStatement(context);
            case BLOCK -> generateBlockStatement(context);
        };
    }

    @Override
    public <T> CtBlock<T> generateBlock(AccessContext context, CtTypeReference<?> returnType, int level) {
        AccessContext newContext = new AccessContext(
                new ArrayList<>(context.localVariables()),
                context.parameters(),
                context.enclosingClass()
        );
        CtBlock<T> ctBlock = this.factory.createBlock();
        int statementCount = this.random.nextInt(this.settings.maxStatementsPerBlock());
        for (int i = 0; i < statementCount; i++) {
            ctBlock.addStatement(generateStatement(newContext));
        }
        if (level == 0 && !returnType.equals(this.factory.Type().VOID_PRIMITIVE)) {
            ctBlock.addStatement(generateReturnStatement(newContext, returnType));
        }
        return ctBlock;
    }

    @Override
    public CtStatement generateBlockStatement(AccessContext context) {
        return switch (randomStatementType()) {
            case LOCAL_VARIABLE_DECLARATION -> generateLocalVariableDeclarationStatement(context);
            case EMPTY -> generateEmptyStatement(context);
            case WHILE -> generateWhileStatement(context);
            case IF -> generateIfStatement(context);
            case EXPRESSION -> generateExpressionStatement(context);
            case BLOCK -> generateBlockStatement(context);
        };
    }

    @Override
    public CtStatement generateLocalVariableDeclarationStatement(AccessContext context) {
        // TODO random initialisation
        CtLocalVariable<Object> localVariable = this.factory.Code().createLocalVariable(
                generateType(false),
                randomLowerCamelCase(),
                null
        );
        context.localVariables().add(localVariable);
        return localVariable;
    }

    @Override
    public CtStatement generateEmptyStatement(AccessContext context) {
        return this.factory.Core().createCodeSnippetStatement();
    }

    @Override
    public CtStatement generateWhileStatement(AccessContext context) {
        CtWhile ctWhile = this.factory.createWhile();
        ctWhile.setLoopingExpression(generateExpression(context, this.booleanType));
        ctWhile.setBody(generateStatement(context));
        return ctWhile;
    }

    @Override
    public CtStatement generateIfStatement(AccessContext context) {
        CtIf ctIf = this.factory.createIf();
        ctIf.setCondition(generateExpression(context, this.booleanType));
        ctIf.setThenStatement(generateStatement(context));
        ctIf.setElseStatement(generateStatement(context));
        return ctIf;
    }

    @Override
    public CtStatement generateExpressionStatement(AccessContext context) {
        // TODO generate method calls and assignment expressions
        return null;
    }

    @Override
    public CtStatement generateReturnStatement(AccessContext context, CtTypeReference<?> type) {
        CtReturn<Object> ctReturn = this.factory.Core().createReturn();
        // TODO more complex returns to make running code
        if (type.equals(this.intType)) {
            ctReturn.setReturnedExpression(this.factory.Code().createLiteral(0));
        } else if (type.equals(this.booleanType)) {
            ctReturn.setReturnedExpression(this.factory.Code().createLiteral(false));
        } else {
            ctReturn.setReturnedExpression(this.factory.Code().createLiteral(null));
        }
        return ctReturn;
    }

    @Override
    public <T> CtExpression<T> generateExpression(AccessContext context, CtTypeReference<?> type) {
        return generateAssignmentExpression(context, type);
    }

    @Override
    public <T> CtExpression<T> generateAssignmentExpression(AccessContext context, CtTypeReference<?> type) {
        return generateLogicalOrExpression(context, type);
    }

    @Override
    public <T> CtExpression<T> generateLogicalOrExpression(AccessContext context, CtTypeReference<?> type) {
        if (!this.booleanType.equals(type) || this.random.nextDouble() > 0.1) {
            return generateLogicalAndExpression(context, type);
        }
        return this.factory.createBinaryOperator(
                generateLogicalOrExpression(context, type),
                generateLogicalAndExpression(context, type),
                BinaryOperatorKind.OR
        );
    }

    @Override
    public <T> CtExpression<T> generateLogicalAndExpression(AccessContext context, CtTypeReference<?> type) {
        if (!this.booleanType.equals(type) || this.random.nextDouble() > 0.1) {
            return generateEqualityExpression(context, type);
        }
        return this.factory.createBinaryOperator(
                generateLogicalAndExpression(context, type),
                generateEqualityExpression(context, type),
                BinaryOperatorKind.AND
        );
    }

    @Override
    public <T> CtExpression<T> generateEqualityExpression(AccessContext context, CtTypeReference<?> type) {
        if (!this.booleanType.equals(type) || this.random.nextDouble() > 0.1) {
            return generateRelationalExpression(context, type);
        }
        return this.factory.createBinaryOperator(
                generateEqualityExpression(context, null),
                generateRelationalExpression(context, null),
                this.random.nextBoolean() ? BinaryOperatorKind.EQ : BinaryOperatorKind.NE
        );
    }

    @Override
    public <T> CtExpression<T> generateRelationalExpression(AccessContext context, CtTypeReference<?> type) {
        if (!this.booleanType.equals(type) || this.random.nextDouble() > 0.15) {
            return generateAdditiveExpression(context, type);
        }
        BinaryOperatorKind[] relationalKinds = new BinaryOperatorKind[]{
                BinaryOperatorKind.LT,
                BinaryOperatorKind.LE,
                BinaryOperatorKind.GT,
                BinaryOperatorKind.GE,
        };
        return this.factory.createBinaryOperator(
                generateRelationalExpression(context, this.intType),
                generateAdditiveExpression(context, this.intType),
                relationalKinds[this.random.nextInt(relationalKinds.length)]
        );
    }

    @Override
    public <T> CtExpression<T> generateAdditiveExpression(AccessContext context, CtTypeReference<?> type) {
        if (!this.intType.equals(type) || this.random.nextDouble() > 0.2) {
            return generateMultiplicativeExpression(context, type);
        }
        return this.factory.createBinaryOperator(
                generateAdditiveExpression(context, this.intType),
                generateMultiplicativeExpression(context, this.intType),
                this.random.nextBoolean() ? BinaryOperatorKind.PLUS : BinaryOperatorKind.MINUS
        );
    }

    @Override
    public <T> CtExpression<T> generateMultiplicativeExpression(AccessContext context, CtTypeReference<?> type) {
        if (!this.intType.equals(type) || this.random.nextDouble() > 0.2) {
            return generateUnaryExpression(context, type);
        }
        return this.factory.createBinaryOperator(
                generateMultiplicativeExpression(context, this.intType),
                generateUnaryExpression(context, this.intType),
                switch (this.random.nextInt(3)) {
                    case 0 -> BinaryOperatorKind.MUL;
                    case 1 -> BinaryOperatorKind.DIV;
                    case 2 -> BinaryOperatorKind.MOD;
                    default -> throw new IllegalArgumentException("???");
                }
        );
    }

    @Override
    public <T> CtExpression<T> generateUnaryExpression(AccessContext context, CtTypeReference<?> type) {
        if (!this.intType.equals(type) || !this.booleanType.equals(type) ||  this.random.nextDouble() > 0.2) {
            return generatePostfixExpression(context, type);
        }
        return this.factory.createUnaryOperator()
                .<CtUnaryOperator<T>>setKind(type.equals(this.intType) ? UnaryOperatorKind.NEG : UnaryOperatorKind.NOT)
                .<CtUnaryOperator<T>>setOperand(generateUnaryExpression(context, type));
    }

    @Override
    public <T> CtExpression<T> generatePostfixExpression(AccessContext context, CtTypeReference<?> type) {
        // TODO ???
        CtExpression<T> expression = generatePrimaryExpression(context, type);
        while (expression == null) {
            expression = generatePrimaryExpression(context, type); // TODO this should not be needed
        }
        if (type != null && (type.equals(expression.getType()) || type.equals(this.factory.Type().NULL_TYPE))) {
            // TODO wrap again? might be fun...
            return expression; // no PostfixOp on this needed
        }
        if (this.intType.equals(type)) {
            return this.factory.Code().createLiteral((T) (Object) this.random.nextInt());
        } else if (this.booleanType.equals(type)) {
            return this.factory.Code().createLiteral((T) (Object) this.random.nextBoolean());
        } else {
            return this.factory.Code().createLiteral(null);
        }
    }

    @Override
    public <T> CtExpression<T> generatePostfixOp(AccessContext context, CtTypeReference<?> type) {
        return null;
    }

    @Override
    public <T> CtInvocation<T> generateMethodInvocation(AccessContext context, CtTypeReference<?> type) {
        List<CtMethod<?>> methods = filterByType(context.enclosingClass().getMethods().stream().toList(), type);
        if (methods.isEmpty()) {
            return null;
        }
        CtMethod<?> ctMethod = randomFromList(methods);
        CtInvocation<T> invocation = this.factory.createInvocation();
        invocation.setTarget(this.factory.createThisAccess(context.enclosingClass().getReference(), true));
        invocation.setExecutable((CtExecutableReference<T>) ctMethod.getReference());
        for (CtParameter<?> parameter : ctMethod.getParameters()) {
            invocation.addArgument(generateArgument(context, parameter.getType()));
        }
        return invocation;
    }

    @Override
    public <T> CtFieldAccess<T> generateFieldAccess(AccessContext context, CtTypeReference<?> type) {
        return null;
    }

    @Override
    public <T> CtArrayAccess<T, ?> generateArrayAccess(AccessContext context, CtTypeReference<?> type) {
        return null;
    }

    @Override
    public <T> CtExpression<T> generateArgument(AccessContext context, CtTypeReference<?> type) {
        return generateExpression(context, type);
    }

    @Override
    public <T> CtExpression<T> generatePrimaryExpression(AccessContext context, CtTypeReference<?> type) {
        double r = this.random.nextDouble();
        if (r < 0.6) {
            // literal
            if (this.intType.equals(type)) {
                return this.factory.Code().createLiteral((T) (Object) this.random.nextInt());
            } else if (this.booleanType.equals(type)) {
                return this.factory.Code().createLiteral((T) (Object) this.random.nextBoolean());
            } else if (r < 0.4) {
                return this.factory.Code().createLiteral(null);
            } else if (r < 0.5 && !(type != null && type.isArray())){ // TODO just to avoid running into the code below...
                // merged with literal to avoid new int() and new boolean() situations
                // new obj
                return generateNewObjectExpression(type); // can be of different type?
            } else {
                // TODO I don't know what to do with that actually...
                // merged with literal to avoid int i = new int[123] situations
                // new array
                return generateNewArrayExpression(context, type);
            }
        } else if (r < 0.7) {
            // IDENT
            // TODO vars/params/fields...
            return null;
        } else if (r < 0.8) {
            // IDENT (args)
            return generateMethodInvocation(context, type);
        } else if (r < 0.9) {
            // this
            //noinspection unchecked
            return (CtExpression<T>) this.factory.createThisAccess(context.enclosingClass().getReference());
        } else {
            // (expr)
            return generateExpression(context, type); // evil recursion?
        }
    }

    @Override
    public <T> CtExpression<T> generateNewObjectExpression(CtTypeReference<?> type) {
        CtTypeReference<?> newType = type != null ? type : randomFromList(this.validMethodReturnTypes);
        while (newType.equals(this.intType) || newType.equals(this.booleanType)) {
            newType = randomFromList(this.validMethodReturnTypes);
        }
        // TODO avoid primitives here
        //noinspection unchecked
        return (CtExpression<T>) this.factory.createConstructorCall(newType);
    }

    @Override
    public <T> CtExpression<T> generateNewArrayExpression(AccessContext context, CtTypeReference<?> type) {
        CtNewArray<T> newArray = this.factory.createNewArray();
        //noinspection unchecked
        newArray.setType((CtTypeReference<T>)  this.factory.createArrayReference(type));
        newArray.addDimensionExpression(generateExpression(context, this.intType));
        return newArray;
    }

    private String randomUpperCamelCase() {
        int approxLength = this.random.nextInt(1, this.settings.approximateNameLength());
        String result = "";
        while (result.length() < approxLength) {
            //noinspection StringConcatenationInLoop
            result += uppercaseFirst(randomName());
        }
        return result;
    }

    private String randomLowerCamelCase() {
        int approxLength = this.random.nextInt(1, this.settings.approximateNameLength());
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

    private StatementType randomStatementType() {
        return this.statementTypes.floorEntry(this.random.nextDouble(this.statementBound)).getValue();
    }

    private CtVariable<?> typedVariableFromContext(AccessContext context, CtTypeReference<?> type) {
        List<? extends CtVariable<?>> list;
        if (this.random.nextDouble() > 0.7 && !(list = filterByType(context.enclosingClass().getFields(), type)).isEmpty()) {
            return randomFromList(list);
        }
        if (this.random.nextBoolean() && !(list = filterByType(context.localVariables(), type)).isEmpty()) {
            return randomFromList(list);
        }
        if (!(list = filterByType(context.parameters(), type)).isEmpty()) {
            return randomFromList(list);
        }
        return null;
    }

    private <V extends CtTypedElement<?>> List<V> filterByType(List<V> list, CtTypeReference<?> type) {
        return list.stream().filter(v -> v.getType().equals(type)).toList();
    }

    @SuppressWarnings("unchecked")
    private <T> CtTypeReference<T> toArrayTypeRef(CtTypeReference<?> reference) {
        return (CtTypeReference<T>) this.factory.Type().createArrayReference(reference,
                // TODO settings
                5 - (int) Math.sqrt(this.random.nextInt(1, 25))
        );
    }
}
