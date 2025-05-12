package com.betapig.launcher.settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Properties;
import com.betapig.launcher.ui.SimpleDocumentListener;

public class LauncherSettings {
    private static final String SETTINGS_FILE = System.getProperty("user.home") + File.separator + ".betapig" + File.separator + "settings.properties";
    
    // Memory settings
    private static final String MEMORY_PRESET = "memory.preset";
    private static final String CUSTOM_MEMORY = "memory.custom";
    
    // Default values
    private static final String DEFAULT_MEMORY_PRESET = "MEDIUM";
    private static final String DEFAULT_CUSTOM_MEMORY = "1024";
    
    private Properties properties;
    
    public enum MemoryPreset {
        LOW("512", "Низкая"),
        MEDIUM("1024", "Средняя"),
        HIGH("2048", "Высокая"),
        CUSTOM("custom", "Пользовательская");
        
        private final String memoryMB;
        private final String displayName;
        
        MemoryPreset(String memoryMB, String displayName) {
            this.memoryMB = memoryMB;
            this.displayName = displayName;
        }
        
        public String getMemoryMB() {
            return memoryMB;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    public LauncherSettings() {
        properties = new Properties();
        load();
    }
    
    private void load() {
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading settings: " + e.getMessage());
                setDefaults();
            }
        } else {
            setDefaults();
        }
    }
    
    private void setDefaults() {
        properties.setProperty(MEMORY_PRESET, DEFAULT_MEMORY_PRESET);
        properties.setProperty(CUSTOM_MEMORY, DEFAULT_CUSTOM_MEMORY);
        save();
    }
    
    public void save() {
        File settingsFile = new File(SETTINGS_FILE);
        settingsFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(settingsFile)) {
            properties.store(fos, "BetaPig Launcher Settings");
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }
    
    public MemoryPreset getMemoryPreset() {
        String preset = properties.getProperty(MEMORY_PRESET, DEFAULT_MEMORY_PRESET);
        try {
            return MemoryPreset.valueOf(preset);
        } catch (IllegalArgumentException e) {
            return MemoryPreset.MEDIUM;
        }
    }
    
    public void setMemoryPreset(MemoryPreset preset) {
        properties.setProperty(MEMORY_PRESET, preset.name());
        save();
    }
    
    public String getCustomMemory() {
        return properties.getProperty(CUSTOM_MEMORY, DEFAULT_CUSTOM_MEMORY);
    }
    
    public void setCustomMemory(String memoryMB) {
        properties.setProperty(CUSTOM_MEMORY, memoryMB);
        save();
    }
    
    public String getEffectiveMemory() {
        MemoryPreset preset = getMemoryPreset();
        return preset == MemoryPreset.CUSTOM ? getCustomMemory() : preset.getMemoryMB();
    }
    
    public JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Memory preset selector
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Пресет памяти:"), gbc);
        
        JComboBox<MemoryPreset> memoryPresetCombo = new JComboBox<>(MemoryPreset.values());
        memoryPresetCombo.setSelectedItem(getMemoryPreset());
        
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(memoryPresetCombo, gbc);
        gbc.weightx = 0.0;
        
        // Custom memory input
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Пользовательская память (МБ):"), gbc);
        
        JTextField customMemoryField = new JTextField(getCustomMemory(), 10);
        customMemoryField.setEnabled(getMemoryPreset() == MemoryPreset.CUSTOM);
        
        gbc.gridx = 1;
        panel.add(customMemoryField, gbc);
        
        // Effective memory display
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Выделенная память:"), gbc);
        
        JLabel effectiveMemoryLabel = new JLabel(getEffectiveMemory() + " МБ");
        gbc.gridx = 1;
        panel.add(effectiveMemoryLabel, gbc);
        
        // Memory usage tips
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        StringBuilder tips = new StringBuilder();
        tips.append("Рекомендации по памяти:\n");
        for (MemoryPreset preset : MemoryPreset.values()) {
            String description;
            switch (preset) {
                case LOW:
                    description = "Для базовой игры";
                    break;
                case MEDIUM:
                    description = "Рекомендуется для большинства";
                    break;
                case HIGH:
                    description = "Для текстур и модов";
                    break;
                case CUSTOM:
                default:
                    description = "Установите свой лимит";
                    break;
            };
            if (preset != MemoryPreset.CUSTOM) {
                tips.append(String.format("- %s (%sМБ): %s\n", preset, preset.getMemoryMB(), description));
            } else {
                tips.append(String.format("- %s: %s\n", preset, description));
            }
        }
        JTextArea tipsArea = new JTextArea(tips.toString());
        tipsArea.setEditable(false);
        tipsArea.setBackground(panel.getBackground());
        tipsArea.setLineWrap(true);
        tipsArea.setWrapStyleWord(true);
        tipsArea.setMargin(new Insets(5, 5, 5, 5));
        panel.add(tipsArea, gbc);
        
        // Add some padding
        gbc.gridy = 4;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);
        
        // Update handlers
        memoryPresetCombo.addActionListener(e -> {
            MemoryPreset selected = (MemoryPreset) memoryPresetCombo.getSelectedItem();
            customMemoryField.setEnabled(selected == MemoryPreset.CUSTOM);
            updateEffectiveMemoryValue(selected, customMemoryField, effectiveMemoryLabel);
        });
        
        customMemoryField.getDocument().addDocumentListener(
            new SimpleDocumentListener(e -> updateEffectiveMemoryValue(
                (MemoryPreset) memoryPresetCombo.getSelectedItem(),
                customMemoryField,
                effectiveMemoryLabel
            ))
        );
        
        // Save button
        JButton saveButton = new JButton("Сохранить");
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(saveButton, gbc);
        
        saveButton.addActionListener(e -> {
            MemoryPreset selected = (MemoryPreset) memoryPresetCombo.getSelectedItem();
            setMemoryPreset(selected);
            if (selected == MemoryPreset.CUSTOM) {
                try {
                    int memory = Integer.parseInt(customMemoryField.getText());
                    if (memory >= 256 && memory <= 8192) {
                        setCustomMemory(String.valueOf(memory));
                    }
                } catch (NumberFormatException ex) {
                    // Keep existing value if invalid
                }
            }
            JOptionPane.showMessageDialog(panel, "Настройки сохранены", "Успех", JOptionPane.INFORMATION_MESSAGE);
        });
        
        return panel;
    }
    
    private void updateEffectiveMemoryValue(MemoryPreset selected, JTextField customField, JLabel effectiveLabel) {
        // Temporarily set the memory preset to update the effective memory
        MemoryPreset currentPreset = getMemoryPreset();
        String currentCustom = getCustomMemory();
        
        // Update properties temporarily
        setMemoryPreset(selected);
        if (selected == MemoryPreset.CUSTOM) {
            try {
                Integer.parseInt(customField.getText());
                setCustomMemory(customField.getText());
            } catch (NumberFormatException e) {
                // Keep existing custom value if invalid
            }
        }
        
        // Get the effective memory
        String memory = getEffectiveMemory();
        
        // Restore original values
        setMemoryPreset(currentPreset);
        setCustomMemory(currentCustom);
        
        // Update the label
        effectiveLabel.setText(memory + " МБ");
    }
}
