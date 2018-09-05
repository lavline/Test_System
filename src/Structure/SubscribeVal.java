package Structure;

import java.io.File;
import java.util.Random;
import java.util.Scanner;

public class SubscribeVal {

	public class Val{
		public int attributeId;
		public double max_val;
		public double min_val;

		public Val() {
			attributeId = 0;
			max_val = 0;
			min_val = 0;
		}
	}
	
	public String SubId;
	public int StockId;
	public int AttributeNum;
	public Val[] subVals;
	
	public SubscribeVal(File file) {
		//从文件读取消息
	}
	public SubscribeVal(int attributeNum, String Id, int stockId) {
		// 从控制台输入消息
		this.StockId = stockId;
		this.SubId = Id;
		this.AttributeNum = attributeNum;
		
		//Scanner input = new Scanner(System.in);
		
		this.subVals = new Val[attributeNum];
		Random r = new Random();
		for(int i = 0; i < attributeNum; i++) {
			//System.out.println("Inpute the " + i + "th Attribute");
			this.subVals[i] = new Val();
			this.subVals[i].attributeId = i;
			this.subVals[i].min_val = r.nextDouble() * 1000;
			double tmp = r.nextDouble() * 1000;
			if(tmp >= this.subVals[i].min_val)
				this.subVals[i].max_val = tmp;
			else{
				this.subVals[i].max_val = this.subVals[i].min_val;
				this.subVals[i].min_val = tmp;
			}
		}

	}
	public SubscribeVal(int attributeNum, String Id, int stockId, int type) {
		// 从控制台输入消息
		this.StockId = stockId;
		this.SubId = Id;
		this.AttributeNum = attributeNum;

		//Scanner input = new Scanner(System.in);

		this.subVals = new Val[attributeNum];
		Random r = new Random();
		for(int i = 0; i < attributeNum; i++) {
			//System.out.println("Inpute the " + i + "th Attribute");
			this.subVals[i] = new Val();
			this.subVals[i].attributeId = i;
			this.subVals[i].min_val = 0.1;
			this.subVals[i].max_val = 999.9;

		}

	}
}
