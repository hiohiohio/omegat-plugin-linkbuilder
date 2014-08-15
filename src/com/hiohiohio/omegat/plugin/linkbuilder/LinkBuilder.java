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
package com.hiohiohio.omegat.plugin.linkbuilder;

import javax.swing.JTextPane;
import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.events.IApplicationEventListener;
import org.omegat.gui.glossary.GlossaryTextArea;
import org.omegat.gui.notes.INotes;
import org.omegat.util.Log;
import com.hiohiohio.omegat.plugin.linkbuilder.attributeinserter.IAttributeInserter;
import com.hiohiohio.omegat.plugin.linkbuilder.attributeinserter.JTextPaneAttributeInserter;
import com.hiohiohio.omegat.plugin.linkbuilder.util.ILogger;

public class LinkBuilder {

    /**
     * To support v2 of OmegaT, this class will be registered as a base-plugin
     * class. (The setting is written in the MANIFEST.MF)
     */
    public LinkBuilder() {
        LinkBuilder.loadPlugins();
    }

    /**
     * OmegaT will call this method when loading.
     */
    public static void loadPlugins() {
        // register Application Event Listner to setup DocumentListner for NotesPane
        // Because of initiating GUI after the loading of plugins, we should wait to finish it.
        CoreEvents.registerApplicationEventListener(generateIApplicationEventListener());
    }

    public static void unloadPlugins() {

    }

    private static IApplicationEventListener generateIApplicationEventListener() {
        return new IApplicationEventListener() {
            public void onApplicationStartup() {
                // get NotesPane from Core object just after constructiong GUI
                final INotes notes = Core.getNotes();
                if (!(notes instanceof JTextPane)) {
                    // If this error happened, it would come from recent updates of OmegaT or Other plugins.
                    Log.log("NotesPane should be extened from JTextPane to use LinkBuilder plugin.");
                    return;
                }

                // get GlossaryPane
                final GlossaryTextArea glossaryTextArea = Core.getGlossary();
                if (!(glossaryTextArea instanceof JTextPane)) {
                    // If this error happened, it would come from recent updates of OmegaT or Other plugins.
                    Log.log("GlossaryPane should be extened from JTextPane to use LinkBuilder plugin.");
                    return;
                }

                // generate logger for OmegaT
                ILogger logger = new ILogger() {

                    public void log(String s) {
                        Log.log(s);
                    }
                };

                // register for Notes
                final IAttributeInserter notesURLInserter = new JTextPaneAttributeInserter((JTextPane) notes, logger);
                notesURLInserter.register();

                // register for Glossary
                final IAttributeInserter glossaryURLInserter = new JTextPaneAttributeInserter((JTextPane) glossaryTextArea, logger);
                glossaryURLInserter.register();
            }

            public void onApplicationShutdown() {
            }
        };
    }
}
