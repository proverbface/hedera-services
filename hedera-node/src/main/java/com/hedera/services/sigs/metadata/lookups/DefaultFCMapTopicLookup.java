package com.hedera.services.sigs.metadata.lookups;

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
import com.hedera.services.sigs.metadata.TopicSigningMetadata;
import com.hederahashgraph.api.proto.java.TopicID;
import com.hedera.services.state.merkle.EntityId;
import com.hedera.services.legacy.exception.InvalidTopicIDException;
import com.swirlds.fcmap.FCMap;

public class DefaultFCMapTopicLookup implements TopicSigMetaLookup {
	private final FCMap<EntityId, Topic> topics;

	public DefaultFCMapTopicLookup(FCMap<EntityId, Topic> topics) {
		this.topics = topics;
	}

	@Override
	public TopicSigningMetadata lookup(TopicID id) throws Exception {
		Topic topic = topics.get(EntityId.fromPojoTopic(id));
		if ((topic == null) || topic.isDeleted()) {
			throw new InvalidTopicIDException("Invalid topic!", id);
		}
		return new TopicSigningMetadata(topic.hasAdminKey() ? topic.getAdminKey() : null,
				topic.hasSubmitKey() ? topic.getSubmitKey() : null);
	}
}
