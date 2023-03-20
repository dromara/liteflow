package com.yomahub.liteflow.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Yun
 */
public class SerialsUtil {

	public static int serialInt = 1;

	private static final DecimalFormat format8 = new DecimalFormat("00000000");

	private static final DecimalFormat format12 = new DecimalFormat("000000000000");

	private static final BigInteger divisor;

	private static final BigInteger divisor12;

	static {
		divisor = BigInteger.valueOf(19999999L).multiply(BigInteger.valueOf(5));
		divisor12 = BigInteger.valueOf(190000000097L).multiply(BigInteger.valueOf(5));
	}

	public static String genSerialNo() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String strNow = sdf.format(new Date());

		// 生成3位随机数
		Random random = new Random();
		int intRandom = random.nextInt(999);

		String strRandom = String.valueOf(intRandom);
		int len = strRandom.length();
		for (int i = 0; i < (3 - len); i++) {
			strRandom = "0" + strRandom;
		}
		String serialStr = SerialsUtil.nextSerial();
		return (strNow + strRandom + serialStr);
	}

	public static synchronized String nextSerial() {
		int serial = serialInt++;
		if (serial > 999) {
			serialInt = 1;
			serial = 1;
		}
		String serialStr = serial + "";
		int len = serialStr.length();
		for (int i = 0; i < (3 - len); i++) {
			serialStr = "0" + serialStr;
		}

		return serialStr;
	}

	/**
	 * 生成一个12位随机数
	 * @param seed 种子值
	 * @return String 随机数
	 */
	public static String randomNum12(long seed) {
		// 被除数
		BigInteger dividend = BigDecimal.valueOf(seed).pow(5).toBigInteger();
		return format12.format(dividend.remainder(divisor12));
	}

	/**
	 * 生成一个8位随机数
	 * @param seed 种子值
	 * @return String 随机数
	 */
	public static String randomNum8(long seed) {
		// 被除数
		BigInteger dividend = BigDecimal.valueOf(seed).pow(5).toBigInteger();
		return format8.format(dividend.remainder(divisor));
	}

	/*
	 * 10进制转32进制(去除0,O,1,I)
	 */
	public static String from10To32(String numStr, int size) {
		long to = 32;
		long num = Long.parseLong(numStr);
		String jg = "";
		while (num != 0) {
			switch (new Long(num % to).intValue()) {
				case 0:
					jg = "B" + jg;
					break;
				case 1:
					jg = "R" + jg;
					break;
				case 2:
					jg = "6" + jg;
					break;
				case 3:
					jg = "U" + jg;
					break;
				case 4:
					jg = "M" + jg;
					break;
				case 5:
					jg = "E" + jg;
					break;
				case 6:
					jg = "H" + jg;
					break;
				case 7:
					jg = "C" + jg;
					break;
				case 8:
					jg = "G" + jg;
					break;
				case 9:
					jg = "Q" + jg;
					break;
				case 10:
					jg = "A" + jg;
					break;
				case 11:
					jg = "8" + jg;
					break;
				case 12:
					jg = "3" + jg;
					break;
				case 13:
					jg = "S" + jg;
					break;
				case 14:
					jg = "J" + jg;
					break;
				case 15:
					jg = "Y" + jg;
					break;
				case 16:
					jg = "7" + jg;
					break;
				case 17:
					jg = "5" + jg;
					break;
				case 18:
					jg = "W" + jg;
					break;
				case 19:
					jg = "9" + jg;
					break;
				case 20:
					jg = "F" + jg;
					break;
				case 21:
					jg = "T" + jg;
					break;
				case 22:
					jg = "D" + jg;
					break;
				case 23:
					jg = "2" + jg;
					break;
				case 24:
					jg = "P" + jg;
					break;
				case 25:
					jg = "Z" + jg;
					break;
				case 26:
					jg = "N" + jg;
					break;
				case 27:
					jg = "K" + jg;
					break;
				case 28:
					jg = "V" + jg;
					break;
				case 29:
					jg = "X" + jg;
					break;
				case 30:
					jg = "L" + jg;
					break;
				case 31:
					jg = "4" + jg;
					break;
				default:
					jg = String.valueOf(num % to) + jg;
					break;
			}
			num = num / to;
		}
		if (jg.length() < size) {
			int loop = size - jg.length();
			for (int i = 0; i < loop; i++) {
				jg = "2" + jg;
			}
		}
		return jg;
	}

	/*
	 * 10进制转32进制(去除0,O,1,I)
	 */
	public static String from10To24(String numStr, int size) {
		long to = 24;
		long num = Long.parseLong(numStr);
		String jg = "";
		while (num != 0) {
			switch (new Long(num % to).intValue()) {
				case 0:
					jg = "B" + jg;
					break;
				case 1:
					jg = "R" + jg;
					break;
				case 2:
					jg = "U" + jg;
					break;
				case 3:
					jg = "M" + jg;
					break;
				case 4:
					jg = "E" + jg;
					break;
				case 5:
					jg = "H" + jg;
					break;
				case 6:
					jg = "C" + jg;
					break;
				case 7:
					jg = "G" + jg;
					break;
				case 8:
					jg = "Q" + jg;
					break;
				case 9:
					jg = "A" + jg;
					break;
				case 10:
					jg = "S" + jg;
					break;
				case 11:
					jg = "J" + jg;
					break;
				case 12:
					jg = "Y" + jg;
					break;
				case 13:
					jg = "W" + jg;
					break;
				case 14:
					jg = "F" + jg;
					break;
				case 15:
					jg = "T" + jg;
					break;
				case 16:
					jg = "D" + jg;
					break;
				case 17:
					jg = "P" + jg;
					break;
				case 18:
					jg = "Z" + jg;
					break;
				case 19:
					jg = "N" + jg;
					break;
				case 20:
					jg = "K" + jg;
					break;
				case 21:
					jg = "V" + jg;
					break;
				case 22:
					jg = "X" + jg;
					break;
				case 23:
					jg = "L" + jg;
					break;
				default:
					jg = String.valueOf(num % to) + jg;
					break;
			}
			num = num / to;
		}
		if (jg.length() < size) {
			int loop = size - jg.length();
			for (int i = 0; i < loop; i++) {
				jg = "B" + jg;
			}
		}
		return jg;
	}

	public static String getUUID() {
		UUID uuid = UUID.randomUUID();
		String str = uuid.toString();
		// 去掉"-"符号
		String temp = str.substring(0, 8) + str.substring(9, 13) + str.substring(14, 18) + str.substring(19, 23)
				+ str.substring(24);
		return temp;
	}

	public static String generateShortUUID() {
		String str = randomNum8(System.nanoTime());
		return from10To24(str, 6);
	}

	public static String generateFileUUID() {
		String str = randomNum12(System.nanoTime());
		return from10To32(str, 8);
	}

	public static String genToken() {
		return from10To32(randomNum12(System.currentTimeMillis()), 8) + from10To32(randomNum12(System.nanoTime()), 8);
	}

	public static void main(String[] args) {
		Set set = new HashSet();
		String str;
		for (int i = 0; i < 300; i++) {
			str = generateShortUUID();
			System.out.println(str);
			set.add(str);
		}
		System.out.println(set.size());
	}

}