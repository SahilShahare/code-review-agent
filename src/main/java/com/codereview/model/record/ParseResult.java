package com.codereview.model.record;

import java.util.List;

public record ParseResult(List<Finding> findings, List<UnparsedBlock> unparsed) {}
