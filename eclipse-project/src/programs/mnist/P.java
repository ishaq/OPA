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

public class P {
	
	public static final int IMAGE_WIDTH = 28;
    public static final int WINDOW_WIDTH = 5;
    public static final int STRIDE = 1;
    public static final int OUTPUT_CHANNELS = 16;

    public static final int IMAGE_CROP = (IMAGE_WIDTH - WINDOW_WIDTH + 1); // 28-5+1 = 24;
    public static final int SIZE_CONVOLUTION_1 = (IMAGE_CROP * IMAGE_CROP); //Intermediate size (24^2 = 576;
    public static final int MAX_POOLING_WIDTH_1 = (IMAGE_CROP / 2); //24/2=12;

    public static final int IMAGE_WIDTH_2 = MAX_POOLING_WIDTH_1;
    public static final int MAX_POOLING_SIZE_1 = (OUTPUT_CHANNELS*MAX_POOLING_WIDTH_1 * MAX_POOLING_WIDTH_1); // 16*12*12;
    public static final int IMAGE_CROP_2 = (MAX_POOLING_WIDTH_1-WINDOW_WIDTH +1); // 12-5+1 = 8;
    public static final int SIZE_KERNELS_2 = (WINDOW_WIDTH*WINDOW_WIDTH);  // 5*5 = 25 ;
    public static final int SIZE_ALL_KERNELS_2 = (SIZE_KERNELS_2 * OUTPUT_CHANNELS); // 16 * 25;

    public static final int SIZE_CONVOLUTION_2 = (IMAGE_CROP_2*IMAGE_CROP_2); // 8*8 = 64;
    public static final int SIZE_RELU_2 = OUTPUT_CHANNELS * IMAGE_CROP_2 * IMAGE_CROP_2; // 16 * 64;

    public static final int MAX_POOLING_WIDTH_2 = (IMAGE_CROP_2 / 2); // 8/2 = 4;
    public static final int MAX_POOLING_SIZE_2 = (OUTPUT_CHANNELS * MAX_POOLING_WIDTH_2 * MAX_POOLING_WIDTH_2);

    public static final int FULLY_CONNECTED_WIDTH = 100; // (7, 9);
    public static final int FINAL_OUTPUT_CHANNELS = 10;

	public static void DT_memset(int[] OUTPUT_res, int len, int val) {
		for(int i = 0; i < len; i++) {
			OUTPUT_res[i] = val;
		}
	}
	
//	public static int relu(int val) {
//		if(val>0) {
//			return val;
//		} else {
//			return 0;
//		}
//	}
	
	public static void decomposed_relu(int[]  in, int[] OUTPUT_res, 
			int len_outer, int len_inner) {
//		int copy[len_inner];
//		int im_res[len_inner];
		for(int i = 0; i < len_outer; i++) {
//			memcpy(copy, in+i*len_inner, len_inner*sizeof(DT));
			int offset = i * len_inner;
//			relu_map(in, im_res, len_inner);
//			memcpy(OUTPUT_res + i*len_inner, im_res, len_inner*sizeof(DT));
			for(int j = 0; j < len_inner; j++) {
				int pos = offset+j;
				int val = in[pos];
				if(val > 0) {
					OUTPUT_res[pos] = val; 
				}
				else {
					OUTPUT_res[pos] = 0;
				}
				// have inlined relu above
				//OUTPUT_res[offset + j] = relu(in[offset+j]);
			}
		}
	}
	
