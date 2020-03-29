/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017-2018 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.shared.debug.Debug;

import java.util.*;
import javax.inject.Inject;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Action.ActionBuilder;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.Node.Metadata;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.OutcomeProvider.Outcome;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.i18n.PreferredLocales;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * An authentication node which calculates an aggregate risk score
 *
 * @author Keith Daly - ForgeRock
 * @version 6.5.0.1
 *
 */
@Node.Metadata(outcomeProvider=RiskScoreAggregatorNode.RiskScoreAggregatorOutcomeProvider.class, configClass=RiskScoreAggregatorNode.Config.class)
public class RiskScoreAggregatorNode implements Node
{
    private final Config config;
    private final Realm realm;
    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/RiskScoreAggregatorNode";
    private static final String DEBUG_FILE = "RiskScoreNode";
    private static final String NODE_NAME = "RiskScoreAggregatorNode";
    protected Debug debug = Debug.getInstance("RiskScoreNode");

    /**
     * Configuration for the node.
     */
    public interface Config {

        @Attribute(order = 100)
        default String riskScoresAttr() { return "riskSCORES"; }

        @Attribute(order = 200)
        default ScoreModel scoreModel() { return ScoreModel.MAXIMUM; }

        @Attribute(order = 310)
        default String minLowRiskScore() { return "1"; }

        @Attribute(order = 320)
        default String minModerateRiskScore() { return "2"; }

        @Attribute(order = 330)
        default String minHighRiskScore() { return "3"; }

    }


    /**
     * Create the node using Guice injection. Just-in-time bindings can be used to obtain instances of other classes
     * from the plugin.
     *
     * @param config The service config.
     * @param realm The realm the node is in.
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Inject
    public RiskScoreAggregatorNode(@Assisted Config config, @Assisted Realm realm) throws NodeProcessException {
        this.config = config;
        this.realm = realm;
    }

    /**
     * Main processing method
     *
     * @param context
     * @return
     * @throws NodeProcessException If the configuration was not valid.
     */
    @Override
    public Action process(TreeContext context)
            throws NodeProcessException
    {
        this.debug.message("[RiskScoreNode:RiskScoreAggregatorNode]  RiskAggregatorNode::process()");

        Action.ActionBuilder action = goTo(RiskScoreAggregatorOutcome.NONE);
        JsonValue newState = context.sharedState.copy();

        Double totalWScore = 0.0;
        Double totalWeight = 0.0;
        Double totalAverage = 0.0;
        Double maxScore = 0.0;
        RiskScoreAggregatorOutcome riskLevel = RiskScoreAggregatorOutcome.NONE;

        String riskScoreAttr = config.riskScoresAttr();
        ScoreModel scoreModel = config.scoreModel();
        Double minLowRiskScore = Double.parseDouble(config.minLowRiskScore());
        Double minModerateRiskScore = Double.parseDouble(config.minModerateRiskScore());
        Double minHighRiskScore = Double.parseDouble(config.minHighRiskScore());

        Map riskScores = new HashMap();
        List<Map> riskEntries = new ArrayList<Map>();

        if (!context.sharedState.get(config.riskScoresAttr()).isNull()) {
            riskEntries = (List) context.sharedState.get(config.riskScoresAttr()).asMap().get("riskEntries");
            debug.message("[" + DEBUG_FILE + ":" + NODE_NAME+ "] - RISK ENTRIES - " + riskEntries);
        }

        try {
            for (int i =0; i < riskEntries.size(); i++) {
                debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] " + riskEntries.get(i));
                Double score = Double.parseDouble( (String) riskEntries.get(i).get("riskScore") );
                Double weight = Double.parseDouble( (String) riskEntries.get(i).get("riskWeight") );
                totalWScore += (score * weight);
                totalWeight += weight;
                if (maxScore < score) maxScore = score;
            }
            totalAverage = totalWScore / totalWeight;
        } catch (Exception e) {
        }

        debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] TotalWScore : " + totalWScore + " | TotalWeight : " + totalWeight + " |  TotalAverage : " + totalAverage + " | MaxScore : " + maxScore);
        newState.put(config.riskScoresAttr(), riskEntries);
        newState.put("riskTotalWeightedScore", totalWScore);
        newState.put("riskAverageScore", totalAverage);
        newState.put("riskMaximumScore", maxScore);

        switch (config.scoreModel()) {
            case AVERAGE:
                newState.put("riskScoreModel", "Average");
                if (totalAverage < minLowRiskScore) {
                    debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] NO RISK - AVERAGE RISK MODEL");
                    newState.put("riskEvaluation", "None");
                    riskLevel = RiskScoreAggregatorOutcome.NONE;
                    action = goTo(RiskScoreAggregatorOutcome.NONE);
                } else if (totalAverage < minModerateRiskScore) {
                    debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] LOW RISK - AVERAGE RISK MODEL");
                    newState.put("riskEvaluation", "Low");
                    riskLevel = RiskScoreAggregatorOutcome.LOW;
                    action = goTo(RiskScoreAggregatorOutcome.LOW);
                } else if (totalAverage < minHighRiskScore) {
                    debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] MODERATE RISK - AVERAGE RISK MODEL");
                    newState.put("riskEvaluation", "Moderate");
                    riskLevel = RiskScoreAggregatorOutcome.MODERATE;
                    action = goTo(RiskScoreAggregatorOutcome.MODERATE);
                } else {
                    debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] HIGH RISK - AVERAGE RISK MODEL");
                    newState.put("riskEvaluation", "High");
                    riskLevel = RiskScoreAggregatorOutcome.HIGH;
                    action = goTo(RiskScoreAggregatorOutcome.HIGH);
                }
                break;
            case MAXIMUM:
                newState.put("riskScoreModel", "Maximum");
                if (maxScore < minLowRiskScore) {
                    debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] NO RISK - MAXIMUM RISK MODEL");
                    riskLevel = RiskScoreAggregatorOutcome.NONE;
                    newState.put("riskEvaluation", "None");
                    action = goTo(RiskScoreAggregatorOutcome.NONE);
                } else if (maxScore < minModerateRiskScore) {
                    debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] LOW RISK - MAXIMUM RISK MODEL");
                    newState.put("riskEvaluation", "Low");
                    riskLevel = RiskScoreAggregatorOutcome.LOW;
                    action = goTo(RiskScoreAggregatorOutcome.LOW);
                } else if (maxScore < minHighRiskScore) {
                    debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] MODERATE RISK - MAXIMUM RISK MODEL");
                    newState.put("riskEvaluation", "Moderate");
                    riskLevel = RiskScoreAggregatorOutcome.MODERATE;
                    action = goTo(RiskScoreAggregatorOutcome.MODERATE);
                } else {
                    debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] HIGH RISK - MAXIMUM RISK MODEL");
                    newState.put("riskEvaluation", "High");
                    riskLevel = RiskScoreAggregatorOutcome.HIGH;
                    action = goTo(RiskScoreAggregatorOutcome.HIGH);
                }
                break;
            case TOTAL:
                newState.put("riskScoreModel", "Total");
                if (totalWScore < minLowRiskScore) {
                    debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] NO RISK - TOTAL RISK MODEL");
                    riskLevel = RiskScoreAggregatorOutcome.NONE;
                    newState.put("riskEvaluation", "None");
                    action = goTo(RiskScoreAggregatorOutcome.NONE);
                } else if (totalWScore < minModerateRiskScore) {
                    debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] LOW RISK - TOTAL RISK MODEL");
                    newState.put("riskEvaluation", "Low");
                    riskLevel = RiskScoreAggregatorOutcome.LOW;
                    action = goTo(RiskScoreAggregatorOutcome.LOW);
                } else if (totalWScore < minHighRiskScore) {
                    debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] MODERATE RISK - TOTAL RISK MODEL");
                    newState.put("riskEvaluation", "Moderate");
                    riskLevel = RiskScoreAggregatorOutcome.MODERATE;
                    action = goTo(RiskScoreAggregatorOutcome.MODERATE);
                } else {
                    debug.message("[" + DEBUG_FILE + ":" + NODE_NAME + "] HIGH RISK - TOTAL RISK MODEL");
                    newState.put("riskEvaluation", "High");
                    riskLevel = RiskScoreAggregatorOutcome.HIGH;
                    action = goTo(RiskScoreAggregatorOutcome.HIGH);
                }
                break;
        }
        return action.replaceSharedState(newState).build();
    }

    private Action.ActionBuilder goTo(RiskScoreAggregatorOutcome outcome) {
        return Action.goTo(outcome.name());
    }

    /**
     * The possible outcomes for the GeoLocationNode - Risk Levels
     */
    public enum RiskScoreAggregatorOutcome {
        NONE,
        LOW,
        MODERATE,
        HIGH,
        UNKNOWN
    }

    public enum ScoreModel {
        MAXIMUM,
        AVERAGE,
        TOTAL
    }

    /**
     * Defines the possible outcomes from this GeoLocation node.
     */
    public static class RiskScoreAggregatorOutcomeProvider implements org.forgerock.openam.auth.node.api.OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(RiskScoreAggregatorNode.BUNDLE,
                    RiskScoreAggregatorNode.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(RiskScoreAggregatorOutcome.NONE.name(), bundle.getString("noRiskOutcome")),
                    new Outcome(RiskScoreAggregatorOutcome.LOW.name(), bundle.getString("lowRiskOutcome")),
                    new Outcome(RiskScoreAggregatorOutcome.MODERATE.name(), bundle.getString("moderateRiskOutcome")),
                    new Outcome(RiskScoreAggregatorOutcome.HIGH.name(), bundle.getString("highRiskOutcome")),
                    new Outcome(RiskScoreAggregatorOutcome.UNKNOWN.name(), bundle.getString("unknownRiskOutcome"))
            );
        }
    }

}
