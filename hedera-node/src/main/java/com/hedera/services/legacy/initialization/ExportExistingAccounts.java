package com.hedera.services.legacy.initialization;

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

import com.hedera.services.state.merkle.EntityId;
import com.hedera.services.context.domain.haccount.HederaAccount;
import com.hedera.services.legacy.core.jproto.HEntityId;

import java.io.FileWriter;
import java.io.IOException;

import com.swirlds.fcmap.FCMap;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ExportExistingAccounts {
  private static final Logger log = LogManager.getLogger(ExportExistingAccounts.class);
  
  @SuppressWarnings("unchecked")
  private static JSONArray getAccountsasJson(FCMap<EntityId, HederaAccount> accountMap) {

    JSONArray accountObjArr = new JSONArray();
    JSONObject cryptoAccount = null;
    HederaAccount mapValue = null;
    HEntityId proxyAccountID = null;
    for (EntityId currKey : accountMap.keySet()) {
      try {
        cryptoAccount = new JSONObject();
        log.info("retrieving account info from path :: Solidity Address in getAccountDetails "
            + currKey.getNum());
        mapValue = accountMap.get(currKey);
        cryptoAccount.put("initialBalance", mapValue.getBalance());
        proxyAccountID = mapValue.getProxy();
        if (proxyAccountID != null) {
          cryptoAccount.put("proxyAccountNum", proxyAccountID.getNum());
          cryptoAccount.put("proxyRealmNum", proxyAccountID.getRealm());
          cryptoAccount.put("proxyShardNum", mapValue.getProxy());
        } else {
          cryptoAccount.put("proxyAccountNum", 0);
          cryptoAccount.put("proxyRealmNum", 0);
          cryptoAccount.put("proxyShardNum", 0);
        }
        cryptoAccount.put("sendRecordThreshold", mapValue.getSenderThreshold());
        cryptoAccount.put("receiveRecordThreshold", mapValue.getReceiverThreshold());
        cryptoAccount.put("receiverSigRequired", mapValue.isReceiverSigRequired());
        cryptoAccount.put("autoRenewPeriod", mapValue.getAutoRenewSecs());
        cryptoAccount.put("shardID", currKey.getShard());
        cryptoAccount.put("realmID", currKey.getRealm());
        cryptoAccount.put("accountNum", currKey.getNum());
        String key = Hex.encodeHexString(SerializationUtils.serialize(mapValue.getKey()));
        cryptoAccount.put("key", key);
      } catch (Exception e) {
        log.error("Exception occurred while fetching Accounts from Account FCMap", e);
      }
      accountObjArr.add(cryptoAccount);
    }
    return accountObjArr;
  }

  /**
   * This method is invoked during start up and executed based upon the configuration settings. It
   * exports all the existing accounts in the JSON format and write it in a file
   */
  public static void exportAccounts(String exportAccountPath, FCMap<EntityId, HederaAccount> accountMap)
      throws IOException {
    JSONArray accountList = getAccountsasJson(accountMap);
    try (FileWriter file = new FileWriter(exportAccountPath)) {
      file.write(accountList.toJSONString());
      log.info("Successfully Copied JSON Object to File");
    } catch (IOException e) {
      log.error("Exception occurred while Exporting Accounts to File", e);
      throw e;
    }
  }
}