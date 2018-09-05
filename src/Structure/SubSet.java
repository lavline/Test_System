package Structure;

import java.util.ArrayList;

public class SubSet {
    public class Val {
        public boolean state;
        public int attribute_num;
        public int matched_num;
        public String SubId;

        public Val(int n, String s) {
            this.state = true;
            this.matched_num = 0;
            this.attribute_num = n;
            this.SubId = s;
        }
    }

    public ArrayList<Val> subset;
    public SubSet(){
        subset = new ArrayList<>();
    }
    public void add(int n, String s){
        subset.add(new Val(n,s));
    }
}