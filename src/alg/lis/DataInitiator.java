/**   
 * @package	alg.nbmf.ipp.wise
 * @File		DataInitiator.java
 * @Crtdate	Aug 9, 2013
 *
 * Copyright (c) 2013 by <a href="mailto:wangyongqing.casia@gmail.com">Allen Wang</a>.   
 */
package alg.lis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import alg.lis.cons.Cons;
import alg.lis.utils.CollectionHelper;
import alg.lis.utils.FileUtil;
import alg.lis.utils.StringHelper;

/**
 * Initiate paramters which used in Neighbor-based PMF
 *
 * @author Allen Wang
 * 
 * Aug 9, 2013 5:51:01 PM
 * @version 1.0
 */
public class DataInitiator {

//	//tasks for mult-thread reading
//	private static List<String	> filePathList = null; //training files 
//	private static boolean isCasInfReady = false;
	
	/**
	 * randomly initialize matrix
	 * 
	 * @param nodeSize
	 * @param featureLen
	 * @return
	 */
	public Double[][] getRandomMatrix(Integer nodeSize, Integer featureLen, Double fac) {
		
		if(nodeSize==null || featureLen==null) {
			return null;
		}
		
		Double[][] matrix = new Double[nodeSize][featureLen];
		
		Random rand = new Random();
		for(int i=0; i<nodeSize; i++) {
			for(int j=0; j<featureLen; j++) {
				matrix[i][j] = fac*rand.nextGaussian();
			}
		}
		
		return matrix;
	}
	
	public Double[][] getUniPolRandomMatrix(Integer nodeSize, Integer featureLen, Double fac) {
		
		if(nodeSize==null || featureLen==null) {
			return null;
		}
		
		Double[][] matrix = new Double[nodeSize][featureLen];
		
		Random rand = new Random();
		for(int i=0; i<nodeSize; i++) {
			for(int j=0; j<featureLen; j++) {
				matrix[i][j] = fac*rand.nextDouble();
			}
		}
		
		return matrix;
	}
	
	/**
	 * fixed value for initializing matrix
	 * 
	 * @param nodeSize
	 * @param featureLen
	 * @param fixedVal
	 * @return
	 */
	public Double[][] getFixedMatrix(Integer nodeSize, Integer featureLen, Double fixedVal) {
		
		if(nodeSize==null || featureLen==null || fixedVal==null) {
			return null;
		}
		
		Double[][] matrix = new Double[nodeSize][featureLen];
		
		for(int i=0; i<nodeSize; i++) {
			for(int j=0; j<featureLen; j++) {
				matrix[i][j] = fixedVal;
			}
		}
		
		return matrix;
	}
	
//	/**
//	 * get training files
//	 * 
//	 * @param fileDir
//	 * @return
//	 */
//	private List<String> getProcFilePathList(String fileDir) {
//		
//		if(StringHelper.isEmpty(fileDir)) {
//			return Collections.emptyList();
//		}
//		
//		List<String> filePathList = new ArrayList<String>();
//		
//		File fd = new File(fileDir);
//		if(fd.isDirectory()) {
//			for(File f : fd.listFiles()) {
//				if(f.isFile()) {
//					filePathList.add(f.getPath());
//				}
//			}
//		}
//		
//		return filePathList;
//	}
	
	public void setCascadeInfo(String filePath) throws IOException {
		
		BufferedReader br = FileUtil.getBufferReader(filePath);
		String line = null;
		Integer preNode = null;
		Integer curNode = null;
		Integer count = 0;
		Map<Integer, Integer> maxInfCol = null;
		Map<Integer, List<Integer>> recMsgCol = null;
 		while((line=br.readLine())!=null) {
			String[] elems = line.split("\\|");
			if(elems.length<4) {
				System.out.println(elems[0]);
				curNode = Integer.parseInt(elems[0]);
				continue;
			}
			Integer status = Integer.parseInt(elems[0]);
			// System.out.println(line);
			Integer maxInfNode = getMaxInfNode(elems[2]);
			Integer mid = Integer.parseInt(elems[3]);
			if(preNode==null || preNode!=curNode) {
				if(preNode!=null) {
					System.out.println("Getting node "+preNode+"'s cascade information.");
					count++;
					System.out.println("count = "+count);
					Cons.maxInfNode.put(preNode, maxInfCol);
					Cons.actRecIndeg.put(preNode, recMsgCol);
//					logWriting(String.valueOf(preNode));
				}
				maxInfCol = new HashMap<Integer, Integer>();
				recMsgCol = new HashMap<Integer, List<Integer>>();
				preNode = curNode;
			}
			List<Integer> infoNodes = getInfNodeList(elems[1]);
			if (status != null && !CollectionHelper.isEmpty(infoNodes)) {
				recMsgCol.put(mid, infoNodes);
			}
			if (maxInfNode != -1 && infoNodes.size()>1) {
				maxInfCol.put(mid, maxInfNode);
			}
		}
	}
	
