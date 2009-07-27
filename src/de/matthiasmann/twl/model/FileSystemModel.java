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
package de.matthiasmann.twl.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

/**
 * An generic file system abstraction which is used as base for file system
 * widgets like FolderBrowser.
 * 
 * @author Matthias Mann
 */
public interface FileSystemModel {

    public interface FileFilter {
        public boolean accept(FileSystemModel model, Object file);
    }
    
    public String getSeparator();
    
    public Object getFile(String name);
    
    public Object getParent(Object file);
    
    public boolean isFolder(Object file);
    
    public boolean isFile(Object file);

    public boolean isHidden(Object file);
    
    public String getName(Object file);
    
    public String getPath(Object file);
    
    public String getRelativePath(Object from, Object to);
    
    public long getSize(Object file);
    
    public long getLastModified(Object file);
    
    public boolean equals(Object file1, Object file2);
    
    public int find(Object[] list, Object file);
    
    public Object[] listRoots();
    
    public Object[] listFolder(Object file, FileFilter filter);
    
    public InputStream openStream(Object file) throws IOException;
    
    public ReadableByteChannel openChannel(Object file) throws IOException;
 
}
