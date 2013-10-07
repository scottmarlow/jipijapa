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

import static org.jboss.as.jpa.hibernate4.JpaLogger.JPA_LOGGER;
import static org.jboss.as.jpa.hibernate4.JpaMessages.MESSAGES;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ejb.packaging.NamedInputStream;
import org.hibernate.ejb.packaging.Scanner;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;


/**
 * Annotation scanner for Hibernate
 *
 * @author Scott Marlow (forked from Ales Justin's ScannerImpl in AS6)
 */
public class HibernateAnnotationScanner implements Scanner {

    private static final ThreadLocal<PersistenceUnitMetadata> PERSISTENCE_UNIT_METADATA_TLS = new ThreadLocal<PersistenceUnitMetadata>();

    /** Caches, used when restarting the persistence unit service */
    private static final Map<PersistenceUnitMetadata, Map<URL, Set<Package>>> PACKAGES_IN_JAR_CACHE = new HashMap<PersistenceUnitMetadata, Map<URL,Set<Package>>>();
    private static final Map<PersistenceUnitMetadata, Map<URL, Map<Class<? extends Annotation>, Set<Class<?>>>>> CLASSES_IN_JAR_CACHE = new HashMap<PersistenceUnitMetadata, Map<URL, Map<Class<? extends Annotation>, Set<Class<?>>>>>();

    public static void setThreadLocalPersistenceUnitMetadata(final PersistenceUnitMetadata pu) {
        PERSISTENCE_UNIT_METADATA_TLS.set(pu);
    }

    public static void clearThreadLocalPersistenceUnitMetadata() {
        PERSISTENCE_UNIT_METADATA_TLS.remove();
    }

    private static void cachePackages(PersistenceUnitMetadata pu, URL jarToScan, Set<Package> packages) {
        synchronized (PACKAGES_IN_JAR_CACHE) {
            Map<URL, Set<Package>> packagesByUrl = PACKAGES_IN_JAR_CACHE.get(pu);
            if (packagesByUrl == null) {
                packagesByUrl = new HashMap<URL, Set<Package>>();
                PACKAGES_IN_JAR_CACHE.put(pu, packagesByUrl);
            }
            packagesByUrl.put(jarToScan, packages);
        }
    }

    private static Set<Package> getCachedPackages(PersistenceUnitMetadata pu, URL jarToScan){
        synchronized (PACKAGES_IN_JAR_CACHE) {
            Map<URL, Set<Package>> packagesByUrl = PACKAGES_IN_JAR_CACHE.get(pu);
            if (packagesByUrl == null) {
                return Collections.emptySet();
            }
            Set<Package> packages = packagesByUrl.get(jarToScan);
            if (packages == null) {
                return Collections.emptySet();
            }
            return packages;

        }
    }

    private static void cacheClasses(PersistenceUnitMetadata pu, URL jarToScan, Class<? extends Annotation> annotation, Set<Class<?>> classes){
        synchronized (CLASSES_IN_JAR_CACHE) {
            Map<URL, Map<Class<? extends Annotation>, Set<Class<?>>>> classesByURL = CLASSES_IN_JAR_CACHE.get(pu);
            if (classesByURL == null) {
                classesByURL = new HashMap<URL, Map<Class<? extends Annotation>, Set<Class<?>>>>();
                CLASSES_IN_JAR_CACHE.put(pu, classesByURL);
            }
            Map<Class<? extends Annotation>, Set<Class<?>>> classesByAnnotation = classesByURL.get(jarToScan);
            if (classesByAnnotation == null) {
                classesByAnnotation = new HashMap<Class<? extends Annotation>, Set<Class<?>>>();
                classesByURL.put(jarToScan, classesByAnnotation);
            }
            classesByAnnotation.put(annotation, classes);
        }

    }

