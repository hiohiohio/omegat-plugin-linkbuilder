/*
 Copyright (C) 2015 Chihiro Hio

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