	private Integer getNumber(String val) {
		
		if(StringHelper.isEmpty(val)) {
			return -1;
		}
		
		if(val.contains("*")) {
			return Integer.parseInt(val.substring(0, val.length()-1));
		}
		
		return Integer.parseInt(val);
	}
	
	public void setNodeCascadeInfoWithoutNeg(String filePath) throws IOException {
		
		BufferedReader br = FileUtil.getBufferReader(filePath);
		String line = null;
		while((line=br.readLine())!=null) {
			String[] elems = line.split(",");
			if(elems.length<2) {
				continue;
			}
			Integer nIndex = Integer.parseInt(elems[0]); //key--node index
			for(int i=1; i<elems.length; i++) {
				String[] nInfo = elems[i].split("\\|");
				if(nInfo.length<3) {
					continue;
				}
				//positive nodes' info
				if(nInfo[0].equalsIgnoreCase("1")) {
					//build indices of msg's potential sending nodes
					String[] snIndices = nInfo[1].split("-");
					String snKey = "";
					//get rid of self-forwarding cases
					for(String val : snIndices) {
						if(nIndex.equals(getNumber(val)) || getNumber(val)<0) {
							continue;
						}
						snKey += val+"-";
					}
					if(StringHelper.isEmpty(snKey)) {
						continue;
					}
					snKey = snKey.substring(0, snKey.length()-1);//key--indices of send nodes
					//if snKey is empty, then skip the case
					if(StringHelper.isEmpty(snKey)) {
						continue;
					}
					Integer num = Integer.parseInt(nInfo[2]);
					Map<String,Integer> tmpMap = new HashMap<String, Integer>();
					
					if(Cons.posNCasInfo.containsKey(nIndex)) {
						tmpMap = Cons.posNCasInfo.get(nIndex);
					} 
					tmpMap.put(snKey, num);
					Cons.posNCasInfo.put(nIndex, tmpMap);
				} 
			}
		}
	}
	
	public void setNodeCascadeInfo(String filePath) throws IOException {
		
		BufferedReader br = FileUtil.getBufferReader(filePath);
		String line = null;
		while((line=br.readLine())!=null) {
			String[] elems = line.split(",");
			if(elems.length<2) {
				continue;
			}

			Integer nIndex = Integer.parseInt(elems[0]); //key--node index

			for(int i=1; i<elems.length; i++) {
				String[] nInfo = elems[i].split("\\|");
				if(nInfo.length<3) {
					continue;
				}
				//build indices of msg's potential sending nodes
				String[] snIndices = nInfo[1].split("-");
				String snKey = "";
				//get rid of self-forwarding cases
				for(String val : snIndices) {
					if(nIndex.equals(getNumber(val)) || getNumber(val)<0) {
						continue;
					}
					snKey += val+"-";
				}
				if(StringHelper.isEmpty(snKey)) {
					continue;
				}
				snKey = snKey.substring(0, snKey.length()-1);//key--indices of send nodes
				//if snKey is empty, then skip the case
				if(StringHelper.isEmpty(snKey)) {
					continue;
				}
				Integer num = Integer.parseInt(nInfo[2]);
				Map<String,Integer> tmpMap = new HashMap<String, Integer>();



				//positive nodes' info
				if(nInfo[0].equalsIgnoreCase("1")) {
					if(Cons.posNCasInfo.containsKey(nIndex)) {
						tmpMap = Cons.posNCasInfo.get(nIndex);
					}

					tmpMap.put(snKey, num);
					Cons.posNCasInfo.put(nIndex, tmpMap);

				} else { //negative nodes' info
					if(Cons.negNCasInfo.containsKey(nIndex)) {
						tmpMap = Cons.negNCasInfo.get(nIndex);
					}

					tmpMap.put(snKey, num);
					Cons.negNCasInfo.put(nIndex, tmpMap);
				}
			}
		}
	}

