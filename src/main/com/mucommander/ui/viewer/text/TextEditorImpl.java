/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2010 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.ui.viewer.text;

import com.mucommander.ui.theme.*;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Text editor implementation used by {@link TextViewer} and {@link TextEditor}.
 *
 * @author Maxence Bernard, Mariusz Jakubowski, Nicolas Rinaudo, Arik Hadas
 */
class TextEditorImpl implements ThemeListener {

    private String searchString;

    private JFrame frame;
    private JTextArea textArea;


    ////////////////////
    // Initialization //
    ////////////////////
    
    public TextEditorImpl(boolean isEditable) {
        // Initialize text area
        initTextArea(isEditable);
        
        // Listen to theme changes to update the text area if it is visible
        ThemeManager.addCurrentThemeListener(this);
    }

    private void initTextArea(boolean isEditable) {
        textArea = new JTextArea() {
        	@Override
            public Insets getInsets() {
                return new Insets(4, 3, 4, 3);
            }
        };

        textArea.setEditable(isEditable);

        // Use theme colors and font
        textArea.setForeground(ThemeManager.getCurrentColor(Theme.EDITOR_FOREGROUND_COLOR));
        textArea.setCaretColor(ThemeManager.getCurrentColor(Theme.EDITOR_FOREGROUND_COLOR));
        textArea.setBackground(ThemeManager.getCurrentColor(Theme.EDITOR_BACKGROUND_COLOR));
        textArea.setSelectedTextColor(ThemeManager.getCurrentColor(Theme.EDITOR_SELECTED_FOREGROUND_COLOR));
        textArea.setSelectionColor(ThemeManager.getCurrentColor(Theme.EDITOR_SELECTED_BACKGROUND_COLOR));
        textArea.setFont(ThemeManager.getCurrentFont(Theme.EDITOR_FONT));
        
        textArea.setWrapStyleWord(true);
    }

    /////////////////
    // Search code //
    /////////////////
    
    void find() {
        FindDialog findDialog = new FindDialog(frame);

        if(findDialog.wasValidated()) {
            searchString = findDialog.getSearchString().toLowerCase();

            if(!searchString.equals(""))
                doSearch(0, true);
        }

        // Request the focus on the text area which could be lost after the Find dialog was disposed
        textArea.requestFocus();
	}

    void findNext() {
    	doSearch(textArea.getSelectionEnd(), true);
    }
    
    void findPrevious() {
    	doSearch(textArea.getSelectionStart() - 1, false);
	}

    private String getTextLC() {
    	return textArea.getText().toLowerCase();
    }

    private void doSearch(int startPos, boolean forward) {
    	if (searchString == null || searchString.length() == 0)
    		return;
		int pos;
		if (forward) {
			pos = getTextLC().indexOf(searchString, startPos);
		} else {
			pos = getTextLC().lastIndexOf(searchString, startPos);
		}
		if (pos >= 0) {
            textArea.select(pos, pos + searchString.length());
        } else {
            // Beep when no match has been found.
            // The beep method is called from a separate thread because this method seems to lock until the beep has
            // been played entirely. If the 'Find next' shortcut is left pressed, a series of beeps will be played when
            // the end of the file is reached, and we don't want those beeps to played one after the other as to:
            // 1/ not lock the event thread
            // 2/ have those beeps to end rather sooner than later
            new Thread() {
                @Override
                public void run() {
                    Toolkit.getDefaultToolkit().beep();
                }
            }.start();
        }
    }

    public boolean isWrap() {
    	return textArea.getLineWrap();
    }

    ////////////////////////////
    // Package-access methods //
    ////////////////////////////

    void wrap(boolean isWrap) {
    	textArea.setLineWrap(isWrap);
    	textArea.repaint();
    }
    
    void copy() {
    	textArea.copy();
    }
    
    void cut() {
    	textArea.cut();
    }
    
    void paste() {
    	textArea.paste();
    }
    
    void selectAll() {
    	textArea.selectAll();
    }
    
    void requestFocus() {
        textArea.requestFocus();
    }

    JTextArea getTextArea() {
        return textArea;
    }

    void addDocumentListener(DocumentListener documentListener) {
    	textArea.getDocument().addDocumentListener(documentListener);
    }
    
    void read(Reader reader) throws IOException {
    	// Feed the file's contents to text area
        textArea.read(reader, null);
        
        // Move cursor to the top
        textArea.setCaretPosition(0);
    }
    
    void write(Writer writer) throws IOException {
    	Document document = textArea.getDocument();
    	
    	try {
    		textArea.getUI().getEditorKit(textArea).write(new BufferedWriter(writer), document, 0, document.getLength());
    	}
        catch(BadLocationException e) {
        	throw new IOException(e.getMessage());
        }
    }

    //////////////////////////////////
    // ThemeListener implementation //
    //////////////////////////////////

    /**
     * Receives theme color changes notifications.
     */
    public void colorChanged(ColorChangedEvent event) {
        switch(event.getColorId()) {
        case Theme.EDITOR_FOREGROUND_COLOR:
            textArea.setForeground(event.getColor());
            break;

        case Theme.EDITOR_BACKGROUND_COLOR:
            textArea.setBackground(event.getColor());
            break;

        case Theme.EDITOR_SELECTED_FOREGROUND_COLOR:
            textArea.setSelectedTextColor(event.getColor());
            break;

        case Theme.EDITOR_SELECTED_BACKGROUND_COLOR:
            textArea.setSelectionColor(event.getColor());
            break;
        }
    }

    /**
     * Receives theme font changes notifications.
     */
    public void fontChanged(FontChangedEvent event) {
        if(event.getFontId() == Theme.EDITOR_FONT)
            textArea.setFont(event.getFont());
    }
}
