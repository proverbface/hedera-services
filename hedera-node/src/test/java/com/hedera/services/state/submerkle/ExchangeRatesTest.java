package com.hedera.services.state.submerkle;

import com.hederahashgraph.api.proto.java.ExchangeRate;
import com.hederahashgraph.api.proto.java.ExchangeRateSet;
import com.hederahashgraph.api.proto.java.TimestampSeconds;
import com.swirlds.common.io.SerializableDataInputStream;
import com.swirlds.common.io.SerializableDataOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.mock;

@RunWith(JUnitPlatform.class)
class ExchangeRatesTest {

	private int expCurrentHbarEquiv = 25;
	private int expCurrentCentEquiv = 1;
	private long expCurrentExpiry = Instant.now().getEpochSecond() + 1_234L;

	private int expNextHbarEquiv = 45;
	private int expNextCentEquiv = 2;
	private long expNextExpiry = Instant.now().getEpochSecond() + 5_678L;

	ExchangeRates subject;

	@BeforeEach
	private void setup() {
		subject = new ExchangeRates(
				expCurrentHbarEquiv, expCurrentCentEquiv, expCurrentExpiry,
				expNextHbarEquiv, expNextCentEquiv, expNextExpiry);
	}

	@Test
	public void notAutoInitialized() {
		// given:
		subject = new ExchangeRates();

		// expect:
		assertFalse(subject.isInitialized());
	}

	@Test
	public void copyWorks() {
		// given:
		var subjectCopy = subject.copy();

		// expect:
		assertEquals(expCurrentHbarEquiv, subjectCopy.getCurrentHbarEquiv());
		assertEquals(expCurrentCentEquiv, subjectCopy.getCurrentCentEquiv());
		assertEquals(expCurrentExpiry, subjectCopy.getCurrentExpiry());
		assertEquals(expNextHbarEquiv, subjectCopy.getNextHbarEquiv());
		assertEquals(expNextCentEquiv, subjectCopy.getNextCentEquiv());
		assertEquals(expNextExpiry, subjectCopy.getNextExpiry());
		assertTrue(subjectCopy.isInitialized());
	}

	@Test
	public void serializesAsExpected() throws IOException {
		// setup:
		var out = mock(SerializableDataOutputStream.class);
		InOrder inOrder = inOrder(out);

		// when:
		subject.serialize(out);

		// then:
		inOrder.verify(out).writeInt(expCurrentHbarEquiv);
		inOrder.verify(out).writeInt(expCurrentCentEquiv);
		inOrder.verify(out).writeLong(expCurrentExpiry);
		// and:
		inOrder.verify(out).writeInt(expNextHbarEquiv);
		inOrder.verify(out).writeInt(expNextCentEquiv);
		inOrder.verify(out).writeLong(expNextExpiry);
	}

	@Test
	public void deserializesAsExpected() throws IOException {
		// setup:
		var in = mock(SerializableDataInputStream.class);
		subject = new ExchangeRates();

		given(in.readLong())
				.willReturn(expCurrentExpiry)
				.willReturn(expNextExpiry);
		given(in.readInt())
				.willReturn(expCurrentHbarEquiv)
				.willReturn(expCurrentCentEquiv)
				.willReturn(expNextHbarEquiv)
				.willReturn(expNextCentEquiv);

		// when:
		subject.deserialize(in);

		// then:
		assertEquals(expCurrentHbarEquiv, subject.getCurrentHbarEquiv());
		assertEquals(expCurrentCentEquiv, subject.getCurrentCentEquiv());
		assertEquals(expCurrentExpiry, subject.getCurrentExpiry());
		assertEquals(expNextHbarEquiv, subject.getNextHbarEquiv());
		assertEquals(expNextCentEquiv, subject.getNextCentEquiv());
		assertEquals(expNextExpiry, subject.getNextExpiry());
		assertTrue(subject.isInitialized());
	}

	@Test
	public void sanityChecks() {
		// expect:
		assertEquals(expCurrentHbarEquiv, subject.getCurrentHbarEquiv());
		assertEquals(expCurrentCentEquiv, subject.getCurrentCentEquiv());
		assertEquals(expCurrentExpiry, subject.getCurrentExpiry());
		assertEquals(expNextHbarEquiv, subject.getNextHbarEquiv());
		assertEquals(expNextCentEquiv, subject.getNextCentEquiv());
		assertEquals(expNextExpiry, subject.getNextExpiry());
		assertTrue(subject.isInitialized());
	}

	@Test
	public void replaces() {
		// setup:
		var newRates = ExchangeRateSet.newBuilder()
				.setCurrentRate(
						ExchangeRate.newBuilder()
								.setHbarEquiv(expCurrentHbarEquiv)
								.setCentEquiv(expCurrentCentEquiv)
								.setExpirationTime(TimestampSeconds.newBuilder().setSeconds(expCurrentExpiry)))
				.setNextRate(
						ExchangeRate.newBuilder()
								.setHbarEquiv(expNextHbarEquiv)
								.setCentEquiv(expNextCentEquiv)
								.setExpirationTime(TimestampSeconds.newBuilder().setSeconds(expNextExpiry)))
				.build();

		// given:
		subject = new ExchangeRates();

		// when:
		subject.replaceWith(newRates);

		// expect:
		assertEquals(expCurrentHbarEquiv, subject.getCurrentHbarEquiv());
		assertEquals(expCurrentCentEquiv, subject.getCurrentCentEquiv());
		assertEquals(expCurrentExpiry, subject.getCurrentExpiry());
		assertEquals(expNextHbarEquiv, subject.getNextHbarEquiv());
		assertEquals(expNextCentEquiv, subject.getNextCentEquiv());
		assertEquals(expNextExpiry, subject.getNextExpiry());
		assertTrue(subject.isInitialized());
	}

	@Test
	public void toStringWorks() {
		// expect:
		assertEquals(
				"ExchangeRates{currentHbarEquiv=" + expCurrentHbarEquiv +
						", currentCentEquiv=" + expCurrentCentEquiv +
						", currentExpiry=" + expCurrentExpiry +
						", nextHbarEquiv=" + expNextHbarEquiv +
						", nextCentEquiv=" + expNextCentEquiv +
						", nextExpiry=" + expNextExpiry + "}",
			subject.toString());
	}
}