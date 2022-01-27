package com.github.firmwehr.reforest;

import spoon.Launcher;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtArrayRead;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
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
import spoon.reflect.reference.CtVariableReference;

import javax.lang.model.SourceVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Comparator.comparing;
import static java.util.stream.Stream.concat;


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
        List<String> names = randomNames(0, this.random.nextInt(this.settings.maxTypes()), this::randomUpperCamelCase);
        List<CtTypeReference<Object>> references = names.stream()
                .map(n -> this.factory.Type().createReference(n))
                .toList();
        this.validFieldTypes.addAll(references);
        this.validMethodReturnTypes.addAll(references);
        List<CtClass<?>> classes = names.stream().distinct().map(this::generateClass).collect(Collectors.toList());
        int w = classes.size();
        for (CtClass<?> aClass : classes) {
            // lazy fields before methods sort
            var members = aClass.getTypeMembers().stream()
                    .sorted(comparing(CtTypeMember::getClass, comparing(Class::getName)))
                    .toList();
            aClass.setTypeMembers(members);
            for (CtMethod<?> method : aClass.getMethods()) {
                method.setBody(generateBlock(
                        new AccessContext(
                                new ArrayList<>(),
                                method.getParameters(),
                                aClass,
                                aClass,
                                method.getType(),
                                0
                        ),
                        method.getType()
                ));
            }
            if (w > 0 && random.nextInt(w--) == 0) {
                w = 0; // found
                // used to prevent direct method/field accesses in static context - yes we're super lazy
                CtClass<?> fakeTarget = factory.createClass();
                CtMethod<?> mainMethod = factory.createMethod(
                        aClass,
                        Set.of(ModifierKind.PUBLIC, ModifierKind.STATIC),
                        factory.Type().voidPrimitiveType(),
                        "main",
                        List.of(factory.createParameter(
                                null,
                                factory.Type().createArrayReference("String"),
                                "args")
                        ),
                        Set.of());
                addThrownTypes(mainMethod);
                mainMethod.setBody(generateBlock(
                        new AccessContext(
                                new ArrayList<>(),
                                new ArrayList<>(),
                                fakeTarget,
                                fakeTarget,
                                factory.Type().voidPrimitiveType(),
                                2
                                ),
                        factory.Type().voidPrimitiveType()
                ));
                aClass.addTypeMember(mainMethod);
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
        List<CtParameter<?>> parameters = new ArrayList<>();
        int parameterCount = this.random.nextInt(this.settings.maxParameters());
        List<String> parameterNames = randomNames(0, parameterCount, this::randomLowerCamelCase);
        for (int i = 0; i < parameterCount; i++) {
            parameters.add(generateParameter(parameterNames.get(i)));
        }
        addThrownTypes(ctMethod);
        ctMethod.setParameters(parameters);
        ctMethod.addModifier(ModifierKind.PUBLIC);
        // body is added later, when method/field references of other classes are available
        return ctMethod;
    }

    private void addThrownTypes(CtMethod<?> method) {
        while (this.random.nextDouble() < 0.1) {
            method.addThrownType(this.factory.Type().createReference(randomUpperCamelCase()));
        }
    }

    @Override
    public <T> CtParameter<T> generateParameter(String name) {
        CtParameter<T> ctParameter = this.factory.Core().createParameter();
        ctParameter.setType(generateType(false));
        ctParameter.setSimpleName(name);
        return ctParameter;
    }

    @Override
    public <T> CtTypeReference<T> generateType(boolean voidAllowed) {
        CtTypeReference<T> typeReference = fixGenerics(randomFromList(
                voidAllowed
                        ? this.validMethodReturnTypes
                        : this.validFieldTypes
        ));
        if (!typeReference.equals(this.factory.Type().VOID_PRIMITIVE)
                && this.random.nextDouble() < this.settings.arrayTypePercentage()) {
            typeReference = toArrayTypeRef(typeReference);
        }
        return typeReference;
    }

    @Override
    public CtStatement generateStatement(AccessContext context) {
        return switch (randomStatementType()) {
            case LOCAL_VARIABLE_DECLARATION, RETURN -> generateStatement(context); // not allowed here, try again
            case EMPTY -> generateEmptyStatement(context);
            case WHILE -> generateWhileStatement(context.incrementComplexity());
            case IF -> generateIfStatement(context.incrementComplexity());
            case EXPRESSION -> generateExpressionStatement(context);
            case BLOCK -> generateBlock(context.incrementComplexity(), context.returnType());
        };
    }

    @Override
    public <T> CtBlock<T> generateBlock(AccessContext context, CtTypeReference<?> returnType) {
        AccessContext newContext = new AccessContext(
                new ArrayList<>(context.localVariables()),
                context.parameters(),
                context.target(),
                context.enclosingClass(),
                context.returnType(),
                context.complexity() + 1
        );
        CtBlock<T> ctBlock = this.factory.createBlock();
        int statementCount = this.random.nextInt(this.settings.maxStatementsPerBlock());
        for (int i = 0; i < statementCount; i++) {
            ctBlock.addStatement(generateBlockStatement(newContext));
        }
        if (context.complexity() == 0 && !this.factory.Type().VOID_PRIMITIVE.equals(returnType)) {
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
            case RETURN -> context.complexity() > 1
                    ? generateReturnStatement(context, context.returnType())
                    : generateBlockStatement(context);
            case BLOCK -> generateBlockStatement(context);
        };
    }

    @Override
    public CtStatement generateLocalVariableDeclarationStatement(AccessContext context) {
        var type = generateType(false);
        CtLocalVariable<Object> localVariable = this.factory.Code().createLocalVariable(
                type,
                randomLowerCamelCase(),
                this.random.nextDouble() < 0.7 ? generateExpression(context, type) : null
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
        CtStatement thenStatement = generateStatement(context);
        if (thenStatement instanceof CtBlock<?> block && block.getStatements().isEmpty()) {
            thenStatement = null; // spoon #4240 workaround
        }
        ctIf.setThenStatement(thenStatement);
        ctIf.setElseStatement(generateStatement(context));
        return ctIf;
    }

    @Override
    public CtStatement generateExpressionStatement(AccessContext context) {
        if (this.random.nextDouble() < 0.6) {
            return generateMethodInvocation(context.incrementComplexity(), randomFromList(this.validMethodReturnTypes));
        } else {
            List<CtLocalVariable<?>> localVariables = context.localVariables();
            List<CtParameter<?>> parameters = context.parameters();
            List<CtField<?>> fields = context.enclosingClass().getFields();
            var variables = concat(
                    concat(localVariables.stream(), parameters.stream()),
                    fields.stream())
                    .toList();
            if (variables.isEmpty()) {
                return null; // can't do an assignment
            }
            CtVariable<?> variable = randomFromList(variables);
            return this.factory.createVariableAssignment(variable.getReference(), false,
                    generateExpression(context.incrementComplexity(), variable.getType()));
        }
    }

    @Override
    public CtStatement generateReturnStatement(AccessContext context, CtTypeReference<?> type) {
        CtReturn<Object> ctReturn = this.factory.Core().createReturn();
        if (!type.equals(this.factory.Type().VOID_PRIMITIVE)) {
            ctReturn.setReturnedExpression(generateLogicalOrExpression(context, type));
        }
        return ctReturn;
    }

    @Override
    public <T> CtExpression<T> generateExpression(AccessContext context, CtTypeReference<?> type) {
        return generateAssignmentExpression(context, type);
    }

    @Override
    public <T> CtExpression<T> generateAssignmentExpression(AccessContext context, CtTypeReference<?> type) {
        // TODO
        CtExpression<T> expression = generateLogicalOrExpression(context, type);
        if (expression instanceof CtVariableAccess access
                && this.random.nextInt(context.complexity()) < context.complexity() / 3 + 1) {
            //noinspection unchecked
            return (CtExpression<T>) this.factory.createVariableAssignment(access.getVariable(), false,
                            generateAssignmentExpression(context.incrementComplexity(), expression.getType()))
                    .setType(type);
        } else if (expression instanceof CtArrayAccess access
                && this.random.nextInt(context.complexity()) < context.complexity() / 3 + 1) {
            var assignment = this.factory.createAssignment();
            //noinspection unchecked
            assignment.setAssigned(access);
            assignment.setAssignment(generateExpression(context.incrementComplexity(), access.getType()));
            assignment.setType(fixGenerics(access.getType()));
            //noinspection unchecked
            return (CtExpression<T>) assignment;
        }
        return expression;
    }

    @Override
    public <T> CtExpression<T> generateLogicalOrExpression(AccessContext context, CtTypeReference<?> type) {
        if (!this.booleanType.equals(type) || this.random.nextDouble() > 0.15) {
            return generateLogicalAndExpression(context, type);
        }
        return this.factory.createBinaryOperator(
                generateLogicalOrExpression(context, type),
                generateLogicalAndExpression(context, type),
                BinaryOperatorKind.OR
        ).setType(fixGenerics(this.booleanType));
    }

    @Override
    public <T> CtExpression<T> generateLogicalAndExpression(AccessContext context, CtTypeReference<?> type) {
        if (!this.booleanType.equals(type) || this.random.nextDouble() > 0.09) {
            return generateEqualityExpression(context, type);
        }
        return this.factory.createBinaryOperator(
                generateLogicalAndExpression(context, type),
                generateEqualityExpression(context, type),
                BinaryOperatorKind.AND
        ).setType(fixGenerics(this.booleanType));
    }

    @Override
    public <T> CtExpression<T> generateEqualityExpression(AccessContext context, CtTypeReference<?> type) {
        if (!this.booleanType.equals(type) || this.random.nextDouble() > 0.08) {
            return generateRelationalExpression(context, type);
        }
        // must be same on both sides to be valid java code
        CtTypeReference<?> equalityType = randomFromList(this.validFieldTypes);
        // we want some random array types in there
        while (this.random.nextDouble() < 0.05) {
            equalityType = toArrayTypeRef(equalityType);
        }
        return this.factory.createBinaryOperator(
                generateEqualityExpression(context, equalityType),
                generateRelationalExpression(context, equalityType),
                this.random.nextBoolean() ? BinaryOperatorKind.EQ : BinaryOperatorKind.NE
        ).setType(fixGenerics(equalityType));
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
        ).setType(fixGenerics(this.intType));
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
        ).setType(fixGenerics(this.intType));
    }

    @Override
    public <T> CtExpression<T> generateMultiplicativeExpression(AccessContext context, CtTypeReference<?> type) {
        if (!this.intType.equals(type) || this.random.nextDouble() > 0.1) {
            return generateUnaryExpression(context, type);
        }
        var newContext = context.incrementComplexity();
        return this.factory.createBinaryOperator(
                generateMultiplicativeExpression(newContext, this.intType),
                generateUnaryExpression(newContext, this.intType),
                switch (this.random.nextInt(3)) {
                    case 0 -> BinaryOperatorKind.MUL;
                    case 1 -> BinaryOperatorKind.DIV;
                    case 2 -> BinaryOperatorKind.MOD;
                    default -> throw new IllegalArgumentException("???");
                }
        ).setType(fixGenerics(this.intType));
    }

    @Override
    public <T> CtExpression<T> generateUnaryExpression(AccessContext context, CtTypeReference<?> type) {
        if (!this.intType.equals(type) || !this.booleanType.equals(type) || this.random.nextDouble() > 0.2) {
            return generatePostfixExpression(context, type);
        }
        return this.factory.createUnaryOperator()
                .<CtUnaryOperator<T>>setKind(type.equals(this.intType) ? UnaryOperatorKind.NEG : UnaryOperatorKind.NOT)
                .<CtUnaryOperator<T>>setOperand(generateUnaryExpression(context.incrementComplexity(), type));
    }

    @Override
    public <T> CtExpression<T> generatePostfixExpression(AccessContext context, CtTypeReference<?> type) {
        CtExpression<T> expression;
        if (context.complexity() > 10
                || (expression = generatePrimaryExpression(context, type)) == null
                || expression.getType().equals(this.factory.Type().VOID_PRIMITIVE)
        ) {
            return createLiteral(type, true);
        }
        if (type.equals(expression.getType())
                || expression.getType().equals(this.factory.Type().NULL_TYPE)
                || expression.getType().equals(this.factory.Type().NULL_TYPE)
        ) {
            // TODO wrap again with probability? might be fun...
            return expression; // no PostfixOp on this needed
        }
/*        if (expression.getType().isArray()) {
            var arrayElementType = this.factory.Type()
                    .get(expression.getType().getQualifiedName().replace("[]", ""));
            if (arrayElementType != null) {
                return expression;
            }
        }*/
        // return createLiteral(type, true);
        // TODO MAKE THIS WORK!!!
        if (expression.getType().equals(this.intType)
                || expression.getType().equals(this.booleanType)) {
            return createLiteral(type, true); // type is not correct and we can't fix it
        }
        expression = generatePostfixOp(expression, context.incrementComplexity(), type);
        if (expression == null || !type.equals(expression.getType())) {
            expression = createLiteral(type, true);
        }
        return expression;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CtExpression<T> generatePostfixOp(CtExpression<?> target, AccessContext context, CtTypeReference<?> type) {
        CtExpression<?> current = target;
        // TODO limit?
        // loop is in here to have a proper context
        while (target.getType().isArray() && !type.equals(current.getType())) {
            var qualifiedName = current.getType().getQualifiedName();
            if (current.getType().isArray() && qualifiedName.startsWith(type.getQualifiedName())) {
                CtArrayAccess<Object, CtExpression<?>> arrayAccess =
                        (CtArrayAccess<Object, CtExpression<?>>) generateArrayAccess(context.incrementComplexity(), type);
                arrayAccess.setTarget(current);
                current = arrayAccess;
                current.setType(this.factory.Type().createReference(qualifiedName.substring(0, qualifiedName.lastIndexOf('['))));
            } else {
                return null;
            }
        }
        //noinspection unchecked
        return (CtExpression<T>) current;
    }

    @Override
    public <T> CtInvocation<T> generateMethodInvocation(AccessContext context, CtTypeReference<?> type) {
        var methods = context.target().getMethods().stream().toList();
        if (methods.isEmpty()) {
            return null; // can't do anything :(
        }
        var correctlyTypedMethods = filterByType(methods, type, true);
        if (!correctlyTypedMethods.isEmpty()) {
            // no method with this type found, we just return a different type then
            methods = correctlyTypedMethods;
        }
        CtMethod<?> ctMethod = randomFromList(methods);
        CtInvocation<T> invocation = this.factory.createInvocation();
        invocation.setTarget(this.factory.createThisAccess(context.enclosingClass().getReference(), true));
        //noinspection unchecked
        invocation.setExecutable((CtExecutableReference<T>) ctMethod.getReference());
        for (CtParameter<?> parameter : ctMethod.getParameters()) {
            invocation.addArgument(generateArgument(context.incrementComplexity(), parameter.getType()));
        }
        return invocation;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CtFieldAccess<T> generateFieldAccess(AccessContext context, CtTypeReference<?> type) {
        var fields = context.target().getFields();
        if (fields.isEmpty()) {
            return null; // can't do anything :(
        }
        var correctlyTypedFields = filterByType(fields, type, true);
        if (!correctlyTypedFields.isEmpty()) {
            // no method with this type found, we just return a different type then
            fields = correctlyTypedFields;
        }
        CtField<?> field = randomFromList(fields);
        CtFieldRead<T> fieldRead = this.factory.createFieldRead();
        fieldRead.setVariable((CtVariableReference<T>) field.getReference());
        return fieldRead;
    }

    @Override
    public <T> CtArrayAccess<T, ?> generateArrayAccess(AccessContext context, CtTypeReference<?> type) {
        CtArrayRead<T> arrayRead = this.factory.createArrayRead();
        arrayRead.setIndexExpression(generateExpression(context, this.intType));
        return arrayRead;
    }

    @Override
    public <T> CtExpression<T> generateArgument(AccessContext context, CtTypeReference<?> type) {
        return generateExpression(context.incrementComplexity(), type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public <T> CtExpression<T> generatePrimaryExpression(AccessContext context, CtTypeReference<?> type) {
        double r = this.random.nextDouble();
        if (r < 0.3 || context.complexity() > 8) {
            // literal
            return createLiteral(type, true);
        } else if (r < 0.4
                && !this.intType.equals(type)
                && !this.booleanType.equals(type)
                && (type == null || !type.isArray())
        ) {
            // new obj
            return generateNewObjectExpression(type); // can be of different type?
        } else if (r < 0.5 && (type != null && type.isArray())) {
            // new array
            return generateNewArrayExpression(context, type);
        } else if (r < 0.7) {
            // IDENT
            // we differ from spec here, as spoon prints 'this.' for field accesses
            // by default
            // TODO?
            List<CtLocalVariable<?>> localVariables = context.localVariables();
            List<CtParameter<?>> parameters = context.parameters();
            List<CtField<?>> fields = context.enclosingClass().getFields();
            var variables = concat(
                    concat(localVariables.stream(), parameters.stream()),
                    fields.stream())
                    .toList();
            var correctlyTypedVariables = filterByType(variables, type, true);
            CtVariable<?> variable;
            if (!correctlyTypedVariables.isEmpty()) {
                variable = randomFromList(correctlyTypedVariables);
            } else if (!variables.isEmpty()) {
                // well, lets get a different type then
                variable = randomFromList(variables);
            } else {
                return createLiteral(type, true); // fallback
            }
            return (CtExpression<T>) this.factory.createVariableRead(variable.getReference(), false)
                    .setType((CtTypeReference) variable.getType());
        } else if (r < 0.8) {
            // IDENT (args)
            return generateMethodInvocation(context, type);
        } else if (r < 0.9) {
            // this
            return (CtExpression<T>) this.factory.createThisAccess(context.enclosingClass().getReference());
        } else if (r >= 0.9) {
            // (expr)
            return generateExpression(context, type); // evil recursion?
        } else {
            return createLiteral(type, false); // fallback
        }
    }

    @Override
    public <T> CtExpression<T> generateNewObjectExpression(CtTypeReference<?> type) {
        CtTypeReference<?> newType = type != null ? type : randomFromList(this.validMethodReturnTypes);
        // TODO avoid primitives here at all?
        while (newType.equals(this.intType) || newType.equals(this.booleanType)) {
            newType = randomFromList(this.validMethodReturnTypes);
        }
        //noinspection unchecked
        return (CtExpression<T>) this.factory.createConstructorCall(newType);
    }

    @Override
    public <T> CtExpression<T> generateNewArrayExpression(AccessContext context, CtTypeReference<?> type) {
        CtNewArray<T> newArray = this.factory.createNewArray();
        //noinspection unchecked
        newArray.setType((CtTypeReference<T>) type);
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
        if (SourceVersion.isIdentifier(result)) {
            return result;
        }
        return result + this.random.nextInt(0, 42);
    }

    private String randomLowerCamelCase() {
        int approxLength = this.random.nextInt(1, this.settings.approximateNameLength());
        String result = randomName();
        while (result.length() < approxLength) {
            //noinspection StringConcatenationInLoop
            result += uppercaseFirst(randomName());
        }
        if (SourceVersion.isName(result)) {
            return result;
        }
        return result + this.random.nextInt(0, 42);
    }

    private String uppercaseFirst(String s) {
        if (s.length() == 1) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private List<String> randomNames(int min, int max, Supplier<String> nameFactory) {
        return IntStream.range(min, max)
                .mapToObj(i -> nameFactory.get())
                .distinct()
                .toList();
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

    private <V extends CtTypedElement<?>> List<V> filterByType(List<V> list, CtTypeReference<?> type, boolean allowArrays) {
        return list.stream().filter(v -> v.getType().equals(type)
                        || (allowArrays && v.getType().isArray() && v.getType().getQualifiedName().startsWith(type.getQualifiedName())))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private <T> CtTypeReference<T> toArrayTypeRef(CtTypeReference<?> reference) {
        return (CtTypeReference<T>) this.factory.Type().createArrayReference(reference,
                // TODO settings
                5 - (int) Math.sqrt(this.random.nextInt(1, 25))
        );
    }

    @SuppressWarnings("unchecked")
    private <T> CtExpression<T> createLiteral(CtTypeReference<?> type, boolean random) {
        if (this.intType.equals(type)) {
            return (CtExpression<T>) this.factory.Code().createLiteral(random ? this.random.nextInt() : 0);
        } else if (this.booleanType.equals(type)) {
            return (CtExpression<T>) this.factory.Code().createLiteral(random && this.random.nextBoolean());
        } else {
            return this.factory.Code().createLiteral(null);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> CtTypeReference<T> fixGenerics(CtTypeReference<?> type) {
        return (CtTypeReference<T>) type;
    }
}
