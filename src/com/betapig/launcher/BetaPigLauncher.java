package com.betapig.launcher;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import com.betapig.launcher.auth.AuthManager;
import com.betapig.launcher.server.ServerStatus;
import com.betapig.launcher.settings.LauncherSettings;
import com.betapig.launcher.ui.SettingsDialog;

public class BetaPigLauncher extends JFrame {
    // Dark mode color palette
    private static final Color BACKGROUND_DARK_GRAY = new Color(32, 33, 36);
    private static final Color PANEL_DARK_GRAY = new Color(45, 47, 51);
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color ACCENT_COLOR = new Color(76, 175, 80);
    private static final Color BORDER_COLOR = new Color(70, 70, 70);

    private JTextField usernameField;
    private JButton launchButton;
    private JLabel newsLabel;

    private JPanel centerPanel;
    private JPanel newsPanel;
    private JPanel serverStatusPanel;

    private ServerStatus serverStatus;
    private Timer statusTimer;
    private JLabel statusIconLabel;
    private JButton refreshButton;
    private static final ImageIcon ONLINE_ICON = createColorIcon(Color.GREEN);
    private static final ImageIcon OFFLINE_ICON = createColorIcon(Color.RED);
    private static final String SETTINGS_FILE = System.getProperty("user.home") + File.separator + ".betapig" + File.separator + "launcher.properties";

