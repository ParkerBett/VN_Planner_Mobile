package com.vnplanner.mobile.io;

import android.content.ContentResolver;
import android.net.Uri;
import com.vnplanner.mobile.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ProjectFileStore {
    private ProjectFileStore() {}

    public static void saveVnproj(ContentResolver resolver, Uri destination, Project project) throws Exception {
        File temp = File.createTempFile("vnplanner-", ".vnproj");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(temp))) {
                zip.putNextEntry(new ZipEntry("project.json"));
                zip.write(toJson(project).toString(2).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            copyToUri(resolver, temp, destination);
        } finally { temp.delete(); }
    }

    public static void saveJson(ContentResolver resolver, Uri destination, Project project) throws Exception {
        try (OutputStream out = resolver.openOutputStream(destination, "wt")) {
            if (out == null) throw new IOException("Cannot open destination");
            out.write(toJson(project).toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }

    public static Project loadVnproj(ContentResolver resolver, Uri source) throws Exception {
        try (InputStream in = resolver.openInputStream(source); ZipInputStream zip = new ZipInputStream(in)) {
            if (in == null) throw new IOException("Cannot open source");
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("project.json".equals(entry.getName())) return fromJson(readText(zip));
            }
        }
        throw new IOException("project.json not found inside .vnproj");
    }

    public static Project loadJson(ContentResolver resolver, Uri source) throws Exception {
        try (InputStream in = resolver.openInputStream(source)) {
            if (in == null) throw new IOException("Cannot open source");
            return fromJson(readText(in));
        }
    }

    private static void copyToUri(ContentResolver resolver, File source, Uri target) throws IOException {
        try (InputStream in = new FileInputStream(source); OutputStream out = resolver.openOutputStream(target, "wt")) {
            if (out == null) throw new IOException("Cannot open destination");
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
        }
    }

    private static String readText(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int n;
        while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    public static JSONObject toJson(Project project) throws Exception {
        JSONObject json = new JSONObject();
        for (Field field : Project.class.getFields()) {
            Object value = field.get(project);
            if (value instanceof java.util.List) {
                JSONArray array = new JSONArray();
                for (Object item : (java.util.List<?>) value) array.put(itemToJson(item));
                json.put(field.getName(), array);
            } else json.put(field.getName(), value == null ? JSONObject.NULL : value);
        }
        return json;
    }

    private static JSONObject itemToJson(Object item) throws Exception {
        JSONObject json = new JSONObject();
        for (Field field : item.getClass().getFields()) {
            Object value = field.get(item);
            if (value instanceof java.util.List) {
                JSONArray array = new JSONArray();
                for (Object child : (java.util.List<?>) value) array.put(itemToJson(child));
                json.put(field.getName(), array);
            } else json.put(field.getName(), value == null ? JSONObject.NULL : value);
        }
        return json;
    }

    public static Project fromJson(String text) throws Exception {
        JSONObject json = new JSONObject(text);
        Project project = new Project();
        for (Field field : Project.class.getFields()) {
            if (!json.has(field.getName()) || json.isNull(field.getName())) continue;
            Object value = json.get(field.getName());
            if (field.getType() == String.class) field.set(project, value.toString());
            else if (java.util.List.class.isAssignableFrom(field.getType())) {
                JSONArray array = (JSONArray) value;
                java.util.List<Object> list = new java.util.ArrayList<>();
                Class<?> type = field.getName().equals("characters") ? CharacterData.class : field.getName().equals("chapters") ? Chapter.class : ChoiceNode.class;
                for (int i=0; i<array.length(); i++) list.add(itemFromJson(array.getJSONObject(i), type));
                field.set(project, list);
            }
        }
        return project;
    }

    private static Object itemFromJson(JSONObject json, Class<?> type) throws Exception {
        Object item = type.getDeclaredConstructor().newInstance();
        for (Field field : type.getFields()) {
            if (!json.has(field.getName()) || json.isNull(field.getName())) continue;
            Object value = json.get(field.getName());
            if (field.getType() == String.class) field.set(item, value.toString());
            else if (field.getType() == boolean.class) field.setBoolean(item, json.getBoolean(field.getName()));
            else if (java.util.List.class.isAssignableFrom(field.getType())) {
                JSONArray array = (JSONArray) value;
                java.util.List<ChoiceNode> children = new java.util.ArrayList<>();
                for (int i=0; i<array.length(); i++) children.add((ChoiceNode) itemFromJson(array.getJSONObject(i), ChoiceNode.class));
                field.set(item, children);
            }
        }
        return item;
    }
}
