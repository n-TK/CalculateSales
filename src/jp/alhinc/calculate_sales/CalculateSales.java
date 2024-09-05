package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";
	private static final String FILE_NAME_COMMODITY_LST ="commodity.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "のフォーマットが不正です";
	private static final String NOT_CONSECUTIVE_NUMBERS = "売上ファイルが連場になっていません";
	private static final String SALES_AMOUNT_OVERFLOW = "合計金額が10桁を超えました";
	private static final String CODE_INVALID_FORMAT ="コードが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) throws IOException {

		//エラー処理 3 コマンドライン引数の確認
		if(args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		//商品コードと商品名を保持するMao
		Map<String, String> commodityNames = new HashMap<>();
		//商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales,"^[0-9]{3}$","支店定義")) {
			return;
		}

		//商品定義ファイル読み込み
		if(!readFile(args[0],FILE_NAME_COMMODITY_LST, commodityNames, commoditySales,"^[0-9A-Za-z]{8}$","商品定義")) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)

		/*
		 * 処理内容2-1 拡張子がrcdかつ、ファイル名が数字八桁のファイルの抽出
		 */
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();

		for(int i = 0; i < files.length; i++) {
			String fileName = files[i].getName();

			//エラー処理3 読み込んだ対象がファイルなのか確認
			if(files[i].isFile() && fileName.matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		/*
		 * エラー処理2-1 ファイルの連番チェック
		 */

		for (int i = 0; i < rcdFiles.size() -1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0,8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0,8));
			if((latter - former) != 1) {
				System.out.println(NOT_CONSECUTIVE_NUMBERS);
				return;
			}
		}

		// 処理2-2 売上集計

		BufferedReader br = null;

		for(int i = 0; i < rcdFiles.size(); i++) {
			try {
				br = new BufferedReader(new FileReader(rcdFiles.get(i)));
				ArrayList<String> fileContents = new ArrayList<>();

				String line = "";
				while((line = br.readLine()) != null) {
					fileContents.add(line);
				}

				String fileName = rcdFiles.get(i).getName();

				if(fileContents.size() != 3) {
					System.out.println(fileName  + FILE_INVALID_FORMAT);
					return;
				}

				String branchCode = fileContents.get(0);
				String commodityCode = fileContents.get(1);
				String sale = fileContents.get(2);

				//エラー処理2-3 mapに支店コードが存在するかのチェック
				if(!branchNames.containsKey(branchCode)) {
					System.out.println(fileName + "の支店" + CODE_INVALID_FORMAT);
					return;
				}

				//追加課題 エラー処理2-4 mapに商品コードが存在するかのチェック
				if(!commodityNames.containsKey(commodityCode)) {
					System.out.println(fileName + "の商品" + CODE_INVALID_FORMAT);
					return;
				}

				//エラー処理3 売上金額が数字か確認
				if(!sale.matches("[0-9]+")){
					System.out.println(UNKNOWN_ERROR);
					return;
				}
				long fileSale = Long.parseLong(sale);
				long branchSaleAmount = branchSales.get(branchCode) + fileSale;
				long commoditySaleAmount = commoditySales.get(commodityCode) + fileSale;

				//エラー処理2-2 合計金額10桁の超過チェック
				if (branchSaleAmount > 1000000000L || commoditySaleAmount > 1000000000L) {
					System.out.println(SALES_AMOUNT_OVERFLOW);
					return;
				}

				branchSales.put(branchCode, branchSaleAmount);
				commoditySales.put(commodityCode, commoditySaleAmount);

			} catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;

			} finally {
				if(br != null) {
					try {
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}

			}
		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

		// 商品別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> names, Map<String, Long> sales, String regax, String type)  {
		BufferedReader br = null;
		File file = new File("test", fileName);
		FileReader fr = new FileReader(file);
		br = new BufferedReader(fr);

		try {


			//エラー処理1-1
			if(!file.exists()) {
				System.out.println(type + FILE_NOT_EXIST);
				return false;
			}


			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む

			while((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items = line.split(",");
				String code = items[0];
				String name = items[1];
				if((items.length != 2 || (!code.matches(regax)))) {
					System.out.println(type + "ファイルの" + FILE_INVALID_FORMAT);
					return false;
				}
				names.put(code, name);
				sales.put(code, 0L);
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> names, Map<String, Long> sales)throws IOException {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)

		BufferedWriter bw = null;
		try {
			File file = new File(path,fileName);
			bw = new BufferedWriter(new FileWriter(file));

			for(String key : names.keySet()) {
				bw.write(key + "," + names.get(key) + "," + sales.get(key));
				bw.newLine();
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;

		} finally {
			if(bw != null) {
				try {
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}
}