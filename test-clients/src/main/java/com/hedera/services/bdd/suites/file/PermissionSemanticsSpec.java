package com.hedera.services.bdd.suites.file;

/*-
 * ‌
 * Hedera Services Test Clients
 * ​
 * Copyright (C) 2018 - 2020 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.hedera.services.bdd.spec.HapiApiSpec;
import com.hedera.services.bdd.spec.keys.ControlForKey;
import com.hedera.services.bdd.spec.keys.KeyFactory;
import com.hedera.services.bdd.spec.keys.KeyShape;
import com.hedera.services.bdd.suites.HapiApiSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.hedera.services.bdd.spec.HapiApiSpec.defaultHapiSpec;
import static com.hedera.services.bdd.spec.keys.KeyShape.sigs;
import static com.hedera.services.bdd.spec.keys.SigControl.OFF;
import static com.hedera.services.bdd.spec.keys.SigControl.ON;
import static com.hedera.services.bdd.spec.queries.QueryVerbs.getFileInfo;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.cryptoCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileAppend;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileCreate;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileDelete;
import static com.hedera.services.bdd.spec.transactions.TxnVerbs.fileUpdate;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.newKeyNamed;
import static com.hedera.services.bdd.spec.utilops.UtilVerbs.withOpContext;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.INVALID_SIGNATURE;
import static com.hederahashgraph.api.proto.java.ResponseCodeEnum.UNAUTHORIZED;

public class PermissionSemanticsSpec extends HapiApiSuite {
	private static final Logger log = LogManager.getLogger(PermissionSemanticsSpec.class);

	public static void main(String... args) {
		new PermissionSemanticsSpec().runSuiteSync();
	}

	@Override
	protected List<HapiApiSpec> getSpecsInSuite() {
		return List.of(new HapiApiSpec[] {
						allowsDeleteWithOneTopLevelSig(),
						supportsImmutableFiles(),
				}
		);
	}

	private HapiApiSpec supportsImmutableFiles() {
		long extensionSecs = 666L;
		AtomicLong approxExpiry = new AtomicLong();

		return defaultHapiSpec("SupportsImmutableFiles").given(
				newKeyNamed("neverToBeUsed").type(KeyFactory.KeyType.LIST),
				cryptoCreate("civilian"),
				fileCreate("eternal")
						.payingWith("civilian")
						.unmodifiable()
		).when(
				fileDelete("eternal")
						.payingWith("civilian")
						.signedBy("civilian")
						.hasKnownStatus(UNAUTHORIZED),
				fileAppend("eternal")
						.payingWith("civilian")
						.signedBy("civilian")
						.content("Ignored.")
						.hasKnownStatus(UNAUTHORIZED),
				fileUpdate("eternal")
						.payingWith("civilian")
						.signedBy("civilian")
						.contents("Ignored.")
						.hasKnownStatus(UNAUTHORIZED),
				fileUpdate("eternal")
						.payingWith("civilian")
						.signedBy("civilian", "neverToBeUsed")
						.wacl("neverToBeUsed")
						.hasKnownStatus(UNAUTHORIZED)
		).then(
				withOpContext((spec, opLog) -> {
					approxExpiry.set(spec.registry().getTimestamp("eternal").getSeconds());
				}),
				fileUpdate("eternal")
						.payingWith("civilian")
						.signedBy("civilian")
						.extendingExpiryBy(extensionSecs),
				getFileInfo("eternal")
						.isUnmodifiable()
						.hasExpiryPassing(l -> Math.abs(l - approxExpiry.get() - extensionSecs) < 5)
		);
	}

	private HapiApiSpec allowsDeleteWithOneTopLevelSig() {
		KeyShape wacl = KeyShape.listOf(
				KeyShape.SIMPLE,
				KeyShape.listOf(2));

		var deleteSig = wacl.signedWith(sigs(ON, sigs(OFF, OFF)));
		var failedDeleteSig = wacl.signedWith(sigs(OFF, sigs(OFF, ON)));

		var updateSig = wacl.signedWith(sigs(ON, sigs(ON, ON)));
		var failedUpdateSig = wacl.signedWith(sigs(ON, sigs(OFF, ON)));

		return defaultHapiSpec("AllowsDeleteWithOneTopLevelSig").given(
				newKeyNamed("wacl").shape(wacl)
		).when(
				fileCreate("tbd").key("wacl")
		).then(
				fileUpdate("tbd")
						.contents("Some more contents!")
						.signedBy(GENESIS, "wacl")
						.sigControl(ControlForKey.forKey("wacl", failedUpdateSig))
						.hasKnownStatus(INVALID_SIGNATURE),
				fileUpdate("tbd")
						.contents("Some new contents!")
						.signedBy(GENESIS, "wacl")
						.sigControl(ControlForKey.forKey("wacl", updateSig)),
				fileDelete("tbd")
						.signedBy(GENESIS, "wacl")
						.sigControl(ControlForKey.forKey("wacl", failedDeleteSig))
						.hasKnownStatus(INVALID_SIGNATURE),
				fileDelete("tbd")
						.signedBy(GENESIS, "wacl")
						.sigControl(ControlForKey.forKey("wacl", deleteSig))
		);
	}

	@Override
	protected Logger getResultsLogger() {
		return log;
	}
}

