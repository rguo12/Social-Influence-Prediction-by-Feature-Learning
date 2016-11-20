/**   
 * @package	alg.nbmf.ipp.wise
 * @File		Cons.java
 * @Crtdate	Aug 9, 2013
 *
 * Copyright (c) 2013 by <a href="mailto:wangyongqing.casia@gmail.com">Allen Wang</a>.   
 */
package alg.lis.cons;

import java.util.*;

/**
 * For parameters
 *
 * @author Allen Wang
 * 
 * Aug 9, 2013 6:01:55 PM
 * @version 1.0
 */
public class Cons {

	public static Double[][] uMatrix = null; //U matrix
	public static Double[][] cMatrix = null; //C matrix
	
	public static Double[][] deltaU = null; //deltaU matrix
	public static Double[][] deltaC = null; //deltaC matrix
	
	//node's active status record in every round
	//[uid, [mid]]
	public static Map<Integer, List<Integer>> actRec = new HashMap<Integer, List<Integer>>(); 
	//node's maximum influence node record in every round (only with successful active status)
	//[uid, [mid, max_inf_node]]
	public static Map<Integer, Map<Integer, Integer>> maxInfNode = new HashMap<Integer, Map<Integer,Integer>>(); 
	//received messages record for each node in every round
	//[uid, [mid, [send_id]]]
	public static Map<Integer, Map<Integer, List<Integer>>> actRecIndeg = new HashMap<Integer, Map<Integer, List<Integer>>>(); 
	//node's outdegree map
	public static Map<Integer, List<Integer>> nodeMap = new HashMap<Integer, List<Integer>>();
	//nodes' positive cascade info (for kdd work), [rid, [sender_pattern, success number]]
	public static Map<Integer, Map<String, Integer>> posNCasInfo = new HashMap<Integer, Map<String,Integer>>();
	//nodes' negative cascade info (for kdd work), [rid, [sender_pattern, failure number]]
	public static Map<Integer, Map<String, Integer>> negNCasInfo = new HashMap<Integer, Map<String,Integer>>();
	
	
	public static Integer nodeSize = 0; //the number of node in network, wise has 13523 nodes, msg number is 10^5
	
	//control parameters
	public static Double lbda = .0;
	public static Double mu = .0;
	public static Double mom = .0; // moment parameter
	public static Double lr = .0;
	public static Integer ftuLen = 0; // feature length
	public static Integer maxEpoch  = 0;
	public static Double beta = .0; //exponential scale parameter
	public static Double gama = .0; //learning rate increase/decrease rate
	public static Double theta = .0; //for NMF Armijo Rule
	public static Double stopThres = .0; //for stop NMF iteration
	public static Integer paramL = 0; //definitive lth-order Markov chain
	public static Double phi = .0; // factor for the added term sum(||Ui||||Ci|| - Ui.Ci)
	//end of control parameters
	
	//others
	public static String trainDatFile = "";
	public static String linkFilePath = "";
	public static String resFilePathRoot = "";
	public static String userMsgMapFile = "";
	public static String logFile = "";
	public static Integer threadNum = 0;
	public static Double threadSleepSec = .0;
	public static Double[] lossEpoch = null;
	//end of others

	//predictions
	//will use the Con.posNCasInfo and Con.negNCasInfo as the ground truth
	//yPred[calcId] = [probVals]
	//yTrue[calcId] = []
	public static Hashtable<Integer,ArrayList<Double>> yPred = new Hashtable<Integer,ArrayList<Double>>();
	public static Hashtable<Integer,ArrayList<Integer>> yTrue = new Hashtable<Integer, ArrayList<Integer>>();
	//public static Map<Integer,List<Integer>> yTrue = new HashMap<Integer,List<Integer>>();
	public static Double probThres = .0;
	public static Integer moreNeg = 0;
	public static Integer neg = 0;
	//end of predictions
	
}
