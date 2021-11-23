package com.github.firmwehr.reforest.util;

import javax.lang.model.SourceVersion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class RandomIdentGenerator {
    private final List<String> words;
    private final RandomGenerator random = RandomGenerator.getDefault();

    public static void main(String[] args) throws IOException {
        var randomIdentGenrator = new RandomIdentGenerator(Files.readAllLines(Path.of("src", "main", "resources", "words.txt"))
                .stream()
                .filter(s -> s.chars().allMatch(i -> 'a' <= i && i <= 'z'))
                .toList());
        List<String> ids = IntStream.range(0, 1 << 20).mapToObj(value -> randomIdentGenrator.generateIdent())
                .toList();
        Files.write(Path.of("src/main/resources/mandy-idents.txt"), ids);
    }

    public RandomIdentGenerator(List<String> words) {
        this.words = words;
    }

    public String generateIdent() {
        return random.nextBoolean() ? randomLowerCamelCase() : randomUpperCamelCase();
    }

    private String randomUpperCamelCase() {
        int approxLength = this.random.nextInt(1, 20);
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
        int approxLength = this.random.nextInt(1, 20);
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

    private String randomName() {
        return randomFromList(this.words);
    }

    private <T> T randomFromList(List<T> elements) {
        int index = this.random.nextInt(elements.size());
        return elements.get(index);
    }
}
