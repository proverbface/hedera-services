package com.hedera.services.context.primitives;

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

import com.hedera.services.state.merkle.Topic;
import com.hedera.services.files.DataMapFactory;
import com.hedera.services.files.MetadataMapFactory;
import com.hedera.services.files.store.FcBlobsBytesStore;
import com.hederahashgraph.api.proto.java.FileGetInfoResponse;
import com.hederahashgraph.api.proto.java.FileID;
import com.hederahashgraph.api.proto.java.Timestamp;
import com.hedera.services.state.merkle.EntityId;
import com.hedera.services.context.domain.haccount.HederaAccount;
import com.hedera.services.state.merkle.BlobMeta;
import com.hedera.services.state.merkle.OptionalBlob;
import com.hedera.services.legacy.core.jproto.JFileInfo;
import com.hedera.services.legacy.core.jproto.JKey;
import com.hedera.services.legacy.core.jproto.JKeyList;
import com.swirlds.fcmap.FCMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;

import static com.hedera.services.utils.EntityIdUtils.readableId;
import static com.hedera.services.legacy.core.jproto.JKey.mapJKey;
import static java.util.Collections.unmodifiableMap;

public class StateView {
	private static final Logger log = LogManager.getLogger(StateView.class);

	private static final byte[] EMPTY_CONTENTS = new byte[0];
	public static final JKey EMPTY_WACL = new JKeyList();

	public static final FCMap<EntityId, Topic> EMPTY_TOPICS =
			new FCMap<>(new EntityId.Provider(), new Topic.Provider());
	public static final FCMap<EntityId, HederaAccount> EMPTY_ACCOUNTS =
			new FCMap<>(new EntityId.Provider(), HederaAccount.LEGACY_PROVIDER);
	public static final FCMap<BlobMeta, OptionalBlob> EMPTY_STORAGE =
			new FCMap<>(new BlobMeta.Provider(), new OptionalBlob.Provider());
	public static final StateView EMPTY_VIEW = new StateView(EMPTY_TOPICS, EMPTY_ACCOUNTS);

	Map<FileID, byte[]> fileContents;
	Map<FileID, JFileInfo> fileAttrs;
	private final FCMap<EntityId, Topic> topics;
	private final FCMap<EntityId, HederaAccount> accounts;

	public StateView(
			FCMap<EntityId, Topic> topics,
			FCMap<EntityId, HederaAccount> accounts
	) {
		this(topics, accounts, EMPTY_STORAGE);
	}

	public StateView(
			FCMap<EntityId, Topic> topics,
			FCMap<EntityId, HederaAccount> accounts,
			FCMap<BlobMeta, OptionalBlob> storage
	) {
		this.topics = topics;
		this.accounts = accounts;

		Map<String, byte[]> blobStore = unmodifiableMap(new FcBlobsBytesStore(OptionalBlob::new, storage));
		fileContents = DataMapFactory.dataMapFrom(blobStore);
		fileAttrs = MetadataMapFactory.metaMapFrom(blobStore);
	}

	public Optional<JFileInfo> attrOf(FileID id) {
		return Optional.ofNullable(fileAttrs.get(id));
	}

	public Optional<byte[]> contentsOf(FileID id) {
		return Optional.ofNullable(fileContents.get(id));
	}

	public Optional<FileGetInfoResponse.FileInfo> infoFor(FileID id) {
		try {
			var attr = fileAttrs.get(id);
			if (attr == null) {
				return Optional.empty();
			}

			var info = FileGetInfoResponse.FileInfo.newBuilder()
					.setFileID(id)
					.setDeleted(attr.isDeleted())
					.setExpirationTime(Timestamp.newBuilder().setSeconds(attr.getExpirationTimeSeconds()))
					.setSize(Optional.ofNullable(fileContents.get(id)).orElse(EMPTY_CONTENTS).length);
			if (!attr.getWacl().isEmpty()) {
				info.setKeys(mapJKey(attr.getWacl()).getKeyList());
			}
			return Optional.of(info.build());
		} catch (Exception unknown) {
			log.warn("Unexpected problem getting info for {}", readableId(id), unknown);
			return Optional.empty();
		}
	}

	public FCMap<EntityId, Topic> topics() {
		return topics;
	}

	public FCMap<EntityId, HederaAccount> accounts() {
		return accounts;
	}
}
