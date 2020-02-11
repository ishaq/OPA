package programs.cryptonets_relu;

import mixedProtocolsAnalysis.*;

public class P {
	// Cryptonets benchmark from HyCC (the unsigned integers version)
	
	// Parameters taken from the paper
	public static final int IMAGE_WIDTH = 28; // 28
	public static final int WINDOW_WIDTH = 5;
	public static final int STRIDE = 2;
	public static final int OUTPUT_CHANNELS = 5; // 5

	public static final int IMAGE_CROP = 13; // 13 with padding
	public static final int SIZE_CONVOLUTION = (IMAGE_CROP * IMAGE_CROP); // 169

	public static final int FULLY_CONNECTED_WIDTH = 100; // (7, 9)
	public static final int FINAL_OUTPUT_CHANNELS = 10;

//	public static int relu(int val) {
//		if(val>0) {
//			return val;
//		} else {
//			return 0;
//		}
//	}
	
	public static int activate_sqr(int val) {
		int res = val*val;
		return res;
	}
	
//	public static void max_pooling_outputs(int[] vals, int[] OUTPUT_res, 
//			int outputs, int cols, int rows) {
//		int size = cols*rows;
//		int rows_res = rows / 2;
//		int cols_res = cols / 2;
//		int output_size = rows_res * cols_res;
//		for(int o = 0; o < outputs; o++) {
//			int input_offset = o * size;
////			int size = cols*rows; 
////			DT input_layer[size]; // We copy data, because compiler is unable to slice array efficiently
////			memcpy(input_layer, vals+o*size, size * sizeof(DT));
////			int output_size = cols/2*rows/2;
////			DT res_layer[output_size];
//			
//			int output_offset = o * output_size;
////			max_pooling(input_layer, res_layer, cols, rows);
////			memcpy(OUTPUT_res+o*output_size, res_layer, output_size * sizeof(DT));
//			
////			int rows_res = rows / 2;
////			int cols_res = cols / 2;
//			for(int i = 0; i < rows_res; i++) {
//				for(int j = 0; j < cols_res; j++) {
//					int x = j * 2;
//					int y = i * 2;
//					int loc1 = input_offset + y * cols + x;
//					int loc2 = input_offset + y * cols + x + 1;
//					int loc3 = input_offset + (y + 1) * cols + x;
//					int loc4 = input_offset + (y + 1) * cols + x + 1;
//					int val1 = vals[loc1];
//					int val2 = vals[loc2];
//					int val3 = vals[loc3];
//					int val4 = vals[loc4];
//					
//					
//					int max = val1;
//					int newMax;
//					if(val2 > max) {
//						newMax = val2;
//					}
//					else {
//						newMax = max;
//					}
//					
//					max = newMax;
//					
//					if(val3 > max) {
//						newMax = val3;
//					}
//					else {
//						newMax = max;
//					}
//					max = newMax;
//					
//					if(val4 > max) {
//						newMax = val4;
//					} 
//					else {
//						newMax = max;
//					}
//					
//					max = newMax;
//					
//					OUTPUT_res[output_offset + i * cols_res + j] = max;
//				}
//			}
//
//		}
//	}
	
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
        int[] inputA_image = new int[IMAGE_WIDTH * IMAGE_WIDTH];
        int[] inputB_kernelsL1 = new int[OUTPUT_CHANNELS * WINDOW_WIDTH * WINDOW_WIDTH];
        int[] inputB_pool_layer = new int[FULLY_CONNECTED_WIDTH * SIZE_CONVOLUTION * OUTPUT_CHANNELS];
        int[] inputB_fc = new int[FINAL_OUTPUT_CHANNELS * FULLY_CONNECTED_WIDTH];
        int[] output_final_layer = new int[FINAL_OUTPUT_CHANNELS];
        
        MPCAnnotation mpc = MPCAnnotationImpl.v();
        for(int i = 0; i < IMAGE_WIDTH * IMAGE_WIDTH; i++) {
        	inputA_image[i] = mpc.IN();
        }
        for(int i = 0; i < OUTPUT_CHANNELS * WINDOW_WIDTH * WINDOW_WIDTH; i++) {
        	inputB_kernelsL1[i] = mpc.IN();
        }
        for(int i = 0; i < FULLY_CONNECTED_WIDTH * SIZE_CONVOLUTION * OUTPUT_CHANNELS; i++) {
        	inputB_pool_layer[i] = mpc.IN();
        }
        for(int i = 0; i < FINAL_OUTPUT_CHANNELS * FULLY_CONNECTED_WIDTH; i++) {
        	inputB_fc[i] = mpc.IN();
        }
        
        // Two lines of padding 
    	int padded_width = IMAGE_WIDTH + 2;
    	int[] convolution_input = new int[padded_width*padded_width];
    	for(int i = 0; i < padded_width; i++) {
    		convolution_input[i] = 0;
    		convolution_input[i+padded_width] = 0;
    		convolution_input[padded_width*i] = 0;
    		convolution_input[padded_width*i+1] = 0;
    	} 
    	for(int y = 0; y < IMAGE_WIDTH; y++) {
    		for(int x = 0; x < IMAGE_WIDTH; x++) {
    			convolution_input[(y+2)*padded_width+(x+2)] = inputA_image[y*IMAGE_WIDTH+x];
    		}
    	}

    	// Convolution (1)
    	int[] convolution_layer = new int[OUTPUT_CHANNELS * SIZE_CONVOLUTION];
    	convolution_naive_outputs(0, convolution_input, inputB_kernelsL1, convolution_layer, padded_width, WINDOW_WIDTH, OUTPUT_CHANNELS, STRIDE, IMAGE_CROP);
    	
    	
    	// Activation Function (2)
    	for(int i = 0; i < OUTPUT_CHANNELS * SIZE_CONVOLUTION; i++) {
    		convolution_layer[i] = activate_sqr(convolution_layer[i]);
    	}
    	
    	// Combination of Mean pooling and Fully connected (3)
    	int[] im_layer = new int[FULLY_CONNECTED_WIDTH];	
    	mmulT_unrolled(inputB_pool_layer, convolution_layer, im_layer, FULLY_CONNECTED_WIDTH, 1, OUTPUT_CHANNELS * SIZE_CONVOLUTION);

    	// Activation Function (4)
    	for(int i = 0; i < FULLY_CONNECTED_WIDTH; i++) {
    		im_layer[i] = activate_sqr(im_layer[i]);
    	}

    	// Fully Connected (5)
    	mmulT_unrolled(inputB_fc, im_layer, output_final_layer, FINAL_OUTPUT_CHANNELS, 1, FULLY_CONNECTED_WIDTH);

        for(int i = 0; i < FINAL_OUTPUT_CHANNELS; i++) {
        	mpc.OUT(output_final_layer[i]);
        }
    }
}