	public static void max_pooling_outputs(int[] vals, int[] OUTPUT_res, 
			int outputs, int cols, int rows) {
		int size = cols*rows;
		int rows_res = rows / 2;
		int cols_res = cols / 2;
		int output_size = rows_res * cols_res;
		for(int o = 0; o < outputs; o++) {
			int input_offset = o * size;
//			int size = cols*rows; 
//			DT input_layer[size]; // We copy data, because compiler is unable to slice array efficiently
//			memcpy(input_layer, vals+o*size, size * sizeof(DT));
//			int output_size = cols/2*rows/2;
//			DT res_layer[output_size];
			
			int output_offset = o * output_size;
//			max_pooling(input_layer, res_layer, cols, rows);
//			memcpy(OUTPUT_res+o*output_size, res_layer, output_size * sizeof(DT));
			
//			int rows_res = rows / 2;
//			int cols_res = cols / 2;
			for(int i = 0; i < rows_res; i++) {
				for(int j = 0; j < cols_res; j++) {
					int x = j * 2;
					int y = i * 2;
					int loc1 = input_offset + y * cols + x;
					int loc2 = input_offset + y * cols + x + 1;
					int loc3 = input_offset + (y + 1) * cols + x;
					int loc4 = input_offset + (y + 1) * cols + x + 1;
					int max = vals[loc1];
					int newMax;
					if(vals[loc2] > max) {
						newMax = vals[loc2];
					}
					else {
						newMax = max;
					}
					max = newMax;
					
					if(vals[loc3] > max) {
						newMax = vals[loc3];
					}
					else {
						newMax = max;
					}
					max = newMax;
					
					if(vals[loc4] > max) {
						newMax = vals[loc4];
					} 
					else {
						newMax = max;
					}
					max = newMax;
					
					OUTPUT_res[output_offset + i * cols_res + j] = max;
				}
			}

		}
	}
	
	public static void convolution_naive_outputs(int image_offset, 
			int[] image, int[]  kernels, int[] OUTPUT_layer,
			int image_width, int window_size, int output_size, 
			int stride, int conv_width) {	
		//unsigned res[conv_width*conv_width*];
		//DT_memset(OUTPUT_layer, conv_width*conv_width*output_size, 0);
		int kernel_size = window_size*window_size;
		for(int o = 0; o < output_size; o++) {
//			DT kernel[kernel_size];
//			DT res[conv_width*conv_width];
//			memcpy(kernel, kernels+ o*kernel_size, kernel_size * sizeof(DT));
			int kernels_offset = o * kernel_size;
			int OUTPUT_layer_offset = o * (conv_width * conv_width);
			
//			convolution_naive(image, kernel, res, image_width, window_size, stride, conv_width);
//			memcpy(OUTPUT_layer + o*(conv_width*conv_width), res, conv_width*conv_width * sizeof(DT));
			
			// int window_unrolled = window_size * window_size;
			// Need to assign each input pixel to the convolution matrix
			int x, y, wx, wy;
			for(y = 0; y < conv_width; y++) { // Inner position in the image
				for(x = 0; x < conv_width; x++) {
					int oPos = x+y*conv_width;
					int tmp = 0;
					for(wy = 0; wy < window_size; wy++) {
						for(wx = 0; wx < window_size; wx++) {
							int convPos = wx+wy*window_size;
							int kernelPos = kernels_offset + convPos;
							tmp += kernels[kernels_offset] * image[image_offset + (y*stride + wy) * image_width + (x*stride + wx)];
						}				
					}
					OUTPUT_layer[OUTPUT_layer_offset + oPos] = tmp;
				}
			}

		}
	}
	
	public static void mmulT_unrolled(int[]  a, int[]  b, int[] OUTPUT_res, 
			int cols_a, int cols_b, int common) {
		for(int i = 0; i < cols_a; i++) {
//			int[] aRow = new int[common];
//			memcpy(aRow, a+i*common, common*sizeof(DT));
			int a_offset = i*common;
			for(int j = 0; j < cols_b; j++) {
//				DT bRow[common];
//				memcpy(bRow, b+j*common, common*sizeof(DT));
				int b_offset = j*common;
//				OUTPUT_res[i*cols_b+j] = mmulT_unrolled_inner(aRow, bRow, common);
				
				int[] mults = new int[common];
				for(int k = 0; k < common; k++) {
					mults[k] = a[a_offset + k] * b[b_offset + k];
				}
				
				int sum = mults[0];
				for(int k = 1; k < common; k++) {
					sum += mults[k];
				}
				
				OUTPUT_res[i*cols_b+j] = sum;
			}
		}
	}



