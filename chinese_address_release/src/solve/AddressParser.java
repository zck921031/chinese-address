package solve;

public class AddressParser {
	CityMatch city = null;
	/**
	 * 
	 * @param str
	 * @param tick
	 * @return []String: 1:省 2:地 :县区 4:街镇乡 5:路 6:路号 7:楼号 8:单元号 9:户号 10:备注
	 */
	public String[] parse(String str, int tick){
		String []ans = null;
		ans = city.search(str);
		
		String regstr[] = RegularParser.parse( ans[0]);
		for (int i=3; i<=10; i++){
			if ( "null"==ans[i] && "null"!=regstr[i] ){
				ans[i] = regstr[i];
			}
		}
		//ans[7]="null";
		//ChineseAddress ca = RegularAddressParser.parse( ans[0]);
		//System.out.println( ca );
		//System.out.println( ToAnalysis.parse( ans[0] ) );
		//System.out.println( str );
		//RegularParser.parse( ans[0]);
		//if ( ca.roads.size() == 1 ) ans[5] = ca.roads.get(0);
		//ca = RegularParser.parse( ans[0] );
		//if ( ca.number!= null && ca.number.length()>1 ) ans[6] = ca.number;
		
//		if ( ca.roads.size()==1 ){
//			System.err.println( "! " +ca.roads.get(0) );
//			System.out.println( ToAnalysis.parse( ca.roads.get(0) );
//		}else if ( ca.roads.size() > 1 ){
//			for ( String s : ca.roads ){
//				System.err.println(s);
//			}
//		}else{
//			System.err.println("null");
//		}
		
		return ans;
	}
	{
		city = new CityMatch();
		city.init_from_txt("data/three_level_modified.txt");
	}
}
