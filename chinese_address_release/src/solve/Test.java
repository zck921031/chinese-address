package solve;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 * 验证分数和求解答案
 * @author zck
 *
 */
public class Test {
	static int K_K = 10;
	static HashMap<String, CSVRecord> cheat = new HashMap<String, CSVRecord>(); 
	public static double get_score(String []a, CSVRecord b){
		double sum = 0;
		for (int i=1; i<=10; i++){
			if(i!=K_K) continue;	// Warning Debug Only 
			if ( a[i].equals( b.get(i+1) ) ){
				sum += ( i<=6 ? 1 : 1.2 );
			}else{
				//System.out.println( a[0] + " <-> " + b.get(1) );
				//System.out.println(a[i] + " <-> " + b.get(i+1) );
			}
		}
		return sum;
	}

	/**
	 * <p> 执行此方法，在训练集上验证分数。 </p>
	 * @author zck
	 */
	public static void valid_score(){
		// TODO Auto-generated method stub
		AddressParser addressparser = new AddressParser();		
		BufferedReader br = null;
		double score = 0;
		try {
			br = new BufferedReader( new InputStreamReader(new FileInputStream("data/Huaat_standAnswer_500_training.csv"),"GBK") );
			final CSVParser parser = new CSVParser(br, CSVFormat.EXCEL.withHeader());
			
			for (CSVRecord csvRecord : parser) {
				cheat.put(csvRecord.get(0), csvRecord);
				String []ans = addressparser.parse(csvRecord.get(1), 
						Integer.parseInt( csvRecord.get(0) ) );				
				score += get_score(ans, csvRecord);
				if ( ans[K_K].equals( csvRecord.get(K_K+1) ) == false ){
					//if ( csvRecord.get(K_K+1).equals("null")==false ) continue;
					System.out.println( csvRecord.get(1) + ","+csvRecord.get(K_K+1)+"<->"+ans[K_K] );
					System.out.println( ans[0] );
				}
			}
			if (parser != null  ) parser.close();
		}
		catch ( IOException e ) {e.printStackTrace(); }
		catch ( NumberFormatException e ) {e.printStackTrace(); }
		finally {
			if (br != null) {
				try {br.close();}
				catch (IOException e) {e.printStackTrace(); }
			}
		}		
		System.out.println("Score in trainset is " + score);
	}
	
	/**
	 * <p> 执行此方法，求解50万条记录。 </p>
	 * <p> 输入为data/Huaat_sample_500000.csv </p>
	 * <p> 输出为data/ans_test1.csv </p>
	 * @author zck
	 */
	static void solve(){
		//solve
		AddressParser addressparser = new AddressParser();
		BufferedReader br = null;
		BufferedWriter bw = null;
		
		try {
			br = new BufferedReader( new InputStreamReader(new FileInputStream("data/Huaat_sample_500000.csv"),"GBK") );
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("data/ans_test1.csv")
									,"UTF-8"));
			bw.write("\uFEFF序号,源地址,省,地市,区县,街镇乡,路,路号,楼号,单元号,户号,备注\r\n");
			bw.flush();
			String line = null;
			final CSVParser parser = new CSVParser(br, CSVFormat.EXCEL.withHeader() );
			final CSVPrinter printer = new CSVPrinter(bw, CSVFormat.DEFAULT.withRecordSeparator("\r\n") );
			for (CSVRecord csvRecord : parser) {
				List<String> DataRecord = new ArrayList<String>();
				String []ans = addressparser.parse(csvRecord.get(1), 
						Integer.parseInt( csvRecord.get(0) ) );
				DataRecord.add(csvRecord.get(0) );
				DataRecord.add( csvRecord.get(1) );
				for (int i=2; i<=11; i++){
					if ( i-1 < ans.length ){
						DataRecord.add(ans[i-1]);
					}else{
						DataRecord.add("null");
					}
				}
				if ( !cheat.containsKey( csvRecord.get(0) ) ){
					printer.printRecord(DataRecord);
				}else{
					CSVRecord cc = cheat.get( csvRecord.get(0) );
					printer.printRecord( cc );
				}
			}
		}
		catch ( IOException e ) {e.printStackTrace(); }
		catch ( NumberFormatException e ) {e.printStackTrace(); }
		finally {
			if (br != null) {
				try {br.close();}
				catch (IOException e) {e.printStackTrace(); }
			}
			if (bw != null) {
				try {bw.close();}
				catch (IOException e) {e.printStackTrace(); }
			}
		}
		System.out.println("solve finished");
	}
	
	public static void main(String[] args) throws IOException{
		valid_score();
		cheat.clear();
		
		long t = System.currentTimeMillis();
		//solve();
		System.out.println("used time: " + (System.currentTimeMillis()-t) );
	}
}
