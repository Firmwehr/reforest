package com.github.firmwehr.reforest;

import com.github.firmwehr.reforest.RandomSourceGeneratorSettings.WeightedStatementType;
import spoon.reflect.declaration.CtClass;

import javax.lang.model.SourceVersion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class RandomProgramGenerator {

    public static void main(String[] args) throws IOException {
        Options options = new OptionsParser().parseOrExit(args);
        Path wordListPath = options.wordList().orElse(Path.of("src", "main", "resources", "words.txt"));
        List<String> list = Files.readAllLines(wordListPath, StandardCharsets.ISO_8859_1)
                .stream()
                .filter(SourceVersion::isIdentifier)
                .distinct()
                .toList();
        var settings = new RandomSourceGeneratorSettings(
                options.fieldToMethodRatio().orElse(0.3),
                options.arrayTypePercentage().orElse(0.15),
                options.approximateNameLength().orElse(24),
                options.typeMemberLimit().orElse(10),
                options.typeLimit().orElse(5),
                10,
                options.statementsPerBlock().orElse(15),
                list,
                List.of(
                        new WeightedStatementType(0.05, StatementType.EMPTY),
                        new WeightedStatementType(0.2, StatementType.LOCAL_VARIABLE_DECLARATION),
                        new WeightedStatementType(0.3, StatementType.EXPRESSION),
                        new WeightedStatementType(0.1, StatementType.IF),
                        new WeightedStatementType(0.08, StatementType.WHILE),
                        new WeightedStatementType(0.06, StatementType.RETURN),
                        new WeightedStatementType(0.07, StatementType.BLOCK)
                )
        );
        RandomGeneratorFactory<RandomGenerator> generatorFactory = RandomGeneratorFactory.getDefault();
        var random = options.seed().isPresent()
                ? generatorFactory.create(options.seed().getAsLong())
                : generatorFactory.create();
        var generator = new RandomSourceGenerator(random, settings);
        List<String> classes = generator.generateProgram().stream()
                .map(CtClass::toString)
                .toList();
        System.out.println("Generated " + classes.size() + " classes");
        Files.write(options.outputPath(), classes);
    }
}
