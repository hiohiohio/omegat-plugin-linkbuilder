/*
 * Copyright 2014 Chihiro Hio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//
// For Regular Expression for URL validation:
//
// https://gist.github.com/dperini/729294
//
// Regular Expression for URL validation
//
// Author: Diego Perini
// Updated: 2010/12/05
// License: MIT
//
// Copyright (c) 2010-2013 Diego Perini (http://www.iport.it)
//
// Permission is hereby granted, free of charge, to any person
// obtaining a copy of this software and associated documentation
// files (the "Software"), to deal in the Software without
// restriction, including without limitation the rights to use,
// copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following
// conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
// OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
// HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
// WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.
package com.hiohiohio.omegat.plugin.linkbuilder.attributeinserter;

import com.hiohiohio.omegat.plugin.linkbuilder.util.DefaultLogger;
import com.hiohiohio.omegat.plugin.linkbuilder.util.ILogger;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class JTextPaneAttributeInserter implements IAttributeInserter {

    private static final String ATTR_LINK = "linkbuilder_link";

    private final JTextPane jTextPane;
    private final ILogger logger;

    public JTextPaneAttributeInserter(final JTextPane pane) {
        this.jTextPane = pane;
        this.logger = new DefaultLogger();
    }

    public JTextPaneAttributeInserter(final JTextPane pane, final ILogger logger) {
        this.jTextPane = pane;
        this.logger = logger;
    }

    @Override
    public void register() {
        final MouseAdapter mouseAdapter = new AttributeInserterMouseListener(jTextPane);

        // Adding mouse listner for actions
        jTextPane.addMouseListener(mouseAdapter);

        // settings for mouseover (changing cursor)
        jTextPane.addMouseMotionListener(mouseAdapter);

        // Those are the main called points from user's activities.
        setDocumentFilter(jTextPane, logger);

        jTextPane.addPropertyChangeListener("document", new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                logger.log("document changed event fired: " + evt.getPropertyName());
                Object source = evt.getSource();
                if (source instanceof JTextPane) {
                    setDocumentFilter((JTextPane) source, logger);
                }
            }
        });
    }

    private static void setDocumentFilter(final JTextPane textPane, ILogger logger) {
        final StyledDocument doc = textPane.getStyledDocument();
        if (doc instanceof AbstractDocument) {
            final AbstractDocument abstractDocument = (AbstractDocument) doc;
            abstractDocument.setDocumentFilter(new AttributeInserterDocumentFilter(doc, logger));
        }
    }

    private static class AttributeInserterMouseListener extends MouseAdapter {

        private final JTextPane jTextPane;

        public AttributeInserterMouseListener(final JTextPane jTextPane) {
            this.jTextPane = jTextPane;
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                final StyledDocument doc = jTextPane.getStyledDocument();
                final Element characterElement = doc.getCharacterElement(jTextPane.viewToModel(e.getPoint()));
                final AttributeSet as = characterElement.getAttributes();
                final Object attr = as.getAttribute(ATTR_LINK);
                if (attr instanceof IAttributeAction) {
                    final IAttributeAction la = (IAttributeAction) attr;
                    la.execute();
                }
            } else {
                super.mouseClicked(e);
            }
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            final StyledDocument doc = jTextPane.getStyledDocument();
            final Element characterElement = doc.getCharacterElement(jTextPane.viewToModel(e.getPoint()));
            final AttributeSet as = characterElement.getAttributes();
            final Object attr = as.getAttribute(ATTR_LINK);
            if (attr instanceof IAttributeAction) {
                jTextPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                jTextPane.setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    private static class AttributeInserterDocumentFilter extends DocumentFilter {

        private static final long REFRESH_DELAY = 2;

        private final ScheduledExecutorService executorService;
        private Future refreshTask;
        private final Runnable refreshRun;

        private final ILogger logger;

        // as default constructor
        public AttributeInserterDocumentFilter(final StyledDocument doc, final ILogger logger) {
            // refresh thread
            this.executorService = new ScheduledThreadPoolExecutor(1);
            this.refreshRun = new RefreshRunnable(doc, logger);
            this.logger = logger;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
//            logger.log("insertString offset: " + offset + " fb.doc.length: " + fb.getDocument().getLength() + " text: " + string);
            super.insertString(fb, offset, string, attr);

            if (attr != null && attr.isDefined(StyleConstants.ComposedTextAttribute)) {
                // ignore
            } else {
                refreshPane(0);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
//            logger.log("remove offset: " + offset + " length: " + length + " fb.doc.length: " + fb.getDocument().getLength());
            boolean refresh = true;
            final AttributeSet attr = ((StyledDocument) fb.getDocument()).getCharacterElement(offset).getAttributes();
            if (attr != null && attr.isDefined(StyleConstants.ComposedTextAttribute)) {
                refresh = false;
            }

            super.remove(fb, offset, length);

            if (refresh && length != 0 && fb.getDocument().getLength() != 0) {
                refreshPane(REFRESH_DELAY);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
//            logger.log("replace offset: " + offset + " length: " + length + " fb.doc.length: " + fb.getDocument().getLength() + " text: " + text);
            super.replace(fb, offset, length, text, attrs);

            if (fb.getDocument().getLength() != 0) {
                refreshPane(REFRESH_DELAY);
            }
        }

        private void refreshPane(final long delay) {
            if (refreshTask != null && !refreshTask.isDone()) {
//                // previous task doing something
//                if (refreshTask.cancel(false)) {
//                    logger.log("refresh REscheduled");
//                    // postpone the task because of user's actions
//                    refreshTask = executorService.schedule(refreshRun, delay, TimeUnit.SECONDS);
//                }
                return;
            }

//            logger.log("refresh scheduled");
            refreshTask = executorService.schedule(refreshRun, delay, TimeUnit.SECONDS);
        }

        private static class RefreshRunnable implements Runnable {

            // Regular Expression for URL validation
            private static final String REGEX_URL = "(?:(?:https?|ftp):\\/\\/)(?:\\S+(?::\\S*)?@)?(?:(?!(?:10|127)(?:\\.\\d{1,3}){3})(?!(?:169\\.254|192\\.168)(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,})))(?::\\d{2,5})?(?:\\/[^\\s]*)?";

            private final StyledDocument doc;
            private final ILogger logger;

            private final Pattern URLmather;
            private final MutableAttributeSet URLAttribute;
            private final AttributeSet defAttribute;
            private final AttributeSet boldAttribute;

            public RefreshRunnable(final StyledDocument doc, final ILogger logger) {
                this.doc = doc;
                this.logger = logger;

                // setting up the URL pattern
                this.URLmather = Pattern.compile(REGEX_URL, Pattern.CASE_INSENSITIVE);

                // setting up attributes
                this.URLAttribute = new SimpleAttributeSet();
                StyleConstants.setUnderline(this.URLAttribute, true);
                StyleConstants.setForeground(this.URLAttribute, Color.blue);

                this.defAttribute = new SimpleAttributeSet().copyAttributes();

                final MutableAttributeSet tmp = new SimpleAttributeSet();
                StyleConstants.setBold(tmp, true);
                this.boldAttribute = tmp.copyAttributes();
            }

            public void run() {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        public void run() {
                            final int docLength = doc.getLength();
                            if (docLength != 0) {
                                try {
                                    // clear attributes
                                    for (int i = 0; i < docLength; ++i) {
                                        if (doc.getCharacterElement(i).getAttributes().containsAttributes(URLAttribute)) {
                                            doc.setCharacterAttributes(i, 1, defAttribute, true);
                                        }
                                    }

                                    // URL detection
                                    final String text = doc.getText(0, docLength);
                                    final Matcher matcher = URLmather.matcher(text);
                                    while (matcher.find()) {
                                        final int offset = matcher.start();
                                        final int targetLength = matcher.end() - offset;

                                        if (!doc.getCharacterElement(offset).getAttributes().containsAttributes(boldAttribute)) {
                                            // transforming into Clickable text
                                            applyStyle(URLAttribute, doc, offset, targetLength, matcher.group());
                                        } else {
                                            // transforming into Clickable text
                                            applyStyle(URLAttribute, doc, offset, targetLength, matcher.group());
                                            doc.setCharacterAttributes(offset, targetLength, boldAttribute.copyAttributes(), false);
                                        }
                                    }

                                } catch (BadLocationException ex) {
                                    logger.log("LinkBuilder: " + JTextPaneAttributeInserter.class.getName() + ex);
                                }
                            }
                        }
                    });
                } catch (InterruptedException ex) {
                    logger.log(ex.toString());
                } catch (InvocationTargetException ex) {
                    logger.log(ex.toString());
                }
            }

            private static void applyStyle(final MutableAttributeSet URLAttribute, final StyledDocument targetDoc, final int offset, final int length, final String target) {
                URLAttribute.addAttribute(ATTR_LINK, new BrowserLaunchAction(target));
                targetDoc.setCharacterAttributes(offset, length, URLAttribute.copyAttributes(), true);
                URLAttribute.removeAttribute(ATTR_LINK);
            }
        }
    }
}
