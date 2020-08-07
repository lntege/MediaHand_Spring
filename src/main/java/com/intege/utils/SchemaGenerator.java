package com.intege.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaGenerator {

    public static final String DDL_FILENAME = "ddl_create.sql";

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaGenerator.class);

    private SchemaGenerator() {
        super();
    }

    public static void main(final String[] args) throws IOException, ClassNotFoundException {
        if (Files.deleteIfExists(Path.of("mediahand/src/main/resources/db/" + DDL_FILENAME))) {
            LOGGER.info("Deleted old schema file " + DDL_FILENAME);
        }
        Map<String, String> settings = new HashMap<>();
        settings.put(Environment.URL, "jdbc:hsqldb:mem:schema");
        settings.put(Environment.PHYSICAL_NAMING_STRATEGY, "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(settings).build();

        MetadataSources metadataSources = new MetadataSources(serviceRegistry);
        for (Class<?> clazz : getClasses("com.intege.mediahand")) {
            if (clazz.isAnnotationPresent(javax.persistence.Entity.class)) {
                metadataSources.addAnnotatedClass(clazz);
            }
        }
        Metadata metadata = metadataSources.buildMetadata();

        SchemaExport schemaExport = new SchemaExport();
        schemaExport.setFormat(true);
        schemaExport.setOutputFile("mediahand/src/main/resources/db/" + DDL_FILENAME);
        schemaExport.createOnly(EnumSet.of(TargetType.SCRIPT), metadata);
    }

    private static List<Class<?>> getClasses(final String packageName) throws ClassNotFoundException {
        String path = packageName.replace('.', '/');
        System.out.println(path);
        File dir = new File("mediahand/src/main/java/" + path);
        return findClasses(dir, packageName);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException if a class was not found
     */

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".java")) {
                classes.add(Class.forName(
                        packageName + '.' + file.getName().substring(0, file.getName().length() - 5)));
            }
        }
        return classes;
    }

}
