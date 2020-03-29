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
 * Copyright 2018 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import javax.inject.Inject;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.*;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.shared.debug.Debug;

import com.google.inject.assistedinject.Assisted;

import org.json.*;

import java.util.*;

/**
 *
 * An authentication node which adds a risk score entry to the shared state
 *
 * @author Keith Daly - ForgeRock
 * @version 6.5.0.1
 *
 */

@Node.Metadata( outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
                configClass = RiskScoreAddNode.Config.class)
public class RiskScoreAddNode extends SingleOutcomeNode {

    private final Logger logger = LoggerFactory.getLogger(RiskScoreAddNode.class);
    private final Config config;
    private final Realm realm;

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/RiskScoreAddNode";
    private final static String DEBUG_FILE = "RiskScoreNode";
    private final static String NODE_NAME = "RiskScoreAddNode";
    protected Debug debug = Debug.getInstance(DEBUG_FILE);

    private String riskName;
    private String riskScore;
    private String riskWeight;

    /**
     * Configuration for the node.
     */
    public interface Config {

        @Attribute(order = 100)
        default String riskScoresAttr() { return "RISK_SCORES"; }

        @Attribute(order = 200)
        default String riskName() { return "Risk Name"; }

        @Attribute(order = 300)
        default String riskScore() { return "2"; }

        @Attribute(order = 400)
        default String riskWeight() { return "1"; }

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
    public RiskScoreAddNode(@Assisted Config config, @Assisted Realm realm) throws NodeProcessException {
        this.config = config;
        this.realm = realm;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
//        debug.error("[" + DEBUG_FILE + ":" + NODE_NAME+ "] " + " Process()");
        debug.message("[" + DEBUG_FILE + ":" + NODE_NAME+ "] " + " Process() :: " + config.riskName()  + " :: " + config.riskScore() + " :: " + config.riskWeight());

//        JSONObject risks = new JSONObject();
//        JSONObject risk = new JSONObject();
//        List listRisk = new ArrayList<String>();

        Map riskScores = new HashMap();
        List<Map> riskEntries = new ArrayList<Map>();
        Map riskEntry = new HashMap();

        JsonValue newState = context.sharedState.copy();


        if (!context.sharedState.get(config.riskScoresAttr()).isNull()) {
            riskEntries = (List) context.sharedState.get(config.riskScoresAttr()).asMap().get("riskEntries");
            debug.message("[" + DEBUG_FILE + ":" + NODE_NAME+ "] - RISK ENTRIES - " + riskEntries);
        }

        try {
            riskEntry.put("riskName", config.riskName());
            riskEntry.put("riskScore", config.riskScore());
            riskEntry.put("riskWeight", config.riskWeight());

            riskEntries.add(riskEntry);

            riskScores.put("riskEntries", riskEntries);

        } catch (Exception e) {
            debug.warning("[" + DEBUG_FILE + ":" + NODE_NAME+ "] " + " Could not set risk entry");
        }

        newState.put(config.riskScoresAttr(), riskScores);

        return goToNext().replaceSharedState(newState).build();

    }

    /**
     * Converts JSON to bar-delimited string
     * @param json
     */
    private String riskJson2String(JSONObject json) {
        List listRisk = new ArrayList<String>();
        return "";
    }

    /**
     * Converts from bar-delimited field to JSON Object
     * @param text
     */
    private JSONObject riskString2Json(String text) {
        return new JSONObject();
    }

}