    private static Set<Class<?>> getCachedClasses(PersistenceUnitMetadata pu, URL jarToScan, Set<Class<? extends Annotation>> annotationsToLookFor){
        synchronized (CLASSES_IN_JAR_CACHE) {
            Map<URL, Map<Class<? extends Annotation>, Set<Class<?>>>> classesByURL = CLASSES_IN_JAR_CACHE.get(pu);
            if (classesByURL == null) {
                return Collections.emptySet();
            }
            Map<Class<? extends Annotation>, Set<Class<?>>> classesByAnnotation = classesByURL.get(jarToScan);
            if (classesByAnnotation == null) {
                return Collections.emptySet();
            }
            Set<Class<?>> classes = new HashSet<Class<?>>();
            for (Class<? extends Annotation> ann : annotationsToLookFor) {
                Set<Class<?>> classesForAnnotation = classesByAnnotation.get(ann);
                if (classesForAnnotation != null) {
                    classes.addAll(classesForAnnotation);
                }
            }
            return classes;
        }
    }

    static void cleanup(PersistenceUnitMetadata pu) {
        synchronized (CLASSES_IN_JAR_CACHE) {
            CLASSES_IN_JAR_CACHE.remove(pu);
        }
        synchronized (PACKAGES_IN_JAR_CACHE) {
            PACKAGES_IN_JAR_CACHE.remove(pu);
        }
    }

    @Override
    public Set<Package> getPackagesInJar(URL jarToScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
        if (jarToScan == null) {
            throw MESSAGES.nullVar("jarToScan");
        }
        JPA_LOGGER.tracef("getPackagesInJar url=%s annotations=%s", jarToScan.getPath(), annotationsToLookFor);
        Set<Class<?>> resultClasses = new HashSet<Class<?>>();

        PersistenceUnitMetadata pu = PERSISTENCE_UNIT_METADATA_TLS.get();
        if (pu == null) {
            throw MESSAGES.missingPersistenceUnitMetadata();
        }

        if (annotationsToLookFor.size() > 0) {  // Hibernate doesn't pass any annotations currently
            resultClasses = getClassesInJar(jarToScan, annotationsToLookFor);
        } else {
            if (pu.getAnnotationIndex() != null) {
                Index index = getJarFileIndex(jarToScan, pu);
                if (index == null) {
                    JPA_LOGGER.tracef("No classes to scan for annotations in jar '%s' (jars with classes '%s')",
                        jarToScan, pu.getAnnotationIndex().keySet());
                    return new HashSet<Package>();
                }
                Collection<ClassInfo> allClasses = index.getKnownClasses();
                for (ClassInfo classInfo : allClasses) {
                    String className = classInfo.name().toString();
                    try {
                        resultClasses.add(pu.getNewTempClassLoader().loadClass(className));
                    } catch (ClassNotFoundException e) {
                        JPA_LOGGER.cannotLoadEntityClass(e, className);
                    } catch (NoClassDefFoundError e) {
                        JPA_LOGGER.cannotLoadEntityClass(e, className);
                    }
                }
            }
        }

        if (pu.getAnnotationIndex() != null || annotationsToLookFor.size() > 0) {
            Map<String, Package> uniquePackages = new HashMap<String, Package>();
            for (Class<?> classWithAnnotation : resultClasses) {
                Package classPackage = classWithAnnotation.getPackage();
                if (classPackage != null) {
                    JPA_LOGGER.tracef("getPackagesInJar found package %s", classPackage);
                    uniquePackages.put(classPackage.getName(), classPackage);
                }
            }
            Set<Package> packages = new HashSet<Package>(uniquePackages.values());
            cachePackages(pu, jarToScan, packages);
            return new HashSet<Package>(packages);
        } else {
            return getCachedPackages(pu, jarToScan);
        }
    }

    private Index getJarFileIndex(final URL jarToScan, final PersistenceUnitMetadata pu) {
        return pu.getAnnotationIndex().get(jarToScan);
    }

