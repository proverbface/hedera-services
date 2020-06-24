package com.hedera.services.context.domain.topic;

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

import com.hedera.services.state.merkle.MerkleTopic;
import com.hedera.test.utils.ByteArrayConverter;
import com.hedera.test.utils.InstantConverter;
import com.hedera.test.utils.JAccountIDConverter;
import com.hedera.test.utils.JEd25519KeyConverter;
import com.hedera.test.utils.JTimestampConverter;
import com.hedera.test.utils.TopicIDConverter;
import com.hederahashgraph.api.proto.java.TopicID;
import com.hedera.services.state.submerkle.EntityId;
import com.hedera.services.legacy.core.jproto.JEd25519Key;
import com.hedera.services.legacy.core.jproto.JKey;
import com.hedera.services.legacy.core.jproto.JKeyList;
import com.hedera.services.state.submerkle.RichInstant;
import org.apache.commons.codec.binary.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(JUnitPlatform.class)
public class MerkleTopicTest {
	@Test
	public void defaultConstructorTestingAccessors() {
		// given:
		var topic = new MerkleTopic();

		// expect:
		assertDefaultTopicAccessors(topic);
	}

	@Test
	public void constructorWithNullsTestingAccessors() {
		// given:
		var topic = new MerkleTopic(null, null, null, 0L, null, null);

		// expect:
		assertDefaultTopicAccessors(topic);
	}

	@Test
	public void constructorWithEmptyValuesTestingAccessors() {
		// given:
		var topic = new MerkleTopic("", new JKeyList(), new JKeyList(), 0L, new EntityId(), new RichInstant());

		// expect:
		assertDefaultTopicAccessors(topic);
	}

	@ParameterizedTest
	@CsvSource({
			"memo, 0000000000000000000000000000000000000000000000000000000000000000, 1111111111111111111111111111111111111111111111111111111111111111, 2, 3.4.5, 6666_777777777",
			"a, 12, 34, 5, 6.7.8, 1_0"
	})
	public void constructorWithValuesTestingAccessors(String memo,
												 	  @ConvertWith(JEd25519KeyConverter.class) JEd25519Key adminKey,
												  	  @ConvertWith(JEd25519KeyConverter.class) JEd25519Key submitKey,
												  	  long autoRenewDurationSeconds,
												  	  @ConvertWith(JAccountIDConverter.class) EntityId autoRenewAccountId,
												  	  @ConvertWith(JTimestampConverter.class) RichInstant expirationTimestamp
	) throws Exception {
		// given:
		var topic = new MerkleTopic(memo, adminKey, submitKey, autoRenewDurationSeconds, autoRenewAccountId,
				expirationTimestamp);

		// expect:
		assertPostConstructorAccessors(topic, memo, adminKey, submitKey, autoRenewDurationSeconds, autoRenewAccountId,
				expirationTimestamp);
		assertEquals(0L, topic.getSequenceNumber());
		assertFalse(topic.hasRunningHash());
		assertArrayEquals(new byte[48], topic.getRunningHash());
	}

	@Test
	public void copyConstructorWithDefaultsTestingAccessors() throws Exception {
		// given:
		var topic = new MerkleTopic(new MerkleTopic());

		// expect:
		assertDefaultTopicAccessors(topic);
	}

	@Test
	public void copyConstructorWithValuesTestingAccessors() throws Exception {
		// given:
		var memo = "memo";
		var adminKey = new JEd25519Key(new byte[32]);
		var submitKey = new JEd25519Key(new byte[32]);
		var autoRenewDurationSeconds = 4L;
		var autoRenewAccountId = new EntityId(1L, 2L, 3L);
		var expirationTimestamp = new RichInstant(111L, 222);
		var from = new MerkleTopic(memo, adminKey, submitKey, autoRenewDurationSeconds, autoRenewAccountId,
				expirationTimestamp);
		from.setRunningHash(new byte[48]);
		var topic = new MerkleTopic(from);

		// expect:
		assertPostConstructorAccessors(topic, memo, adminKey, submitKey, autoRenewDurationSeconds, autoRenewAccountId,
				expirationTimestamp);
		assertEquals(0L, topic.getSequenceNumber());
		assertTrue(topic.hasRunningHash());
		assertArrayEquals(from.getRunningHash(), topic.getRunningHash());
		assertNotSame(from.getAdminKey(), topic.getAdminKey());
		assertNotSame(from.getSubmitKey(), topic.getSubmitKey());
		assertNotSame(from.getAutoRenewAccountId(), topic.getAutoRenewAccountId());
		assertNotSame(from.getExpirationTimestamp(), topic.getExpirationTimestamp());
		assertNotSame(from.getRunningHash(), topic.getRunningHash());
	}

