package solve;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * 用于识别前4级中文地名
 * <p>
 * first: init_from_txt("data/three_level_modified.txt");
 * </p>
 * <p>
 * second: search("苏州市吴中区林泉街翰林缘小区9栋", -4);
 * </p>
 * @author zck
 */
public class CityMatch {	
	Node root = new Node(0, "中国", 0, null);;
	Map<String, List<Node>> prefix_table = new HashMap<String, List<Node>>();
	Map<String, List<Node>> exact_table = new HashMap<String, List<Node>>();
	Map<String, HashSet<Long>> level4_table = new HashMap<String, HashSet<Long>>();
	Map<String, HashSet<String>> level4_prefix = new HashMap<String, HashSet<String>>();
	long _tick=0;
	/**
	 * 利用three_level_modified.txt和level4.txt生成地名的 前缀hash和地址树。
	 * 
	 * @param filename
	 *            records must be sorted by key first!
	 * @return 0 for success
	 * @author zck
	 */
	public int init_from_txt(String filename) {
		prefix_table = new HashMap<String, List<Node>>();
		exact_table = new HashMap<String, List<Node>>();
		level4_table = new HashMap<String, HashSet<Long>>();
		level4_prefix = new HashMap<String, HashSet<String>>();
		// add a Root(China)
		List<Node> buffer = new ArrayList<Node>(1024);
		for (int i = 0; i < 1024; i++)
			buffer.add(root);
		/*
		 * 以下代码修正一些不一致的地名表达，并建立前3级地址树。
		 */
		boolean show_reject = false;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] strs = line.split("[\\s+,'　']");
				if (strs.length <= 1)
					continue;
				int zipcode = Integer.parseInt(strs[0]);
				String name = strs[strs.length - 1];
				int level = (zipcode % 10000 == 0 ? 1 : (zipcode % 100 == 0 ? 2 : 3));

				// rule1 fixed "市辖区"
				switch (zipcode) {
				case 110100:
					name = "北京市辖区";
					break;
				case 120100:
					name = "天津市辖区";
					break;
				case 310100:
					name = "上海市辖区";
					break;
				case 500100:
					name = "重庆市辖区";
					break;
				}
				if (name.equals("市辖区")) {
					if (zipcode % 100 == 0) {
						System.out.println("unfixed: " + line);
					} else {
						if (show_reject)
							System.out.println("reject: " + line);
						continue;
					}
				}
				// rule2 reject "县"
				if (name.equals("县")) {
					if (show_reject)
						System.out.println("reject: " + line);
					continue;
				}
				// Build a tree
				Node u = new Node(zipcode, name, level, buffer.get(level - 1));
				buffer.set(level, u);

