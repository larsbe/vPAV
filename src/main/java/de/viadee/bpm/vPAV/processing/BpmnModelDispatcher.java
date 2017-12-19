/**
 * Copyright � 2017, viadee Unternehmensberatung GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the viadee Unternehmensberatung GmbH.
 * 4. Neither the name of the viadee Unternehmensberatung GmbH nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <viadee Unternehmensberatung GmbH> ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.viadee.bpm.vPAV.processing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.xml.sax.SAXException;

import de.viadee.bpm.vPAV.BPMNScanner;
import de.viadee.bpm.vPAV.config.model.Rule;
import de.viadee.bpm.vPAV.output.JsOutputWriter;
import de.viadee.bpm.vPAV.output.OutputWriterException;
import de.viadee.bpm.vPAV.processing.checker.CheckerFactory;
import de.viadee.bpm.vPAV.processing.checker.ElementChecker;
import de.viadee.bpm.vPAV.processing.checker.ModelChecker;
import de.viadee.bpm.vPAV.processing.checker.ProcessVariablesModelChecker;
import de.viadee.bpm.vPAV.processing.model.data.AnomalyContainer;
import de.viadee.bpm.vPAV.processing.model.data.BpmnElement;
import de.viadee.bpm.vPAV.processing.model.data.CheckerIssue;
import de.viadee.bpm.vPAV.processing.model.graph.IGraph;
import de.viadee.bpm.vPAV.processing.model.graph.Path;

/**
 * Calls model and element checkers for a concrete bpmn processdefinition
 *
 */
public class BpmnModelDispatcher {

    /**
     * The BpmnModelDispatcher reads a model and creates a collection of all elements. Iterates through collection and
     * checks each element for validity Additionally a graph is created to check for invalid paths.
     *
     * @param processdefinition
     *            Holds the path to the BPMN model
     * @param decisionRefToPathMap
     *            decisionRefToPathMap
     * @param processIdToPathMap
     *            Map of prozessId to bpmn file
     * @param messageIdToVariables
     *            Map of messages and their variables
     * @param processIdToVariables
     *            map of processId with their variables
     * @param resourcesNewestVersions
     *            collection with newest versions of class files
     * @param conf
     *            ruleSet
     * @return issues
     * @throws ConfigItemNotFoundException
     *             ruleSet couldn't be read
     */
    public static Collection<CheckerIssue> dispatch(final File processdefinition,
            final Map<String, String> decisionRefToPathMap, final Map<String, String> processIdToPathMap,
            final Map<String, Collection<String>> messageIdToVariables,
            final Map<String, Collection<String>> processIdToVariables,
            final Collection<String> resourcesNewestVersions, final Map<String, Rule> conf)
            throws ConfigItemNotFoundException {

        // create BPMNScanner
        BPMNScanner bpmnScanner;
        try {
            bpmnScanner = new BPMNScanner(processdefinition.getPath());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("Model couldn't be parsed");
        }

        // parse bpmn model
        final BpmnModelInstance modelInstance = Bpmn.readModelFromFile(processdefinition);

        // hold bpmn elements
        final Collection<BaseElement> baseElements = modelInstance
                .getModelElementsByType(BaseElement.class);

        final ElementGraphBuilder graphBuilder = new ElementGraphBuilder(decisionRefToPathMap,
                processIdToPathMap, messageIdToVariables,
                processIdToVariables, bpmnScanner);

        // create data flow graphs for bpmn model
        final Collection<IGraph> graphCollection = graphBuilder.createProcessGraph(modelInstance,
                processdefinition.getPath(), new ArrayList<String>());

        // add data flow information to graph and calculate invalid paths
        final Map<AnomalyContainer, List<Path>> invalidPathMap = graphBuilder
                .createInvalidPaths(graphCollection);

        final Collection<CheckerIssue> issues = new ArrayList<CheckerIssue>();

        // call model checkers
        // TODO: move it to a factory class later
        final Rule processVariablesModelRule = conf
                .get(getClassName(ProcessVariablesModelChecker.class));
        if (processVariablesModelRule == null)
            throw new ConfigItemNotFoundException(
                    getClassName(ProcessVariablesModelChecker.class) + " not found");
        if (processVariablesModelRule.isActive()) {
            final ModelChecker processVarChecker = new ProcessVariablesModelChecker(
                    processVariablesModelRule, invalidPathMap);
            issues.addAll(processVarChecker.check(modelInstance));
        }

        // execute element checkers
        for (final BaseElement baseElement : baseElements) {
            BpmnElement element = graphBuilder.getElement(baseElement.getId());
            if (element == null) {
                // if element is not in the data flow graph, create it.
                element = new BpmnElement(processdefinition.getPath(), baseElement);
            }
            final Collection<ElementChecker> checkerCollection = CheckerFactory
                    .createCheckerInstancesBpmnElement(conf, resourcesNewestVersions, element,
                            bpmnScanner);
            for (final ElementChecker checker : checkerCollection) {
                issues.addAll(checker.check(element));
            }

        }

        // write js with processvariables
        JsOutputWriter jWriter = new JsOutputWriter();
        try {
            jWriter.writeVars(baseElements, graphBuilder, processdefinition);
        } catch (OutputWriterException e) {
            e.printStackTrace();
        }

        return issues;
    }

    private static String getClassName(Class<?> clazz) {
        return clazz.getSimpleName();
    }
}
