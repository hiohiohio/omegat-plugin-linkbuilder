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
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class JTextPaneAttributeInserter implements IAttributeInserter {

    // Regular Expression for URL validation
    final static private String REGEX_URL = "(?:(?:https?|ftp):\\/\\/)(?:\\S+(?::\\S*)?@)?(?:(?!(?:10|127)(?:\\.\\d{1,3}){3})(?!(?:169\\.254|192\\.168)(?:\\.\\d{1,3}){2})(?!172\\.(?:1[6-9]|2\\d|3[0-1])(?:\\.\\d{1,3}){2})(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[1-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,})))(?::\\d{2,5})?(?:\\/[^\\s]*)?";

    final static private String ATTR_LINK = "linkbuilder_link";
    final private JTextPane jTextPane;
    final private StyledDocument doc;
    final private ILogger logger;

    public JTextPaneAttributeInserter(JTextPane pane) {
        this.jTextPane = pane;
        this.doc = pane.getStyledDocument();
        this.logger = new DefaultLogger();
    }

    public JTextPaneAttributeInserter(JTextPane pane, ILogger logger) {
        this.jTextPane = pane;
        this.doc = pane.getStyledDocument();
        this.logger = logger;
    }

    public void register() {
        // Adding mouse listner
        jTextPane.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    final Element characterElement = doc.getCharacterElement(jTextPane.viewToModel(e.getPoint()));
                    final AttributeSet as = characterElement.getAttributes();
                    final IAttributeAction la = (IAttributeAction) as.getAttribute(ATTR_LINK);
                    if (la != null) {
                        la.execute();
                    }
                } else {
                    super.mouseClicked(e);
                }
            }
        });

        // settings for mouseover (changing cursor)
        jTextPane.addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                final Element characterElement = doc.getCharacterElement(jTextPane.viewToModel(e.getPoint()));
                final AttributeSet as = characterElement.getAttributes();
                final IAttributeAction la = (IAttributeAction) as.getAttribute(ATTR_LINK);
                if (la != null) {
                    jTextPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    jTextPane.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        // Those are the main called points from user's activities.
        doc.addDocumentListener(new DocumentListener() {
            final private ScheduledExecutorService executorService;
            private Future refreshTask;
            final private Runnable refreshRun;
            final private Pattern URLmather;
            final private MutableAttributeSet URLAttribute;
            final private AttributeSet defAttribute;

            // as default constructor
            {
                this.executorService = new ScheduledThreadPoolExecutor(1);
                this.refreshRun = new Runnable() {
                    public void run() {
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {

                                public void run() {
                                    try {
                                        // clear all attributes
                                        doc.setCharacterAttributes(0, doc.getLength(), defAttribute, true);

                                        // URL detection
                                        final String text = doc.getText(0, doc.getLength());
                                        final Matcher matcher = URLmather.matcher(text);
                                        while (matcher.find()) {
                                            // transforming into Clickable text
                                            applyStyle(doc, matcher.start(), matcher.end() - matcher.start(), matcher.group());
                                        }
                                    } catch (BadLocationException ex) {
                                        logger.log("LinkBuilder: " + JTextPaneAttributeInserter.class.getName() + ex);
                                    }
                                }
                            });
                        } catch (InterruptedException ex) {
                            logger.log(ex.toString());
                        } catch (InvocationTargetException ex) {
                            logger.log(ex.toString());
                        }
                    }
                };

                // setting up the URL pattern
                this.URLmather = Pattern.compile(REGEX_URL, Pattern.CASE_INSENSITIVE);

                // setting up attributes
                this.URLAttribute = new SimpleAttributeSet();
                StyleConstants.setUnderline(this.URLAttribute, true);
                StyleConstants.setForeground(this.URLAttribute, Color.blue);

                this.defAttribute = new SimpleAttributeSet().copyAttributes();
            }

            private void applyStyle(final StyledDocument targetDoc, final int offset, final int length, final String target) {
                URLAttribute.addAttribute(ATTR_LINK, new BrowserLaunchAction(target));
                targetDoc.setCharacterAttributes(offset, length, URLAttribute.copyAttributes(), true);
                URLAttribute.removeAttribute(ATTR_LINK);
            }

            public void insertUpdate(DocumentEvent e) {
                // this method should be called from Swing thread.
                refreshPane();
            }

            public void removeUpdate(DocumentEvent e) {
                // this method should be called from Swing thread.
                refreshPane();
            }

            public void changedUpdate(DocumentEvent e) {
                // this method should be called from Swing thread.
                refreshPane();
            }

            private void refreshPane() {
                // this method should be called from Swing thread.
                if (refreshTask != null && !refreshTask.isDone()) {
                    // previous task doing something
//                    if (refreshTask.cancel(false)) {
//                        // postpone the task because of user's actions
//                        refreshTask = executorService.schedule(refreshRun, 3, TimeUnit.SECONDS);
//                    }
                    return;
                }

                refreshTask = executorService.schedule(refreshRun, 3, TimeUnit.SECONDS);
            }
        });
    }
}
