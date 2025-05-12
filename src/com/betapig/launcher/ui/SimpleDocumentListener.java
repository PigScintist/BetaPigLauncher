package com.betapig.launcher.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class SimpleDocumentListener implements DocumentListener {
    private final DocumentUpdate update;
    
    public SimpleDocumentListener(DocumentUpdate update) {
        this.update = update;
    }
    
    @Override
    public void insertUpdate(DocumentEvent e) {
        update.update(e);
    }
    
    @Override
    public void removeUpdate(DocumentEvent e) {
        update.update(e);
    }
    
    @Override
    public void changedUpdate(DocumentEvent e) {
        update.update(e);
    }
}