    public static void sum(int[] OUTPUT_agg, int[] agg, int[] add, int len) {
        for(int i = 0; i < len; i++) {
            OUTPUT_agg[i] = agg[i] + add[i];
        }
    }

    public static void main(String[] args) {
        //int inputA_image[IMAGE_WIDTH * IMAGE_WIDTH];
        int[] inputA_image = new int[IMAGE_WIDTH * IMAGE_WIDTH];
        //int inputB_kernelsL1[OUTPUT_CHANNELS * WINDOW_WIDTH * WINDOW_WIDTH]; // (1)
        int[] inputB_kernelsL1 = new int[OUTPUT_CHANNELS * WINDOW_WIDTH * WINDOW_WIDTH]; // (1)
        //int inputB_kernelsL2[OUTPUT_CHANNELS * SIZE_KERNELS_2]; // (16 * 
        int[] inputB_kernelsL2 = new int[OUTPUT_CHANNELS * SIZE_KERNELS_2]; // (16 * 
        //int inputB_kernelsFC1[FULLY_CONNECTED_WIDTH * MAX_POOLING_SIZE_2]; // (16 * 4 * 4) * 100 = 256 * 100
        int[] inputB_kernelsFC1 = new int[FULLY_CONNECTED_WIDTH * MAX_POOLING_SIZE_2]; // (16 * 4 * 4) * 100 = 256 * 100
        //int inputB_kernelsFC2[FINAL_OUTPUT_CHANNELS * FULLY_CONNECTED_WIDTH]; // 100 * 10
        int[] inputB_kernelsFC2 = new int[FINAL_OUTPUT_CHANNELS * FULLY_CONNECTED_WIDTH]; // 100 * 10
        //int output_final_layer[FINAL_OUTPUT_CHANNELS];
        int[] output_final_layer = new int[FINAL_OUTPUT_CHANNELS];
        
        MPCAnnotation mpc = MPCAnnotationImpl.v();
        for(int i = 0; i < IMAGE_WIDTH * IMAGE_WIDTH; i++) {
        	inputA_image[i] = mpc.IN();
        }
        for(int i = 0; i < OUTPUT_CHANNELS * WINDOW_WIDTH * WINDOW_WIDTH; i++) {
        	inputB_kernelsL1[i] = mpc.IN();
        }
        for(int i = 0; i < OUTPUT_CHANNELS * SIZE_KERNELS_2; i++) {
        	inputB_kernelsL2[i] = mpc.IN();
        }
        for(int i = 0; i < FULLY_CONNECTED_WIDTH * MAX_POOLING_SIZE_2; i++) {
        	inputB_kernelsFC1[i] = mpc.IN();
        }
        for(int i = 0; i < FINAL_OUTPUT_CHANNELS * FULLY_CONNECTED_WIDTH; i++) {
        	inputB_kernelsFC2[i] = mpc.IN();
        }


        int[] convolution_layer = new int[OUTPUT_CHANNELS * SIZE_CONVOLUTION_1];
        int[] convolution_relu = new int[OUTPUT_CHANNELS * SIZE_CONVOLUTION_1];

        // Convolution (1)
        //, DT* kernels, DT* OUTPUT_layer, unsigned image_width, unsigned window_size, 
        // unsigned output_size, unsigned stride, unsigned conv_width) {    
        convolution_naive_outputs(0, inputA_image, inputB_kernelsL1, convolution_layer, IMAGE_WIDTH, 
            WINDOW_WIDTH, OUTPUT_CHANNELS, STRIDE, IMAGE_CROP);
        
        // Relu (2)
        //for(unsigned i = 0; i < OUTPUT_CHANNELS * SIZE_CONVOLUTION_1; i++) {
        decomposed_relu(convolution_layer, convolution_relu, OUTPUT_CHANNELS, SIZE_CONVOLUTION_1);

        // Max pooling (3)
        int[] pooling_layer = new int[MAX_POOLING_SIZE_1]; // Size is 16 * 12 *12
        max_pooling_outputs(convolution_relu, pooling_layer, OUTPUT_CHANNELS, 
        		IMAGE_CROP, IMAGE_CROP);
        
        int[] convolution_layer_2 = new int[OUTPUT_CHANNELS * SIZE_CONVOLUTION_2]; // 16 * (8*8)
        int[] convolution_relu_2 = new int[OUTPUT_CHANNELS * SIZE_CONVOLUTION_2]; // 16 * (8*8)
        DT_memset(convolution_layer_2, OUTPUT_CHANNELS * SIZE_CONVOLUTION_2, 0);
        for(int o = 0; o < OUTPUT_CHANNELS; o++) { // Accumulate convolutions
            int[] convolution_layer_tmp = new int[OUTPUT_CHANNELS * SIZE_CONVOLUTION_2]; // 16 * (8*8)
            int[] convolution_layer_tmp_2 = new int[OUTPUT_CHANNELS * SIZE_CONVOLUTION_2]; // 16 * (8*8)
            //int[] image = new int[IMAGE_WIDTH_2*IMAGE_WIDTH_2]; // 12*12=144
            //int[] kernels = new int[SIZE_ALL_KERNELS_2];
            //memcpy(kernels, INPUT_B.kernelsL2, SIZE_ALL_KERNELS_2*sizeof(DT));
            //memcpy(image, pooling_layer+o*IMAGE_WIDTH_2*IMAGE_WIDTH_2, IMAGE_WIDTH_2*IMAGE_WIDTH_2*sizeof(DT));
            int image_start_offset = o*IMAGE_WIDTH_2*IMAGE_WIDTH_2;
            convolution_naive_outputs(image_start_offset, inputA_image, inputB_kernelsL2, 
            		convolution_layer_tmp, IMAGE_WIDTH_2, WINDOW_WIDTH, 
            		OUTPUT_CHANNELS, STRIDE, IMAGE_CROP_2);
            sum(convolution_layer_2, convolution_layer_2, convolution_layer_tmp,
            		OUTPUT_CHANNELS * SIZE_CONVOLUTION_2);
            //memcpy(convolution_layer_2, convolution_layer_tmp_2, OUTPUT_CHANNELS * SIZE_CONVOLUTION_2);
        }
        
        decomposed_relu(convolution_layer_2, convolution_relu_2, OUTPUT_CHANNELS, SIZE_CONVOLUTION_2);
        
        
        // Max pooling (6)
        int[] pooling_layer_2 = new int[MAX_POOLING_SIZE_2]; // Size is 16 * 4 * 4
        max_pooling_outputs(convolution_relu_2, pooling_layer_2, 
        		OUTPUT_CHANNELS, IMAGE_CROP_2, IMAGE_CROP_2);  
        
        // FC (7)
        int[] fc_layer = new int[FULLY_CONNECTED_WIDTH];
        //DT_memset(pooling_layer_2, MAX_POOLING_SIZE_2, 2);
        mmulT_unrolled(inputB_kernelsFC1, pooling_layer_2, fc_layer,
        		FULLY_CONNECTED_WIDTH, 1, MAX_POOLING_SIZE_2);
        
        // RELU (8)
        int[] fc_relu = new int[FULLY_CONNECTED_WIDTH];
        decomposed_relu(fc_layer, fc_relu, FULLY_CONNECTED_WIDTH, 1);
        
        // Temporary output
        //  memcpy(output.final_layer, pooling_layer_2, FINAL_OUTPUT_CHANNELS*sizeof(DT));

        mmulT_unrolled(inputB_kernelsFC2, fc_layer, output_final_layer, 
        		FINAL_OUTPUT_CHANNELS, 1, FULLY_CONNECTED_WIDTH);
        
        
        for(int i = 0; i < FINAL_OUTPUT_CHANNELS; i++) {
        	mpc.OUT(output_final_layer[i]);
        }
    }
}