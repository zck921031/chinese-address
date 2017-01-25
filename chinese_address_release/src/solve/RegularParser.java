package solve;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于规则的中文地址要素识别类
 * @author zck
 *
 */
public class RegularParser {
    static final String reg = "[\u4e00-\u9fa5_0-9]";
    static final Pattern ms_Pattern_qu = Pattern.compile(reg + "+(?:县|区)");
    static final Pattern ms_Pattern_zhen = Pattern.compile(reg + "+?(?:路中段|街道办|街道|胡同|弄堂|街|路(?!口)|镇|开发区|子沟)");
    static final Pattern ms_Pattern_lu = Pattern.compile(reg + "+{2,10}?(?:路中段|道中段|街道|胡同|弄堂|街|巷|路(?!口)|道|镇|大道|开发区|里)");
    static final Pattern ms_Pattern_luhao = Pattern.compile("(?:([付_附]{0,1}[0-9_-]+?(?:号院|号|弄))|"
    		+ "([\u4e00-\u9fa5_0-9]+?(?:村|小区|社区|大厦|公寓|机场|广场|步行街|小镇|国际|公馆|购物中心|大楼|基地|雅居)))");    
    static final Pattern ms_Pattern_louhao = Pattern.compile("(?:([0-9_-_零_一_二_三_四_五_六_七_八_九_十]+?(?:栋|号楼|座|号院))"
    		+ "|([\u4e00-\u9fa5_0-9]+{2,}?(?:商务楼|码头|村|小区|社区|大厦|公寓|机场|广场|步行街|小镇|国际|公馆|阁)))");
    static final Pattern ms_Pattern_danyuanhao = Pattern.compile("[0-9_-_零_一_二_三_四_五_六_七_八_九_十]+?(?:单元|栋|幢|号楼|座|号院)");
    static final Pattern ms_Pattern_huhao = Pattern.compile("[0-9_-_零_一_二_三_四_五_六_七_八_九_十_A-Z_a-z_._-]+?(?:单元|号楼|号铺|座|室|号|楼|#|门面|店面)");  
    static final Pattern ms_Pattern_number = Pattern.compile("[0-9]+"); 
    static final Pattern ms_Pattern_note = Pattern.compile("(?<=\\()(.+?)(?=\\))");
    static final Pattern ms_Pattern_diqu = Pattern.compile(".+(?:地区|休闲区)");

    /**
     * 利用特定的正则表达式，识别中文地址要素，将答案存入数组，返回去除该要素后的结果。
     * @param str String 地址记录字符串
     * @param pat Pattern 正则表达式Pattern对象
     * @param split boolean 返回结果是否去除该地址要素
     * @param minlen int 识别结果最短长度
     * @param maxlen int 识别结果最长长度
     * @param ans String[] 存放答案的数据
     * @param pos int 该要素所在数组的位置
     * @param continuity boolean 是否要求识别出的实体是查询字符串的前缀
     * @return 是否去除识别出的要素后的字符串
     */
    static public String addressRecognize(String str, Pattern pat, boolean split,
    		int minlen, int maxlen, String[] ans, int pos, boolean continuity){
    	String ret = str;
    	Matcher m;
    	m = pat.matcher(str);
    	if ( m.find() ){
    		int len = m.end() - m.start();
    		if ( ( (m.group().startsWith("与") || m.group().startsWith("和") ) && m.group().length()>3 ||
    				str.substring(m.end()).startsWith("与") || str.substring(m.end()).startsWith("和") ) ){
    			//System.err.println("skip: " + str);
    		}else{
    			if ( (m.group().contains("与") || m.group().contains("和")) && m.group().length()>3 ){
    				return str;
    			}
	    		if ( len>=minlen && len<=maxlen ){
	    			if ( !continuity || m.start()==0 ){
		    			if (split) ret = str.substring(0,m.start()) + str.substring(m.end());
		    			ans[pos] = m.group();
	    			}
	    		}
    		}
        }
    	return ret;
    }
    
    /**
     * 对中文地址的4~6级命名实体进行识别
     * @param query_str
     * @param ans
     * @return
     */
    static public String module_456(String query_str, String ans[]){
    	String str = query_str;
    	str = addressRecognize(str, ms_Pattern_qu, true, 0,5, ans, 3, true);
    	// Fix 小区
    	if ( ans[3]!=null && ans[3].endsWith("小区") ){
    		ans[3] = null;
    		str = query_str;
    	}
    	
    	str = addressRecognize(str, ms_Pattern_zhen, true, 0,9, ans, 4, false);
    	str = addressRecognize(str, ms_Pattern_lu, true, 3,7, ans, 5, false);
    	
    	if ( !addressRecognize(str, ms_Pattern_luhao, true, 0,8, ans, 6, true).equals(str) ){
        	str = addressRecognize(str, ms_Pattern_luhao, true, 0,8, ans, 6, true);
    	}else if ( !addressRecognize(str, ms_Pattern_number, true, 0,8, ans, 6, true).equals(str) ){
    		str = addressRecognize(str, ms_Pattern_number, true, 0,8, ans, 6, true);
    	}else{
        	str = addressRecognize(str, ms_Pattern_luhao, true, 0,8, ans, 6, false);    		
    	}
    			
    	if ( ans[4]!=null && !ans[4].endsWith("镇") && !ans[4].endsWith("开发区") && ans[5]==null ){
    		ans[5] = ans[4];
    		ans[4] = null;
    	}
    	if ( ans[4]!=null && !ans[4].endsWith("镇") && !ans[4].endsWith("开发区") && !ans[4].equals("街道") && 
    			!ans[4].equals("街道办") ){
    		ans[4] = null;
    	}

    	return str;
    }
    
