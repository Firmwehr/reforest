package com.github.firmwehr.reforest;

import java.util.List;

public record RandomSourceGeneratorSettings(
        double fieldToMethodRatio,
        double arrayTypePercentage,
        int approximateNameLength,
        int maxTypeMembers,
        int maxTypes,
        int maxParameters,
        int maxStatementsPerBlock,
        List<String> identList,
        List<WeightedStatementType> statementWeights
) {

    public record WeightedStatementType(double weight, StatementType type) { }
}
