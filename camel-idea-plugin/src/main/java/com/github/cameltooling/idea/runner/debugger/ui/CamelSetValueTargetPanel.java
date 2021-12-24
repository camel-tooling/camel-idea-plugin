package com.github.cameltooling.idea.runner.debugger.ui;

import com.intellij.openapi.ui.ComboBox;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CamelSetValueTargetPanel {
    private ComboBox<String> targetComboBox;
    private JTextField targetName;
    private JPanel myPanel;

    private void createUIComponents() {
        targetComboBox = new ComboBox(new String[]{"Message Header", "Exchange Property", "Body"});
        targetComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                targetName.setVisible(!("Body".equals(targetComboBox.getItem())));
            }
        });
    }

    public String getTargetType() {
        return targetComboBox.getItem();
    }

    public String getTargetName() {
        return "Body".equals(targetComboBox.getItem()) ? null : targetName.getText();
    }

    public JTextField getTargetNameComponent() {
        return targetName;
    }

    public JPanel getPanel() {
        return myPanel;
    }
}
