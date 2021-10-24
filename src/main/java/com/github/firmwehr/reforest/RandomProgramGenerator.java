package com.github.firmwehr.reforest;

import com.github.firmwehr.reforest.RandomSourceGeneratorSettings.WeightedStatementType;
import spoon.reflect.declaration.CtClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.random.RandomGeneratorFactory;

public class RandomProgramGenerator {

    public static void main(String[] args) throws IOException {
        List<String> list = Files.readAllLines(Path.of("src", "main", "resources", "words.txt"));
        var settings = new RandomSourceGeneratorSettings(
                0.3,
                0.15,
                20,
                30,
                40,
                7,
                10,
                list,
                List.of(
                        new WeightedStatementType(0.05, StatementType.EMPTY),
                        new WeightedStatementType(0.3, StatementType.LOCAL_VARIABLE_DECLARATION),
                        new WeightedStatementType(0.3, StatementType.EXPRESSION),
                        new WeightedStatementType(0.1, StatementType.IF),
                        new WeightedStatementType(0.08, StatementType.WHILE),
                        new WeightedStatementType(0.06, StatementType.RETURN),
                        new WeightedStatementType(0.07, StatementType.BLOCK)
                )
        );
        var random = RandomGeneratorFactory.getDefault().create(1337L << 42 | 98652677);
        var generator = new RandomSourceGenerator(random, settings);
        List<String> classes = generator.generateProgram().stream().map(CtClass::toString)
                        .toList();
        classes.forEach(System.out::println);
        Files.write(Path.of("src/main/resources/output.txt"), classes);
    }
}
