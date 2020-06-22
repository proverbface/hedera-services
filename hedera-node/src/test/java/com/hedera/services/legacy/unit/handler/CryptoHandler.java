package com.hedera.services.legacy.unit.handler;

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

import com.hedera.services.context.primitives.StateView;
import com.hederahashgraph.api.proto.java.AccountID;
import com.hedera.services.state.merkle.EntityId;
import com.hedera.services.context.domain.haccount.HederaAccount;
import com.swirlds.fcmap.FCMap;

public class CryptoHandler {
  private FCMap<EntityId, HederaAccount> map = StateView.EMPTY_ACCOUNTS;

  public boolean validateAccountID(AccountID accountID) {
    boolean isValid = false;
    EntityId entityId = EntityId.fromPojoAccountId(accountID);
    if (map.containsKey(entityId)) {
      HederaAccount mapValue = map.get(entityId);
      if (mapValue != null) {
        isValid = !mapValue.isSmartContract();
      }
    }
    return isValid;
  }

  public boolean isAccountSetForDelete(AccountID accountID) {
    EntityId accountKey = EntityId.fromPojoAccountId(accountID);
    if (map.containsKey(accountKey)) {
      HederaAccount accountValue = map.get(accountKey);
      return accountValue.isDeleted();
    }
    return false;
  }
}