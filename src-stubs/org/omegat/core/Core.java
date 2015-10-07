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
package org.omegat.core;

import org.omegat.gui.glossary.GlossaryTextArea;
import org.omegat.gui.notes.INotes;

public class Core {

    public static void pluginLoadingError(String errorText) {
        System.out.println("Core.pluginLoadingError: " + errorText);
    }

    public static INotes getNotes() {
        return null;
    }

    public static GlossaryTextArea getGlossary() {
        return null;
    }
}
