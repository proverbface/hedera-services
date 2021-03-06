package com.hedera.services.fees.calculation.file.txns;

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
import com.hedera.services.fees.calculation.TxnResourceUsageEstimator;
import com.hederahashgraph.api.proto.java.FeeData;
import com.hederahashgraph.api.proto.java.TransactionBody;
import com.hederahashgraph.exception.InvalidTxBodyException;
import com.hederahashgraph.fee.FileFeeBuilder;
import com.hederahashgraph.fee.SigValueObj;

import static com.hedera.services.fees.calculation.FeeCalcUtils.lookupFileExpiry;

public class FileAppendResourceUsage implements TxnResourceUsageEstimator {
	private final FileFeeBuilder usageEstimator;

	public FileAppendResourceUsage(FileFeeBuilder usageEstimator) {
		this.usageEstimator = usageEstimator;
	}

	@Override
	public boolean applicableTo(TransactionBody txn) {
		return txn.hasFileAppend();
	}

	@Override
	public FeeData usageGiven(TransactionBody txn, SigValueObj sigUsage, StateView view) throws InvalidTxBodyException {
		var fid = txn.getFileAppend().getFileID();
		return usageEstimator.getFileAppendTxFeeMatrices(txn, lookupFileExpiry(fid, view), sigUsage);
	}
}
