interface Multiplex {
	public int MUX(int a, int b, boolean cond);
}

class MultiplexImpl implements Multiplex {
	private static Multiplex v = null;

	private MultiplexImpl() {

	}

	public int MUX(int a, int b, boolean cond) {
		return (cond ? a : b);
	}

	public static Multiplex v() {
		if (v == null) {
			v = new MultiplexImpl();
		}
		return v;
	}
}

public class P {
	static final int LEN = 32;

	static final  int NUM_REVIEWERS = 100;
	static final int NUM_RATINGS = 100;

	static final int INTERVALS = 2;
	
	public static void init(int[][] reviews) {
		// initialize reviews
	}
	
	/* returns val/mod, integer division */
	public static int quot(int val, int mod) {
		int quot = 0;
		int rem = 0;
		for (int j = LEN - 1; j >= 0; j--) {
			// rem <<= 1
			rem = rem << 1;
			// rem[0] = val[j]
			rem = rem + ((val >> j) & 1); // EMPHASIS HERE
			boolean flag = (rem >= mod);
			int newrem = rem - mod;
			int newquot = quot + 1;
			rem = MultiplexImpl.v().MUX(newrem, rem, flag);
			quot = MultiplexImpl.v().MUX(newquot, quot, flag);
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
			boolean lowerBoundFlag = (low <= num);
			boolean upperBoundFlag = (high > num);
			boolean betweenBoundsFlag = lowerBoundFlag && upperBoundFlag;
//			if (low <= num && num < high) {
//				bucket = m + j;
//			}
			bucket = MultiplexImpl.v().MUX(j + m, bucket, betweenBoundsFlag);
		}

		return bucket;
	}

	public static void main(String[] args) {

		/* 100 reviwers each giving 100 ratings */
		int[][] reviews = new int[NUM_REVIEWERS][NUM_RATINGS];

		init(reviews);

		/* buckets from 0 to 8 */
		int size = INTERVALS * 5 - 1;
		int[] result = new int[size];

		for (int i = 0; i < NUM_REVIEWERS; i++) {
			int bucket = map(reviews[i]);
			for (int j = 0; j < size; j++) {
//				if (j == bucket) {
//					result[j] = result[j] + 1;
//				}
				result[j] = MultiplexImpl.v().MUX(result[j] + 1, result[j], (j == bucket));
			}

		}

	}
}