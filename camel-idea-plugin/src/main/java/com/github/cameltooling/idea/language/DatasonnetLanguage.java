package com.github.cameltooling.idea.language;


import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;

public class DatasonnetLanguage extends Language {

    public static final String LANGUAGE_ID = "datasonnet";
    private static DatasonnetLanguage instance = new DatasonnetLanguage();

    protected DatasonnetLanguage() {
        super(LANGUAGE_ID);
    }

    public static DatasonnetLanguage getInstance() {
        return instance;
    }

    @Override
    public @NotNull @NlsSafe String getDisplayName() {
        return "DataSonnet";
    }

    @Override
    public LanguageFileType getAssociatedFileType() {
        //TODO Do we need file type?
        return PlainTextFileType.INSTANCE;
    }

}
