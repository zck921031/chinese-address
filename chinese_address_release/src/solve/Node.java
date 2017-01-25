package solve;

import java.util.ArrayList;
import java.util.List;

/**
 * 地址树的节点类
 * @author zck
 */
public class Node {
	List<Node> ch; // children
	Node father;
	long zipcode;
	int level;
	String name;	
	long tick = -1; //tick is a timestamp used for faster searching.
	int count = 0;

	/**
	 * 构造函数
	 * @param _zipcode long 行政区划的id
	 * @param _name String 行政区划的名字
	 * @param _level int 行政区划的level
	 * @param _father Node 该行政区划的父亲节点
	 */
	public Node(long _zipcode, String _name, int _level, Node _father) {
		zipcode = _zipcode;
		name = _name;
		level = _level;
		father = _father;
		ch = new ArrayList<Node>();
		if ( father!=null ) father.ch.add(this);
	}

	/**
	 * 给地址树上的这个节点增加分数
	 * @param value 
	 * @param _tick 时间戳
	 */
	public void addcount(int value, long _tick) {
		resume_tick(_tick);
		count += value;
	}

	/**
	 * 计算该节点到树根的分数总和
	 * @param _tick 时间戳
	 * @return int 分数
	 */
	public int score(long _tick) {
		resume_tick(_tick);
		if (null == father)
			return 0;
		return count + father.score(_tick);
	}
	
	/**
	 * 输出该节点到树根的路径
	 * @return String 格式化的路径
	 */
	public String path() {
		if (null == father)
			return name;
		return father.path() + "->" + name;
	}

	public String toStr(String prefix) {
		return prefix + String.valueOf(zipcode) + ";" + name + ";" + String.valueOf(level) + ";";
	}

	public void printallson() {
		System.out.println(toStr(""));
		for (Node son : ch) {
			System.out.println(son.toStr(".."));
		}
	}

	public void print(String prefix) {
		System.out.println(toStr(prefix));
		for (Node son : ch) {
			son.print(prefix + "..");
		}
	}

	// For query
	void resume_tick(long _tick) {
		if (tick != _tick) {
			count = 0;
			tick = _tick;
		}
	}
}
