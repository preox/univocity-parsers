/*******************************************************************************
 * Copyright 2014 uniVocity Software Pty Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.univocity.parsers.common.input;

import java.io.*;

import com.univocity.parsers.common.*;

/**
 * 
 * The base class for implementing different flavours of {@link CharInputReader}.
 * 
 * <p> It provides the essential conversion of sequences of newline characters defined by {@link Format#getLineSeparator()} into the normalized newline character provided in {@link Format#getNormalizedNewline()}.
 * <p> It also provides a default implementation for most of the methods specified by the {@link CharInputReader} interface.
 * <p> Extending classes must essentially read characters from a given {@link java.io.Reader} and assign it to the public {@link AbstractCharInputReader#buffer} when requested (in the {@link AbstractCharInputReader#reloadBuffer()} method).    
 *
 * @see com.univocity.parsers.common.Format
 * @see com.univocity.parsers.common.input.DefaultCharInputReader
 * @see com.univocity.parsers.common.input.concurrent.ConcurrentCharInputReader
 * 
 * @author uniVocity Software Pty Ltd - <a href="mailto:parsers@univocity.com">parsers@univocity.com</a>
 *
 */

public abstract class AbstractCharInputReader implements CharInputReader {

	private final char lineSeparator1;
	private final char lineSeparator2;
	private final char normalizedLineSeparator;

	private char current = '\0';
	private char next = '\0';

	private int lineCount;
	private int charCount;
	private int i;

	public char[] buffer;
	public int length = -1;

	/**
	 * Creates a new instance with the mandatory characters for handling newlines transparently.
	 * @param lineSeparator the sequence of characters that represent a newline, as defined in {@link Format#getLineSeparator()}
	 * @param normalizedLineSeparator the normalized newline character (as defined in {@link Format#getNormalizedNewline()}) that is used to replace any lineSeparator sequence found in the input. 
	 */
	public AbstractCharInputReader(char[] lineSeparator, char normalizedLineSeparator) {
		if (lineSeparator == null || lineSeparator.length == 0) {
			throw new IllegalArgumentException("Invalid line separator. Expected 1 to 2 characters");
		}
		if (lineSeparator.length > 2) {
			throw new IllegalArgumentException("Invalid line separator. Up to 2 characters are expected. Got " + lineSeparator.length + " characters.");
		}
		this.lineSeparator1 = lineSeparator[0];
		this.lineSeparator2 = lineSeparator.length == 2 ? lineSeparator[1] : '\0';
		this.normalizedLineSeparator = normalizedLineSeparator;
	}

	/**
	 * Passes the {@link java.io.Reader} provided in the {@link AbstractCharInputReader#start(Reader)} method to the extending class so it can begin loading characters from it.
	 * @param reader the {@link java.io.Reader} provided in {@link AbstractCharInputReader#start(Reader)} 
	 */
	protected abstract void setReader(Reader reader);

	/**
	 * Informs the extending class that the buffer has been read entirely and requests for another batch of characters.
	 * Implementors must assign the new character buffer to the public {@link AbstractCharInputReader#buffer} attribute, as well as the number of characters available to the public {@link AbstractCharInputReader#length} attribute.
	 * To notify the input does not have any more characters, {@link AbstractCharInputReader#length} must receive the <b>-1</b> value  
	 */
	protected abstract void reloadBuffer();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start(Reader reader) {
		stop();
		setReader(reader);
		lineCount = 0;

		updateBuffer();
		if (length > 0) {
			next = buffer[i++];
		}
	}

	/**
	 * Requests the next batch of characters from the implementing class and updates 
	 * the character count.
	 * 
	 * <p> If there are no more characters in the input, the reading will stop by invoking the {@link AbstractCharInputReader#stop()} method.
	 */
	private void updateBuffer() {
		reloadBuffer();

		charCount += i;
		i = 0;

		if (length == -1) {
			stop();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public char nextChar() {
		current = next;

		if (i >= length) {
			if (length != -1) {
				updateBuffer();
			} else {
				return '\0';
			}
		}

		next = buffer[i++];

		if (lineSeparator1 == current && (lineSeparator2 == '\0' || lineSeparator2 == next)) {
			lineCount++;
			if (lineSeparator2 != '\0') {
				current = normalizedLineSeparator;

				if (i >= length) {
					if (length != -1) {
						updateBuffer();
					}
				}

				if (i < length) {
					next = buffer[i++];
				}
			}
		}

		return current;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int lineCount() {
		return lineCount;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void skipLines(int lines) {
		if (lines < 1) {
			return;
		}
		int expectedLineCount = this.lineCount + lines;

		char ch = '\0';
		do {
			ch = nextChar();
		} while (lineCount < expectedLineCount && ch != '\0');
		if (ch == '\0' && lineCount < lines) {
			throw new IllegalArgumentException("Unable to skip " + lines + " lines from line " + (expectedLineCount - lines) + ". End of input reached");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int charCount() {
		return charCount + i;
	}
}