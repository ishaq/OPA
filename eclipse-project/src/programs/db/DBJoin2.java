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

public class DBJoin2 {
	public static final int BIT_LEN = 32;
    
    public static final int LEN_A = 30;
    public static final int LEN_B = 30;
    public static final int ATT_A = 2; //Number of attributes
    public static final int ATT_B = 2;

    public static final int LEN = (LEN_A + LEN_B);
    public static final int ATT = (ATT_A + ATT_B - 1); // Number of joined attributes


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

    // agg_variance
    public static int agg_variance(int[] db, int len, int att) {
        //int exp = mean(db, len);
    	// inlined mean
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
        	int dist;
        	if(db[i] > exp) {
        		dist = db[i] - exp;
        	}
        	else {
        		dist = exp - db[i];
        	}
            int sqr = dist * dist;
            
            if(db[i] != 0) {
            	variance[i] = sqr;
            }
            else {
            	variance[i] = 0;
            }
        }


        int sum = 0;
        for(int i = 0; i < len; i++) {
            sum = sum + variance[i];
        }
        int joined = db[len*att];
        //return sum / joined;
        val = sum;
    	mod = joined;
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

    public static int cross_join_trivial(int[] OUTPUT_db, int[] a, int[] b) {
        int id_a = 0;
        int id_b = 0;
        int id_out = 0;
    
        for(int i = 0; i < LEN_A*LEN_B*ATT+1; i++) {
            OUTPUT_db[i] = 0;//-1;
        }
        
        for(int i = 0; i < LEN_A; i++) {
            for(int j = 0; j < LEN_B; j++) {
                int new_val_1;
                int new_val_2;
                int new_val_3;
                int new_id_out;     
                if(a[i*ATT_A] == b[j*ATT_B]) {
                    new_id_out = id_out + 1;
                    new_val_1 = a[i*ATT_A];
                    new_val_2 = a[i*ATT_A+1];
                    new_val_3 = b[j*ATT_B+1];
                }
                else {
                    new_id_out = id_out;
                    new_val_1 = OUTPUT_db[(i*LEN_B+j)*ATT];
                    new_val_2 = OUTPUT_db[(i*LEN_B+j)*ATT+1];
                    new_val_3 = OUTPUT_db[(i*LEN_B+j)*ATT+2];
                }

                id_out = new_id_out;
                OUTPUT_db[(i*LEN_B+j)*ATT] = new_val_1;
                OUTPUT_db[(i*LEN_B+j)*ATT+1] = new_val_2;
                OUTPUT_db[(i*LEN_B+j)*ATT+2] = new_val_3;
            }
        }
        
        return id_out;
    }

    public static int agg_mean(int[] db, int len, int att) {
        int[] sum = new int[len];
        for(int i = 0; i < len; i++) {
            sum[i] = db[i*att+1] + db[i*att+2];
        }
        // return mean_with_abort(sum, len);
        // manually inlined mean_with_abort below
        int mean = 0;
        int numNonZero = 0;
        int flag = 0;
        for(int i = 0; i < len; i++) {
            int newFlag = 0;
            if(sum[i] < 0) {
                newFlag = 1;
            }
            else {
                newFlag = flag;
            }
            flag = newFlag;

            int numConds = flag; // we need both flag and sum[i] >= 0 to be true
            if(sum[i] >= 0) {
                numConds = numConds + 1;
            }

            if(numConds == 2) {
                mean = mean + sum[i];
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
                
        // DB-Join2 start
        int[] db = new int[LEN_A*LEN_B*ATT+1];
        // merge databases
        output_joined = cross_join_trivial(db, inputA_db, inputB_db);
        
        if(output_joined >= LEN_A*LEN_B) { // Limits the last element
                output_joined = LEN_A*LEN_B-1;
        }
        db[LEN_A*LEN_B*ATT] = output_joined;
        output_analysis1 = agg_mean(db, LEN_A*LEN_B, ATT);
        output_analysis2 = output_analysis1;
        //res.analysis2 = variance(db, LEN_A*LEN_B);

        // DB-Join2 end

        mpc.OUT(output_analysis1);
        mpc.OUT(output_analysis2);
        mpc.OUT(output_joined);
    }
}
