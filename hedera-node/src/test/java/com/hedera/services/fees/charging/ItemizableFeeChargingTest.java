package com.hedera.services.fees.charging;

/*-
 * ‌
 * Hedera Services Node
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

import com.hedera.services.context.properties.PropertySource;
import com.hedera.services.fees.FeeExemptions;
import com.hedera.services.ledger.HederaLedger;
import com.hedera.services.utils.SignedTxnAccessor;
import com.hedera.test.utils.IdUtils;
import com.hederahashgraph.api.proto.java.AccountAmount;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hederahashgraph.api.proto.java.TransactionBody;
import com.hederahashgraph.api.proto.java.TransferList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

import static com.hedera.services.fees.charging.ItemizableFeeCharging.CACHE_RECORD_FEE;
import static com.hedera.services.fees.charging.ItemizableFeeCharging.NETWORK_NODE_SERVICE_FEES;
import static com.hedera.services.fees.charging.ItemizableFeeCharging.THRESHOLD_RECORD_FEE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static com.hedera.services.fees.charging.ItemizableFeeCharging.NETWORK_FEE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.*;
import com.hedera.services.fees.TxnFeeType;
import static com.hedera.services.fees.TxnFeeType.*;

@RunWith(JUnitPlatform.class)
class ItemizableFeeChargingTest {
	long network = 500L, service = 200L, node = 100L, thresholdRecord = 150L, cacheRecord = 50L;

	AccountID submittingNode = IdUtils.asAccount("0.0.3");
	AccountID payer = IdUtils.asAccount("0.0.1001");
	AccountID funding = IdUtils.asAccount("0.0.98");
	AccountID participant = IdUtils.asAccount("0.0.2002");

	HederaLedger ledger;
	FeeExemptions exemptions;
	PropertySource properties;
	TransactionBody txn;
	SignedTxnAccessor accessor;

	ItemizableFeeCharging subject;

	@BeforeEach
	private void setup() {
		txn = mock(TransactionBody.class);
		ledger = mock(HederaLedger.class);
		accessor = mock(SignedTxnAccessor.class);
		exemptions = mock(FeeExemptions.class);
		properties = mock(PropertySource.class);

		given(txn.getNodeAccountID()).willReturn(submittingNode);
		given(accessor.getTxn()).willReturn(txn);
		given(accessor.getPayer()).willReturn(payer);
		given(properties.getAccountProperty("ledger.funding.account")).willReturn(funding);

		subject = new ItemizableFeeCharging(exemptions, properties);
		subject.setLedger(ledger);

		subject.resetFor(accessor);
	}

	@Test
	public void reportsChargedFees() {
		givenKnownFeeAmounts();

		// when:
		subject.chargePayer(NETWORK_NODE_SERVICE_FEES);

		// then:
		assertEquals(network, subject.chargedToPayer(NETWORK));
		assertEquals(node, subject.chargedToPayer(NODE));
		assertEquals(service, subject.chargedToPayer(SERVICE));
		// and:
		assertEquals(0, subject.chargedToPayer(CACHE_RECORD));
	}

	@Test
	public void reportsTotalNonThresholdPayerFees() {
		givenKnownFeeAmounts();

		// when:
		subject.chargePayer(EnumSet.of(NODE, THRESHOLD_RECORD, CACHE_RECORD));

		// then:
		assertEquals(node + cacheRecord, subject.totalNonThresholdFeesChargedToPayer());
	}

	@Test
	public void reportsNumChargedThresholdFees() {
		givenKnownFeeAmounts();

		// when:
		subject.chargeParticipant(payer, THRESHOLD_RECORD_FEE);
		subject.chargeParticipant(participant, THRESHOLD_RECORD_FEE);

		// then:
		assertEquals(2, subject.numThresholdFeesCharged());
	}

	@Test
	public void doesntRecordSelfPayments() {
		givenKnownFeeAmounts();
		given(accessor.getPayer()).willReturn(submittingNode);

		// when:
		subject.chargePayer(EnumSet.of(NODE));
		subject.chargePayerUpTo(EnumSet.of(NODE));

		// then:
		assertTrue(subject.payerFeesCharged.isEmpty());
	}

	@Test
	public void chargesNodeOnlyWhatsAvailableIfNecessary() {
		givenKnownFeeAmounts();
		given(ledger.getBalance(submittingNode)).willReturn(network / 2);

		// when:
		subject.chargeNodeUpTo(EnumSet.of(NETWORK));

		// then:
		verify(ledger).doTransfer(submittingNode, funding, network / 2);
		assertEquals(network / 2, subject.nodeFeesCharged.get(NETWORK).longValue());
	}

	@Test
	public void chargesNodeSuggestedIfPossible() {
		givenKnownFeeAmounts();
		given(ledger.getBalance(submittingNode)).willReturn(network * 2);

		// when:
		subject.chargeNodeUpTo(EnumSet.of(NETWORK));

		// then:
		verify(ledger).doTransfer(submittingNode, funding, network);
		assertEquals(network, subject.nodeFeesCharged.get(NETWORK).longValue());
	}

	@Test
	public void ignoresDegenerateFees() {
		// given:
		subject.setFor(THRESHOLD_RECORD, 0L);

		// when:
		subject.chargeParticipant(participant, EnumSet.of(THRESHOLD_RECORD));

		// then:
		verify(ledger, never()).doTransfer(any(), any(), anyLong());
	}

	@Test
	public void chargesExemptParticipantNothingForRecord() {
		// setup:
		EnumSet<TxnFeeType> thresholdRecordFee = EnumSet.of(THRESHOLD_RECORD);

		givenKnownFeeAmounts();
		given(exemptions.isExemptFromRecordFees(participant)).willReturn(true);

		// when:
		subject.chargeParticipant(participant, thresholdRecordFee);

		// then:
		verify(ledger, never()).doTransfer(any(), any(), anyLong());
		// and:
		assertTrue(subject.thresholdFeePayers.isEmpty());
	}

	@Test
	public void chargesPayerNothingWhenExempt() {
		// setup:
		EnumSet<TxnFeeType> allPossibleFees = EnumSet.of(NETWORK, NODE, SERVICE, CACHE_RECORD, THRESHOLD_RECORD);

		givenKnownFeeAmounts();
		given(ledger.getBalance(payer)).willReturn(Long.MAX_VALUE);
		given(exemptions.isExemptFromFees(txn)).willReturn(true);

		// when:
		subject.chargePayer(allPossibleFees);
		subject.chargePayerUpTo(allPossibleFees);

		// then:
		verify(ledger, never()).doTransfer(any(), any(), anyLong());
		// and:
		assertTrue(subject.payerFeesCharged.isEmpty());
	}

	@Test
	public void itemizesIrresponsibleSubmission() {
		givenKnownFeeAmounts();
		given(ledger.getBalance(submittingNode)).willReturn(network - 1);

		// when:
		subject.chargeNodeUpTo(NETWORK_FEE);
		// and:
		TransferList itemizedFees = subject.itemizedFees();

		// then:
		assertThat(
				itemizedFees.getAccountAmountsList(),
				contains(
						aa(funding, network - 1),
						aa(submittingNode, 1 - network)));
	}

	@Test
	public void itemizesWhenNodeIsPayer() {
		givenKnownFeeAmounts();
		given(ledger.getBalance(any())).willReturn(Long.MAX_VALUE);
		given(accessor.getPayer()).willReturn(submittingNode);

		// when:
		subject.chargePayer(NETWORK_NODE_SERVICE_FEES);
		subject.chargePayer(CACHE_RECORD_FEE);
		subject.chargeParticipant(participant, THRESHOLD_RECORD_FEE);
		subject.chargeParticipant(payer, THRESHOLD_RECORD_FEE);
		// and:
		TransferList itemizedFees = subject.itemizedFees();

		// then:
		assertThat(
				itemizedFees.getAccountAmountsList(),
				contains(
						aa(funding, network),
						aa(submittingNode, -network),
						aa(funding, service),
						aa(submittingNode, -service),
						aa(submittingNode, -cacheRecord),
						aa(funding, cacheRecord),
						aa(funding, thresholdRecord),
						aa(payer, -thresholdRecord),
						aa(funding, thresholdRecord),
						aa(participant, -thresholdRecord)));
	}

	@Test
	public void itemizesStandardEvents() {
		givenKnownFeeAmounts();
		given(ledger.getBalance(any())).willReturn(Long.MAX_VALUE);

		// when:
		subject.chargePayer(NETWORK_NODE_SERVICE_FEES);
		subject.chargePayer(CACHE_RECORD_FEE);
		subject.chargeParticipant(participant, THRESHOLD_RECORD_FEE);
		subject.chargeParticipant(payer, THRESHOLD_RECORD_FEE);
		// and:
		TransferList itemizedFees = subject.itemizedFees();

		// then:
		assertThat(
				itemizedFees.getAccountAmountsList(),
				contains(
						aa(funding, network),
						aa(payer, -network),
						aa(submittingNode, node),
						aa(payer, -node),
						aa(funding, service),
						aa(payer, -service),
						aa(payer, -cacheRecord),
						aa(funding, cacheRecord),
						aa(funding, thresholdRecord),
						aa(payer, -thresholdRecord),
						aa(funding, thresholdRecord),
						aa(participant, -thresholdRecord)));
	}

	private AccountAmount aa(AccountID who, long what) {
		return AccountAmount.newBuilder().setAccountID(who).setAmount(what).build();
	}

	@Test
	public void chargesParticipantWithCorrectBeneficiaries() {
		givenKnownFeeAmounts();

		// when:
		subject.chargeParticipant(participant, EnumSet.of(NETWORK, NODE, SERVICE));

		// then:
		verify(ledger).doTransfer(participant, funding, network);
		verify(ledger).doTransfer(participant, funding, service);
		verify(ledger).doTransfer(participant, submittingNode, node);
		// and:
		assertTrue(subject.nodeFeesCharged.isEmpty());
		assertTrue(subject.payerFeesCharged.isEmpty());
	}

	@Test
	public void recognizesParticipantsWhoPayedForThresholdRecords() {
		givenKnownFeeAmounts();

		// when:
		subject.chargeParticipant(payer, EnumSet.of(NETWORK, CACHE_RECORD));
		subject.chargeParticipant(participant, EnumSet.of(THRESHOLD_RECORD));

		// then:
		assertEquals(1, subject.thresholdFeePayers.size());
		assertTrue(subject.thresholdFeePayers.contains(participant));
		// and:
		assertEquals(2, subject.payerFeesCharged.size());
		assertEquals(network, subject.payerFeesCharged.get(NETWORK).longValue());
		assertEquals(cacheRecord, subject.payerFeesCharged.get(CACHE_RECORD).longValue());
	}

	@Test
	public void chargesPayerWithCorrectBeneficiaries() {
		givenKnownFeeAmounts();

		// when:
		subject.chargePayer(EnumSet.of(NETWORK, NODE, SERVICE));

		// then:
		verify(ledger).doTransfer(payer, funding, network);
		verify(ledger).doTransfer(payer, funding, service);
		verify(ledger).doTransfer(payer, submittingNode, node);
		// and:
		assertEquals(network, subject.payerFeesCharged.get(NETWORK).longValue());
		assertEquals(service, subject.payerFeesCharged.get(SERVICE).longValue());
		assertEquals(node, subject.payerFeesCharged.get(NODE).longValue());
	}

	@Test
	public void chargesPayerWithCorrectBeneficiariesUpToAvailable() {
		givenKnownFeeAmounts();
		given(ledger.getBalance(payer))
				.willReturn(network + (node / 2))
				.willReturn(node / 2);

		// when:
		subject.chargePayerUpTo(EnumSet.of(NETWORK, NODE));

		// then:
		verify(ledger).doTransfer(payer, funding, network);
		verify(ledger).doTransfer(payer, submittingNode, node / 2);
		// and:
		assertEquals(network, subject.payerFeesCharged.get(NETWORK).longValue());
		assertEquals(node / 2, subject.payerFeesCharged.get(NODE).longValue());
	}

	@Test
	public void resetsForNewTxn() {
		// setup:
		Set<AccountID> threshPayers = mock(Set.class);
		EnumMap<TxnFeeType, Long> paidByPayer = mock(EnumMap.class);
		EnumMap<TxnFeeType, Long> paidByNode = mock(EnumMap.class);

		// given:
		subject.funding = submittingNode;
		subject.nodeFeesCharged = paidByNode;
		subject.payerFeesCharged = paidByPayer;
		subject.thresholdFeePayers = threshPayers;

		// when:
		subject.resetFor(accessor);

		// then:
		verify(paidByNode).clear();
		verify(paidByPayer).clear();
		verify(threshPayers).clear();
		assertEquals(funding, subject.funding);
	}

	private void givenKnownFeeAmounts() {
		subject.setFor(NETWORK, network);
		subject.setFor(SERVICE, service);
		subject.setFor(NODE, node);
		subject.setFor(CACHE_RECORD, cacheRecord);
		subject.setFor(THRESHOLD_RECORD, thresholdRecord);
	}
}