    @Override
    public Set<Class<?>> getClassesInJar(URL jarToScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
        if (jarToScan == null) {
            throw MESSAGES.nullVar("jarToScan");
        }
        JPA_LOGGER.tracef("getClassesInJar url=%s annotations=%s", jarToScan.getPath(), annotationsToLookFor);
        PersistenceUnitMetadata pu = PERSISTENCE_UNIT_METADATA_TLS.get();
        if (pu == null) {
            throw MESSAGES.missingPersistenceUnitMetadata();
        }
        if (pu.getAnnotationIndex() != null) {
            Index index = getJarFileIndex(jarToScan, pu);
            if (index == null) {
                JPA_LOGGER.tracef("No classes to scan for annotations in jar '%s' (jars with classes '%s')",
                    jarToScan, pu.getAnnotationIndex().keySet());
                return new HashSet<Class<?>>();
            }
            if (annotationsToLookFor == null) {
                throw MESSAGES.nullVar("annotationsToLookFor");
            }
            if (annotationsToLookFor.size() == 0) {
                throw MESSAGES.emptyParameter("annotationsToLookFor");
            }

            Set<Class<?>> result = new HashSet<Class<?>>();

            for (Class<? extends Annotation> annClass : annotationsToLookFor) {
                DotName annotation = DotName.createSimple(annClass.getName());
                List<AnnotationInstance> classesWithAnnotation = index.getAnnotations(annotation);
                Set<Class<?>> classesForAnnotation = new HashSet<Class<?>>();
                for (AnnotationInstance annotationInstance : classesWithAnnotation) {
                    // verify that the annotation target is actually a class, since some frameworks
                    // may generate bytecode with annotations placed on methods (see AS7-2559)
                    if (annotationInstance.target() instanceof ClassInfo) {
                        String className = annotationInstance.target().toString();
                        try {
                            JPA_LOGGER.tracef("getClassesInJar found class %s with annotation %s", className, annClass.getName());
                            Class<?> clazz = pu.getNewTempClassLoader().loadClass(className);
                            result.add(clazz);
                            classesForAnnotation.add(clazz);
                        } catch (ClassNotFoundException e) {
                            JPA_LOGGER.cannotLoadEntityClass(e, className);
                        } catch (NoClassDefFoundError e) {
                            JPA_LOGGER.cannotLoadEntityClass(e, className);
                        }
                    }
                }
                cacheClasses(pu, jarToScan, annClass, classesForAnnotation);
            }
            return result;
        } else {
            return getCachedClasses(pu, jarToScan, annotationsToLookFor);
        }
    }

    @Override
    public Set<NamedInputStream> getFilesInJar(URL jarToScan, Set<String> filePatterns) {
        if (jarToScan == null)
            throw MESSAGES.nullVar("jarToScan");
        if (filePatterns == null)
            throw MESSAGES.nullVar("filePatterns");

        Set<NamedInputStream> result = new HashSet<NamedInputStream>();
        Map<String, Set<NamedInputStream>> map;
        map = new HashMap<String, Set<NamedInputStream>>();
        findFiles(jarToScan, filePatterns, map, result);
        return result;
    }

    private void findFiles(URL jarToScan, Set<String> filePatterns, Map<String, Set<NamedInputStream>> map, Set<NamedInputStream> result) {
        if (filePatterns.isEmpty()) {
            for (Set<NamedInputStream> nims : map.values())
                result.addAll(nims);
        } else {
            VirtualFile root = null;
            for (String pattern : filePatterns) {
                Set<NamedInputStream> niss = map.get(pattern);
                if (niss == null) {
                    if (root == null)
                        root = getFile(jarToScan);

                    try {
                        List<VirtualFile> children = root.getChildrenRecursively(new HibernatePatternFilter(pattern));
                        niss = toNIS(children);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (niss != null)
                    result.addAll(niss);
            }
        }
    }

    private Set<NamedInputStream> toNIS(Iterable<VirtualFile> files) {
        Set<NamedInputStream> result = new HashSet<NamedInputStream>();
        for (VirtualFile file : files) {
            NamedInputStream nis = new HibernateVirtualFileNamedInputStream(file);
            result.add(nis);
        }
        return result;
    }

    @Override
    public Set<NamedInputStream> getFilesInClasspath(Set<String> filePatterns) {
        throw MESSAGES.notYetImplemented();  // not currently called
    }

    @Override
    public String getUnqualifiedJarName(URL jarUrl) {
        VirtualFile file = getFile(jarUrl);
        return file.getName();
    }

    private VirtualFile getFile(URL url) {
        try {
            return VFS.getChild(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

}