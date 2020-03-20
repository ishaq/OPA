interface MPCAnnotation {
    // is used to mark output variables
    public void OUT(int x);
    
    // used to mark input variables (input vars should be assigned the return value)
    // it's a convenient method to shutup the compiler when it complains that variables 
    // are not initialized. it also helps in recognizing  which variables are 
    // input variables when one is looking at shimple code.
    public int IN();
}
class MPCAnnotationImpl implements MPCAnnotation {
    private static MPCAnnotation v = null;

    private MPCAnnotationImpl() {
    }

    public void OUT(int x) {
    }
    
    public static MPCAnnotation v() {
        if (v == null) {
            v = new MPCAnnotationImpl();
        }
        return v;
    }
    
    public int IN() {
        return 57; // Grothendieck Prime
    }
}


public class DBMerge {
    
	public static final int BIT_LEN = 32;
	
    public static final int LEN_A = 100;
    public static final int LEN_B = 100;
    public static final int ATT_A = 1; //Number of attributes
    public static final int ATT_B = 1;

    public static final int LEN = (LEN_A + LEN_B);
    public static final int ATT = (ATT_A + ATT_B - 1); // Number of joined attributes

	/* returns val/mod, integer division */
	public static int quot(int val, int mod) {
		int quot = 0;
		int rem = 0;
		for (int j = BIT_LEN - 1; j >= 0; j--) {
			// rem <<= 1
			rem = rem << 1;
			// rem[0] = val[j]
			rem = rem + ((val >> j) & 1); // EMPHASIS HERE
			
			int newrem = rem - mod;
			// quot[j] = 1;
			int newquot = (quot | (1 << j));
			if(rem >= mod) {
				rem = newrem;
				quot = newquot;
			}
		}
		return quot;
	}

    public static int sum_tree(int[] data, int len) {
        int sum = 0;
        for(int i = 0; i < len; i++) {
            sum = sum + data[i];
        }
        return sum;
    }

    public static int sum_gt_zero(int[] data, int len) {
        int[] flags = new int[len];
        for(int i = 0; i < len; i++) {
            if(data[i] > 0) {
                flags[i] = 1;
            }
            else {
                flags[i] = 0;
            }
        }

        int sum = 0;
        for(int i = 0; i < len; i ++) {
            sum = sum + flags[i];
        }
        return sum;
    }

    public static int mean_with_abort(int[] db, int len) {
        int mean = 0;
        int numNonZero = 0;
        int flag = 0;
        for(int i = 0; i < len; i++) {
            int newFlag = 0;
            if(db[i] < 0) {
                newFlag = 1;
            }
            else {
                newFlag = flag;
            }
            flag = newFlag;

            int numConds = flag; // we need both flag and [db] >= 0 to be true
            if(db[i] >= 0) {
                numConds = numConds + 1;
            }

            if(numConds == 2) {
                mean = mean + db[i];
            }
        }
        // int retVal = mean/numNonZero;
        int val = mean;
    	int mod = numNonZero;
    	// manually inlined quot start
		int quot = 0;
		int rem = 0;
		for (int j = BIT_LEN - 1; j >= 0; j--) {
			// rem <<= 1
			rem = rem << 1;
			// rem[0] = val[j]
			rem = rem + ((val >> j) & 1); // EMPHASIS HERE
			
			int newrem = rem - mod;
			// quot[j] = 1;
			int newquot = (quot | (1 << j));
			if(rem >= mod) {
				rem = newrem;
				quot = newquot;
			}
		}
		int retVal = quot;
		// manually inlined quot end

        if(numNonZero > 0) {
            // return mean / numNonZero;
    		return retVal;
        }
        else {
            return 0;
        }

    }

    public static int mean(int[] db, int len) {
        int mean = 0;
        for(int i = 0; i < len; i++) {
            mean = mean + db[i];
        }
        //return mean / len;
        int val = mean;
    	int mod = len;
    	// manually inlined quot start
		int quot = 0;
		int rem = 0;
		for (int j = BIT_LEN - 1; j >= 0; j--) {
			// rem <<= 1
			rem = rem << 1;
			// rem[0] = val[j]
			rem = rem + ((val >> j) & 1); // EMPHASIS HERE
			
			int newrem = rem - mod;
			// quot[j] = 1;
			int newquot = (quot | (1 << j));
			if(rem >= mod) {
				rem = newrem;
				quot = newquot;
			}
		}
		int retVal = quot;
		// manually inlined quot end
		return retVal;
    }

    public static int variance(int[] db, int len) {
        //int exp = mean(db, len);
        int exp = 0;
        for(int i = 0; i < len; i++) {
            exp = exp + db[i];
        }
        //exp = exp / len;
        int val = exp;
    	int mod = len;
    	// manually inlined quot start
		int quot = 0;
		int rem = 0;
		for (int j = BIT_LEN - 1; j >= 0; j--) {
			// rem <<= 1
			rem = rem << 1;
			// rem[0] = val[j]
			rem = rem + ((val >> j) & 1); // EMPHASIS HERE
			
			int newrem = rem - mod;
			// quot[j] = 1;
			int newquot = (quot | (1 << j));
			if(rem >= mod) {
				rem = newrem;
				quot = newquot;
			}
		}
		int retVal = quot;
		// manually inlined quot end
		exp = retVal;

        int[] variance = new int[len];
        for(int i = 0; i < len; i++) {
            int dist = db[i] - exp;
            variance[i] = dist * dist;
        }


        int sum = 0;
        for(int i = 0; i < len; i++) {
            sum = sum + variance[i];
        }

        //return sum / len;
        val = sum;
    	mod = len;
    	// manually inlined quot start
		quot = 0;
		rem = 0;
		for (int j = BIT_LEN - 1; j >= 0; j--) {
			// rem <<= 1
			rem = rem << 1;
			// rem[0] = val[j]
			rem = rem + ((val >> j) & 1); // EMPHASIS HERE
			
			int newrem = rem - mod;
			// quot[j] = 1;
			int newquot = (quot | (1 << j));
			if(rem >= mod) {
				rem = newrem;
				quot = newquot;
			}
		}
		retVal = quot;
		// manually inlined quot end
		return retVal;

    }
    

    public static void main(String[] args) {
        int[] inputA_db = new int[LEN_A * ATT_A];
        int[] inputB_db = new int[LEN_B * ATT_B];
        int output_analysis1;
        int output_analysis2;
        int output_joined;

        MPCAnnotation mpc = MPCAnnotationImpl.v();
        for(int i = 0; i < LEN_A * ATT_A; i++) {
            inputA_db[i] = mpc.IN();
        }
        for(int i = 0; i < LEN_B * ATT_B; i++) {
            inputB_db[i] = mpc.IN();
        }
                
        // DB-Merge start
        int[] db = new int[LEN];
        for(int i = 0; i < LEN_A; i++) {
            db[i] = inputA_db[i];
        }
        for(int i = 0; i < LEN_B; i++) {
            db[i+LEN_A] = inputB_db[i];
        }

        output_joined = LEN;
        output_analysis1 = mean(db, LEN);
        output_analysis2 = variance(db, LEN);
        // DB-Merge end

        mpc.OUT(output_analysis1);
        mpc.OUT(output_analysis2);
        mpc.OUT(output_joined);
    }
}
