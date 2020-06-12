package com.hedera.services.bdd.suites.utils.sysfiles.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.services.bdd.suites.utils.sysfiles.ExchangeRatesPojo;
import com.hederahashgraph.api.proto.java.ExchangeRateSet;
import org.apache.commons.lang3.NotImplementedException;

public class XRatesJsonToGrpcBytes implements SysFileSerde<String> {
	static ObjectMapper mapper = new ObjectMapper();

	@Override
	public String fromRawFile(byte[] bytes) {
		try {
			var pojoRates = ExchangeRatesPojo.fromProto(ExchangeRateSet.parseFrom(bytes));
			return mapper
					.writerWithDefaultPrettyPrinter()
					.writeValueAsString(pojoRates);
		} catch (InvalidProtocolBufferException | JsonProcessingException e) {
			throw new IllegalArgumentException("Not an exchange rates set!", e);
		}
	}

	@Override
	public byte[] toRawFile(String styledFile) {
		throw new NotImplementedException("TBD");
	}

	@Override
	public String preferredFileName() {
		return "exchangeRates.json";
	}
}
