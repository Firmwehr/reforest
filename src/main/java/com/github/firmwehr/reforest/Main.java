package com.github.firmwehr.reforest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.random.RandomGenerator;

public class Main {

    public static void main(String[] args) throws IOException {
        List<String> list = Files.readAllLines(Path.of("src", "main", "resources", "words.txt"));
        var settings = new RandomSourceGeneratorSettings(0.2, 25, 50, list);
        var generator = new RandomSourceGenerator(RandomGenerator.getDefault(), settings);
        System.out.println(generator.generateClass());
    }
}
