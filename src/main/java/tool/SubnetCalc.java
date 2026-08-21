package tool;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.net.util.SubnetUtils;

/**
 * ネットワークアドレスおよびサブネットマスク情報を計算・表示するユーティリティクラスです。
 * 
 * <p>IPアドレスやCIDR表記、サブネットマスク文字列を受け取り、ネットワークアドレス、
 * ブロードキャストアドレス、利用可能なホストアドレス範囲などを算出して出力します。</p>
 * 
 * <pre>
 * 使用例 (CLI):
 *   java -jar SubnetCalc.jar --ip 192.168.1.10/24
 *   java -jar SubnetCalc.jar --mask 255.255.255.0
 * 
 * 使用例 (Java API):
 *   SubnetCalc info = new SubnetCalc(new String[]{"--ip", "192.168.1.1/24"}, false);
 *   String mask = info.getSubnetMask(24);
 * </pre>
 */
public final class SubnetCalc {

	/** クラス名 */
	private static final String CLASS_NAME = SubnetCalc.class.getSimpleName();

	/** 引数マップキー: IPアドレス */
	private static final String ARG_KEY_IP_ADDR = "argIpAddr";

	/** 引数マップキー: サブネットマスク */
	private static final String ARG_KEY_MASK = "argMask";

	/** オプション: IPアドレス (--ip) */
	private static final String OPTION_IP_LONG = "--ip";

	/** オプション: IPアドレス (-ip) */
	private static final String OPTION_IP_SHORT = "-ip";

	/** オプション: マスク (--mask) */
	private static final String OPTION_MASK_LONG = "--mask";

	/** オプション: マスク (-mask) */
	private static final String OPTION_MASK_SHORT = "-mask";

	/** ヘルプオプション一覧 */
	private static final String[] HELP_OPTIONS = {"-h", "--help", "-help", "-?", "/?"};

	/** デフォルトおよび最大マスクビット数 */
	private static final int MAX_MASK_BIT = 32;

	/** 最小有効マスクビット数 */
	private static final int MIN_MASK_BIT = 8;

	/** 最小IPv4マスクビット数 */
	private static final int MIN_IPV4_MASK_BIT = 0;

	/** 正常終了コード */
	private static final int EXIT_CODE_NORMAL = 0;

	/** Usage表示終了コード */
	private static final int EXIT_CODE_USAGE = 10;

	/**
	 * コマンドラインからアプリケーションを起動するためのエントリポイントです。
	 * 
	 * <pre>
	 * 使用例:
	 *   SubnetCalc.main(new String[]{"--ip", "192.168.0.1/24"});
	 * </pre>
	 * 
	 * @param args コマンドライン引数の配列
	 */
	public static void main(final String[] args) {
		new SubnetCalc(args, true);
	}

	/** コマンドライン引数を保持するマップ */
	private final Map<String, String> argsMap = new LinkedHashMap<>();

	/** System.exit()でJVMごと強制終了させるか否かのフラグ */
	private boolean killJvm;

	/**
	 * 引数を指定して {@code SubnetCalc} インスタンスを生成し、処理を実行します。
	 * 
	 * <pre>
	 * 使用例:
	 *   SubnetCalc app = new SubnetCalc(new String[]{"--ip", "10.0.0.1/16"}, false);
	 * </pre>
	 * 
	 * @param args    コマンドライン引数の配列
	 * @param killJvm 処理完了時に {@link System#exit(int)} でJVMを強制終了させるか否か
	 */
	public SubnetCalc(final String[] args, final boolean killJvm) {
		exec(args, killJvm);
	}

	/**
	 * 指定されたIPアドレスおよびサブネットマスクに基づき、ネットワークアドレス情報をコンソールに出力します。
	 * 
	 * <p>IPアドレス、サブネットマスク、ネットワークアドレス、ブロードキャストアドレス、総アドレス数、
	 * 利用可能なホストアドレス範囲を出力します。</p>
	 * 
	 * <pre>
	 * 使用例:
	 *   showIpAddrInfo("192.168.1.100/24");
	 * </pre>
	 * 
	 * @param ipAddrWithMask "IPアドレス/マスクビット" 形式の文字列 (例: "192.168.1.1/24")
	 */
	public void showIpAddrInfo(final String ipAddrWithMask) {
		if (ipAddrWithMask == null || ipAddrWithMask.isEmpty()) {
			return;
		}
		final String[] elements = ipAddrWithMask.split("/");
		final String ipAddr = elements[0];
		int maskBit = MAX_MASK_BIT;
		if (1 < elements.length) {
			maskBit = getValidMaskBit(elements[1]);
		}
		final String mask = getSubnetMask(maskBit);
		final SubnetUtils subnet = new SubnetUtils(ipAddr + "/" + maskBit);
		subnet.setInclusiveHostCount(true);
		final String networkAddress = subnet.getInfo().getNetworkAddress();
		final String broadcastAddress = subnet.getInfo().getBroadcastAddress();
		final String[] allAddresses = subnet.getInfo().getAllAddresses();
		final int totalAddresses = allAddresses.length;
		System.out.println("IPアドレス              ：" + ipAddr);
		System.out.println("サブネットマスク        ：" + mask + "/" + maskBit);
		System.out.println("ネットワークアドレス    ：" + networkAddress);
		System.out.println("ブロードキャストアドレス：" + broadcastAddress);
		System.out.println("総アドレス数            ：" + totalAddresses);
		final String startHostAddr = (totalAddresses < 3 ? allAddresses[0] : allAddresses[1]);
		final String endHostAddr = (2 < totalAddresses ? allAddresses[totalAddresses - 2] : allAddresses[totalAddresses - 1]);
		System.out.println("ホストアドレス          ：" + startHostAddr + " - " + endHostAddr);
	}

