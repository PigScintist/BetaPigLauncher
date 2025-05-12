package com.betapig.launcher.ui;

import com.betapig.launcher.settings.LauncherSettings;
import javax.swing.*;
import java.awt.*;

public class SettingsDialog extends JDialog {
    public SettingsDialog(JFrame parent, LauncherSettings settings) {
        super(parent, "Настройки", true);
        setLayout(new BorderLayout());
        setSize(400, 400);
        setLocationRelativeTo(parent);
        
        // Create settings panel
        add(settings.createSettingsPanel(), BorderLayout.CENTER);
        
        // Close button
        JButton closeButton = new JButton("Закрыть");
        closeButton.addActionListener(e -> dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
}
