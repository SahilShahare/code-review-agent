package com.codereview.model.records;

import java.util.List;

public record ParseResult(List<Finding> findings, List<UnparsedBlock> unparsed) {}
