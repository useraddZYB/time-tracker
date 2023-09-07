package com.programmerartist.timetracker.util;

import java.util.regex.Matcher;

/**
 * @Author 程序员Artist
 * @Date 2018/6/20 下午2:47
 **/
public class StringUtill {
    private static final String FORMAT_SYMBOL = "{}";

    /**
     * @param str
     * @return
     */
    public static boolean isBlank(String str) {
        return null == str || "".equals(str.trim());
    }

    /**
     * @param str
     * @return
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * @param arr
     * @return
     */
    public static boolean isAnyBlank(String... arr) {
        if (null == arr) return true;

        for (String str : arr) {
            if (isBlank(str)) return true;
        }

        return false;
    }

    /**
     * @param arr
     * @return
     */
    public static boolean isAllBlank(String... arr) {
        if (null == arr) return true;

        for (String str : arr) {
            if (isNotBlank(str)) return false;
        }

        return true;
    }

    /**
     * 替换默认的 {} 占位符
     *
     * @param source eg: query finish, cost={}, size={}
     * @param values eg: 10ms 100
     * @return 新字符串
     */
    public static String format(String source, Object... values) {
        return StringUtill.formatSymbol(source, FORMAT_SYMBOL, values);
    }

    /**
     * 替换指定的 symbol 占位符
     *
     * @param source eg: query finish, cost={}, size={}
     * @param symbol eg: {} 或者 # ### 等等
     * @param values eg: 10ms 100
     * @return 新字符串
     */
    public static String formatSymbol(String source, String symbol, Object... values) {
        if (isAnyBlank(source, symbol) || null == values || values.length == 0) return source;

        char[] sourceChar = source.toCharArray();
        char[] symbolChar = symbol.toCharArray();
        int sourceLen = sourceChar.length;
        int symbolLen = symbolChar.length;
        int valueLen = values.length;

        StringBuilder target = new StringBuilder(sourceLen);
        int valueIndex = 0;
        for (int i = 0; i < sourceLen; i++) {
            if (valueIndex >= valueLen) {
                target.append(sourceChar[i]);
                continue;
            }

            boolean hit = true;
            for (int j = 0; j < symbolLen; j++) {
                if (sourceChar[i + j] != symbolChar[j]) {
                    hit = false;
                    break;
                }
            }

            if (hit) {
                target.append(values[valueIndex++]);
                i += (symbolLen - 1);
            } else {
                target.append(sourceChar[i]);
            }
        }

        // System.out.println(target.toString());
        return target.toString();
    }


    /**
     * 替换
     *
     * @param source      源字符串
     * @param regex       要被替换的
     * @param replacement 被替换成啥
     * @return 替换过后的字符串
     */
    public static String replaceFirst(String source, String regex, String replacement) {
        if (isAnyBlank(source, regex) || null == replacement) return source;

        return source.replaceFirst(regex, Matcher.quoteReplacement(replacement));

    }

    /**
     * 替换
     *
     * @param source      源字符串
     * @param regex       要被替换的
     * @param replacement 被替换成啥
     * @return 替换过后的字符串
     */
    public static String replaceAll(String source, String regex, String replacement) {
        if (isAnyBlank(source, regex) || null == replacement) return source;

        return source.replaceAll(regex, Matcher.quoteReplacement(replacement));

    }


    /**
     * test
     *
     * @param args
     */
    public static void main(String[] args) {

        String source = "i love my country={}, my daughter={}, and my family={}, is good";
        System.out.println(StringUtill.format(source, "chi$na", "hanhan", "zhang"));
    }

}