	void setUCMatrix(String ufilePath,String cfilePath) throws IOException{
		Cons.uMatrix = setMatrix(ufilePath,Cons.nodeSize,Cons.ftuLen);
		Cons.cMatrix = setMatrix(cfilePath,Cons.nodeSize,Cons.ftuLen);
		//Cons.yPred = new Hashtable<Integer,Double[]>();
	}


	
//	public void logWriting(String str) {
//		
//		OutputStreamWriter osw = FileUtil.getOutputStreamWriter("./node_log", true);
//		FileUtil.writeln(osw, str);
//	}
	
//	public void setCascadeInfo(String fileDir, Integer threadNum, Float sleepSec) {
//		
//		filePathList = getProcFilePathList(fileDir);
//		//the number of nodes with information < the number of nodes in network
//		Integer infoNodesNum = filePathList.size(); 
//		ExecutorService exec = Executors.newCachedThreadPool();
//		for(int i=0; i<threadNum; i++) {
//			exec.execute(new Initiator());
//		}
//		//waiting until cascade information was loaded
//		while(!isCasInfReady) {
//			if(Cons.maxInfNode.size()==infoNodesNum
//					&& Cons.actRecIndeg.size()==infoNodesNum) {
//				isCasInfReady = true;
//			}
//			try {
//				System.out.println("$$$$$$$$Waiting for load data$$$$$$$$");
//				Thread.sleep((long) (1000*sleepSec));
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
	
	private Integer getMaxInfNode(String info) {

		if (StringHelper.isEmpty(info.trim()) || info.equalsIgnoreCase("null")) {
			return -1;
		}

		return Integer.parseInt(info);
	}

	private List<Integer> getInfNodeList(String info) {

		if(StringHelper.isEmpty(info)) {
			return Collections.emptyList();
		}
		
		List<Integer> nodeList = new ArrayList<Integer>();
		String[] elems = info.split(",");
		if(elems.length<1) {
			return Collections.emptyList();
		}
		for (String val : elems) {
			nodeList.add(Integer.parseInt(val));
		}

		return nodeList;
	}

	public Map<Integer, List<Integer>> getUserMsgMap(String filePath) throws IOException {

		Map<Integer, List<Integer>> userMsgMap = new HashMap<Integer, List<Integer>>();
		BufferedReader br = FileUtil.getBufferReader(filePath);
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] elems = line.split("\\|");
			if (elems.length < 3) {
				continue;
			}
			String[] midList = elems[1].split(",");
			Integer uid = Integer.parseInt(elems[0]);
			List<Integer> mids = new ArrayList<Integer>();
			for (int i = 0; i < midList.length; i++) {
				mids.add(Integer.parseInt(midList[i]));
			}
			if(!userMsgMap.containsKey(uid)) {
				userMsgMap.put(uid, mids);
			}
		}
		br.close();

		return userMsgMap;
	}
	
	public Double[][] setMatrix(String filePath, Integer nodeSize, Integer ftuLen) throws IOException {
		
		Double[][] matrix = new Double[nodeSize][ftuLen];
		
		BufferedReader br = FileUtil.getBufferReader(filePath);
		String line = null;
		int rowNum = 0;
		while((line=br.readLine())!=null) {
			if(StringHelper.isEmpty(line)) {
				continue;
			}
			String[] elems = line.split("\t");
			int count = 0;
			for(String val : elems) {
				if(StringHelper.isEmpty(val)) {
					continue;
				}
				matrix[rowNum][count++] = Double.parseDouble(val);
			}
			rowNum++;
		}
		
		return matrix;
	}
	
