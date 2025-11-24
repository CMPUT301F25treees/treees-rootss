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

/**
 * This is a class that is responsible for exporting a list of names as a CSV file
 * to then devices storage.
 *
 * This helper manages the ACTION_CREATE_DOCUMENT flow, opens the system
 * file picker so the user can choose a save location, and writes the generated
 * CSV content to the selected URI.
 */
public class ExportHelper {

    private final Fragment fragment;
    private final ActivityResultLauncher<Intent> launcher;

    private String pendingText;

    /**
     * Creates a new ExportHelper instance bound to the provided Fragment.
     *
     * Internally registers an ActivityResultLauncher that listens for the result
     * of the ACTION_CREATE_DOCUMENT intent. When a URI is returned, this helper
     * writes any pending text content into the selected file.
     *
     * @param fragment the Fragment used to register activity result callbacks
     *                 and provide context for file writing and Toast messages
     */
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

    /**
     * Exports the given list of names as a single-column CSV file.
     *
     * If the list is empty, a Toast message is shown and the export is cancelled.
     * Otherwise, a filename is generated based on the event name and the
     * current list mode, and the system
     * the file picker is launched so the user can select where to save the CSV.
     *
     * @param eventName the name of the event, used for generating the file name
     * @param modeLabel a label representing the active list (waiting, invited, etc.)
     * @param names     the list of participant names to export
     */
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

    /**
     * Builds a CSV string from a list of names.
     *
     * The CSV contains a single header column named "Name" followed by
     * one row per entry in the list. Values are escaped as needed to
     * ensure CSV format compatibility.
     *
     * @param names the list of names to convert into CSV format
     * @return a properly formatted CSV string
     */
    private String buildCsvFromNames(List<String> names) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name\n");
        for (String name : names) {
            sb.append(escapeCsv(name)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Escapes a CSV value by wrapping it in quotes if needed and doubling
     * internal quotes, this ensures that everything remains valid in CSV format.
     *
     * @param value the raw string to escape
     * @return an escaped CSV-safe representation of the value
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        boolean needsQuotes =
                value.contains(",") || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

}
