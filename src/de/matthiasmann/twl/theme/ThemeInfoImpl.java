/*
 * Copyright (c) 2008, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.twl.theme;

import de.matthiasmann.twl.Border;
import de.matthiasmann.twl.ThemeInfo;
import java.util.HashMap;

/**
 * The ThemeInfo implementation
 *
 * @author Matthias Mann
 */
class ThemeInfoImpl extends ParameterMapImpl implements ThemeInfo {

    private final String name;
    final ThemeInfoImpl parent;
    final HashMap<String, ThemeInfoImpl> children;
    Border border;
    boolean maybeUsedFromWildcard;
    String wildcardImportPath;

    public ThemeInfoImpl(ThemeManager manager, String name, ThemeInfoImpl parent) {
        super(manager);
        this.name = name;
        this.parent = parent;
        this.children = new HashMap<String, ThemeInfoImpl>();
    }

    void copy(ThemeInfoImpl src) {
        if(src.border != null) {
            border = src.border;
        }
        children.putAll(src.children);
        params.putAll(src.params);
        wildcardImportPath = src.wildcardImportPath;
    }

    public String getName() {
        return name;
    }

    public Border getBorder() {
        return border;
    }

    public ThemeInfo getChildTheme(String theme) {
        ThemeInfo info = children.get(theme);
        if(info == null) {
            if(wildcardImportPath != null) {
                info = manager.resolveWildcard(wildcardImportPath, theme);
            }
            if(info == null) {
                System.err.println("Missing child theme \"" + theme + "\" for \"" + getThemePath() + "\"");
            }
        }
        return info;
    }

    public String getThemePath() {
        return getThemePath(0).toString();
    }

    private StringBuilder getThemePath(int length) {
        StringBuilder sb;
        length += getName().length();
        if(parent != null) {
            sb = parent.getThemePath(length + 1);
            sb.append('.');
        } else {
            sb = new StringBuilder(length);
        }
        sb.append(getName());
        return sb;
    }

    @Override
    protected void missingParameter(String paramName) {
        logger.warning("Parameter \"" + paramName + "\" not set for \"" + getThemePath() + "\"");
    }

}
