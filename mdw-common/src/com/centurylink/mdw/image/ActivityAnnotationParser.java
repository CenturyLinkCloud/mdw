package com.centurylink.mdw.image;

import com.centurylink.mdw.model.workflow.ActivityImplementor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For annotation-based implementors.  Custom impl classes cannot be compiled, so this crude
 * parsing mechanism is used to determine image category, icon and pagelet.
 * Kotlin limitation: file name must be the same as impl class name.
 */
public class ActivityAnnotationParser {

    // ignore closing paren within strings (https://stackoverflow.com/a/6464500)
    private static final String ACTIVITY_REGEX = "@Activity\\s*\\((.*?\\)(?=([^\"\\\\]*(\\\\.|\"([^\"\\\\]*\\\\.)*[^\"\\\\]*\"))*[^\"]*$))";
    private static final Pattern ACTIVITY_ANNOTATION = Pattern.compile(ACTIVITY_REGEX, Pattern.DOTALL);
    private static final Pattern CATEGORY_VALUE = Pattern.compile("category\\s*=\\s*\"(.*?)\"");
    private static final Pattern ICON_VALUE = Pattern.compile("icon\\s*=\\s*\"(.*?)\"");
    private static final Pattern PAGELET_VALUE = Pattern.compile("pagelet\\s*=\\s*\"(.*?)\"");

    private final File rootDirectory;

    public ActivityAnnotationParser(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public ActivityImplementor parse(File sourceFile) throws IOException {
        String contents = new String(Files.readAllBytes(sourceFile.toPath()));

        Matcher matcher = ACTIVITY_ANNOTATION.matcher(contents);
        if (matcher.find()) {
            // only implClass and image are needed (other values can be hardcoded)
            String pkg = sourceFile.getParentFile().getAbsolutePath().substring(rootDirectory.getAbsolutePath().length() + 1).replace('/', '.');
            String implClassName = sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.'));
            String label = implClassName; // TODO label is a bogus placeholder
            String params = matcher.group(1);
            String category = "com.centurylink.mdw.activity.types.GeneralActivity";
            Matcher categoryMatcher = CATEGORY_VALUE.matcher(params);
            if (categoryMatcher.find()) {
                category = categoryMatcher.group(1);
            }
            String icon = "shape:activity";
            Matcher iconMatcher = ICON_VALUE.matcher(params);
            if (iconMatcher.find()) {
                icon = iconMatcher.group(1);
            }
            Matcher pageletMatcher = PAGELET_VALUE.matcher(params);
            String pagelet = "{}";
            if (pageletMatcher.find()) {
                pagelet = pageletMatcher.group(1);
            }
            return new ActivityImplementor(pkg + "." + implClassName, category, label, icon, pagelet);
        }
        return null;
    }

}
