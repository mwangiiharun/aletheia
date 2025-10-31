package com.aletheia.tools;

import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Prints concise JaCoCo coverage summary (lines, branches, methods). */
public final class CoverageSummaryPrinter {
    public static void main(String[] args) throws Exception {
        Path csv = Paths.get("target/site/jacoco/jacoco.csv");
        if (!Files.exists(csv)) {
            System.out.println("=== 📊 No coverage file found ===");
            System.exit(0);
        }

        List<String> lines = Files.readAllLines(csv);
        long instrMiss = 0, instrCov = 0, branchMiss = 0, branchCov = 0,
                lineMiss = 0, lineCov = 0, methodMiss = 0, methodCov = 0;

        for (String line : lines) {
            if (line.startsWith("GROUP") || line.trim().isEmpty()) continue;
            String[] c = line.split(",");
            instrMiss += Long.parseLong(c[3]);
            instrCov  += Long.parseLong(c[4]);
            branchMiss += Long.parseLong(c[5]);
            branchCov  += Long.parseLong(c[6]);
            lineMiss += Long.parseLong(c[7]);
            lineCov  += Long.parseLong(c[8]);
            methodMiss += Long.parseLong(c[11]);
            methodCov  += Long.parseLong(c[12]);
        }

        double linePct   = 100.0 * lineCov / (lineMiss + lineCov);
        double branchPct = 100.0 * branchCov / (branchMiss + branchCov);
        double methodPct = 100.0 * methodCov / (methodMiss + methodCov);

        System.out.printf(
                "=== 📊 COVERAGE: Lines %.1f%% | Branches %.1f%% | Methods %.1f%%%n",
                linePct, branchPct, methodPct
        );
    }
}