	/**
	 * 指定されたサブネットマスク文字列またはビット表記から有効なマスクビット数を取得します。
	 * 
	 * <p>戻り値は有効範囲である 8 から 32 の範囲に正規化されます。</p>
	 * 
	 * <pre>
	 * 使用例:
	 *   int bit1 = getValidMaskBit("255.255.255.0"); // 24
	 *   int bit2 = getValidMaskBit("/16");            // 16
	 *   int bit3 = getValidMaskBit("24");             // 24
	 * </pre>
	 * 
	 * @param maskStr サブネットマスク (例: "255.255.255.0") またはマスクビット表記 (例: "/24", "24")
	 * @return 有効なマスクビット数 (8〜32)
	 */
	public int getValidMaskBit(final String maskStr) {
		if (maskStr == null || maskStr.isEmpty()) {
			return MAX_MASK_BIT;
		}
		int maskBit;
		if (maskStr.contains(".")) {
			maskBit = convertToCidr(maskStr);
		} else {
			try {
				maskBit = Integer.parseInt(maskStr.replace("/", ""));
			} catch (final NumberFormatException e) {
				maskBit = MAX_MASK_BIT;
			}
		}
		if (MAX_MASK_BIT < maskBit) {
			maskBit = MAX_MASK_BIT;
		}
		if (maskBit < MIN_MASK_BIT) {
			maskBit = MIN_MASK_BIT;
		}
		return maskBit;
	}

	/**
	 * サブネットマスクおよびマスクビット数を "サブネットマスク/ビット数" 形式で返却します。
	 * 
	 * <pre>
	 * 使用例:
	 *   String maskWithBit = getMaskWithBit("255.255.255.0"); // "255.255.255.0/24"
	 * </pre>
	 * 
	 * @param maskStr サブネットマスクまたはマスクビット文字列
	 * @return "サブネットマスク/マスクビット" 形式の文字列 (例: "255.255.255.0/24")
	 */
	public String getMaskWithBit(final String maskStr) {
		final int maskBit = getValidMaskBit(maskStr);
		return getSubnetMask(maskBit) + "/" + maskBit;
	}

	/**
	 * マスクビット数からドット区切りのサブネットマスク文字列を取得します。
	 * 
	 * <pre>
	 * 使用例:
	 *   String mask = getSubnetMask(24); // "255.255.255.0"
	 *   String mask0 = getSubnetMask(0); // "0.0.0.0"
	 * </pre>
	 * 
	 * @param maskBits マスクビット数 (0〜32)
	 * @return ドット区切りのサブネットマスク文字列。範囲外の場合は空文字列
	 */
	public String getSubnetMask(final int maskBits) {
		if (maskBits < MIN_IPV4_MASK_BIT || MAX_MASK_BIT < maskBits) {
			return "";
		}
		if (maskBits == 0) {
			return "0.0.0.0";
		}
		final long maskLong = (0xFFFFFFFFL << (MAX_MASK_BIT - maskBits)) & 0xFFFFFFFFL;
		return String.format("%d.%d.%d.%d",
				(maskLong >> 24) & 0xFF,
				(maskLong >> 16) & 0xFF,
				(maskLong >> 8) & 0xFF,
				maskLong & 0xFF);
	}

