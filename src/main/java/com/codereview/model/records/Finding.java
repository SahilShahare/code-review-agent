package com.codereview.model.records;

import com.codereview.model.enums.Severity;

public record Finding(String file, Severity severity, String location, String message) {}
