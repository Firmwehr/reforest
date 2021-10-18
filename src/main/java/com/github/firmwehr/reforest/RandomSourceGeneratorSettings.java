package com.github.firmwehr.reforest;

import java.util.List;

public record RandomSourceGeneratorSettings(
        double fieldToMethodRatio,
        int approximateNameLength,
        int maxTypeMembers,
        List<String> identList
) {
}
