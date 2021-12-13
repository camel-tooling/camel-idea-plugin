package com.github.cameltooling.idea.language;


import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;

public class SimpleLanguage extends Language {

    public static final String LANGUAGE_ID = "simple";
    private static SimpleLanguage instance = new SimpleLanguage();

    protected SimpleLanguage() {
        super(LANGUAGE_ID);
    }

    public static SimpleLanguage getInstance() {
        return instance;
    }

    @Override
    public @NotNull @NlsSafe String getDisplayName() {
        return "Simple";
    }

    @Override
    public LanguageFileType getAssociatedFileType() {
        //TODO Do we need file type?
        return PlainTextFileType.INSTANCE;
    }
}
