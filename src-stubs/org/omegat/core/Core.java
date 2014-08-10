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
