package com.github.cameltooling.idea.debugger;

import com.github.cameltooling.idea.service.CamelPreferenceService;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CamelSpringBootRunConfigurationType implements ConfigurationType {

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Camel SpringBoot Application";
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) String getConfigurationTypeDescription() {
        return "Camel SpringBoot Application";
    }

    @Override
    public Icon getIcon() {
        return CamelPreferenceService.getService().getCamelIcon();
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "camel.springboot";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[0];
    }
}
