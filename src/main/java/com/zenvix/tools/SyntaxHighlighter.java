package com.zenvix.tools;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SyntaxHighlighter provides regular expression evaluation mapping CSS styling tokens 
 * for RichTextFX nodes dynamically analyzing properties, yaml, and xml components cleanly.
 */
public class SyntaxHighlighter {

    private static final Pattern PROPERTIES_PATTERN = Pattern.compile(
            "(?<COMMENT>^[#!].*)|(?<KEY>^[a-zA-Z0-9_.-]+)(?<SEPARATOR>\\s*[=:]\\s*)(?<VALUE>.*)", 
            Pattern.MULTILINE
    );

    private static final Pattern YAML_PATTERN = Pattern.compile(
            "(?<COMMENT>#.*)|(?<KEY>^[\\s]*[a-zA-Z0-9_.-]+\\s*:)|(?<STRING>\"[^\"]*\"|'[^']*')|(?<NUMBER>\\b\\d+\\.?\\d*\\b)|(?<BOOLEAN>\\b(true|false)\\b)", 
            Pattern.MULTILINE
    );

    private static final Pattern XML_PATTERN = Pattern.compile(
            "(?<COMMENT><!--[\\s\\S]*?-->)|(?<TAG><[/?]?[a-zA-Z0-9_:-]+)|(?<ATTRIBUTE>\\b[a-zA-Z0-9_:-]+\\b\\s*=)|(?<STRING>\"[^\"]*\")|(?<CLOSETAG>[/?]?>)", 
            Pattern.MULTILINE
    );

    public static StyleSpans<Collection<String>> computePropertiesHighlighting(String text) {
        Matcher matcher = PROPERTIES_PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            if (matcher.group("COMMENT") != null) {
                spansBuilder.add(Collections.singleton("comment"), matcher.end() - matcher.start());
            } else {
                spansBuilder.add(Collections.singleton("key"), matcher.group("KEY").length());
                spansBuilder.add(Collections.emptyList(), matcher.group("SEPARATOR").length());
                spansBuilder.add(Collections.singleton("value"), matcher.group("VALUE").length());
            }
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    public static StyleSpans<Collection<String>> computeYamlHighlighting(String text) {
        Matcher matcher = YAML_PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            if (matcher.group("COMMENT") != null) {
                spansBuilder.add(Collections.singleton("comment"), matcher.end() - matcher.start());
            } else if (matcher.group("KEY") != null) {
                spansBuilder.add(Collections.singleton("key"), matcher.end() - matcher.start());
            } else if (matcher.group("STRING") != null) {
                spansBuilder.add(Collections.singleton("string"), matcher.end() - matcher.start());
            } else if (matcher.group("NUMBER") != null) {
                spansBuilder.add(Collections.singleton("number"), matcher.end() - matcher.start());
            } else if (matcher.group("BOOLEAN") != null) {
                spansBuilder.add(Collections.singleton("boolean"), matcher.end() - matcher.start());
            }
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    public static StyleSpans<Collection<String>> computeXmlHighlighting(String text) {
        Matcher matcher = XML_PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            if (matcher.group("COMMENT") != null) {
                spansBuilder.add(Collections.singleton("comment"), matcher.end() - matcher.start());
            } else if (matcher.group("TAG") != null) {
                spansBuilder.add(Collections.singleton("tag"), matcher.end() - matcher.start());
            } else if (matcher.group("ATTRIBUTE") != null) {
                spansBuilder.add(Collections.singleton("attribute"), matcher.end() - matcher.start());
            } else if (matcher.group("STRING") != null) {
                spansBuilder.add(Collections.singleton("string"), matcher.end() - matcher.start());
            } else if (matcher.group("CLOSETAG") != null) {
                spansBuilder.add(Collections.singleton("tag"), matcher.end() - matcher.start());
            }
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
