package Structure;

import java.io.File;
import java.util.Random;
import java.util.Scanner;


public class EventVal {

	public class Val{
		public int attributeId;
		public double val;
		
		public Val() {
			attributeId = 0;
			val = 0;
		}
	}
	
	public int StockId;
	public int AttributeNum;
	public long EventProduceTime;
	public long EventArriveTime;
	public long EventMatchTime;
	public long EventStartSendTime;
	public long EventGetTime;
	public Val[] eventVals;
	
	public EventVal(File file) {
		//从文件读取消息
	}
	public EventVal(int attributeNum, int Id) {
		// 从控制台输入消息
		EventProduceTime = 0;
		EventArriveTime = 0;
		EventMatchTime = 0;
		EventStartSendTime = 0;
		EventGetTime = 0;

		this.StockId = Id;
		this.AttributeNum = attributeNum;
		
		//Scanner input = new Scanner(System.in);
		Random r = new Random();
		this.eventVals = new Val[attributeNum];
		
		for(int i = 0; i < attributeNum; i++) {
			//System.out.println("请输入第 " + i + " 组属性");
			this.eventVals[i] = new Val();
			this.eventVals[i].attributeId = i;
			this.eventVals[i].val = r.nextDouble() * 999 + 0.9;
		}

	}
}
