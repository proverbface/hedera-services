package com.hedera.services.files.store;

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

import com.hedera.services.legacy.core.StorageKey;
import com.hedera.services.legacy.core.StorageValue;
import com.swirlds.fcmap.FCMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

@RunWith(JUnitPlatform.class)
class FcBlobsBytesStoreTest {
	byte[] aData = "BlobA".getBytes(), bData = "BlobB".getBytes();
	StorageKey pathA = new StorageKey("pathA"), pathB = new StorageKey("pathB");

	StorageValue blobA, blobB;
	Function<byte[], StorageValue> blobFactory;
	FCMap<StorageKey, StorageValue> pathedBlobs;

	FcBlobsBytesStore subject;

	@Test
	public void putDeletesReplacedValueIfNoCopyIsHeld() {
		// setup:
		FCMap<StorageKey, StorageValue> blobs = new FCMap<>(StorageKey::deserialize, StorageValue::deserialize);

		// given:
		blobs.put(at("path"), new StorageValue("FIRST".getBytes()));

		// when:
		var replaced = blobs.put(at("path"), new StorageValue("SECOND".getBytes()));

		// then:
		assertTrue(replaced.getBinaryObject().isDeleted());
	}

	@Test
	public void putDoesNotDeleteReplacedValueIfCopyIsHeld() {
		// setup:
		FCMap<StorageKey, StorageValue> blobs = new FCMap<>(StorageKey::deserialize, StorageValue::deserialize);

		// given:
		blobs.put(at("path"), new StorageValue("FIRST".getBytes()));

		// when:
		var copy = blobs.copy();
		var replaced = blobs.put(at("path"), new StorageValue("SECOND".getBytes()));

		// then:
		assertFalse(replaced.getBinaryObject().isDeleted());
	}

	private StorageKey at(String key) {
		return new StorageKey(key);
	}

	@BeforeEach
	private void setup() {
		pathedBlobs = mock(FCMap.class);
		blobFactory = mock(Function.class);

		givenMockBlobs();
		given(blobFactory.apply(any()))
				.willReturn(blobA)
				.willReturn(blobB);

		subject = new FcBlobsBytesStore(blobFactory, pathedBlobs);
	}

	@Test
	public void delegatesClear() {
		// when:
		subject.clear();

		// then:
		verify(pathedBlobs).clear();
	}

	@Test
	public void delegatesRemoveOfMissing() {
		given(pathedBlobs.remove(pathA)).willReturn(null);

		// when:
		var prev = subject.remove(pathA.getPath());

		// then:
		assertNull(prev);
	}

	@Test
	public void delegatesRemove() {
		given(pathedBlobs.remove(pathA)).willReturn(blobA);

		// when:
		byte[] prev = subject.remove(pathA.getPath());

		// then:
		assertEquals(new String(prev), new String(blobA.getData()));
	}

	@Test
	public void delegatesPut() {
		// setup:
		ArgumentCaptor<StorageKey> keyCaptor = ArgumentCaptor.forClass(StorageKey.class);
		ArgumentCaptor<StorageValue> valueCaptor = ArgumentCaptor.forClass(StorageValue.class);
		// when:
		var oldBytes = subject.put(pathA.getPath(), blobA.getData());

		// then:
		verify(pathedBlobs).put(keyCaptor.capture(), valueCaptor.capture());
		// and:
		assertEquals(pathA, keyCaptor.getValue());
		assertTrue(blobA == valueCaptor.getValue());
		assertNull(oldBytes);
	}

	@Test
	public void propagatesNullFromGet() {
		given(pathedBlobs.get(argThat(sk -> ((StorageKey)sk).getPath().equals(pathA.getPath())))).willReturn(null);

		// when:
		byte[] blob = subject.get(pathA.getPath());

		// then:
		assertNull(blob);
	}

	@Test
	public void delegatesGet() {
		given(pathedBlobs.get(argThat(sk -> ((StorageKey)sk).getPath().equals(pathA.getPath())))).willReturn(blobA);

		// when:
		byte[] blob = subject.get(pathA.getPath());

		// then:
		assertEquals(new String(blobA.getData()), new String(blob));
	}

	@Test
	public void delegatesContainsKey() {
		given(pathedBlobs.containsKey(argThat(sk -> ((StorageKey)sk).getPath().equals(pathA.getPath()))))
				.willReturn(true);

		// when:
		boolean flag = subject.containsKey(pathA.getPath());

		// then:
		assertTrue(flag);
	}

	@Test
	public void delegatesIsEmpty() {
		given(pathedBlobs.isEmpty()).willReturn(true);

		// when:
		boolean flag = subject.isEmpty();

		// then:
		assertTrue(flag);
		// and:
		verify(pathedBlobs).isEmpty();
	}

	@Test
	public void delegatesSize() {
		given(pathedBlobs.size()).willReturn(123);

		// expect:
		assertEquals(123, subject.size());
	}

	@Test
	public void delegatesEntrySet() {
		// setup:
		Set<Entry<StorageKey, StorageValue>> blobEntries = Set.of(
				new AbstractMap.SimpleEntry<>(pathA, blobA),
				new AbstractMap.SimpleEntry<>(pathB, blobB));

		given(pathedBlobs.entrySet()).willReturn(blobEntries);

		// when:
		Set<Entry<String, byte[]>> entries = subject.entrySet();

		// then:
		assertEquals(
				entries
						.stream()
						.sorted(Comparator.comparing(Entry::getKey))
						.map(entry -> String.format("%s->%s", entry.getKey(), new String(entry.getValue())))
						.collect(Collectors.joining(", ")),
				"pathA->BlobA, pathB->BlobB"
		);
	}

	private void givenMockBlobs() {
		blobA = mock(StorageValue.class);
		blobB = mock(StorageValue.class);

		given(blobA.getData()).willReturn(aData);
		given(blobB.getData()).willReturn(bData);
	}
}