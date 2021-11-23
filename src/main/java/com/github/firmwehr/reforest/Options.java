package com.github.firmwehr.reforest;

import net.jbock.Command;
import net.jbock.Option;
import net.jbock.util.StringConverter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

@Command
public interface Options {

    @Option(
            names = {"--output", "-o"},
            paramLabel = "PATH",
            converter = NonExistentFileConverter.class,
            description = "The path of the file to write to"
    )
    Path outputPath();

    @Option(
            names = {"--seed", "-s"},
            paramLabel = "LONG",
            description = "The seed to use for RNG"
    )
    OptionalLong seed();

    @Option(
            names = {"--words", "-w"},
            paramLabel = "PATH",
            converter = ExistingFileConverter.class,
            description = "The path to the word list file to use"
    )
    Optional<Path> wordList();

    @Option(
            names = {"--field-to-method-ration", "--fm"},
            paramLabel = "NUMBER",
            description = "The ration between fields and methods. Defaults to 0.3"
    )
    OptionalDouble fieldToMethodRatio();

    @Option(
            names = {"--array-type-percentage"},
            paramLabel = "NUMBER",
            description = "The percentage of array types. Defaults to 0.15"
    )
    OptionalDouble arrayTypePercentage();

    @Option(
            names = {"--approx-name-length", "-l"},
            paramLabel = "INTEGER",
            description = "The approximate length of identifiers. Defaults to 24"
    )
    OptionalInt approximateNameLength();

    class ExistingFileConverter extends StringConverter<Path> {

        @Override
        protected Path convert(String token) {
            Path path = Path.of(token);

            if (Files.notExists(path)) {
                throw new IllegalArgumentException("The file '%s' does not exist".formatted(path));
            }
            if (!Files.isReadable(path)) {
                throw new IllegalArgumentException("The file '%s' is not readable".formatted(path));
            }

            return path;
        }
    }

    class NonExistentFileConverter extends StringConverter<Path> {
        @Override
        protected Path convert(String token) throws Exception {
            Path path = Path.of(token);
            if (Files.exists(path)) {
                throw new IllegalArgumentException("The file '%s' already exists".formatted(path));
            }
            return path;
        }
    }
}