	@Test
	public void unimplementedMethodsThrowUnsupported() {
		// given:
		MerkleTopic merkleTopic = new MerkleTopic();

		// expect:
		assertThrows(UnsupportedOperationException.class, () -> merkleTopic.copyFrom(null));
		assertThrows(UnsupportedOperationException.class, () -> merkleTopic.copyFromExtra(null));
		assertThrows(UnsupportedOperationException.class, () -> merkleTopic.diffCopyTo(null, null));
		assertThrows(UnsupportedOperationException.class, () -> merkleTopic.diffCopyFrom(null, null));
	}

	@Test
	public void equalsDefault() {
		// expect:
		assertTrue(new MerkleTopic().equals(new MerkleTopic()));
	}

	@Test
	public void equalsNull() {
		// expect:
		assertFalse(new MerkleTopic().equals(null));
	}

	@Test
	public void equalsWrongClass() {
		// expect:
		assertFalse(new MerkleTopic().equals(""));
	}

	@Test
	public void equalsSame() {
		// given:
		var topic = new MerkleTopic();
		// expect:
		assertTrue(topic.equals(topic));
	}

	@ParameterizedTest
	@CsvSource({
			"memo, 0000000000000000000000000000000000000000000000000000000000000000, 1111111111111111111111111111111111111111111111111111111111111111, 2, 3.4.5, 6666_777777777",
			", , , 0, , 0_0"
	})
	public void equalsViaCopy(String memo, @ConvertWith(JEd25519KeyConverter.class) JEd25519Key adminKey,
							  @ConvertWith(JEd25519KeyConverter.class) JEd25519Key submitKey,
							  long autoRenewDurationSeconds,
							  @ConvertWith(JAccountIDConverter.class) EntityId autoRenewAccountId,
							  @ConvertWith(JTimestampConverter.class) RichInstant expirationTimestamp) {
		// given:
		var topic = new MerkleTopic(memo, adminKey, submitKey, autoRenewDurationSeconds, autoRenewAccountId,
				expirationTimestamp);

		// expect:
		assertTrue(topic.equals(new MerkleTopic(topic)));
	}