    /**
     * 分析3~10级地址命名实体
     * @param String 地址字符串，尽量不包含省市县
     * @return []String: 1:省 2:地市 3:县区 4:街镇乡 5:路 6:路号 7:楼号 8:单元号 9:户号 10:备注
     */
    static public String[] parse(String query_str){
    	Matcher m;
    	String str = query_str;    	
    	String []ans = new String[11];
    	
    	// 识别备注
    	ans[10]="";
    	str = str.replaceAll("（", "(").replace("）", ")");
    	m = ms_Pattern_note.matcher(str);
    	while( m.find() ) {
    		ans[10] += m.group();
    	}
    	if (ans[10].equals("") ) ans[10] = "null";
    	ans[10] = ans[10].replaceAll("[\\)_\\(]", "");
    	str = str.replaceAll("\\(.*?\\)","");    	
    	
    	
    	// 清洗数据
		str = str.replaceAll("其他地区", "其他").replaceAll(reg+"*其他", "");		
		m = ms_Pattern_diqu.matcher(str);
		if ( m.find() && m.start()==0 ){
			String note = m.group();
			if ( note.length()>5 ){
				if ( ans[10].equals("null") ){
					ans[10] = note;
				}else{
					ans[10] = note + ans[10];
				}
			}
			str = str.substring( m.end() );		
		}
    	
		// 识别4~6级地址
		str=module_456(str, ans);    	
    	
		// 识别7~10级地址
    	str = addressRecognize(str, ms_Pattern_louhao, true, 0,80, ans, 7, false);
    	str = addressRecognize(str, ms_Pattern_danyuanhao, true, 0,80, ans, 8, false);    	
    	str = addressRecognize(str, ms_Pattern_huhao, true, 0,80, ans, 9, false);    	
    	if ( !addressRecognize(str, ms_Pattern_huhao, true, 0,80, ans, 0, false).equals(str) ){
    		if ( ans[8]==null || ans[8].equals("null") ){
    			ans[8] = ans[9];
    			str = addressRecognize(str, ms_Pattern_huhao, true, 0,80, ans, 9, false);
    		}
    	}    	
    	if ( !addressRecognize(str, ms_Pattern_number, true, 0,80, ans, 0, true).equals(str) ){
    		if ( ans[9]==null || ans[9].equals("null") ){
    			str = addressRecognize(str, ms_Pattern_number, true, 0,80, ans, 9, true);
    		}
    	}

    	// 修改备注
    	if ( ans[10].equals("null") && query_str.endsWith(str) && str.length()>0 ){
    		ans[10] = str;
    	}
    	
    	// 修正答案
    	for(int i=0; i<11; i++){
    		if ( null==ans[i] ) ans[i]="null";
    	}

    	// 清洗答案中的重复前缀模式
    	clean_ans(ans);    	
    	
    	return ans;
    	
    }
    
    /**
     * 对答案中的重复前缀模式进行清洗，长度为2~3的重复前缀将被清洗
     * @param ans String[] 答案数组
     * @return true 有信息被清洗; false 没有信息被清洗
     */
    static public boolean clean_ans(String []ans){    
    	boolean flag = false;
    	for ( int i=1; i<=9; i++ ){
    		if ( ans[i].length()>=6 ){
    			if ( ans[i].substring(0,3).equals(ans[i].substring(3,6) ) ) {
    				ans[i] = ans[i].substring(3);
    				flag = true;
    			}
    		}
    		if ( ans[i].length()>=4 ){
    			if ( ans[i].substring(0,2).equals(ans[i].substring(2,4) ) ) {
    				ans[i] = ans[i].substring(2);
    				flag = true;
    			}
    		}
    	}
    	return flag;
    }
    
    /**
     * 简单的测试
     * @param args
     */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
        RegularParser.parse("中区玉函路25-3号");
        RegularParser.parse("西平东骏路38号玫瑰公馆4栋商铺101");
        RegularParser.parse("天河公园 员村四横路口进500米");
        RegularParser.parse("观前街地区（市中心）乔司空巷27-33号观前粤海广场");        
        RegularParser.parse("西塘西塘镇南苑西路翠南新村一期"); 
        RegularParser.parse("兴庆雅居一单元904");
        RegularParser.parse("运河公园");
        RegularParser.parse("向阳路付8号");
        RegularParser.parse("艾湖小区31栋附近");
        RegularParser.parse("观前街地区（市中心）乔司空巷27-33号观前粤海广场");
        RegularParser.parse("山西太原武宿机场新营村东3巷2号");
        RegularParser.parse("花果园购物中心Garland1楼");
        RegularParser.parse("和平街十五区11号楼(地铁13号线光熙门站)");       
        
	}

}
