package com.cybershield.dto;

import java.util.List;

/** Labels and values for one Chart.js chart, calculated from MySQL. */
public record ChartData(String title, List<String> labels, List<Long> values) {
}
