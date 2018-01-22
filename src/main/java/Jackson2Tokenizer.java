/*
 * Copyright 2002-2017 the original author or authors.
 *
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
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.util.TokenBuffer;

public class Jackson2Tokenizer {

	private final JsonParser parser;

	private final boolean tokenizeArrayElements;

	private TokenBuffer tokenBuffer;

	private int objectDepth;

	private int arrayDepth;

	private final ByteArrayFeeder inputFeeder;


	Jackson2Tokenizer(JsonParser parser, boolean tokenizeArrayElements) {


		this.parser = parser;
		this.tokenizeArrayElements = tokenizeArrayElements;
		this.tokenBuffer = new TokenBuffer(parser);
		this.inputFeeder = (ByteArrayFeeder) this.parser.getNonBlockingInputFeeder();
	}

	public static List<TokenBuffer> tokenize(List<byte[]> dataBuffers, JsonFactory jsonFactory,
			boolean tokenizeArrayElements) throws IOException {

		Jackson2Tokenizer tokenizer = new Jackson2Tokenizer(jsonFactory.createNonBlockingByteArrayParser(),
						tokenizeArrayElements);
		List<TokenBuffer> tokenBuffers = dataBuffers.stream().flatMap(bytes -> tokenizer.tokenize(bytes).stream()).collect(Collectors.toList());
		tokenBuffers.addAll(tokenizer.endOfInput());
		return tokenBuffers;

	}

	private List<TokenBuffer> tokenize(byte[] bytes) {
		try {
			this.inputFeeder.feedInput(bytes, 0, bytes.length);
			return parseTokenBufferFlux();
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private List<TokenBuffer> endOfInput() throws IOException {
		this.inputFeeder.endOfInput();
		return parseTokenBufferFlux();
	}

	private List<TokenBuffer> parseTokenBufferFlux() throws IOException {
		List<TokenBuffer> result = new ArrayList<>();

		while (true) {
			JsonToken token = this.parser.nextToken();
			if (token == null || token == JsonToken.NOT_AVAILABLE) {
				break;
			}
			updateDepth(token);

			if (!this.tokenizeArrayElements) {
				processTokenNormal(token, result);
			}
			else {
				processTokenArray(token, result);
			}
		}
		return result;
	}

	private void updateDepth(JsonToken token) {
		switch (token) {
			case START_OBJECT:
				this.objectDepth++;
				break;
			case END_OBJECT:
				this.objectDepth--;
				break;
			case START_ARRAY:
				this.arrayDepth++;
				break;
			case END_ARRAY:
				this.arrayDepth--;
				break;
		}
	}

	private void processTokenNormal(JsonToken token, List<TokenBuffer> result) throws IOException {
		this.tokenBuffer.copyCurrentEvent(this.parser);

		if ((token.isStructEnd() || token.isScalarValue()) &&
				this.objectDepth == 0 && this.arrayDepth == 0) {
			result.add(this.tokenBuffer);
			this.tokenBuffer = new TokenBuffer(this.parser);
		}

	}

	private void processTokenArray(JsonToken token, List<TokenBuffer> result) throws IOException {
		if (!isTopLevelArrayToken(token)) {
			this.tokenBuffer.copyCurrentEvent(this.parser);
		}

		if (this.objectDepth == 0 && (this.arrayDepth == 0 || this.arrayDepth == 1) &&
				(token == JsonToken.END_OBJECT || token.isScalarValue())) {
			result.add(this.tokenBuffer);
			this.tokenBuffer = new TokenBuffer(this.parser);
		}
	}

	private boolean isTopLevelArrayToken(JsonToken token) {
		return this.objectDepth == 0 && ((token == JsonToken.START_ARRAY && this.arrayDepth == 1) ||
				(token == JsonToken.END_ARRAY && this.arrayDepth == 0));
	}

}
