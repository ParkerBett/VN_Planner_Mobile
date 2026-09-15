package com.vnplanner.mobile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.vnplanner.mobile.io.ProjectFileStore;
import com.vnplanner.mobile.model.Project;
import java.lang.reflect.Field;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQUEST_OPEN = 10;
    private static final int REQUEST_SAVE = 11;
    private static final int REQUEST_EXPORT = 12;
    private static final int REQUEST_IMPORT = 13;
    private Project project = new Project();
    private Uri currentUri;
    private Spinner sectionSpinner;
    private LinearLayout editor;
    private final Map<String, EditText> inputs = new LinkedHashMap<>();
    private boolean restoring;

    private static final LinkedHashMap<String, String[]> SECTIONS = new LinkedHashMap<>();
    static {
        SECTIONS.put("Project", new String[]{"title","workingTitle","oneSentencePitch","coreIdea","genreTone","playerFeel"});
        SECTIONS.put("Story", new String[]{"centralConflict","beginning","middle","revelations","climax","ending"});
        SECTIONS.put("Protagonist", new String[]{"protagonistName","protagonistAge","protagonistPersonality","protagonistWant","protagonistNeed","protagonistFear","protagonistArc"});
        SECTIONS.put("Characters", new String[]{"characters"});
        SECTIONS.put("Chapters", new String[]{"chapters"});
        SECTIONS.put("Choices & Endings", new String[]{"amountOfChoice","importantChoices","branches","differentEndings","endingRequirements"});
        SECTIONS.put("World & Lore", new String[]{"setting","importantLocations","worldRules","loreHistory","secrets"});
        SECTIONS.put("Presentation", new String[]{"visualStyle","musicAudio","uiPresentation","inspirations"});
        SECTIONS.put("Development", new String[]{"engineTools","mustHave","niceToHave","scopeLimits"});
        SECTIONS.put("Free Notes", new String[]{"freeNotes"});
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        if (getIntent() != null && getIntent().getData() != null) loadUri(getIntent().getData());
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        Button newButton = button("New");
        Button openButton = button("Open");
        Button saveButton = button("Save");
        Button saveAsButton = button("Save As");
        Button importButton = button("Import JSON");
        Button exportButton = button("Export JSON");
        addToolbarButton(bar, newButton);
        addToolbarButton(bar, openButton);
        addToolbarButton(bar, saveButton);
        addToolbarButton(bar, saveAsButton);
        addToolbarButton(bar, importButton);
        addToolbarButton(bar, exportButton);
        HorizontalScrollView actionScroll = new HorizontalScrollView(this);
        actionScroll.setHorizontalScrollBarEnabled(true);
        actionScroll.setScrollbarFadingEnabled(false);
        actionScroll.setHorizontalFadingEdgeEnabled(true);
        actionScroll.setFillViewport(false);
        actionScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        actionScroll.addView(bar, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(actionScroll, new LinearLayout.LayoutParams(-1, dp(58)));

        sectionSpinner = new Spinner(this);
        sectionSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(SECTIONS.keySet())));
        root.addView(sectionSpinner, new LinearLayout.LayoutParams(-1, dp(52)));
        editor = new LinearLayout(this);
        editor.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(editor);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        sectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) {}
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { showSection(position); }
        });
        newButton.setOnClickListener(v -> { project = new Project(); currentUri = null; restoreAllFields(); toast("New project"); });
        openButton.setOnClickListener(v -> openDocument());
        saveButton.setOnClickListener(v -> save(false));
        saveAsButton.setOnClickListener(v -> save(true));
        importButton.setOnClickListener(v -> openJsonDocument());
        exportButton.setOnClickListener(v -> exportJson());
    }

    private void showSection(int position) {
        if (restoring) return;
        saveVisibleFields();
        editor.removeAllViews();
        String section = new ArrayList<>(SECTIONS.keySet()).get(position);
        for (String name : SECTIONS.get(section)) {
            if (name.equals("characters") || name.equals("chapters")) {
                TextView message = new TextView(this);
                message.setText(name.equals("characters") ? "Dynamic characters are enabled in the next phase." : "Dynamic chapters are enabled in the next phase.");
                message.setPadding(0, dp(16), 0, dp(16));
                editor.addView(message);
                continue;
            }
            TextView label = new TextView(this);
            label.setText(labelFor(name));
            label.setTextSize(16);
            label.setPadding(0, dp(14), 0, dp(5));
            editor.addView(label);
            EditText input = inputs.get(name);
            if (input == null) { input = new EditText(this); inputs.put(name, input); }
            input.setText(value(name));
            input.setTextSize(16);
            input.setPadding(dp(12), dp(10), dp(12), dp(10));
            input.setSingleLine(false);
            input.setMinLines(name.equals("title") || name.equals("workingTitle") || name.equals("genreTone") || name.equals("protagonistName") || name.equals("protagonistAge") ? 1 : 3);
            input.setGravity(Gravity.TOP | Gravity.START);
            editor.addView(input, new LinearLayout.LayoutParams(-1, name.equals("title") || name.equals("workingTitle") ? dp(58) : dp(112)));
        }
        editor.requestLayout();
    }

    private String value(String name) {
        try { Object v = Project.class.getField(name).get(project); return v == null ? "" : v.toString(); }
        catch (Exception e) { return ""; }
    }

    private void saveVisibleFields() {
        if (editor == null) return;
        for (Map.Entry<String, EditText> e : inputs.entrySet()) {
            if (e.getValue().getParent() != null) setValue(e.getKey(), e.getValue().getText().toString());
        }
    }

    private void setValue(String name, String value) {
        try { Field field = Project.class.getField(name); if (field.getType() == String.class) field.set(project, value); }
        catch (Exception ignored) {}
    }

    private void restoreAllFields() {
        inputs.clear();
        int pos = sectionSpinner == null ? 0 : sectionSpinner.getSelectedItemPosition();
        if (sectionSpinner != null) {
            restoring = false;
            showSection(pos);
        }
    }

    private void syncProject() { saveVisibleFields(); }

    private void openDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_OPEN);
    }

    private void openJsonDocument() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    private void save(boolean saveAs) {
        syncProject();
        if (!saveAs && currentUri != null) { writeVnproj(currentUri); return; }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, (project.title.trim().isEmpty() ? "New Visual Novel" : project.title) + ".vnproj");
        startActivityForResult(intent, REQUEST_SAVE);
    }

    private void exportJson() {
        syncProject();
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, (project.title.trim().isEmpty() ? "project" : project.title) + ".json");
        startActivityForResult(intent, REQUEST_EXPORT);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION); } catch (Exception ignored) {}
        if (requestCode == REQUEST_SAVE) writeVnproj(uri);
        else if (requestCode == REQUEST_EXPORT) writeJson(uri);
        else if (requestCode == REQUEST_IMPORT) loadJsonUri(uri);
        else loadUri(uri);
    }

    private void loadUri(Uri uri) {
        try {
            String type = getContentResolver().getType(uri);
            String name = uri.toString().toLowerCase(Locale.ROOT);
            boolean isJson = (type != null && (type.contains("json") || type.contains("text"))) || name.contains(".json");
            project = isJson ? ProjectFileStore.loadJson(getContentResolver(), uri) : ProjectFileStore.loadVnproj(getContentResolver(), uri);
            currentUri = uri;
            restoreAllFields();
            toast("Project opened");
        } catch (Exception ex) { toast("Could not open project: " + ex.getMessage()); }
    }

    private void loadJsonUri(Uri uri) {
        try {
            project = ProjectFileStore.loadJson(getContentResolver(), uri);
            currentUri = null;
            restoreAllFields();
            toast("JSON imported");
        } catch (Exception ex) { toast("Could not import JSON: " + ex.getMessage()); }
    }

    private void writeVnproj(Uri uri) { try { ProjectFileStore.saveVnproj(getContentResolver(), uri, project); currentUri = uri; toast("Project saved"); } catch (Exception ex) { toast("Could not save project: " + ex.getMessage()); } }
    private void writeJson(Uri uri) { try { ProjectFileStore.saveJson(getContentResolver(), uri, project); toast("JSON exported"); } catch (Exception ex) { toast("Could not export JSON: " + ex.getMessage()); } }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setMinHeight(dp(48));
        b.setMinWidth(dp(88));
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setAllCaps(false);
        return b;
    }

    private void addToolbarButton(LinearLayout bar, Button button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(0, 0, dp(4), 0);
        bar.addView(button, params);
    }
    private int dp(int px) { return Math.round(px * getResources().getDisplayMetrics().density); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }
    private String labelFor(String name) { StringBuilder b = new StringBuilder(); for (char c : name.toCharArray()) { if (Character.isUpperCase(c)) b.append(' '); b.append(c); } return Character.toUpperCase(b.charAt(0)) + b.substring(1); }
}
