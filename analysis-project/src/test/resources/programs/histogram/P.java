package programs.histogram;

import mixedProtocolsAnalysis.*;

public class P {
	static final int LEN = 32;

	static final  int NUM_REVIEWERS = 100;
	static final int NUM_RATINGS = 100;

	static final int INTERVALS = 2;
	/* buckets from 0 to 8 */
	static final int NUM_BUCKETS = (INTERVALS * 5) - 1;
	
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
			// quot[j] = 1;
			int newquot = (quot | (1 << j));
			if(rem >= mod) {
				rem = newrem;
				quot = newquot;
			}
		}
		return quot;
	}

	// param: takes an array of int review ratings
	// requires: each review rating is from 1 to 5, i.e., 1 <= reviews[i] <= 5
	// returns: scaled bucket the avg rating falls into:
	// rating of 1: bucket 0, 1.5: bucket 1, 2: 2, 2.5: 3, etc.
	// maps each ratings array to an avg rating bucket
	public static int map(int[] reviews) {
		// NOTE: Got rid of totalReviews, since NUM_RATINGS has the same information
		int sumRatings = 0;
		// float avgReview = 0.0f, absReview, fraction, outValue = 0.0f;
		int absReview, fraction;
		// NOTE: since we don't have -ve numbers, therefore using NUM_RATINGS+1 instead of -1
		// NUM_RATINGS + 1 is as invalid a value for a bucket as -1 :-)
		int bucket = NUM_RATINGS+1;
		for (int i = 0; i < NUM_RATINGS; i++) {
			sumRatings = sumRatings + reviews[i];
		}
		
		int val = sumRatings;
		int mod = NUM_RATINGS;
		// % ------ QUOT START -------------------
		int quot = 0;
		int rem = 0;
		for (int j = LEN - 1; j >= 0; j--) {
			// rem <<= 1
			rem = rem << 1;
			// rem[0] = val[j]
			rem = rem + ((val >> j) & 1); // EMPHASIS HERE
			
			int newrem = rem - mod;
			// quot[j] = 1
			int newquot = (quot | (1 << j));
			if(rem >= mod) {
				rem = newrem;
				quot = newquot;
			}
		}
		// % ------------- QUOT END ---------------
		absReview = quot;
		fraction = rem;

		int m = INTERVALS * (absReview - 1);
		int num = fraction * INTERVALS;
		// TODO: should pre-calculate interval bounds (it would make that loop parallelizable)
		// and thereby use parallel costs. it will also mean that loop wouldn't have MUX
		// so would make sense to convert to arithmetic
		for (int j = 0; j < INTERVALS; j++) {
			int low = j * NUM_RATINGS;
			int high = (j + 1) * NUM_RATINGS;
			int cond1;
			if(low <= num) {
				cond1 = 1;
			}
			else {
				cond1 = 0;
			}
			int cond2;
			if(high > num) {
				cond2 = 1;
			}
			else {
				cond2 = 0;
			}
			int cond = cond1 + cond2;
			
			int newBucket;
			if(cond == 2) {
				newBucket = m + j;
			}
			else {
				newBucket = bucket;
			}
			
			bucket = newBucket;
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

		int[] result = new int[NUM_BUCKETS];
		for (int i = 0; i < NUM_REVIEWERS; i++) {
			int bucket = map(reviews[i]);
			for (int j = 0; j < NUM_BUCKETS; j++) {
				int temp;
				if (j == bucket) {
					temp = result[j] + 1;
				}
				else {
					temp = result[j];
				}
				result[j] = temp;
			}

		}
		
		// OUTPUT
		for(int i = 0; i < NUM_BUCKETS; i++) {
			mpc.OUT(result[i]);
		}

	}
}