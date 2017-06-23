/**
 * Logback: .
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are under the terms of the Apache License Version 2.0.
 */
package ch.qos.logback.assemble.rolling;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.pattern.MDCConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.ConverterUtil;
import ch.qos.logback.core.pattern.LiteralConverter;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.Parser;
import ch.qos.logback.core.pattern.util.AlmostAsIsEscapeUtil;
import ch.qos.logback.core.rolling.helper.DateTokenConverter;
import ch.qos.logback.core.rolling.helper.FileFilterUtil;
import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import ch.qos.logback.core.rolling.helper.MonoTypedConverter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.ScanException;

/**
 * @author Lawnstein.Chan
 * @version $Revision:$
 *
 */
public class FileNamePattern extends ContextAwareBase {

	static final Map<String, String> CONVERTER_MAP = new HashMap<String, String>();
	static {
		CONVERTER_MAP.put(IntegerTokenConverter.CONVERTER_KEY, IntegerTokenConverter.class.getName());
		CONVERTER_MAP.put(DateTokenConverter.CONVERTER_KEY, DateTokenConverter.class.getName());
		CONVERTER_MAP.put("X", MDCConverter.class.getName());
	}

	String pattern;
	Converter<Object> headTokenConverter;

	public FileNamePattern(String patternArg, Context contextArg) {
		// the pattern is slashified
		setPattern(FileFilterUtil.slashify(patternArg));
		setContext(contextArg);
		parse();
		ConverterUtil.startConverters(this.headTokenConverter);
	}

	void parse() {
		try {
			// http://jira.qos.ch/browse/LBCORE-130
			// we escape ')' for parsing purposes. Note that the original
			// pattern is preserved
			// because it is shown to the user in status messages. We don't want
			// the escaped version
			// to leak out.
			String patternForParsing = escapeRightParantesis(pattern);
			Parser<Object> p = new Parser<Object>(patternForParsing, new AlmostAsIsEscapeUtil());
			p.setContext(context);
			Node t = p.parse();
			this.headTokenConverter = p.compile(t, CONVERTER_MAP);

		} catch (ScanException sce) {
			addError("Failed to parse pattern \"" + pattern + "\".", sce);
		}
	}

	String escapeRightParantesis(String in) {
		return pattern.replace(")", "\\)");
	}

	public String toString() {
		return pattern;
	}

	public DateTokenConverter<Object> getPrimaryDateTokenConverter() {
		Converter<Object> p = headTokenConverter;

		while (p != null) {
			if (p instanceof DateTokenConverter) {
				DateTokenConverter<Object> dtc = (DateTokenConverter<Object>) p;
				// only primary converters should be returned as
				if (dtc.isPrimary())
					return dtc;
			}

			p = p.getNext();
		}

		return null;
	}

	public IntegerTokenConverter getIntegerTokenConverter() {
		Converter<Object> p = headTokenConverter;

		while (p != null) {
			if (p instanceof IntegerTokenConverter) {
				return (IntegerTokenConverter) p;
			}

			p = p.getNext();
		}
		return null;
	}

	public boolean hasIntegerTokenCOnverter() {
		IntegerTokenConverter itc = getIntegerTokenConverter();
		return itc != null;
	}

	public String convertMultipleArguments(Object... objectList) {
		StringBuilder buf = new StringBuilder();
		Converter<Object> c = headTokenConverter;
		while (c != null) {
			if (c instanceof MonoTypedConverter) {
				MonoTypedConverter monoTyped = (MonoTypedConverter) c;
				for (Object o : objectList) {
					if (monoTyped.isApplicable(o)) {
						buf.append(c.convert(o));
					}
				}
			} else {
				buf.append(c.convert(objectList));
			}
			c = c.getNext();
		}
		return buf.toString();
	}

	public String convert(Object o) {
		StringBuilder buf = new StringBuilder();
		Converter<Object> p = headTokenConverter;
		while (p != null) {
			buf.append(p.convert(o));
			p = p.getNext();
		}
		return buf.toString();
	}

	public String convert(ILoggingEvent event) {
		StringBuilder buf = new StringBuilder();
		Converter p = headTokenConverter;
		while (p != null) {
			// System.out.println("current Converter(ILoggingEvent) " +
			// p.getClass() + ", parameter : " + (event == null ? "nvl" :
			// event.getClass()));

			if (p instanceof MDCConverter) {
				buf.append(p.convert(event));
			} else if (p instanceof DateTokenConverter) {
				buf.append(p.convert(new Date(event.getTimeStamp())));
			} else if (p instanceof IntegerTokenConverter) {
				buf.append("%i");
			} else {
				buf.append(p.convert(null));
			}
			p = p.getNext();
		}
		return buf.toString();
	}

	public String convertInt(int i) {
		return convert(i);
	}

	public void setPattern(String pattern) {
		if (pattern != null) {
			// Trailing spaces in the pattern are assumed to be undesired.
			this.pattern = pattern.trim();
		}
	}

	public String getPattern() {
		return pattern;
	}

	/**
	 * Given date, convert this instance to a regular expression.
	 *
	 * Used to compute sub-regex when the pattern has both %d and %i, and the
	 * date is known.
	 */
	public String toRegexForFixedDate(Date date) {
		StringBuilder buf = new StringBuilder();
		Converter<Object> p = headTokenConverter;
		while (p != null) {
			if (p instanceof LiteralConverter) {
				buf.append(p.convert(null));
			} else if (p instanceof IntegerTokenConverter) {
				buf.append("(\\d{1,3})");
			} else if (p instanceof DateTokenConverter) {
				buf.append(p.convert(date));
			}
			p = p.getNext();
		}
		return buf.toString();
	}

	/**
	 * Given date, convert this instance to a regular expression
	 */
	public String toRegex() {
		StringBuilder buf = new StringBuilder();
		Converter<Object> p = headTokenConverter;
		while (p != null) {
			if (p instanceof LiteralConverter) {
				buf.append(p.convert(null));
			} else if (p instanceof IntegerTokenConverter) {
				buf.append("\\d{1,2}");
			} else if (p instanceof DateTokenConverter) {
				DateTokenConverter<Object> dtc = (DateTokenConverter<Object>) p;
				buf.append(dtc.toRegex());
			}
			p = p.getNext();
		}
		return buf.toString();
	}
}