	// Set each attribute of 2 memos, differing by a single field and verify that they do not equal each other.
	@ParameterizedTest
	@CsvSource({
			"memo, 0000, 1111, 2, 3.4.5, 6666_777777777, false, 0, 22, wrong-memo, 0000, 1111, 2, 3.4.5, 6666_777777777, false, 0, 22",
			"memo, 0009, 1111, 2, 3.4.5, 6666_777777777, false, 0, 22, memo, 0000, 1111, 2, 3.4.5, 6666_777777777, false, 0, 22",
			"memo, 0000, 1119, 2, 3.4.5, 6666_777777777, false, 0, 22, memo, 0000, 1111, 2, 3.4.5, 6666_777777777, false, 0, 22",
			"memo, 0000, 1111, 9, 3.4.5, 6666_777777777, false, 0, 22, memo, 0000, 1111, 2, 3.4.5, 6666_777777777, false, 0, 22",
			"memo, 0000, 1111, 2, 3.4.9, 6666_777777777, false, 0, 22, memo, 0000, 1111, 2, 3.4.5, 6666_777777777, false, 0, 22",
			"memo, 0000, 1111, 2, 3.4.5, 6666_777777779, false, 0, 22, memo, 0000, 1111, 2, 3.4.5, 6666_777777777, false, 0, 22",
			"memo, 0000, 1111, 2, 3.4.5, 6666_777777777, true, 0, 22, memo, 0000, 1111, 2, 3.4.5, 6666_777777777, false, 0, 22",
			"memo, 0000, 1111, 2, 3.4.5, 6666_777777777, false, 9, 22, memo, 0000, 1111, 2, 3.4.5, 6666_777777777, false, 0, 22",
			"memo, 0000, 1111, 2, 3.4.5, 6666_777777777, false, 0, 29, memo, 0000, 1111, 2, 3.4.5, 6666_777777777, false, 0, 22"
	})
	public void notEquals(String aMemo, @ConvertWith(JEd25519KeyConverter.class) JEd25519Key aAdminKey,
					      @ConvertWith(JEd25519KeyConverter.class) JEd25519Key aSubmitKey,
					      long aAutoRenewDurationSeconds,
						  @ConvertWith(JAccountIDConverter.class) EntityId aAutoRenewAccountId,
						  @ConvertWith(JTimestampConverter.class) RichInstant aExpirationTimestamp,
					      boolean aDeleted, long aSequenceNumber,
					      @ConvertWith(ByteArrayConverter.class) byte[] aRunningHash,
					      String bMemo, @ConvertWith(JEd25519KeyConverter.class) JEd25519Key bAdminKey,
					      @ConvertWith(JEd25519KeyConverter.class) JEd25519Key bSubmitKey,
					      long bAutoRenewDurationSeconds,
					      @ConvertWith(JAccountIDConverter.class) EntityId bAutoRenewAccountId,
					      @ConvertWith(JTimestampConverter.class) RichInstant bExpirationTimestamp,
					      boolean bDeleted, long bSequenceNumber,
					      @ConvertWith(ByteArrayConverter.class) byte[] bRunningHash) {
		// given:
		var a = new MerkleTopic(aMemo, aAdminKey, aSubmitKey, aAutoRenewDurationSeconds, aAutoRenewAccountId,
				aExpirationTimestamp);
		a.setDeleted(aDeleted);
		a.setSequenceNumber(aSequenceNumber);
		a.setRunningHash(aRunningHash);
		var b = new MerkleTopic(bMemo, bAdminKey, bSubmitKey, bAutoRenewDurationSeconds, bAutoRenewAccountId,
				bExpirationTimestamp);
		b.setDeleted(bDeleted);
		b.setSequenceNumber(bSequenceNumber);
		b.setRunningHash(bRunningHash);

		// expect:
		assertFalse(a.equals(b));
	}

	@Test
	public void hashIsSafeOnDefault() {
		// expect:
		assertDoesNotThrow(() -> new MerkleTopic().hashCode());
	}

	@ParameterizedTest
	@CsvSource({
			"memo, 0000000000000000000000000000000000000000000000000000000000000000, 1111111111111111111111111111111111111111111111111111111111111111, 2, 3.4.5, 6666_777777777",
			", , , 55, , 1_234567890"
	})
	public void hashCodeIsSafe(String memo, @ConvertWith(JEd25519KeyConverter.class) JEd25519Key adminKey,
							   @ConvertWith(JEd25519KeyConverter.class) JEd25519Key submitKey,
							   long autoRenewDurationSeconds,
							   @ConvertWith(JAccountIDConverter.class) EntityId autoRenewAccountId,
							   @ConvertWith(JTimestampConverter.class) RichInstant expirationTimestamp) {
		// expect:
		assertDoesNotThrow(() -> new MerkleTopic(memo, adminKey, submitKey, autoRenewDurationSeconds, autoRenewAccountId,
				expirationTimestamp).hashCode());
	}

	@Test
	public void setRunningHashNull() {
		// given:
		var topic = new MerkleTopic();

		// when:
		topic.setRunningHash(null);

		// expect:
		assertFalse(() -> topic.hasRunningHash());
	}

	@Test
	public void setRunningHashEmpty() {
		// given:
		var topic = new MerkleTopic();

		// when:
		topic.setRunningHash(new byte[0]);

		// expect:
		assertFalse(() -> topic.hasRunningHash());
	}

