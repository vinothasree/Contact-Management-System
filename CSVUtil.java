package com.example.contactmanagement.util;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/** Small RFC 4180-style CSV reader and writer for the application's fixed contact schema. */
public final class CSVUtil {
    private CSVUtil() { }

    public static List<List<String>> readAll(Reader source) throws IOException {
        PushbackReader reader = new PushbackReader(source, 1);
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean afterQuote = false;
        boolean recordTouched = false;
        int value;

        while ((value = reader.read()) != -1) {
            char character = (char) value;
            if (quoted) {
                if (character == '"') {
                    int next = reader.read();
                    if (next == '"') field.append('"');
                    else {
                        quoted = false;
                        afterQuote = true;
                        if (next != -1) reader.unread(next);
                    }
                } else field.append(character);
                continue;
            }
            if (afterQuote) {
                if (character == ',') {
                    fields.add(field.toString()); field.setLength(0); afterQuote = false; recordTouched = true;
                } else if (character == '\r' || character == '\n') {
                    fields.add(field.toString());
                    records.add(List.copyOf(fields));
                    fields.clear(); field.setLength(0); afterQuote = false; recordTouched = false;
                    consumeLineFeed(reader, character);
                } else throw new IOException("Unexpected character after a quoted CSV field");
                continue;
            }
            if (character == '"') {
                if (field.length() != 0) throw new IOException("Quote found inside an unquoted CSV field");
                quoted = true; recordTouched = true;
            } else if (character == ',') {
                fields.add(field.toString()); field.setLength(0); recordTouched = true;
            } else if (character == '\r' || character == '\n') {
                fields.add(field.toString());
                records.add(List.copyOf(fields));
                fields.clear(); field.setLength(0); recordTouched = false;
                consumeLineFeed(reader, character);
            } else {
                field.append(character); recordTouched = true;
            }
        }

        if (quoted) throw new IOException("Unterminated quoted CSV field");
        if (afterQuote || recordTouched || !fields.isEmpty()) {
            fields.add(field.toString());
            records.add(List.copyOf(fields));
        }
        return List.copyOf(records);
    }

    public static void writeRecord(Writer writer, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) writer.write(',');
            String value = values.get(index);
            if (value == null) value = "";
            boolean quote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
            if (quote) writer.write('"');
            for (int charIndex = 0; charIndex < value.length(); charIndex++) {
                char character = value.charAt(charIndex);
                if (character == '"') writer.write('"');
                writer.write(character);
            }
            if (quote) writer.write('"');
        }
        writer.write("\r\n");
    }

    private static void consumeLineFeed(PushbackReader reader, char character) throws IOException {
        if (character == '\r') {
            int next = reader.read();
            if (next != -1 && next != '\n') reader.unread(next);
        }
    }
}