	/**
	 * ドット区切りのサブネットマスクからCIDR形式のマスクビット数を算出します。
	 * 
	 * <pre>
	 * 使用例:
	 *   int cidr = convertToCidr("255.255.255.0"); // 24
	 * </pre>
	 * 
	 * @param subnetMask ドット区切りのサブネットマスク (例: "255.255.255.0")
	 * @return マスクビット数 (CIDR値)
	 */
	public int convertToCidr(final String subnetMask) {
		if (subnetMask == null || subnetMask.isEmpty()) {
			return 0;
		}
		try {
			return Arrays.stream(subnetMask.split("\\."))
					.mapToInt(Integer::parseInt)
					.map(Integer::bitCount)
					.sum();
		} catch (final NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * 引数の解析を行い、指定されたネットワーク情報表示処理を実行します。
	 * 
	 * <pre>
	 * 使用例:
	 *   exec(new String[]{"--mask", "255.255.0.0"}, false);
	 * </pre>
	 * 
	 * @param args    引数配列
	 * @param killJvm 処理終了時にJVMを終了するかどうか
	 */
	private void exec(final String[] args, final boolean killJvm) {
		final int exitCode = EXIT_CODE_NORMAL;
		boolean isShowUsage = false;
		this.killJvm = killJvm;

		// 引数処理
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				if (isHelpOption(args[i])) {
					isShowUsage = true;
				} else if (OPTION_IP_LONG.equalsIgnoreCase(args[i]) || OPTION_IP_SHORT.equalsIgnoreCase(args[i])) {
					if ((i + 1) < args.length && args[i + 1] != null && !args[i + 1].isEmpty()) {
						if (!args[i + 1].startsWith("-")) {
							argsMap.put(ARG_KEY_IP_ADDR, args[i + 1]);
						}
					}
				} else if (OPTION_MASK_LONG.equalsIgnoreCase(args[i]) || OPTION_MASK_SHORT.equalsIgnoreCase(args[i])) {
					if ((i + 1) < args.length && args[i + 1] != null && !args[i + 1].isEmpty()) {
						if (!args[i + 1].startsWith("-")) {
							argsMap.put(ARG_KEY_MASK, args[i + 1]);
						}
					}
				}
			}
		}

		// USAGEチェック
		if (isShowUsage) {
			showUsage(EXIT_CODE_USAGE);
		}

		// 処理
		if (argsMap.containsKey(ARG_KEY_IP_ADDR)) {
			final String argIpAddr = argsMap.getOrDefault(ARG_KEY_IP_ADDR, "");
			if (!argIpAddr.isEmpty()) {
				final String[] elements = argIpAddr.split("/");
				final String ipAddr = elements[0];
				int maskBit = MAX_MASK_BIT;
				if (1 < elements.length) {
					maskBit = getValidMaskBit(elements[1]);
				}
				if (argsMap.containsKey(ARG_KEY_MASK)) {
					final String argMask = argsMap.getOrDefault(ARG_KEY_MASK, "");
					if (!argMask.isEmpty()) {
						maskBit = getValidMaskBit(argMask);
					}
				}
				showIpAddrInfo(ipAddr + "/" + maskBit);
			}
		} else if (argsMap.containsKey(ARG_KEY_MASK)) {
			final String argMask = argsMap.getOrDefault(ARG_KEY_MASK, "");
			if (!argMask.isEmpty()) {
				System.out.println("サブネットマスク        ：" + getMaskWithBit(argMask));
			}
		}

		terminate(exitCode);
	}

	/**
	 * 指定された引数がヘルプオプションであるか判定します。
	 * 
	 * @param arg 引数文字列
	 * @return ヘルプオプションの場合は true、それ以外は false
	 */
	private boolean isHelpOption(final String arg) {
		for (final String helpOpt : HELP_OPTIONS) {
			if (helpOpt.equalsIgnoreCase(arg)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * プログラムの終了処理を実行します。
	 * 
	 * <p>{@code killJvm} が有効な場合のみ {@link System#exit(int)} を呼び出します。</p>
	 * 
	 * <pre>
	 * 使用例:
	 *   terminate(0);
	 * </pre>
	 * 
	 * @param exitCode 終了コード
	 */
	private void terminate(final int exitCode) {
		if (killJvm) {
			System.exit(exitCode);
		}
	}

	/**
	 * コマンドの使用方法 (Usage) をコンソールに表示し、終了処理を行います。
	 * 
	 * <pre>
	 * 使用例:
	 *   showUsage(10);
	 * </pre>
	 * 
	 * @param exitCode 終了コード
	 */
	private void showUsage(final int exitCode) {
		System.out.println("");
		System.out.println("Usage:   java -jar " + CLASS_NAME + ".jar [option...]");
		System.out.println("");
		System.out.println("  --ip ip address    IP ADDRESS/MASK     (Val = " + argsMap.getOrDefault(ARG_KEY_IP_ADDR, "") + ")");
		System.out.println("  --mask mask        MASK                (Val = " + argsMap.getOrDefault(ARG_KEY_MASK, "") + ")");
		System.out.println("");
		System.out.println("Help options:");
		System.out.println("  -h                           SHOW THIS HELP MESSAGE");
		System.out.println("");
		System.out.println("exit code: NORMAL=0 / WARN=10 / ERROR=20 or HTTPCODE(200以外)");
		System.out.println("");
		terminate(exitCode);
	}

}
