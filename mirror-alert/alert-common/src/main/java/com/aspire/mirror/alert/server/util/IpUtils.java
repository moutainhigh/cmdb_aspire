package com.aspire.mirror.alert.server.util;

import java.net.UnknownHostException;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(IpUtils.class);

	public static final String _255 = "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

	public static final Pattern PATTEN = Pattern.compile("^(?:" + _255 + "\\.){3}" + _255 + "$");

	public static String longToIpV4(long longIp) {

		int octet3 = (int) ((longIp >> 24) % 256);

		int octet2 = (int) ((longIp >> 16) % 256);

		int octet1 = (int) ((longIp >> 8) % 256);

		int octet0 = (int) ((longIp) % 256);

		return octet3 + "." + octet2 + "." + octet1 + "." + octet0;

	}

	public static long ipV4ToLong(String ip) {

		String[] octets = ip.split("\\.");

		return (Long.parseLong(octets[0]) << 24) + (Integer.parseInt(octets[1]) << 16)

				+ (Integer.parseInt(octets[2]) << 8) + Integer.parseInt(octets[3]);

	}

	public static boolean isIPv4Private(String ip) {

		long longIp = ipV4ToLong(ip);

		return (longIp >= ipV4ToLong("10.0.0.0") && longIp <= ipV4ToLong("10.255.255.255"))

				|| (longIp >= ipV4ToLong("172.16.0.0") && longIp <= ipV4ToLong("172.31.255.255"))

				|| longIp >= ipV4ToLong("192.168.0.0") && longIp <= ipV4ToLong("192.168.255.255");

	}

	public static boolean isIPv4Valid(String ip) {

		return PATTEN.matcher(ip).matches();

	}

	/**
	 * 
	 * 获取请求主机IP地址,如果通过代理进来，则透过防火墙获取真实IP地址;
	 *
	 * 
	 * 
	 * @param request
	 * 
	 * @return
	 * @throws UnknownHostException 
	 * 
	 */

	public final static String getIpAddress(HttpServletRequest request) throws UnknownHostException {


		if (request == null) {

			LOGGER.info("IpUtils.getIpAddress request is null");

			return null;

		}

		String headerType = "X-Forwarded-For";

		String ip = request.getHeader("X-Forwarded-For");

		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {

			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {

				headerType = "Proxy-Client-IP";

				ip = request.getHeader("Proxy-Client-IP");

			}

			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {

				headerType = "WL-Proxy-Client-IP";

				ip = request.getHeader("WL-Proxy-Client-IP");

			}

			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {

				headerType = "HTTP_CLIENT_IP";

				ip = request.getHeader("HTTP_CLIENT_IP");

			}

			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {

				headerType = "HTTP_X_FORWARDED_FOR";

				ip = request.getHeader("HTTP_X_FORWARDED_FOR");

			}

			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {

				headerType = "getRemoteAddr";

				ip = request.getRemoteAddr();

			}

		} else if (ip.length() > 15) {

			String[] ips = ip.split(",");

			for (int index = 0; index < ips.length; index++) {

				String strIp = (String) ips[index];

				if (!("unknown".equalsIgnoreCase(strIp))) {

					ip = strIp;

					break;

				}

			}

		}

		LOGGER.info("IpUtils.getIpAddress {} ip = {}", headerType, ip);

		if(ip.equals("0:0:0:0:0:0:0:1")) {
	        //InetAddress address = InetAddress.getLocalHost();
	        ip = "127.0.0.1";
		}
		return ip;

	}

}
