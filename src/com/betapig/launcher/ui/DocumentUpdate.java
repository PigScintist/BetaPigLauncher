package com.betapig.launcher.ui;

import javax.swing.event.DocumentEvent;

@FunctionalInterface
public interface DocumentUpdate {
    void update(DocumentEvent e);
}