//	/**
//	 * Multi-thread for fast reading training data
//	 *
//	 * @author Allen Wang
//	 * 
//	 * Aug 9, 2013 6:29:08 PM
//	 * @version 1.0
//	 */
//	class Initiator implements Runnable {
//		
//		private String objFilePath = null;
//		private Integer objNode = null;
//		private Map<Integer, Integer> maxInfCol = null;
//		private Map<Integer, List<Integer>> recMsgCol = null;
//		
//		Initiator() {
//			
//		}
//		
//		/**
//		 * parse object node id from file name
//		 * 
//		 * @param fileName
//		 * @return
//		 */
//		private Integer getObjNode(String fileName) {
//
//			String nodeNum = fileName.substring(fileName.lastIndexOf("-") + 1);
//
//			return Integer.parseInt(nodeNum);
//		}
//		
//		/**
//		 * get current thread's jobs
//		 */
//		private void missionDispatcher() {
//			
//			if(CollectionHelper.isEmpty(filePathList)) {
//				this.objFilePath = null;
//				this.objNode = null;
//				return;
//			}
//			
//			synchronized(filePathList) {
//				this.objFilePath = filePathList.remove(0);
//				this.objNode = getObjNode(objFilePath);
//			}
//		}
//		
//		/**
//		 * reset list
//		 */
//		private void storeReset() {
//			
//			this.maxInfCol = new HashMap<Integer, Integer>();
//			this.recMsgCol = new HashMap<Integer, List<Integer>>();
//		}
//		
//		private Integer getMaxInfNode(String info) {
//			
//			if(StringHelper.isEmpty(info.trim()) || info.equalsIgnoreCase("null")) {
//				return -1;
//			}
//			
//			return Integer.parseInt(info);
//		}
//		
//		private List<Integer> getInfNodeList(String info) {
//			
//			List<Integer> nodeList = new ArrayList<Integer>();
//			String[] elems = info.split(",");
//			for(String val : elems) {
//				nodeList.add(Integer.parseInt(val));
//			}
//			
//			return nodeList;
//		}
//		
//		private void setCascadeParams() throws IOException {
//			
//			storeReset();
//			
//			BufferedReader br = FileUtil.getBufferReader(objFilePath);
//			String line = null;
//			while((line=br.readLine())!=null) {
//				String[] elems = line.split("\\|");
//				if(elems.length<4) {
//					continue;
//				}
//				Integer status = Integer.parseInt(elems[0]);
////				System.out.println(line);
//				Integer maxInfNode = getMaxInfNode(elems[2]);
//				Integer mid = Integer.parseInt(elems[3]);
//				if(maxInfNode!=-1) {
//					maxInfCol.put(mid, maxInfNode);
//				}
//				List<Integer> infoNodes = getInfNodeList(elems[1]);
//				if(status!=null && !CollectionHelper.isEmpty(infoNodes)) {
//					recMsgCol.put(mid, infoNodes);
//				}
//			}
//		}
//		
//		private void setNodeInfo() {
//			
//			synchronized(Cons.maxInfNode) {
//				if(Cons.maxInfNode.containsKey(objNode)) {
//					return;
//				}
//				Cons.maxInfNode.put(objNode, maxInfCol);
////				System.out.println(maxInfCol.size());
//			}
//			synchronized(Cons.actRecIndeg) {
//				if(Cons.actRecIndeg.containsKey(objNode)) {
//					return;
//				}
//				Cons.actRecIndeg.put(objNode, recMsgCol);
////				System.out.println(recMsgCol.size());
//			}
//		}
//
//		/* (non-Javadoc)
//		 * @see java.lang.Runnable#run()
//		 */
//		public void run() {
//			
//			while(!CollectionHelper.isEmpty(filePathList)) {
//				missionDispatcher();
//				System.out.println("Start to get node "+objNode+"'s cascading data.");
//				try {
//					setCascadeParams();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				System.out.println("Complete getting node "+objNode+"'s cascading data.");
//				setNodeInfo();
//				System.out.println("******Complete setting node "+objNode+"'s cascading info in memory.******");
//				System.out.println("Now rest node size is "+filePathList.size());
//			}
//		}
//	}
	
}