	@ParameterizedTest
	@CsvSource({
			"0, message, 1.2.3, 0000, 1577401723000000000, ccfff7a72f4203fc215e9d3c59492ebcbb9487535c68e92a41d43fcc4212bd61c99a775cbf8df8b7dc06ad8ccd536f6e",
			"1, '', 0.0.0, , 1577401723987654321, 337b5ab555695927181c9a82297c079be415448925481690ebca78f65c3b23a156f19647e67a423399e0a7b5c47f5935",
			"0, , , , , 6c5943b38db24baa690c553be60c9b06ab2a03141df3744dc2c99d1c07d4196dda8b6861bd7d2ea6aa090a80abbfa61a"
	})
	public void updateRunningHash(long initialSequenceNumber, String message,
								  @ConvertWith(TopicIDConverter.class) TopicID topicId,
								  @ConvertWith(ByteArrayConverter.class) byte[] initialRunningHash,
								  @ConvertWith(InstantConverter.class) Instant consensusTimestampSeconds,
								  @ConvertWith(ByteArrayConverter.class) byte[] expectedRunningHash) throws Exception {
		// given:
		var topic = new MerkleTopic();
		topic.setSequenceNumber(initialSequenceNumber);
		topic.setRunningHash(initialRunningHash);
		topic.updateRunningHashAndSequenceNumber(StringUtils.getBytesUtf8(message), topicId, consensusTimestampSeconds);

		// expect:
		assertEquals(initialSequenceNumber + 1, topic.getSequenceNumber());
		assertArrayEquals(expectedRunningHash, topic.getRunningHash());
	}

	/**
	 * Assert that all the accessors for topic return default values.
	 * @param merkleTopic
	 */
	private void assertDefaultTopicAccessors(MerkleTopic merkleTopic) {
		assertFalse(merkleTopic.hasMemo());
		assertEquals("", merkleTopic.getMemo());
		assertFalse(merkleTopic.hasAdminKey());
		assertTrue(merkleTopic.getAdminKey().isEmpty());
		assertFalse(merkleTopic.hasSubmitKey());
		assertTrue(merkleTopic.getSubmitKey().isEmpty());
		assertEquals(0L, merkleTopic.getAutoRenewDurationSeconds());
		assertFalse(merkleTopic.hasAutoRenewAccountId());
		assertEquals(new EntityId(), merkleTopic.getAutoRenewAccountId());
		assertFalse(merkleTopic.hasExpirationTimestamp());
		assertEquals(new RichInstant(), merkleTopic.getExpirationTimestamp());
		assertFalse(merkleTopic.isDeleted());
		assertEquals(0L, merkleTopic.getSequenceNumber());
		assertFalse(merkleTopic.hasRunningHash());
		assertArrayEquals(new byte[48], merkleTopic.getRunningHash());
	}

	/**
	 * Assert that topic has all values set (all the hasXyz methods return true) and the values returned are those
	 * expected (input params).
	 * @param merkleTopic
	 * @param memo
	 * @param adminKey
	 * @param submitKey
	 * @param autoRenewDurationSeconds
	 * @param autoRenewAccountId
	 * @param expirationTimestamp
	 * @throws IOException
	 */
	public void assertPostConstructorAccessors(MerkleTopic merkleTopic, String memo, JKey adminKey, JKey submitKey,
											   long autoRenewDurationSeconds, EntityId autoRenewAccountId,
											   RichInstant expirationTimestamp) throws IOException {
		assertTrue(merkleTopic.hasMemo());
		assertEquals(memo, merkleTopic.getMemo());

		// No good equality operator for JKey - compare serialize()d output.
		assertTrue(merkleTopic.hasAdminKey());
		assertArrayEquals(adminKey.serialize(), merkleTopic.getAdminKey().serialize());
		assertTrue(merkleTopic.hasSubmitKey());
		assertArrayEquals(submitKey.serialize(), merkleTopic.getSubmitKey().serialize());

		assertEquals(autoRenewDurationSeconds, merkleTopic.getAutoRenewDurationSeconds());
		assertTrue(merkleTopic.hasAutoRenewAccountId());
		assertEquals(autoRenewAccountId, merkleTopic.getAutoRenewAccountId());
		assertTrue(merkleTopic.hasExpirationTimestamp());
		assertEquals(expirationTimestamp, merkleTopic.getExpirationTimestamp());
		assertFalse(merkleTopic.isDeleted());
	}
}