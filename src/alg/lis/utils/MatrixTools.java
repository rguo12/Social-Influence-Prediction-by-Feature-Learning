/**   
 * @package	anaylsis.data.util
 * @File		MatrixTools.java
 * @Crtdate	Aug 11, 2013
 *
 * Copyright (c) 2013 by <a href="mailto:wangyongqing.casia@gmail.com">Allen Wang</a>.   
 */
package alg.lis.utils;

/**
 *
 * @author Allen Wang
 * 
 * Aug 11, 2013 12:14:02 AM
 * @version 1.0
 */
public class MatrixTools {

	public static Double[][] matrixAdd(Double[][] mat1, Double[][] mat2) {
		
		if(mat1==null || mat2==null || mat1.length==0 
				|| mat2.length==0 || mat1.length!=mat2.length) {
			throw new IllegalArgumentException("Matrix is error!");
		}
		
		Integer rowNum = mat1.length;
		Integer colNum = mat1[0].length;
		Double[][] mat = new Double[rowNum][colNum];
		for(int i=0; i<rowNum; i++) {
			for(int j=0; j<colNum; j++) {
				mat[i][j] = mat1[i][j]+mat2[i][j];
			}
		}
		
		return mat;
	}
	
	public static Double[][] matrixMinus(Double[][] mat1, Double[][] mat2) {
		
		if(mat1==null || mat2==null || mat1.length==0 
				|| mat2.length==0 || mat1.length!=mat2.length) {
			throw new IllegalArgumentException("Matrix is error!");
		}
		
		Integer rowNum = mat1.length;
		Integer colNum = mat1[0].length;
		Double[][] mat = new Double[rowNum][colNum];
		for(int i=0; i<rowNum; i++) {
			for(int j=0; j<colNum; j++) {
				mat[i][j] = mat1[i][j]-mat2[i][j];
			}
		}
		
		return mat;
	}
	
	public static Double[][] copyMatrix(Double[][] mat) {
		
		if(mat==null || mat.length==0) {
			throw new IllegalArgumentException("Matrix is error!");
		}
		
		Integer rowNum = mat.length;
		Integer colNum = mat[0].length;
		
		Double[][] newMat = new Double[rowNum][colNum];
		for(int i=0; i<rowNum; i++) {
			for(int j=0; j<colNum; j++) {
				newMat[i][j] = mat[i][j];
			}
		}
		
		return newMat;
	}
	
	public static Double hadamardProduct(Double[][] mat1, Double[][] mat2) {
		
		if(mat1==null || mat2==null || mat1.length==0 
				|| mat2.length==0 || mat1.length!=mat2.length) {
			throw new IllegalArgumentException("Matrix is error!");
		}
		
		Integer rowNum = mat1.length;
		Integer colNum = mat1[0].length;
		Double retVal = .0;
		for(int i=0; i<rowNum; i++) {
			for(int j=0; j<colNum; j++) {
				retVal += mat1[i][j]*mat2[i][j];
			}
		}
		
		return retVal;
	}
	
	public static Double[][] matrixAdd(Double[][] mat1, Double[][] mat2, Double bound, Boolean isBigger) {
		
		if(mat1==null || mat2==null || mat1.length==0 
				|| mat2.length==0 || mat1.length!=mat2.length) {
			throw new IllegalArgumentException("Matrix is error!");
		}
		
		Integer rowNum = mat1.length;
		Integer colNum = mat1[0].length;
		Double[][] mat = new Double[rowNum][colNum];
		for(int i=0; i<rowNum; i++) {
			for(int j=0; j<colNum; j++) {
				mat[i][j] = mat1[i][j]+mat2[i][j];
				if(isBigger && mat[i][j]<bound) {
					mat[i][j] = bound;
//					System.out.println("matrix change @["+i+","+j+"]");
					mat2[i][j] = bound-mat1[i][j];
				}
				if(!isBigger && mat[i][j]>bound) {
					mat[i][j] = bound;
//					System.out.println("matrix change @["+i+","+j+"]");
					mat2[i][j] = bound-mat1[i][j];
				}
			}
		}
		
		return mat;
	}
	
	public static Double getMatrixSelfAbsSum(Double[][] mat) {
		
		if(mat==null || mat.length==0) {
			throw new IllegalArgumentException("Matrix is error!");
		}
		
		Integer rowNum = mat.length;
		Integer colNum = mat[0].length;
		Double retVal = .0;
		for(int i=0; i<rowNum; i++) {
			for(int j=0; j<colNum; j++) {
//				retVal += mat[i][j];
				retVal += Math.abs(mat[i][j]);
			}
		}
		
		return retVal;
	}

	public static Double getMatrixSelfSum(Double[][] mat) {
		
		if(mat==null || mat.length==0) {
			throw new IllegalArgumentException("Matrix is error!");
		}
		
		Integer rowNum = mat.length;
		Integer colNum = mat[0].length;
		Double retVal = .0;
		for(int i=0; i<rowNum; i++) {
			for(int j=0; j<colNum; j++) {
				retVal += mat[i][j];
//				retVal += Math.abs(mat[i][j]);
			}
		}
		
		return retVal;
	}
	
	public static Double[][] dotMultiply(Double val, Double[][] mat) {
		
		if(mat==null || mat.length==0) {
			throw new IllegalArgumentException("Matrix is error!");
		}
		
		Integer rowNum = mat.length;
		Integer colNum = mat[0].length;
		Double[][] dotMult = new Double[rowNum][colNum];
		for(int i=0; i<rowNum; i++) {
			for(int j=0; j<colNum; j++) {
				dotMult[i][j] = val*mat[i][j];
			}
		}
		
		return dotMult;
	}
	
	public static Double[][] negMatrix(Double[][] mat) {
		
		if(mat==null || mat.length==0) {
			throw new IllegalArgumentException("Matrix is error!");
		}
		
		Integer rowNum = mat.length;
		Integer colNum = mat[0].length;
		Double[][] negMatrix = new Double[rowNum][colNum];
		for(int i=0; i<rowNum; i++) {
			for(int j=0; j<colNum; j++) {
				negMatrix[i][j] = -mat[i][j];
			}
		}
		
		return negMatrix;
	}
}
