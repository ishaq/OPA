interface MPCAnnotation {
	// is used to mark output variables
	public void OUT(int x);
	
	// used to mark input variables (input vars should be assigned the return value)
	// it's a convenient method to shutup the compiler when it complains that variables 
	// are not initialized
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

public class P {
	static final int LEN = 32;

	static final  int NUM_REVIEWERS = 100;
	static final int NUM_RATINGS = 100;

	static final int INTERVALS = 2;
	
	/* returns val/mod, integer division */
	public static int quot(int val, int mod) {
		int quot = 0;
		int rem = 0;
		for (int j = LEN - 1; j >= 0; j--) {
			// rem <<= 1
			rem = rem << 1;
			// rem[0] = val[j]
			rem = rem + ((val >> j) & 1); // EMPHASIS HERE
			
			int newrem = rem - mod;
			int newquot = quot + 1;
			if(rem >= mod) {
				rem = newrem;
				quot = newquot;
			}
//			boolean flag = (rem >= mod);
//			rem = MultiplexImpl.v().MUX(newrem, rem, flag);
//			quot = MultiplexImpl.v().MUX(newquot, quot, flag);
		}
		return quot;
	}

	// param: takes an array of int review ratings
	// requires: each review rating is from 1 to 5, i.e., 1 <= reviews[i] <= 5
	// returns: scaled bucket the avg rating falls into:
	// rating of 1: bucket 0, 1.5: bucket 1, 2: 2, 2.5: 3, etc.
	// maps each ratings array to an avg rating bucket
	public static int map(int[] reviews) {
		int rating;
		// NOTE: Got rid of totalReviews, since NUM_RATINGS has the same information
		int sumRatings = 0;
		// float avgReview = 0.0f, absReview, fraction, outValue = 0.0f;
		int absReview, fraction;
		// NOTE: since we don't have -ve numbers, therefore using NUM_RATINGS+1 instead of -1
		// NUM_RATINGS + 1 is as invalid a value for a bucket as -1 :-)
		int bucket = NUM_RATINGS+1;
		for (int i = 0; i < NUM_RATINGS; i++) {
			rating = reviews[i];
			sumRatings += rating;
		}

		absReview = quot(sumRatings, NUM_RATINGS);
		fraction = sumRatings % NUM_RATINGS;

		int m = INTERVALS * (absReview - 1);
		int num = fraction * INTERVALS;
		for (int j = 0; j < INTERVALS; j++) {
			int low = j * sumRatings;
			int high = (j + 1) * sumRatings;
//			if (low <= num && num < high) {
//			bucket = m + j;
//		}
			boolean lowerBoundFlag = (low <= num);
			boolean upperBoundFlag = (high > num);
			if((lowerBoundFlag && upperBoundFlag) == true) {
				bucket = m + j;
			}

//			bucket = MultiplexImpl.v().MUX(j + m, bucket, betweenBoundsFlag);
		}

		return bucket;
	}

	public static void main(String[] args) {
		
		MPCAnnotation mpc = MPCAnnotationImpl.v();

		/* 100 reviewers each giving 100 ratings */
		int[][] reviews = new int[NUM_REVIEWERS][NUM_RATINGS];

		// INPUT
		for(int i = 0; i < NUM_REVIEWERS; i++) {
			for(int j = 0; j < NUM_RATINGS; i++) {
				reviews[i][j] = mpc.IN();
			}
		}

		/* buckets from 0 to 8 */
		int size = INTERVALS * 5 - 1;
		int[] result = new int[size];

		for (int i = 0; i < NUM_REVIEWERS; i++) {
			int bucket = map(reviews[i]);
			for (int j = 0; j < size; j++) {
				int newResultJ = result[j] + 1;
				if (j == bucket) {
					result[j] = newResultJ;
				}
//				result[j] = MultiplexImpl.v().MUX(result[j] + 1, result[j], (j == bucket));
			}

		}
		
		// OUTPUT
		for(int i = 0; i < size; i++) {
			mpc.OUT(result[i]);
		}

	}
}