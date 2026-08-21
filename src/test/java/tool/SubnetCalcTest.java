package tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link SubnetCalc} の単体テストクラスです。
 */
public final class SubnetCalcTest {

	/** テスト用一時ディレクトリパス */
	private static final Path TEMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "SubnetCalc", "SubnetCalcTest");

	/**
	 * 全テスト実行前の初期化処理を行います。
	 * 
	 * @throws IOException ディレクトリ作成に失敗した場合
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws IOException {
		if (!Files.exists(TEMP_DIR)) {
			Files.createDirectories(TEMP_DIR);
		}
	}

	/**
	 * 全テスト実行後のクリーンアップ処理を行います。
	 * 
	 * @throws IOException ディレクトリ削除に失敗した場合
	 */
	@AfterClass
	public static void tearDownAfterClass() throws IOException {
		if (Files.exists(TEMP_DIR)) {
			Files.deleteIfExists(TEMP_DIR);
		}
	}

	/** 標準出力キャプチャ用ストリーム */
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

	/** 標準エラー出力キャプチャ用ストリーム */
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

	/** 元の標準出力ストリーム */
	private final PrintStream originalOut = System.out;

	/** 元の標準エラー出力ストリーム */
	private final PrintStream originalErr = System.err;

	/**
	 * 各テスト実行前に出力ストリームを設定します。
	 */
	@Before
	public void setUpStreams() {
		outContent.reset();
		errContent.reset();
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
	}

	/**
	 * 各テスト実行後に出力ストリームを復元します。
	 */
	@After
	public void restoreStreams() {
		System.setOut(originalOut);
		System.setErr(originalErr);
	}

	/**
	 * {@link SubnetCalc#convertToCidr(String)} のテストです。
	 */
	@Test
	public void testConvertToCidr() {
		final SubnetCalc info = new SubnetCalc(new String[]{}, false);

		assertEquals(32, info.convertToCidr("255.255.255.255"));
		assertEquals(24, info.convertToCidr("255.255.255.0"));
		assertEquals(23, info.convertToCidr("255.255.254.0"));
		assertEquals(16, info.convertToCidr("255.255.0.0"));
		assertEquals(8, info.convertToCidr("255.0.0.0"));
		assertEquals(0, info.convertToCidr("0.0.0.0"));

		// 異常系
		assertEquals(0, info.convertToCidr(null));
		assertEquals(0, info.convertToCidr(""));
		assertEquals(0, info.convertToCidr("invalid.mask"));
	}

	/**
	 * {@link SubnetCalc#getSubnetMask(int)} のテストです。
	 */
	@Test
	public void testGetSubnetMask() {
		final SubnetCalc info = new SubnetCalc(new String[]{}, false);

		assertEquals("255.255.255.255", info.getSubnetMask(32));
		assertEquals("255.255.255.0", info.getSubnetMask(24));
		assertEquals("255.255.254.0", info.getSubnetMask(23));
		assertEquals("255.255.0.0", info.getSubnetMask(16));
		assertEquals("255.0.0.0", info.getSubnetMask(8));
		assertEquals("0.0.0.0", info.getSubnetMask(0));

		// 範囲外
		assertEquals("", info.getSubnetMask(-1));
		assertEquals("", info.getSubnetMask(33));
	}

	/**
	 * {@link SubnetCalc#getValidMaskBit(String)} のテストです。
	 */
	@Test
	public void testGetValidMaskBit() {
		final SubnetCalc info = new SubnetCalc(new String[]{}, false);

		assertEquals(24, info.getValidMaskBit("24"));
		assertEquals(16, info.getValidMaskBit("/16"));
		assertEquals(24, info.getValidMaskBit("255.255.255.0"));
		assertEquals(16, info.getValidMaskBit("255.255.0.0"));
		assertEquals(8, info.getValidMaskBit("255.0.0.0"));

		// 境界値補正 (8〜32)
		assertEquals(32, info.getValidMaskBit("35"));
		assertEquals(8, info.getValidMaskBit("4"));

		// 異常系
		assertEquals(32, info.getValidMaskBit(null));
		assertEquals(32, info.getValidMaskBit(""));
		assertEquals(32, info.getValidMaskBit("invalid"));
	}

	/**
	 * {@link SubnetCalc#getMaskWithBit(String)} のテストです。
	 */
	@Test
	public void testGetMaskWithBit() {
		final SubnetCalc info = new SubnetCalc(new String[]{}, false);

		assertEquals("255.255.255.0/24", info.getMaskWithBit("255.255.255.0"));
		assertEquals("255.255.255.0/24", info.getMaskWithBit("24"));
		assertEquals("255.255.0.0/16", info.getMaskWithBit("/16"));
	}

	/**
	 * {@link SubnetCalc#showIpAddrInfo(String)} のテストです。
	 */
	@Test
	public void testShowIpAddrInfo() {
		final SubnetCalc info = new SubnetCalc(new String[]{}, false);
		outContent.reset();

		info.showIpAddrInfo("192.168.1.100/24");
		final String output = outContent.toString();

		assertTrue(output.contains("IPアドレス              ：192.168.1.100"));
		assertTrue(output.contains("サブネットマスク        ：255.255.255.0/24"));
		assertTrue(output.contains("ネットワークアドレス    ：192.168.1.0"));
		assertTrue(output.contains("ブロードキャストアドレス：192.168.1.255"));
		assertTrue(output.contains("総アドレス数            ：256"));
		assertTrue(output.contains("ホストアドレス          ：192.168.1.1 - 192.168.1.254"));
	}

	/**
	 * {@link SubnetCalc#showIpAddrInfo(String)} で単一ホスト /32 サブネットおよび null/空文字指定時のテストです。
	 */
	@Test
	public void testShowIpAddrInfoSmallSubnet() {
		final SubnetCalc info = new SubnetCalc(new String[]{}, false);
		outContent.reset();

		info.showIpAddrInfo("192.168.1.1/32");
		final String output = outContent.toString();

		assertTrue(output.contains("IPアドレス              ：192.168.1.1"));
		assertTrue(output.contains("サブネットマスク        ：255.255.255.255/32"));
		assertTrue(output.contains("総アドレス数            ：1"));

		// null や空文字の場合は出力なし
		outContent.reset();
		info.showIpAddrInfo(null);
		info.showIpAddrInfo("");
		assertEquals("", outContent.toString());
	}

	/**
	 * {@code --ip} オプション指定時の実行テストです。
	 */
	@Test
	public void testExecWithIpOption() {
		outContent.reset();
		new SubnetCalc(new String[]{"--ip", "10.0.0.1/8"}, false);
		final String output = outContent.toString();

		assertTrue(output.contains("IPアドレス              ：10.0.0.1"));
		assertTrue(output.contains("サブネットマスク        ：255.0.0.0/8"));
		assertTrue(output.contains("ネットワークアドレス    ：10.0.0.0"));
		assertTrue(output.contains("ブロードキャストアドレス：10.255.255.255"));
	}

	/**
	 * {@code --ip} および {@code --mask} オプション併用指定時の実行テストです。
	 */
	@Test
	public void testExecWithIpAndMaskOptions() {
		outContent.reset();
		new SubnetCalc(new String[]{"--ip", "172.16.0.5", "--mask", "255.255.0.0"}, false);
		final String output = outContent.toString();

		assertTrue(output.contains("IPアドレス              ：172.16.0.5"));
		assertTrue(output.contains("サブネットマスク        ：255.255.0.0/16"));
		assertTrue(output.contains("ネットワークアドレス    ：172.16.0.0"));
	}

	/**
	 * {@code --mask} オプション単独指定時の実行テストです。
	 */
	@Test
	public void testExecWithMaskOptionOnly() {
		outContent.reset();
		new SubnetCalc(new String[]{"--mask", "255.255.255.0"}, false);
		final String output = outContent.toString();

		assertTrue(output.contains("サブネットマスク        ：255.255.255.0/24"));
	}

	/**
	 * ヘルプオプション指定時の実行テストです。
	 */
	@Test
	public void testExecWithHelpOptions() {
		final String[] helpOptions = new String[]{"-h", "--help", "-help", "-?", "/?"};
		for (final String opt : helpOptions) {
			outContent.reset();
			new SubnetCalc(new String[]{opt}, false);
			final String output = outContent.toString();
			assertTrue("Help output should contain Usage for option: " + opt, output.contains("Usage:"));
			assertTrue("Help output should contain Help options for option: " + opt, output.contains("Help options:"));
		}
	}

	/**
	 * 引数なしまたは null 指定時の実行テストです。
	 */
	@Test
	public void testExecWithEmptyOrNullArgs() {
		outContent.reset();
		new SubnetCalc(new String[]{}, false);
		new SubnetCalc(null, false);
		assertEquals("", outContent.toString());
	}
}