				// build hashtabel as a trie tree
				{
					if (!exact_table.containsKey(name)) {
						exact_table.put(name, new ArrayList<Node>());
					}
					List<Node> p = exact_table.get(name);
					p.add(u);
				}
				for (int i = 2; i <= name.length(); i++) {
					String str = name.substring(0, i);
					if (!prefix_table.containsKey(str)) {
						prefix_table.put(str, new ArrayList<Node>());
					}
					List<Node> p = prefix_table.get(str);
					p.add(u);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					return 1;
				}
			}
		}

		/*
		 *  Read level4
		 *  建立4级地名的Hash Table。
		 */
		br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream("data/level4.txt"), "UTF-8"));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] strs = line.split(",");
				if (strs.length == 2) {
					if (strs[1].length() < 3)
						continue;
					if (!level4_table.containsKey(strs[1])) {
						level4_table.put(strs[1], new HashSet<Long>());
					}
					HashSet<Long> hs = level4_table.get(strs[1]);
					hs.add(Long.parseLong(strs[0]) / 100000000l);
					// Create level4_prefix
					for (int i = 2; i <= strs[1].length(); i++) {
						if (level4_prefix.containsKey(strs[1].substring(0, i)) == false) {
							level4_prefix.put(strs[1].substring(0, i), new HashSet<String>());
						}
						HashSet<String> htemp = level4_prefix.get(strs[1].substring(0, i));
						htemp.add(strs[1]);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return 1;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return 1;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
					return 1;
				}
			}
		}
		return 0;
	}
	
	/**
	 * 识别中文地址的前4级，并得到剩余的字符串后缀。
	 * @param query_str 待查询的地址记录
	 * @param tick 唯一时间戳，要求和上一次查询给定的tick不一致，否则出错。
	 * @return String[] 11维数组，第0维是剩余未识别的字符串，第1~10维是1~10级地址。
	 * @author zck
	 */
	private String[] search(String query_str, long tick) {
		int idx = 0;
		Map<Node, Integer> active = new HashMap<Node, Integer>();
		// Greedy match
		int base_score = 1 << 25;
		int Level = 0;
		while (idx < query_str.length()) {
			int k = idx + 1;
			while (k + 1 <= query_str.length() && prefix_table.containsKey(query_str.substring(idx, k + 1))) {
				k++;
			}
			// No match
			if (idx + 1 == k) {
				idx = k;
			}
			// Query_str[idx,k) matched
			else {
				List<Node> p;
				if (exact_table.containsKey(query_str.substring(idx, k))) {
					p = exact_table.get(query_str.substring(idx, k));
				} else {
					p = prefix_table.get(query_str.substring(idx, k));
				}
				int level = 12;
				for (Node t : p) {
					if (k - idx < (t.name.length() + 1) / 2)
						continue;
					if (t.level > Level)
						level = Math.min(level, t.level);
				}
				if (level < 12) {
					for (Node t : p) {
						if (k - idx < (t.name.length() + 1) / 2)
							continue;
						if (t.level != level)
							continue;
						t.addcount(base_score, tick);
						active.put(t, k);
					}
					// System.out.println( query_str.substring(idx, k)+" "+
					// level );
					base_score >>= 1;
					Level = level;
				}
				idx = k;
			}
		}

		int max_score = -1;
		String ans = "";
		int ans_k = 0;
		long level3_zipcode = -1;
		for (Map.Entry<Node, Integer> entry : active.entrySet()) {
			Node p = entry.getKey();
			int temp = p.score(tick);
			// entry.setValue( temp );
			if (max_score < temp) {
				max_score = temp;
				ans = p.path();
				ans_k = entry.getValue();
				level3_zipcode = p.zipcode;
			}
		}
		String[] strs = ans.split("->");
		String[] ret = new String[12];
		for (int i = 0; i < 11; i++) {
			if (i < strs.length)
				ret[i] = strs[i];
			else
				ret[i] = "null";
		}

		// Check level4
		{
			int k = -1;
			String level4_str = "null";
			for (int i = 1; i + ans_k <= query_str.length(); i++) {
				String[] qs = { query_str.substring(ans_k, ans_k + i) };
				for (String s : qs) {
					if (level4_table.containsKey(s) && level4_table.get(s).contains(level3_zipcode / 100)) {
						k = i;
						level4_str = s;
					}
				}
			}
			if (-1 != k) {
				ret[4] = level4_str;
				ans_k += k;
			}
		}

		ret[0] = query_str.substring(ans_k);
		return ret;
	}

	/**
	 * 识别中文地址的前4级，并得到剩余的字符串后缀。
	 * @param query_str 待查询的地址记录
	 * @return String[] 11维数组，第0维是剩余未识别的字符串，第1~10维是1~10级地址。
	 * @author zck
	 */
	public String[] search(String query_str) {		
		_tick = _tick+1;
		return search(query_str, _tick);
	}
	
	/**
	 * 简单测试
	 * @param args
	 * @throws IOException
	 * @author zck
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		CityMatch city = new CityMatch();
		city.init_from_txt("data/three_level_modified.txt");

		city.search("青岛市", 0);
		city.search("苏州市吴中区林泉街翰林缘小区9栋", -1);
		city.search("苏州姐吴中姨林泉街翰林缘小区9栋", -2);
		city.search("广州市增城区增城凤凰城内", -3);
		city.search("卧槽会出bug吗", -4);
		city.search("辽宁营口营口鲅鱼圈区红运商业街东邻", -3);
		city.search("山东蓝岛市", -4);
	}

}
