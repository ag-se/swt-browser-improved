package de.fu_berlin.inf.ag_se.browser.utils;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.*;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

public class StringUtils {

	private static final Pattern BODY_PATTERN = Pattern.compile(
            ".*<body.*?>(.*)</body>.*", Pattern.CASE_INSENSITIVE
                    | Pattern.DOTALL);

	public static interface IStringAdapter<T> {
		public String getString(T object);
	}

	public static String join(List<String> strings, String separator) {
		if (strings == null) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0, m = strings.size(); i < m; i++) {
			String string = strings.get(i);
			if (string == null) {
				string = "";
			}

			sb.append(string);
			if (i + 1 < m) {
				sb.append(separator);
			}
		}
		return sb.toString();
	}

	/**
	 * Returns the longest prefix of the given strings.
	 *
	 * @param stringAdapter
	 *            adapts a value object to a string
	 * @param string1
	 * @param string2
	 *
	 * @return
	 *
	 * @see <a
	 *      href="http://stackoverflow.com/questions/8033655/java-find-longest-common-prefix-in-java">http://stackoverflow.com/questions/8033655/java-find-longest-common-prefix-in-java</a>
	 */
	public static String getLongestCommonPrefix(String string1, String string2) {
		if (string1 == null || string2 == null) {
			throw new IllegalArgumentException();
		}
		int minLength = Math.min(string1.length(), string2.length());
		for (int i = 0; i < minLength; i++) {
			if (string1.charAt(i) != string2.charAt(i)) {
				return string1.substring(0, i);
			}
		}
		return string1.substring(0, minLength);
	}

	/**
	 * Returns the longest prefix of the given strings.
	 *
	 * @param stringAdapter
	 *            adapts a value object to a string
	 * @param objects
	 *
	 * @return
	 *
	 * @see <a
	 *      href="http://stackoverflow.com/questions/1916218/find-the-longest-common-starting-substring-in-a-set-of-strings">Find
	 *      the longest common starting substring in a set of strings</a>
	 */
	public static <T> String getLongestCommonPrefix(
			IStringAdapter<T> stringAdapter, T... objects) {
		if (objects == null) {
			throw new IllegalArgumentException();
		}

		List<String> strings = new ArrayList<String>();
		for (T object : objects) {
			if (object == null && stringAdapter == null) {
				throw new IllegalArgumentException();
			}
			strings.add(stringAdapter != null ? stringAdapter.getString(object)
					: object.toString());
		}

		if (strings.size() == 0) {
			return "";
		}
		if (strings.size() == 1) {
			return strings.get(0);
		}

		try {
			Collections.sort(strings);
		} catch (NullPointerException e) {
			throw new IllegalArgumentException(e);
		}

		return getLongestCommonPrefix(strings.get(0),
				strings.get(strings.size() - 1));
	}

	/**
	 * Returns all found common prefixes and their occurrences.
	 * <p>
	 * Prefixes that are not common and thus have occurrence one are not part of
	 * the result.<br/>
	 * Prefixes can only be of length 1 or greater.
	 * <p>
	 * e.g. given AAA, AAB, BBB returns prefix AA with number of occurrences 2.
	 *
	 * @param stringAdapter
	 *            TODO
	 * @param objects
	 *
	 * @return
	 */
	public static <T> Map<String, Integer> getLongestCommonPrefix(
			IStringAdapter<T> stringAdapter, int partitionLength, T... objects) {
		if (objects == null) {
			throw new IllegalArgumentException();
		}

		Map<String, List<String>> partitionedStrings = new HashMap<String, List<String>>();
		for (T object : objects) {
			if (object == null && stringAdapter == null) {
				throw new IllegalArgumentException();
			}
			String string = stringAdapter != null ? stringAdapter
					.getString(object) : object.toString();
			if (string == null) {
				throw new IllegalArgumentException();
			}
			if (string.length() < partitionLength) {
				continue;
			}
			String key = string.substring(0, partitionLength);
			if (!partitionedStrings.containsKey(key)) {
				partitionedStrings.put(key, new ArrayList<String>());
			}
			partitionedStrings.get(key).add(string);
		}

		Map<String, Integer> rs = new HashMap<String, Integer>();
		for (List<String> partition : partitionedStrings.values()) {
			String longestPrefix = getLongestCommonPrefix(null,
					partition.toArray());
			rs.put(longestPrefix, partition.size());
		}

		return rs;
	}

	/**
	 * Creates a random string that only contains a-z and 0-9.
	 *
	 * @param length
	 * @return
	 */
	public static String createRandomString(int length) {
        StringBuilder builder = new StringBuilder();
		while (builder.length() < length) {
            builder.append(new BigInteger(130, new SecureRandom()).toString(32));
        }
        return builder.substring(0, length);
	}

	public static String rtfToBody(String rtf) throws IOException {
		String html = rtfToHtml(new StringReader(rtf));
		if (html.contains("<body")) {
			html = BODY_PATTERN.matcher(html).replaceAll("$1");
		}
		return html;
	}

	public static String rtfToHtml(String rtf) throws IOException {
		return rtfToHtml(new StringReader(rtf));
	}

	public static String rtfToPlain(String rtf) {
		RTFEditorKit rtfParser = new RTFEditorKit();
		Document document = rtfParser.createDefaultDocument();
		try {
			rtfParser.read(new ByteArrayInputStream(rtf.getBytes()), document,
					0);
			return document.getText(0, document.getLength());
		} catch (Exception e) {
			throw new RuntimeException("Error converting RTF to plain text", e);
		}
	}

	/**
	 * @see http 
	 *      ://www.codeproject.com/Tips/136483/Java-How-to-convert-RTF-into-HTML
	 * @param rtf
	 * @return
	 * @throws java.io.IOException
	 */
	public static String rtfToHtml(Reader rtf) throws IOException {
		JEditorPane p = new JEditorPane();
		p.setContentType("text/rtf");
		EditorKit kitRtf = p.getEditorKitForContentType("text/rtf");
		try {
			kitRtf.read(rtf, p.getDocument(), 0);
			kitRtf = null;
			EditorKit kitHtml = p.getEditorKitForContentType("text/html");
			Writer writer = new StringWriter();
			kitHtml.write(writer, p.getDocument(), 0, p.getDocument()
					.getLength());
			return writer.toString();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String htmlToPlain(String html) {
		HTMLEditorKit htmlParser = new HTMLEditorKit();
		Document document = htmlParser.createDefaultDocument();
		try {
			htmlParser.read(new ByteArrayInputStream(html.getBytes()),
					document, 0);
			String plain = document.getText(0, document.getLength());
			return plain;
		} catch (Exception e) {
			throw new RuntimeException("Error converting HTML to plain text", e);
		}
	}

	public static String plainToHtml(String plain) {
		DefaultEditorKit plainParser = new DefaultEditorKit();
		Document document = plainParser.createDefaultDocument();
		try {
			plainParser.read(new ByteArrayInputStream(plain.getBytes()),
					document, 0);
			String html = document.getText(0, document.getLength());
			return html;
		} catch (Exception e) {
			throw new RuntimeException("Error converting plain text to HTML", e);
		}
	}

	public static String shorten(String script, int length) {
		String shortened = script.length() > length ? script.substring(0,
				length).intern()
				+ "..." : script;
		return shortened.replace("\n", " ").replace("\r", " ")
				.replace("\t", " ");
	}

	public static String shorten(String script) {
		return shorten(script, 100);
	}

	public static int findClosingParenthese(String string, int openPos) {
		int closePos = openPos;
		int counter = 1;
		while (counter > 0) {
			char c = string.charAt(++closePos);
			if (c == '(') {
				counter++;
			} else if (c == ')') {
				counter--;
			}
		}
		return closePos;
	}
}
