package com.github.firmwehr.reforest.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class WordListExtractor {

    public static void main(String[] args) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("http://www-personal.umich.edu/~jlawler/wordlist"))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofLines());
        var list = response.body()
                .filter(s -> !s.isBlank())
                .filter(s -> Character.isJavaIdentifierStart(s.charAt(0))
                        && s.chars().skip(1).allMatch(Character::isJavaIdentifierPart))
                .toList();
        var path = Path.of("src", "main", "resources", "words.txt");
        System.out.println(path.toAbsolutePath());
        Files.write(path, list, StandardOpenOption.CREATE);
    }
}
