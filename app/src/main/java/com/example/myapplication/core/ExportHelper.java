package com.example.myapplication.core;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExportHelper {

    private final Fragment fragment;
    private final ActivityResultLauncher<Intent> launcher;

    private String pendingText;

    public ExportHelper(@NonNull Fragment fragment) {
        this.fragment = fragment;
        this.launcher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Uri uri = result.getData().getData();
                    if (uri == null || pendingText == null) return;

                    try (OutputStream os = fragment.requireContext()
                            .getContentResolver()
                            .openOutputStream(uri);
                         BufferedWriter writer = new BufferedWriter(
                                 new OutputStreamWriter(os, StandardCharsets.UTF_8))) {

                        writer.write(pendingText);
                        writer.flush();
                        Toast.makeText(fragment.requireContext(),
                                "File exported successfully.",
                                Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(fragment.requireContext(),
                                "Failed to save file: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    public void exportNamesCsv(String eventName,
                               String modeLabel,
                               List<String> names) {

        if (names == null || names.isEmpty()) {
            Toast.makeText(fragment.requireContext(),
                    "No entries to export.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        pendingText = buildCsvFromNames(names);

        String safeEventName = (eventName == null || eventName.trim().isEmpty())
                ? "event"
                : eventName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");

        String safeMode = modeLabel.replace(" ", "_");
        String suggestedName = safeEventName + "_" + safeMode + ".csv";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, suggestedName);

        launcher.launch(intent);
    }


    private String buildCsvFromNames(List<String> names) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name\n");
        for (String name : names) {
            sb.append(escapeCsv(name)).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        boolean needsQuotes =
                value.contains(",") || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

}
