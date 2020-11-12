/*
 *
 *  *  Copyright (c) 2020  Stefan Spiska (Vitasystems GmbH) and Hannover Medical School
 *  *  This file is part of Project EHRbase
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *  http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *
 */

package org.ehrbase.webtemplate.parser;

import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.composition.EventContext;
import com.nedap.archie.rm.composition.IsmTransition;
import com.nedap.archie.rm.datastructures.Element;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import com.nedap.archie.rminfo.RMAttributeInfo;
import com.nedap.archie.rminfo.RMTypeInfo;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.terminology.client.terminology.TermDefinition;
import org.ehrbase.terminology.client.terminology.TerminologyProvider;
import org.ehrbase.terminology.client.terminology.ValueSet;
import org.ehrbase.webtemplate.model.WebTemplate2;
import org.ehrbase.webtemplate.model.WebTemplateInput;
import org.ehrbase.webtemplate.model.WebTemplateInputValue;
import org.ehrbase.webtemplate.model.WebTemplateNode;
import org.openehr.schemas.v1.ARCHETYPETERM;
import org.openehr.schemas.v1.CARCHETYPEROOT;
import org.openehr.schemas.v1.CATTRIBUTE;
import org.openehr.schemas.v1.CCODEPHRASE;
import org.openehr.schemas.v1.CCODEREFERENCE;
import org.openehr.schemas.v1.CCOMPLEXOBJECT;
import org.openehr.schemas.v1.CDOMAINTYPE;
import org.openehr.schemas.v1.CDVORDINAL;
import org.openehr.schemas.v1.CDVQUANTITY;
import org.openehr.schemas.v1.CDVSTATE;
import org.openehr.schemas.v1.COBJECT;
import org.openehr.schemas.v1.IntervalOfInteger;
import org.openehr.schemas.v1.OBJECTID;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.StringDictionaryItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class OPTParser {
    public static final String PATH_DIVIDER = "/";
    public static final ArchieRMInfoLookup ARCHIE_RM_INFO_LOOKUP = ArchieRMInfoLookup.getInstance();
    private static final TerminologyProvider TERMINOLOGY_PROVIDER = new TerminologyProvider();

    private final OPERATIONALTEMPLATE operationaltemplate;
    private final String defaultLanguage;

    public OPTParser(OPERATIONALTEMPLATE operationaltemplate) {
        this.operationaltemplate = operationaltemplate;
        defaultLanguage = operationaltemplate.getLanguage().getCodeString();
    }

    public WebTemplate2 parse() {
        WebTemplate2 webTemplate = new WebTemplate2();

        webTemplate.setTemplateId(operationaltemplate.getTemplateId().getValue());
        webTemplate.setDefaultLanguage(defaultLanguage);
        webTemplate.setVersion("2.3");
        webTemplate.setTree(parseCARCHETYPEROO(operationaltemplate.getDefinition(), ""));
        return webTemplate;
    }

    private WebTemplateNode parseCARCHETYPEROO(CARCHETYPEROOT carchetyperoot, String aqlPath) {

        // extract local Terminologies
        Map<String, Map<String, TermDefinition>> termDefinitionMap = new HashMap<>();
        for (ARCHETYPETERM term : carchetyperoot.getTermDefinitionsArray()) {
            String code = term.getCode();
            String value = null;
            String description = null;
            for (StringDictionaryItem item : term.getItemsArray()) {
                if ("text".equals(item.getId())) {
                    value = item.getStringValue();
                }
                if ("description".equals(item.getId()))
                    description = item.getStringValue();
            }
            termDefinitionMap.computeIfAbsent(code, c -> new HashMap<>()).put(defaultLanguage, new TermDefinition(code, value, description));

        }
        WebTemplateNode node = parseCCOMPLEXOBJECT(carchetyperoot, aqlPath, termDefinitionMap, null);
        node.setNodeId(carchetyperoot.getArchetypeId().getValue());
        addRMAttributes(node, aqlPath, termDefinitionMap);
        return node;
    }

    private WebTemplateNode parseCCOMPLEXOBJECT(CCOMPLEXOBJECT ccomplexobject, String aqlPath, Map<String, Map<String, TermDefinition>> termDefinitionMap, String rmAttributeName) {

        Optional<String> expliziteName = OptNameHelper.extractName(ccomplexobject);
        WebTemplateNode node = buildNode(ccomplexobject, rmAttributeName, termDefinitionMap);
        node.setAqlPath(aqlPath);

        if (StringUtils.isNotBlank(ccomplexobject.getNodeId()) && expliziteName.isPresent()) {
            FlatPath path = new FlatPath(node.getAqlPath());
            FlatPath lastSegment = path;
            while (lastSegment.getChild() != null) {
                lastSegment = lastSegment.getChild();
            }
            lastSegment.addOtherPredicate("name/value", expliziteName.get());
            node.setAqlPath(path.format(true));
            aqlPath = path.format(true);
        }

        CATTRIBUTE[] cattributes = ccomplexobject.getAttributesArray();

        for (CATTRIBUTE cattribute : cattributes) {
            String pathLoop = aqlPath + PATH_DIVIDER + cattribute.getRmAttributeName();
            if (
                //Will be set via Attributes
                    pathLoop.equals("/category")
                            || pathLoop.endsWith("/name")) {
                continue;
            }
            List<WebTemplateNode> newChildren = new ArrayList<>();
            for (COBJECT cobject : cattribute.getChildrenArray()) {
                WebTemplateNode childNode = parseCOBJECT(cobject, pathLoop, termDefinitionMap, cattribute.getRmAttributeName());
                if (childNode != null) {
                    newChildren.add(childNode);
                }
            }

            List<WebTemplateNode> ismTransitionList = newChildren.stream()
                    .filter(n -> "ISM_TRANSITION".equals(n.getRmType()))
                    .collect(Collectors.toList());
            if (!ismTransitionList.isEmpty()) {

                WebTemplateNode ismTransition = new WebTemplateNode();
                ismTransition.setName(cattribute.getRmAttributeName());
                ismTransition.setId(buildId(cattribute.getRmAttributeName()));
                ismTransition.setMin(ismTransitionList.get(0).getMin());
                ismTransition.setMax(ismTransitionList.get(0).getMax());
                ismTransition.setRmType("ISM_TRANSITION");
                ismTransition.setAqlPath(aqlPath + "/" + cattribute.getRmAttributeName());

                WebTemplateNode careflowStep = new WebTemplateNode();
                WebTemplateNode careflowStepProto = ismTransitionList.get(0).findMatching(n -> n.getId().equals("careflow_step")).get(0);
                careflowStep.setMin(careflowStepProto.getMin());
                careflowStep.setMax(careflowStepProto.getMin());
                careflowStep.setName("Careflow_step");
                careflowStep.setId("careflow_step");
                careflowStep.setRmType("DV_CODED_TEXT");
                careflowStep.setAqlPath(aqlPath + "/" + cattribute.getRmAttributeName() + "/" + "careflow_step");
                WebTemplateInput code = new WebTemplateInput();
                code.setSuffix("code");
                code.setType("CODED_TEXT");

                ismTransitionList.forEach(
                        i -> {
                            WebTemplateInputValue value = i.findChildById("careflow_step").get().getInputs().get(0).getList().get(0);
                            value.getCurrentStates().addAll(i.findChildById("current_state").get().getInputs().get(0).getList().stream().map(WebTemplateInputValue::getValue).collect(Collectors.toList()));
                            code.getList().add(value);
                        }
                );
                careflowStep.getInputs().add(code);
                ismTransition.getChildren().add(careflowStep);


                WebTemplateNode currentState = new WebTemplateNode();
                WebTemplateNode currentStateProto = ismTransitionList.get(0).findMatching(n -> n.getId().equals("current_state")).get(0);
                currentState.setMin(currentStateProto.getMin());
                currentState.setMax(currentStateProto.getMin());
                currentState.setRmType("DV_CODED_TEXT");
                currentState.setName("Current_state");
                currentState.setId("current_state");
                currentState.setAqlPath(aqlPath + "/" + cattribute.getRmAttributeName() + "/" + "current_state");
                WebTemplateInput code2 = new WebTemplateInput();
                code2.setSuffix("code");
                code2.setType("CODED_TEXT");
                code2.getList().addAll(
                        ismTransitionList.stream()
                                .map(n -> n.findMatching(k -> k.getId().equals("current_state")))
                                .flatMap(List::stream)
                                .map(WebTemplateNode::getInputs)
                                .flatMap(List::stream)
                                .map(WebTemplateInput::getList)
                                .flatMap(List::stream)
                                .collect(Collectors.toList())
                );
                currentState.getInputs().add(code2);
                ismTransition.getChildren().add(currentState);
                WebTemplateNode transition = ismTransitionList.get(0).findChildById("transition").get();
                transition.setAqlPath(aqlPath + "/" + cattribute.getRmAttributeName() + "/" + "transition");
                ismTransition.getChildren().add(transition);
                node.getChildren().add(ismTransition);
            }

            node.getChildren().addAll(newChildren);
        }
        // Handle choice children
        node.getChoicesInChildren()
                .values()
                .stream()
                .flatMap(List::stream)
                .filter(n -> n.getRmType().startsWith("DV_"))
                .forEach(n -> n.setId(n.getRmType().replace("DV_", "").toLowerCase() + "_value"));

        //Inherit name for Element values
        if (node.getRmType().equals("ELEMENT")) {
            if (node.getChildren().size() == 1) {
                WebTemplateNode value = node.getChildren().get(0);
                value.setId(node.getId(false));
                value.setName(node.getName());
                value.setMax(node.getMax());
                value.setMin(node.getMin());
                value.getLocalizedDescriptions().putAll(node.getLocalizedDescriptions());
                value.getLocalizedNames().putAll(node.getLocalizedNames());
                value.setLocalizedName(node.getLocalizedName());
                // If contains a choice of DV_TEXT and DV_CODED_TEXT add a merged node
            } else if (node.getChildren().size() == 2 && node.getChildren().stream().map(WebTemplateNode::getRmType).collect(Collectors.toList()).containsAll(List.of("DV_TEXT", "DV_CODED_TEXT"))) {
                WebTemplateNode merged = new WebTemplateNode();
                merged.setId(node.getId(false));
                merged.setName(node.getName());
                merged.setMax(node.getMax());
                merged.setMin(node.getMin());
                merged.setRmType("DV_CODED_TEXT");
                WebTemplateNode codedTextValue = node.findChildById("coded_text_value").orElseThrow();
                merged.getInputs().addAll(codedTextValue.getInputs());
                merged.setAqlPath(codedTextValue.getAqlPath());
                merged.getLocalizedDescriptions().putAll(node.getLocalizedDescriptions());
                merged.getLocalizedNames().putAll(node.getLocalizedNames());
                merged.setLocalizedName(node.getLocalizedName());
                WebTemplateInput other = new WebTemplateInput();
                other.setType("TEXT");
                other.setSuffix("other");
                merged.getInputs().add(other);
                node.getChildren().add(merged);
            }
        }


        //Push inputs for DV_CODED_TEXT up
        if (node.getRmType().equals("DV_CODED_TEXT")) {
            List<WebTemplateNode> matching = node.findMatching(n -> n.getRmType().equals("CODE_PHRASE"));
            if (!matching.isEmpty()) {
                node.getInputs().addAll(matching.get(0).getInputs());
                WebTemplateInput code = node.getInputs().get(0);
                if (code.getList().isEmpty()) {
                    WebTemplateInput value = new WebTemplateInput();
                    value.setType("TEXT");
                    value.setSuffix("value");
                    value.setTerminology(code.getTerminology());
                }
            }
        }


        addRMAttributes(node, aqlPath, termDefinitionMap);

        makeIdUnique(node);


        return node;
    }

    public static void makeIdUnique(WebTemplateNode node) {
        //Make ids unique in for a children
        node.getChildren().stream()
                .collect(Collectors.groupingBy(node1 -> node1.getId(false)))
                .values()
                .forEach(l -> {
                            if (l.size() > 1) {
                                for (int i = 0; i < l.size(); i++) {
                                    if (i > 0) {
                                        WebTemplateNode n = l.get(i);
                                        n.setOptionalIdNumber((i + 1));
                                    }
                                }
                            } else {
                                l.get(0).setOptionalIdNumber(null);
                            }
                        }
                );
    }

    private void addRMAttributes(WebTemplateNode node, String aqlPath, Map<String, Map<String, TermDefinition>> termDefinitionMap) {
        //Add RM Attributes
        RMTypeInfo typeInfo = ARCHIE_RM_INFO_LOOKUP.getTypeInfo(node.getRmType());
        if (typeInfo != null && (Locatable.class.isAssignableFrom(typeInfo.getJavaClass()) || EventContext.class.isAssignableFrom(typeInfo.getJavaClass()) || IsmTransition.class.isAssignableFrom(typeInfo.getJavaClass())) && !Element.class.isAssignableFrom(typeInfo.getJavaClass())) {

            node.getChildren().addAll(typeInfo.getAttributes()
                    .values()
                    .stream()
                    .filter(s -> !List.of("value").contains(s.getRmName()))
                    .filter(s -> !Locatable.class.isAssignableFrom(s.getTypeInCollection()))
                    .map(i -> buildNodeForAttribute(i, aqlPath, termDefinitionMap))
                    //only add if not already there
                    .filter(n -> node.getChildren().stream().map(WebTemplateNode::getId).noneMatch(s -> s.equals(n.getId())))
                    .collect(Collectors.toList()));
        }
    }

    private WebTemplateNode buildNodeForAttribute(RMAttributeInfo attributeInfo, String aqlPath, Map<String, Map<String, TermDefinition>> termDefinitionMap) {
        WebTemplateNode node = new WebTemplateNode();
        node.setAqlPath(aqlPath + PATH_DIVIDER + attributeInfo.getRmName());
        node.setName(attributeInfo.getRmName());
        node.setId(buildId(attributeInfo.getRmName()));
        node.setRmType(attributeInfo.getTypeNameInCollection());
        node.setMax(1);
        node.setMin(0);
        return node;
    }

    private WebTemplateNode parseCOBJECT(COBJECT cobject, String aqlPath, Map<String, Map<String, TermDefinition>> termDefinitionMap, String rmAttributeName) {


        if (cobject instanceof CARCHETYPEROOT) {
            String nodeId = ((CARCHETYPEROOT) cobject).getArchetypeId().getValue();
            final String pathLoop;
            if (StringUtils.isNotBlank(nodeId)) {
                pathLoop = aqlPath + "[" + nodeId + "]";
            } else {
                pathLoop = aqlPath;
            }
            return parseCARCHETYPEROO((CARCHETYPEROOT) cobject, pathLoop);

        } else if (cobject instanceof CCOMPLEXOBJECT) {
            String nodeId = cobject.getNodeId();
            final String pathLoop;
            if (StringUtils.isNotBlank(nodeId)) {
                pathLoop = aqlPath + "[" + nodeId + "]";
            } else {
                pathLoop = aqlPath;
            }
            return parseCCOMPLEXOBJECT((CCOMPLEXOBJECT) cobject, pathLoop, termDefinitionMap, rmAttributeName);

        } else if (cobject instanceof CDOMAINTYPE) {
            String nodeId = cobject.getNodeId();
            final String pathLoop;
            if (StringUtils.isNotBlank(nodeId)) {
                pathLoop = aqlPath + "[" + nodeId + "]";
            } else {
                pathLoop = aqlPath;
            }
            return parseCDOMAINTYPE((CDOMAINTYPE) cobject, pathLoop, termDefinitionMap, rmAttributeName);
        }
        return null;
    }

    private WebTemplateNode parseCDOMAINTYPE(CDOMAINTYPE cdomaintype, String aqlPath, Map<String, Map<String, TermDefinition>> termDefinitionMap, String rmAttributeName) {

        WebTemplateNode node = buildNode(cdomaintype, rmAttributeName, termDefinitionMap);
        node.setAqlPath(aqlPath);
        if (cdomaintype instanceof CDVSTATE) {

        } else if (cdomaintype instanceof CDVQUANTITY) {

            WebTemplateInput magnitude = new WebTemplateInput();
            magnitude.setSuffix("magnitude");
            magnitude.setType("DECIMAL");
            node.getInputs().add(magnitude);

            WebTemplateInput unit = new WebTemplateInput();
            unit.setSuffix("unit");
            unit.setType("CODED_TEXT");
            node.getInputs().add(unit);

            Arrays.stream(((CDVQUANTITY) cdomaintype).getListArray()).forEach(
                    o -> {
                        WebTemplateInputValue value = new WebTemplateInputValue();
                        value.setLabel(o.getUnits());
                        value.setValue(o.getUnits());
                        unit.getList().add(value);
                    }
            );

        } else if (cdomaintype instanceof CDVORDINAL) {
            WebTemplateInput code = new WebTemplateInput();
            code.setType("CODED_TEXT");
            node.getInputs().add(code);
            Arrays.stream(((CDVORDINAL) cdomaintype).getListArray()).forEach(
                    o -> {
                        WebTemplateInputValue value = new WebTemplateInputValue();
                        value.setOrdinal(o.getValue());
                        value.setValue(o.getSymbol().getDefiningCode().getCodeString());
                        value.getLocalizedLabels().putAll(
                                Optional.ofNullable(termDefinitionMap.get(value.getValue()))
                                        .map(Map::entrySet)
                                        .stream()
                                        .flatMap(Set::stream)
                                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue()))
                        );
                        value.setLabel(value.getLocalizedLabels().get(defaultLanguage));
                        code.getList().add(value);
                    }
            );


        } else if (cdomaintype instanceof CCODEPHRASE) {
            WebTemplateInput code = new WebTemplateInput();
            code.setType("CODED_TEXT");
            node.getInputs().add(code);
            if (cdomaintype instanceof CCODEREFERENCE) {
                code.setTerminology(Optional.of((CCODEREFERENCE) cdomaintype).map(CCODEREFERENCE::getReferenceSetUri).map(s -> StringUtils.removeStart(s, "terminology:")).orElse(null));
            } else {
                code.setTerminology(Optional.of((CCODEPHRASE) cdomaintype).map(CCODEPHRASE::getTerminologyId).map(OBJECTID::getValue).orElse(null));
            }
            if (code.getTerminology().equals("local")) {
                Arrays.stream(((CCODEPHRASE) cdomaintype).getCodeListArray()).forEach(
                        o -> {
                            WebTemplateInputValue value = new WebTemplateInputValue();
                            TermDefinition termDefinition = termDefinitionMap.get(o).get(defaultLanguage);
                            value.setValue(termDefinition.getCode());
                            value.setLabel(termDefinition.getValue());
                            code.getList().add(value);
                        }

                );
            } else if (code.getTerminology().equals("openehr")) {
                ValueSet valueSet = TerminologyProvider.findOpenEhrValueSet(code.getTerminology(), ((CCODEPHRASE) cdomaintype).getCodeListArray());
                valueSet.getTherms().forEach(
                        t -> {
                            WebTemplateInputValue value = new WebTemplateInputValue();
                            value.setValue(t.getCode());
                            value.setLabel(t.getValue());
                            code.getList().add(value);
                        }
                );
            }

            if (code.getList().isEmpty() && StringUtils.isBlank(code.getTerminology())) {
                code.setType("TEXT");
            } else {
                code.setType("CODED_TEXT");
            }
        } else {
            throw new RuntimeException(String.format("Unexpected class: %s", cdomaintype.getClass().getSimpleName()));
        }
        return node;
    }

    private WebTemplateNode buildNode(COBJECT cobject, String rmAttributeName, Map<String, Map<String, TermDefinition>> termDefinitionMap) {
        WebTemplateNode node = new WebTemplateNode();
        node.setRmType(cobject.getRmTypeName());
        IntervalOfInteger occurrences = cobject.getOccurrences();
        node.setMin(occurrences.getLowerUnbounded() ? -1 : occurrences.getLower());
        node.setMax(occurrences.getUpperUnbounded() ? -1 : occurrences.getUpper());
        String nodeId = cobject.getNodeId();
        if (StringUtils.isNotBlank(nodeId)) {

            Optional<String> expliziteName;
            if (cobject instanceof CCOMPLEXOBJECT) {
                expliziteName = OptNameHelper.extractName((CCOMPLEXOBJECT) cobject);
            } else {
                expliziteName = Optional.empty();
            }

            String name = expliziteName.orElse(termDefinitionMap.get(nodeId).get(defaultLanguage).getValue());
            node.setName(name);
            node.setId(buildId(name));
            node.setNodeId(nodeId);
            node.setLocalizedName(name);
            node.getLocalizedNames().putAll(termDefinitionMap.get(nodeId).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue())));
            node.getLocalizedDescriptions().putAll(termDefinitionMap.get(nodeId).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getDescription())));
        } else {
            String name = StringUtils.isNotBlank(rmAttributeName) ? rmAttributeName : cobject.getRmTypeName();
            node.setId(buildId(name));
            node.setName(name);
            node.setLocalizedName(name);
        }
        return node;
    }

    private String buildId(String term) {

        String normalTerm = StringUtils.normalizeSpace(term.toLowerCase().replaceAll("[^a-z0-9äüöß._\\-]", " ").trim()).replace(" ", "_");
        if (StringUtils.isNumeric(normalTerm.substring(0, 1))) {
            normalTerm = "a" + normalTerm;
        }

        return normalTerm;
    }
}
