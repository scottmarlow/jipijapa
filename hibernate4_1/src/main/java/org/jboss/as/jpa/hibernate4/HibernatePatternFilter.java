/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.jpa.hibernate4;

import static org.jboss.as.jpa.hibernate4.JpaMessages.MESSAGES;

import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

/**
 * Mock work of NativeScanner matching.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Scott Marlow
 */
public class HibernatePatternFilter implements VirtualFileFilter {
    private final String pattern;
    private final boolean exact;

    public HibernatePatternFilter(String pattern) {
        if (pattern == null)
            throw MESSAGES.nullVar("pattern");

        exact = !pattern.contains("/"); // no path split or glob
        if (exact == false && (pattern.startsWith("**/*"))) {
            this.pattern = pattern.substring(4);
        } else {
            this.pattern = pattern;
        }
    }

    protected boolean accepts(String name) {
        return exact ? name.equals(pattern) : name.endsWith(pattern);
    }

    public boolean accepts(VirtualFile file) {
        String name = exact ? file.getName() : file.getPathName();
        return accepts(name);
    }

}
