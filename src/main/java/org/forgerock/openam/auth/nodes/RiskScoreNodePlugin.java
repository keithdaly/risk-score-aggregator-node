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

import static java.util.Arrays.asList;

import java.util.Map;

import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.plugins.PluginException;

import com.google.common.collect.ImmutableMap;

/**
 * Risk score node with no engine dependencies.
 *
 * @author Keith Daly - ForgeRock
 * @version 6.5.0.1
 *
 */
public class RiskScoreNodePlugin extends AbstractNodeAmPlugin {

	static private String currentVersion = "6.5.0.1";

	/**
	 * Specify the Map of list of node classes that the plugin is providing. These will then be installed and
	 *  registered at the appropriate times in plugin lifecycle.
	 *
	 * @return The list of node classes.
	 */
	@Override
	protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
		return ImmutableMap.of(
				currentVersion, asList(
						RiskScoreAddNode.class,
						RiskScoreAggregatorNode.class)
		);
	}

	/**
	 * This method will be called when the version returned by {@link #getPluginVersion()} is higher than the
	 * version already installed. This method will be called before the {@link #onStartup()} method.
	 *
	 * No need to implement this untils there are multiple versions of your auth node.
	 *
	 * @param fromVersion The old version of the plugin that has been installed.
	 */
	@Override
	public void upgrade(String fromVersion) throws PluginException {
		//if (fromVersion.equals("1.0.0")) {
			//pluginTools.upgradeAuthNode(ZeroPageLoginNode.class);
		//}
		super.upgrade(fromVersion);
	}

	/**
	 * The plugin version. This must be in semver (semantic version) format.
	 *
	 * @return The version of the plugin.
	 * @see <a href="https://www.osgi.org/wp-content/uploads/SemanticVersioning.pdf">Semantic Versioning</a>
	 */
	@Override
	public String getPluginVersion() {
		return RiskScoreNodePlugin.currentVersion;
	}

}