    private static ImageIcon createColorIcon(Color color) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(0, 0, 15, 15);
        g2.dispose();
        return new ImageIcon(image);
    }

    // Custom dark mode scroll bar UI
    private class DarkScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(70, 70, 70);
            thumbDarkShadowColor = new Color(50, 50, 50);
            thumbLightShadowColor = new Color(90, 90, 90);
            trackColor = new Color(45, 45, 45);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
    }

    public BetaPigLauncher() {
        // Set dark theme for entire application
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            UIManager.put("control", BACKGROUND_DARK_GRAY);
            UIManager.put("text", TEXT_COLOR);
            UIManager.put("nimbusBase", BACKGROUND_DARK_GRAY);
            UIManager.put("nimbusAlertYellow", ACCENT_COLOR);
            UIManager.put("nimbusDisabledText", new Color(128, 128, 128));
            UIManager.put("nimbusFocus", ACCENT_COLOR);
            UIManager.put("nimbusGreen", ACCENT_COLOR);
            UIManager.put("nimbusInfoBlue", ACCENT_COLOR);
            UIManager.put("nimbusLightBackground", PANEL_DARK_GRAY);
            UIManager.put("nimbusSelectionBackground", ACCENT_COLOR);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Frame setup
        getContentPane().setBackground(BACKGROUND_DARK_GRAY);
        setSize(1000, 500);
        setLocationRelativeTo(null);

        // Set up the main layout
        setLayout(new BorderLayout(10, 10));

        // Left panel for username
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(BACKGROUND_DARK_GRAY);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Username section
        JPanel usernamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        usernamePanel.setBackground(BACKGROUND_DARK_GRAY);

        // Dark mode label
        JLabel nickLabel = new JLabel("Ник");
        nickLabel.setForeground(TEXT_COLOR);
        usernamePanel.add(nickLabel);

        // Dark mode text field
        usernameField = new JTextField(15);
        usernameField.setText(loadLastUsername());
        usernameField.setBackground(PANEL_DARK_GRAY);
        usernameField.setForeground(TEXT_COLOR);
        usernameField.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        usernamePanel.add(usernameField);
        leftPanel.add(usernamePanel);

        // Add username save on change
        usernameField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { saveUsername(); }
            public void removeUpdate(DocumentEvent e) { saveUsername(); }
            public void insertUpdate(DocumentEvent e) { saveUsername(); }
        });

        add(leftPanel, BorderLayout.WEST);

        // Initialize centerPanel and newsPanel
        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(BACKGROUND_DARK_GRAY);

        // News panel
        newsPanel = new JPanel(new BorderLayout());
        newsPanel.setBackground(BACKGROUND_DARK_GRAY);
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                GameManager.SERVER_NAME + " - " + GameManager.SERVER_VERSION
        );
        titledBorder.setTitleColor(TEXT_COLOR);
        newsPanel.setBorder(titledBorder);

        StringBuilder newsBuilder = new StringBuilder()
                .append("<html><body style='width: 300px; padding: 5px; color: " + getHexColor(TEXT_COLOR) + ";'>")
                .append("<h2 style='color: " + getHexColor(ACCENT_COLOR) + "; text-align: center;'>Добро пожаловать на BetaPig!</h2>")
                .append("<hr>")
                .append("<p><b>🎮 Версия сервера:</b> Beta 1.7.3</p>")
                .append("<p><b>🌟 Особенности:</b></p>")
                .append("<ul>")
                .append("<li>Классический Minecraft без модификаций</li>")
                .append("<li>Дружелюбное сообщество</li>")
                .append("<li>Регулярные обновления</li>")
                .append("</ul>")
                .append("<p><b>📢 Последние новости:</b></p>")
                .append("<ul>")
                .append("<li>Оптимизирован лаунчер</li>")
                .append("<li>Улучшена стабильность сервера</li>")
                .append("<li>Добавлены настройки памяти</li>")
                .append("</ul>")
                .append("<p style='color: #666; font-style: italic; text-align: center;'>Присоединяйтесь к нам в Discord!</p>")
                .append("</body></html>");

        newsLabel = new JLabel(newsBuilder.toString());
        newsLabel.setVerticalAlignment(JLabel.TOP);

        JScrollPane newsScroll = new JScrollPane(newsLabel);
        newsScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        newsScroll.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        newsScroll.setBackground(PANEL_DARK_GRAY);
        newsScroll.getViewport().setBackground(PANEL_DARK_GRAY);
        newsPanel.add(newsScroll, BorderLayout.CENTER);

        centerPanel.add(newsPanel, BorderLayout.CENTER);

        // Server status panel
        serverStatus = new ServerStatus(GameManager.SERVER_ADDRESS, GameManager.SERVER_PORT);
        serverStatusPanel = createServerStatusPanel();
        serverStatusPanel.setBackground(BACKGROUND_DARK_GRAY);
        JPanel statusContainer = new JPanel(new BorderLayout(5, 5));
        statusContainer.setBackground(BACKGROUND_DARK_GRAY);
        statusContainer.add(serverStatusPanel, BorderLayout.CENTER);

        // Dark mode refresh button
        refreshButton = new JButton("Обновить ↻");
        refreshButton.setFocusPainted(false);
        refreshButton.setBackground(PANEL_DARK_GRAY);
        refreshButton.setForeground(TEXT_COLOR);
        refreshButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add hover effect
        refreshButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (refreshButton.isEnabled()) {
                    refreshButton.setBackground(new Color(60, 62, 66));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (refreshButton.isEnabled()) {
                    refreshButton.setBackground(PANEL_DARK_GRAY);
                }
            }
        });
        refreshButton.addActionListener(e -> refreshServerStatus());

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        refreshPanel.setBackground(BACKGROUND_DARK_GRAY);
        refreshPanel.add(refreshButton);
        statusContainer.add(refreshPanel, BorderLayout.SOUTH);

        centerPanel.add(statusContainer, BorderLayout.EAST);

        add(centerPanel, BorderLayout.CENTER);
        refreshButton.setVisible(false);

        // Buttons: Mods, Settings, Launch
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BACKGROUND_DARK_GRAY);

        // Mods button
        JButton modsButton = createDarkModeButton("Моды 📦");
        modsButton.addActionListener(e -> showModList());
        buttonPanel.add(modsButton);

        // Settings button
        JButton settingsButton = createDarkModeButton("Настройки ⚙");
        settingsButton.addActionListener(e -> {
            LauncherSettings settings = new LauncherSettings();
            SettingsDialog dialog = new SettingsDialog(this, settings);
            dialog.setVisible(true);
        });
        buttonPanel.add(settingsButton);

        // Launch button with dark mode gradient
        launchButton = new JButton("ИГРАТЬ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Create gradient paint
                GradientPaint gradient;
                if (getModel().isPressed()) {
                    gradient = new GradientPaint(
                            0, 0, new Color(46, 125, 50),
                            0, getHeight(), new Color(27, 94, 32));
                } else if (getModel().isRollover()) {
                    gradient = new GradientPaint(
                            0, 0, new Color(67, 160, 71),
                            0, getHeight(), new Color(56, 142, 60));
                } else {
                    gradient = new GradientPaint(
                            0, 0, new Color(76, 175, 80),
                            0, getHeight(), new Color(67, 160, 71));
                }

                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Add subtle shine effect
                g2.setPaint(new GradientPaint(
                        0, 0, new Color(255, 255, 255, 50),
                        0, getHeight()/2, new Color(255, 255, 255, 0)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Draw text with shadow
                g2.setFont(new Font("Dialog", Font.BOLD, 20));
                FontMetrics fm = g2.getFontMetrics();
                Rectangle2D r = fm.getStringBounds(getText(), g2);

                // Ensure text fits within button with padding
                int padding = 30; // 15px padding on each side
                double textWidth = r.getWidth();
                double availableWidth = getWidth() - (padding * 2);
                double scale = Math.min(1.0, availableWidth / textWidth);

                // Calculate centered position
                int x = (int)((getWidth() - (r.getWidth() * scale)) / 2);
                int y = (int)((getHeight() - (r.getHeight() * scale)) / 2 + fm.getAscent() * scale);

                // Apply scaling if needed
                if (scale < 1.0) {
                    g2.scale(scale, scale);
                    x = (int)(x / scale);
                    y = (int)(y / scale);
                }

                // Draw text with shadow
                g2.setColor(new Color(0, 0, 0, 50));
                g2.drawString(getText(), x+1, y+1);
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), x, y);

                g2.dispose();
            }
        };

        // Configure launch button properties
        launchButton.setFont(new Font("Dialog", Font.BOLD, 20));
        launchButton.setForeground(Color.WHITE);
        launchButton.setFocusPainted(false);
        launchButton.setBorderPainted(false);
        launchButton.setContentAreaFilled(false);
        launchButton.setPreferredSize(new Dimension(400, 50));
        launchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add subtle pulse animation when enabled
        Timer pulseTimer = new Timer(2000, new ActionListener() {
            private float scale = 1.0f;
            private boolean growing = true;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (launchButton.isEnabled()) {
                    if (growing) {
                        scale += 0.02f;
                        if (scale >= 1.05f) {
                            growing = false;
                        }
                    } else {
                        scale -= 0.02f;
                        if (scale <= 1.0f) {
                            growing = true;
                        }
                    }

                    launchButton.setSize((int)(launchButton.getPreferredSize().width * scale),
                            (int)(launchButton.getPreferredSize().height * scale));
                    launchButton.revalidate();
                }
            }
        });
        pulseTimer.start();

        // Add launch button to panel
        buttonPanel.add(launchButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add action listeners
        launchButton.addActionListener(e -> launchGame());

        // Initial status check and start timer
        refreshServerStatus();
        statusTimer = new Timer(30000, e -> refreshServerStatus());
        statusTimer.start();

        // Set minimum size
        setMinimumSize(new Dimension(800, 500));
    }

    // Helper method to create dark mode buttons
    private JButton createDarkModeButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(PANEL_DARK_GRAY);
        button.setForeground(TEXT_COLOR);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(60, 62, 66));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(PANEL_DARK_GRAY);
            }
        });

        return button;
    }

    // Convert Color to hex string for HTML
    private String getHexColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void launchGame() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Пожалуйста, введите имя пользователя",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        launchButton.setEnabled(false);
        GameManager.getInstance().downloadAndLaunchGame(username, new GameManager.ProgressCallback() {
            @Override
            public void onProgress(String status, int progress) {
                SwingUtilities.invokeLater(() -> {
                    launchButton.setText(status + " (" + progress + "%)");
                    if (progress == 100) {
                        dispose();
                    }
                });
            }
        });
    }

    @Override
    public void dispose() {
        if (statusTimer != null) {
            statusTimer.stop();
        }
        super.dispose();
    }

    private void updateServerStatus() {
        // Run server query in a background thread
        Thread queryThread = new Thread(() -> {
            try {
                serverStatus.queryServer();
                boolean isOnline = serverStatus.isOnline();
                int playerCount = serverStatus.getPlayerCount();
                int maxPlayers = serverStatus.getMaxPlayers();
                long ping = serverStatus.getPing();

                SwingUtilities.invokeLater(() -> {
                    // Update status indicators
                    String statusText = isOnline ? serverStatus.getStatusMessage() : "Оффлайн";
                    if (!isOnline && serverStatus.getStatusMessage() != null) {
                        statusText = serverStatus.getStatusMessage();
                    }
                    updateLabel("status", statusText);
                    updateLabel("players", String.format("Игроки: %d/%d", playerCount, maxPlayers));
                    updateLabel("ping", String.format("Пинг: %dms", ping));

                    // Update status icon and color
                    statusIconLabel.setIcon(isOnline ? ONLINE_ICON : OFFLINE_ICON);
                    Component foundStatusLabel = findLabelByName("status");
                    if (foundStatusLabel instanceof JLabel) {
                        foundStatusLabel.setForeground(isOnline ? new Color(46, 204, 113) : new Color(231, 76, 60));
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    updateLabel("status", "Ошибка: " + e.getMessage());
                    statusIconLabel.setIcon(OFFLINE_ICON);
                    Component foundStatusLabel = findLabelByName("status");
                    if (foundStatusLabel instanceof JLabel) {
                        foundStatusLabel.setForeground(new Color(231, 76, 60));
                    }
                });
            }
        });
        queryThread.setDaemon(true);
        queryThread.start();
    }

    private void refreshServerStatus() {
        if (refreshButton != null) {
            refreshButton.setEnabled(false);
        }
        updateLabel("status", "Проверка..."); // "Checking..."
        updateServerStatus();
        if (refreshButton != null) {
            refreshButton.setEnabled(true);
        }
    }

    private void updateLabel(String name, String text) {
        Component label = findLabelByName(name);
        if (label instanceof JLabel) {
            ((JLabel) label).setText(text);
        }
    }

    private Component findLabelByName(String name) {
        for (Component comp : serverStatusPanel.getComponents()) {
            if (comp instanceof JPanel) {
                for (Component panelComp : ((JPanel) comp).getComponents()) {
                    if (panelComp instanceof JLabel && name.equals(panelComp.getName())) {
                        return panelComp;
                    }
                }
            }
        }
        return null;
    }

    private JPanel createServerStatusPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_DARK_GRAY);

        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                "Статус сервера (WIP)"
        );
        titledBorder.setTitleColor(TEXT_COLOR);
        panel.setBorder(titledBorder);
        panel.setPreferredSize(new Dimension(150, 150));

        // Server status panel with refresh button
        serverStatusPanel = new JPanel(new BorderLayout());
        serverStatusPanel.setBackground(BACKGROUND_DARK_GRAY);
        serverStatusPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Status content panel
        JPanel statusContent = new JPanel(new GridLayout(3, 1, 0, 5));
        statusContent.setBackground(BACKGROUND_DARK_GRAY);

        // Status row
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusRow.setBackground(BACKGROUND_DARK_GRAY);
        statusIconLabel = new JLabel(OFFLINE_ICON);
        JLabel mainStatusLabel = new JLabel("Проверка...");
        mainStatusLabel.setName("status");
        mainStatusLabel.setFont(mainStatusLabel.getFont().deriveFont(Font.BOLD));
        mainStatusLabel.setForeground(TEXT_COLOR);
        statusRow.add(statusIconLabel);
        statusRow.add(mainStatusLabel);

        // Players row
        JPanel playersRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        playersRow.setBackground(BACKGROUND_DARK_GRAY);
        JLabel playersLabel = new JLabel("Игроки: 0/0");
        playersLabel.setName("players");
        playersLabel.setForeground(TEXT_COLOR);
        playersRow.add(playersLabel);

        // Ping row
        JPanel pingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pingRow.setBackground(BACKGROUND_DARK_GRAY);
        JLabel pingLabel = new JLabel("Пинг: -");
        pingLabel.setName("ping");
        pingLabel.setForeground(TEXT_COLOR);
        pingRow.add(pingLabel);

        statusContent.add(statusRow);
        statusContent.add(playersRow);
        statusContent.add(pingRow);

        // Add refresh button to the right
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BACKGROUND_DARK_GRAY);
        refreshButton = new JButton("↻"); // Unicode refresh symbol
        refreshButton.setToolTipText("Обновить статус");
        refreshButton.setFont(refreshButton.getFont().deriveFont(16f));
        refreshButton.setBorderPainted(false);
        refreshButton.setContentAreaFilled(false);
        refreshButton.setFocusPainted(false);
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.setForeground(new Color(150, 150, 150));
        refreshButton.addActionListener(e -> refreshServerStatus());
        buttonPanel.add(refreshButton);

        serverStatusPanel.add(statusContent, BorderLayout.CENTER);
        serverStatusPanel.add(buttonPanel, BorderLayout.EAST);


        // Add server address
        JLabel addressLabel = new JLabel(String.format("%s:%d", GameManager.SERVER_ADDRESS, GameManager.SERVER_PORT));
        addressLabel.setForeground(new Color(100, 100, 100));
        addressLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        panel.add(serverStatusPanel);
        serverStatusPanel.setVisible(false);
        return panel;
    }

    private String loadLastUsername() {
        try {
            File file = new File(SETTINGS_FILE);
            if (file.exists()) {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                    return props.getProperty("username", "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void saveUsername() {
        try {
            Properties props = new Properties();
            props.setProperty("username", usernameField.getText());
            File settingsFile = new File(SETTINGS_FILE);
            settingsFile.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(settingsFile)) {
                props.store(out, "BetaPig Launcher Settings");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showModList() {
        GameManager gameManager = new GameManager();
        List<GameManager.ModInfo> mods = gameManager.getInstalledMods();

        JDialog dialog = new JDialog(this, "Установленные моды", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.getContentPane().setBackground(BACKGROUND_DARK_GRAY);

        // Create list model and JList
        DefaultListModel<GameManager.ModInfo> listModel = new DefaultListModel<>();
        mods.forEach(listModel::addElement);
        JList<GameManager.ModInfo> modList = new JList<>(listModel);
        modList.setBackground(PANEL_DARK_GRAY);
        modList.setForeground(TEXT_COLOR);
        modList.setSelectionBackground(ACCENT_COLOR);
        modList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                GameManager.ModInfo mod = (GameManager.ModInfo) value;
                String text = "<html><b style='color: " + getHexColor(TEXT_COLOR) + ";'>" + mod.getName() + "</b>";
                if (mod.getVersion() != null) {
                    text += " v" + mod.getVersion();
                }
                if (mod.getDescription() != null) {
                    text += "<br><i style='color: #999999;'>" + mod.getDescription() + "</i>";
                }
                text += "</html>";
                setText(text);
                return this;
            }
        });

        JScrollPane scrollPane = new JScrollPane(modList);
        scrollPane.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        scrollPane.setBackground(PANEL_DARK_GRAY);
        scrollPane.getViewport().setBackground(PANEL_DARK_GRAY);
        dialog.add(scrollPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BACKGROUND_DARK_GRAY);

        JButton openFolderButton = createDarkModeButton("Открыть папку");
        openFolderButton.addActionListener(e -> {
            try {
                File modsDir = new File(GameManager.MODS_DIR);
                if (!modsDir.exists()) {
                    modsDir.mkdirs();
                }
                Desktop.getDesktop().open(modsDir);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Не удалось открыть папку с модами: " + ex.getMessage(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton refreshButton = createDarkModeButton("Обновить");
        refreshButton.addActionListener(e -> {
            listModel.clear();
            gameManager.getInstalledMods().forEach(listModel::addElement);
        });

        JButton closeButton = createDarkModeButton("Закрыть");
        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(openFolderButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        // Set system look and feel
        try {
            // Set a cross-platform look and feel with dark theme customization
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

            // Dark mode customization for Swing components
            UIManager.put("Panel.background", BACKGROUND_DARK_GRAY);
            UIManager.put("List.background", PANEL_DARK_GRAY);
            UIManager.put("List.foreground", TEXT_COLOR);
            UIManager.put("List.selectionBackground", ACCENT_COLOR);
            UIManager.put("ScrollPane.background", PANEL_DARK_GRAY);
            UIManager.put("ScrollPane.foreground", TEXT_COLOR);
            UIManager.put("TextField.background", PANEL_DARK_GRAY);
            UIManager.put("TextField.foreground", TEXT_COLOR);
            UIManager.put("TextField.caretForeground", TEXT_COLOR);
            UIManager.put("Button.background", PANEL_DARK_GRAY);
            UIManager.put("Button.foreground", TEXT_COLOR);
            UIManager.put("Label.foreground", TEXT_COLOR);
            UIManager.put("OptionPane.background", BACKGROUND_DARK_GRAY);
            UIManager.put("OptionPane.messageForeground", TEXT_COLOR);
            UIManager.put("TitledBorder.titleColor", TEXT_COLOR);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Enable anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Launch the application on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            BetaPigLauncher launcher = new BetaPigLauncher();
            launcher.setVisible(true);
        });
    }
}