/**   
 * @package	anaylsis.data.util
 * @File		VecTools.java
 * @Crtdate	Aug 10, 2013
 *
 * Copyright (c) 2013 by <a href="mailto:wangyongqing.casia@gmail.com">Allen Wang</a>.   
 */
package alg.lis.utils;

/**
 *
 * @author Allen Wang
 * 
 * Aug 10, 2013 8:36:16 PM
 * @version 1.0
 */
public class VecTools {

	public static Double[] doubleVecInit(Integer ftuLen) {
		
		Double[] vec = new Double[ftuLen];
		
		for(int i=0; i<ftuLen; i++) {
			vec[i] = .0;
		}
		
		return vec;
	}
	
	public static Double[] vecAdd(Double[] vec1, Double[] vec2) {
		
		if(vec1==null || vec2==null || vec1.length!=vec2.length) {
			throw new IllegalArgumentException("Vector is error!");
		}
		Integer len = vec1.length;
		Double[] sum = new Double[len];
		for(int i=0; i<len; i++) {
			sum[i] = vec1[i]+vec2[i];
		}
		
		return sum;
	}
	
	public static Double vecMultiply(Double[] vec1, Double[] vec2) {
		
		if(vec1==null || vec2==null || vec1.length!=vec2.length) {
			throw new IllegalArgumentException("Vector is error!");
		}
		Double val = .0;
		Integer len = vec1.length;
		for(int i=0; i<len; i++) {
			val += vec1[i]*vec2[i];
		}
		
		return val;
	}
	
	public static Double[] dotMultiply(Double val, Double[] vec) {
		
		if(vec==null) {
			throw new IllegalArgumentException("Vector is error!");
		}
		
		Integer len = vec.length;
		Double[] dotMult = new Double[len];
		for(int i=0; i<len; i++) {
			dotMult[i] = val*vec[i];
		}
		
		return dotMult;
	}
	
	public static Double[] negVec(Double[] vec) {
		
		if(vec==null) {
			throw new IllegalArgumentException("Vector is error!");
		}
		
		Integer len = vec.length;
		Double[] negVec = new Double[len];
		for(int i=0; i<len; i++) {
			negVec[i] = -vec[i];
		}
		
		return negVec;
	}

	public static Double l2Norm(Double[] vec) {
		if (vec == null) {
			throw new IllegalArgumentException("Vector is error!");
		}
		Integer len = vec.length;
		Double val = .0;
		for (int i = 0; i < len; i++) {
			val += vec[i] * vec[i];
		}
		val = Math.sqrt(val);
		return val;
	}
}
