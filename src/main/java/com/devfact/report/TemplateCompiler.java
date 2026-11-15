package com.devfact.report;

import net.sf.jasperreports.engine.JasperCompileManager;

/**
 * Régénère le modèle PDF précompilé à partir du source JRXML.
 * <p>
 * À exécuter après toute modification de {@code document.jrxml} ; sans cela, c'est l'ancien
 * {@code document.jasper} qui reste utilisé par l'application.
 * <pre>
 * mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
 * java -cp "target/classes:$(cat target/cp.txt)" com.devfact.report.TemplateCompiler
 * </pre>
 */
public final class TemplateCompiler {

    private static final String DEFAULT_SOURCE = "src/main/resources/reports/document.jrxml";
    private static final String DEFAULT_TARGET = "src/main/resources/reports/document.jasper";

    private TemplateCompiler() {
    }

    public static void main(String[] args) throws Exception {
        String source = args.length > 0 ? args[0] : DEFAULT_SOURCE;
        String target = args.length > 1 ? args[1] : DEFAULT_TARGET;
        JasperCompileManager.compileReportToFile(source, target);
        System.out.println("Modèle précompilé régénéré : " + target);
    }
}
