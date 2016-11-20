package alg.lis;

import alg.lis.cons.Cons;
import alg.lis.utils.CollectionHelper;
import alg.lis.utils.FileUtil;
import alg.lis.utils.MatrixTools;
import alg.lis.utils.VecTools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 
 * u refers to susceptibility
 * c refers to influence
 *
 * @author King Wang
 * 
 * Dec 5, 2013 5:41:47 PM
 * @version 1.0
 */
public class AdditiveInfModel_beta {

	private DataInitiator initiator = new DataInitiator();
	private static List<Integer> missionNodes = new ArrayList<Integer>();
	private static Integer missionOver = 0;
	private Map<String, String> config = new HashMap<String, String>();
	private Double[][] dU = null;
	private Double[][] dC = null;
	private Boolean updateUStop = false;
	private Boolean updateCStop = false;
	private Boolean hasStopped = false;

	private Double initPartialUVal = .0;
	private Double initPartialCVal = .0;

	public AdditiveInfModel_beta(String confPath){
		
		try {
			initialData(confPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static synchronized void missionOver() {
		System.out.println("Complete thread is " +missionOver);
		missionOver++;
	}
	
//	private DBCollection getMongoDBCol() throws UnknownHostException {
//		
//		System.out.println("Connect mongo db server"+mongoDBAddress+":"+Integer.toString(port));
//		
//		Mongo mongo = new Mongo(new ServerAddress(mongoDBAddress,port));
//		
//		System.out.println("Connect db"+dbName);
//		DB db = mongo.getDB(dbName);
//		
//		System.out.println("Connect collection"+colName);
//		DBCollection coll = db.getCollection(colName);
//		
//		return coll;
//	}
	
	private void initialData(String confPath) throws IOException {
		
		setBasicConfig(confPath);
		System.out.println("Get configuration!");
		
		Cons.uMatrix = initiator.getUniPolRandomMatrix(Cons.nodeSize, Cons.ftuLen, .3);
		Cons.cMatrix = initiator.getUniPolRandomMatrix(Cons.nodeSize, Cons.ftuLen, .3);
		Cons.deltaU = initiator.getFixedMatrix(Cons.nodeSize, Cons.ftuLen, .0);
		Cons.deltaC = initiator.getFixedMatrix(Cons.nodeSize, Cons.ftuLen, .0);
		dU = initiator.getFixedMatrix(Cons.nodeSize, Cons.ftuLen, .0);
		dC = initiator.getFixedMatrix(Cons.nodeSize, Cons.ftuLen, .0);
		System.out.println("Initialize matrices!");
		
		Cons.lossEpoch = new Double[Cons.maxEpoch+1];
		for(int i=0; i<Cons.maxEpoch+1; i++) {
			Cons.lossEpoch[i] = .0;
		}
		
		System.out.println("Start to fetch cascade date from file "+Cons.trainDatFile);
		initiator.setNodeCascadeInfo(Cons.trainDatFile);
		System.out.println("===========Complete getting cascade date, and be ready for training===========");
	}
	
	private void initMissionNodes() {
		
		synchronized(missionNodes){
			if(Cons.nodeSize>0) {
				missionNodes = new ArrayList<Integer>();
				for(int i=0; i<Cons.nodeSize; i++) {
					missionNodes.add(i);
				}
			}
		}
		
		missionStart();
	}
	
	private void missionStart() {
		
		synchronized(missionOver) {
			missionOver = 0;
		}
	}
	
	private Map<String, String> getConfParams(String confPath) throws IOException {
		
		Map<String, String> confs = new HashMap<String, String>();
		
		BufferedReader br = FileUtil.getBufferReader(confPath);
		String line = null;
		while((line=br.readLine())!=null) {
			String[] elems = line.split("=");
			if(elems.length<2) {
				continue;
			}
			String key = elems[0];
			String val = elems[1];
			if(!confs.containsKey(key)) {
				confs.put(key, val);
			}
		}
		
		return confs;
	}
	
	private void setBasicConfig(String confPath) {
		try {
			config = getConfParams(confPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(config!=null && !config.isEmpty()) {
			Cons.trainDatFile = config.get("train_dat_file");
			Cons.resFilePathRoot = config.get("result_file_root");
			Cons.lbda = Double.parseDouble(config.get("lambda"));
			Cons.mu = Double.parseDouble(config.get("mu"));
//			Cons.mom = Double.parseDouble(config.get("momentum"));
			Cons.lr = Double.parseDouble(config.get("learning_rate"));
			Cons.ftuLen = Integer.parseInt(config.get("feature_length"));
			Cons.maxEpoch = Integer.parseInt(config.get("max_epoch"));
			Cons.beta = Double.parseDouble(config.get("beta"));
			Cons.gama = Double.parseDouble(config.get("gama"));
			Cons.theta = Double.parseDouble(config.get("theta"));
			Cons.stopThres = Double.parseDouble(config.get("stop_threshold"));
			Cons.nodeSize = Integer.parseInt(config.get("node_size"));
			Cons.logFile = config.get("log_file");
			Cons.threadNum = Integer.parseInt(config.get("thread_number"));
			Cons.threadSleepSec = Double.parseDouble(config.get("thread_sleep_time"));
			Cons.paramL = Integer.parseInt(config.get("paramL"));
			Cons.phi = Double.parseDouble(config.get("phi"));
			if(config.get("has_stopped").equalsIgnoreCase("false")) {
				hasStopped = false;
			} else if(config.get("has_stopped").equalsIgnoreCase("true")) {
				hasStopped = true;
			}
		}
	}
	
	private void getDeltaUC(Integer threadNum, Double sleepSec) {
		
		initMissionNodes();//multi-thread mission initiator
		
		ExecutorService exec = Executors.newCachedThreadPool();
		for (int i = 0; i < threadNum; i++) {
			exec.execute(new UCCalculator());
		}
		// waiting until cascade information was loaded
		while (missionOver!=Cons.threadNum) {
			try {
//				System.out.println("$$$$$$$$Waiting for delta U and C calculating$$$$$$$$");
				Thread.sleep((long) (1000 * sleepSec));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void getCurLoss(Integer epoch, Integer threadNum, Double sleepSec) {
		
		initMissionNodes();

		Cons.lossEpoch[epoch] = .0;
		ExecutorService exec = Executors.newCachedThreadPool();
		for (int i = 0; i < threadNum; i++) {
			exec.execute(new UCCalculator(false, epoch));
		}
		// waiting until cascade information was loaded
		while (missionOver!=Cons.threadNum) {
			try {
//				System.out.println("$$$$$$$$Waiting for loss calculating$$$$$$$$");
				Thread.sleep((long) (1000 * sleepSec));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeCurLoss(Integer epoch) throws IOException {
		
		OutputStreamWriter osw = FileUtil.getOutputStreamWriter(Cons.logFile, true);
		FileUtil.writeln(osw, Double.toString(Cons.lossEpoch[epoch]));
		osw.close();
	}
	
	private void oneTurnLearning(Integer curEpoch) {
		
		getDeltaUC(Cons.threadNum, Cons.threadSleepSec);
		Double[][] u_muMatrix = MatrixTools.dotMultiply(Cons.mu, Cons.uMatrix);
		Double[][] c_muMatrix = MatrixTools.dotMultiply(Cons.mu, Cons.cMatrix);
		dU = MatrixTools.matrixAdd(dU, MatrixTools.dotMultiply(Cons.lbda
				, MatrixTools.matrixAdd(Cons.uMatrix, u_muMatrix)));
		dC = MatrixTools.matrixAdd(dC, MatrixTools.dotMultiply(Cons.lbda
				, MatrixTools.matrixAdd(Cons.cMatrix, c_muMatrix)));
		if(!updateUStop) {
			Cons.deltaU = calcDeltaU(curEpoch);
		}
		if(!updateCStop) {
			Cons.deltaC = calcDeltaC(curEpoch);
		}
		if(!updateUStop) {
			Cons.uMatrix = MatrixTools.matrixAdd(Cons.uMatrix, Cons.deltaU, .0, true);
		}
		if(!updateCStop) {
			Cons.cMatrix = MatrixTools.matrixAdd(Cons.cMatrix, Cons.deltaC, .0, true);
		}
	}
	
	private Double[	][] calcDeltaU(Integer curEpoch) {
		
		System.out.println("===========in calculating delta U============");
		
		Double alpha = Cons.lr;
		Double[][] uMatrixCopy = MatrixTools.copyMatrix(Cons.uMatrix);
		
		Double[][] tmpDeltaU = MatrixTools.dotMultiply(alpha, dU);
		Cons.uMatrix = MatrixTools.matrixAdd(uMatrixCopy, tmpDeltaU, .0, true);
		getCurLoss(curEpoch, Cons.threadNum, Cons.threadSleepSec);
		
		Double preAlpha = alpha;
		Boolean isCalc = false;
		System.out.println("Loss gap is "+(Cons.lossEpoch[curEpoch]-Cons.lossEpoch[curEpoch-1]));
		System.out.println("Hadamard product is "+Cons.theta*MatrixTools.hadamardProduct(dU, tmpDeltaU));
		
		while(!isCalc && (Cons.lossEpoch[curEpoch]-Cons.lossEpoch[curEpoch-1])
				<Cons.theta*MatrixTools.hadamardProduct(dU, tmpDeltaU)) {
			alpha = alpha*Cons.gama;
			preAlpha = alpha;
			if(alpha<10e-10) {
				break;
			}
			tmpDeltaU = MatrixTools.dotMultiply(alpha, dU);
			Cons.uMatrix = MatrixTools.matrixAdd(uMatrixCopy, tmpDeltaU, .0, true);
			getCurLoss(curEpoch, Cons.threadNum, Cons.threadSleepSec);
			System.out.println("Temporay learning rate is "+alpha);
			System.out.println("Loss gap is "+(Cons.lossEpoch[curEpoch]-Cons.lossEpoch[curEpoch-1]));
			System.out.println("Hadamard product is "+Cons.theta*MatrixTools.hadamardProduct(dU, tmpDeltaU));
		}
		
		tmpDeltaU = MatrixTools.dotMultiply(preAlpha, dU);
		
		if(hasStopped) {
			if(initPartialUVal==.0) {
				initPartialUVal = Math.abs(MatrixTools.getMatrixSelfSum(tmpDeltaU));
				System.out.println("initPartialUVal = " + initPartialUVal);
			} else {
				Double curParitialUVal = Math.abs(MatrixTools.getMatrixSelfSum(tmpDeltaU));
				System.out.println("initPartialUVal = " + initPartialUVal);
				System.out.println("current PartialUVal = " + curParitialUVal);
				if(curParitialUVal<Cons.stopThres*initPartialUVal || curParitialUVal<10e-5) {
					updateUStop = true;
				}
			}
		}
		System.out.println("learning rate is "+preAlpha);
		Cons.uMatrix = uMatrixCopy;
		return tmpDeltaU;
	}
	
	private Double[	][] calcDeltaC(Integer curEpoch) {
		
		System.out.println("===========in calculating delta C============");
		
		Double alpha = Cons.lr;
		Double[][] cMatrixCopy = MatrixTools.copyMatrix(Cons.cMatrix);
		
		Double[][] tmpDeltaC = MatrixTools.dotMultiply(alpha, dC);
		Cons.cMatrix = MatrixTools.matrixAdd(cMatrixCopy, tmpDeltaC, .0, true);
		getCurLoss(curEpoch, Cons.threadNum, Cons.threadSleepSec);
		
		Double preAlpha = alpha;
		Boolean isCalc = false;
		System.out.println("Loss gap is "+(Cons.lossEpoch[curEpoch]-Cons.lossEpoch[curEpoch-1]));
		System.out.println("Hadamard product is "+Cons.theta*MatrixTools.hadamardProduct(dC, tmpDeltaC));
		
		while(!isCalc && (Cons.lossEpoch[curEpoch]-Cons.lossEpoch[curEpoch-1])
				<Cons.theta*MatrixTools.hadamardProduct(dC, tmpDeltaC)) {
			alpha = alpha*Cons.gama;
			preAlpha = alpha;
			if(alpha<10e-10) {
				break;
			}
			tmpDeltaC = MatrixTools.dotMultiply(alpha, dC);
			Cons.cMatrix = MatrixTools.matrixAdd(cMatrixCopy, tmpDeltaC, .0, true);
			getCurLoss(curEpoch, Cons.threadNum, Cons.threadSleepSec);
			System.out.println("Temporay learning rate is "+alpha);
			System.out.println("Loss gap is "+(Cons.lossEpoch[curEpoch]-Cons.lossEpoch[curEpoch-1]));
			System.out.println("Hadamard product is "+Cons.theta*MatrixTools.hadamardProduct(dC, tmpDeltaC));
		}
		
		tmpDeltaC = MatrixTools.dotMultiply(preAlpha, dC);
		
		if(hasStopped) {
			if(initPartialCVal==.0) {
				initPartialCVal = Math.abs(MatrixTools.getMatrixSelfSum(tmpDeltaC));
				System.out.println("initPartialCVal = " + initPartialCVal);
			} else {
				Double curParitialCVal = Math.abs(MatrixTools.getMatrixSelfSum(tmpDeltaC));
				System.out.println("initPartialCVal = " + initPartialCVal);
				System.out.println("current PartialCVal = " + curParitialCVal);
				if(curParitialCVal<Cons.stopThres*initPartialCVal || curParitialCVal<10e-5) {
					updateCStop = true;
				}
			}
		}
		Cons.cMatrix = cMatrixCopy;
		System.out.println("learning rate is "+preAlpha);
		return tmpDeltaC;
	}
	
	public void learningProc() throws IOException {
		
		getCurLoss(0, Cons.threadNum, Cons.threadSleepSec);
		writeCurLoss(0);
		for(int i=1; i<Cons.maxEpoch+1; i++) {
			System.out.println("=========Start to settle U and C matrix in epoch "+i+"==========");
			oneTurnLearning(i);
			System.out.println("=========Complete settle U and C matrix in epoch "+i+"==========");
			getCurLoss(i, Cons.threadNum, Cons.threadSleepSec);
			System.out.println("=========Complete loss calculating in epoch "+i+"==========");
			writeCurLoss(i);
			System.out.println("loss gap is "+(Cons.lossEpoch[i]-Cons.lossEpoch[i-1]));
			if(i%10==0) {
				writeRes(i);
			}
			if(updateUStop && updateCStop) {
				System.out.println("stopped!");
				break;
			}
		}
		writeRes(Cons.maxEpoch);
	}
	
	private void writeMatrix(Double[][] matrix, String pos) throws IOException {
		
		OutputStreamWriter osw = FileUtil.getOutputStreamWriter(Cons.resFilePathRoot+pos);
		for(int i=0; i<Cons.nodeSize; i++) {
			String wLine = "";
			for(int j=0; j<Cons.ftuLen; j++) {
				wLine += matrix[i][j]+"\t";
			}
			FileUtil.writeln(osw, wLine);
		}
		osw.close();
	}
	
	private void writeRes(Integer epoch) {
		
		try {
			writeMatrix(Cons.uMatrix, "_uMatrix"+"_"+epoch);
			writeMatrix(Cons.cMatrix, "_cMatrix"+"_"+epoch);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Calculate delta U and delta C with multi-thread
	 * Here U refers to S, C refers to I
	 *
	 * @author King Wang
	 * 
	 * Aug 10, 2013 11:56:33 PM
	 * @version 1.0
	 */
	class UCCalculator implements Runnable {
		
		private boolean isTraining = true;
		private Integer curEpoch = null;
		
		UCCalculator() {
			
		}
		
		UCCalculator(boolean isTraining, Integer curEpoch) {
			
			this.isTraining = isTraining;
			this.curEpoch = curEpoch;
		}
		
		private Double[] getSumC(List<Integer> sdList) {

			Double[] sdCVec = VecTools.doubleVecInit(Cons.ftuLen);
			
			if(CollectionHelper.isEmpty(sdList)) {
				return sdCVec;
			}
			
			for(Integer sdId : sdList) {
				sdCVec = VecTools.vecAdd(sdCVec, Cons.cMatrix[sdId]);
			}
			
			return sdCVec;
		}
		
		private Double getProbVal(Double[] sumC, Integer recId) {
			
			Double[] recUVec = Cons.uMatrix[recId];
			Double ucMult = VecTools.vecMultiply(recUVec, sumC);
			
			return 1.-Math.exp(-Cons.beta*ucMult);
		}
		
		private Integer getNumber(String val) {
			
			if(val.contains("*")) {
				return Integer.parseInt(val.substring(0, val.length()-1));
			}
			
			return Integer.parseInt(val);
		}
		
		private void calcPosCases(Integer calcId) {
			
			if(Cons.posNCasInfo.containsKey(calcId)) {
				Map<String, Integer> snInfos = Cons.posNCasInfo.get(calcId);
				for(Map.Entry<String, Integer> infos : snInfos.entrySet()) {
					List<Integer> sidList = new ArrayList<Integer>();
					String[] snIndices = infos.getKey().split("-");
					if(snIndices.length<1) {
						continue;
					}
					for(String val : snIndices) {
						if(getNumber(val)<0) {
							continue;
						}
						sidList.add(getNumber(val));
					}
					if(sidList.size()<1) {
						continue;
					}
					Double[] sumC = getSumC(sidList);
					Double probVal = getProbVal(sumC, calcId);
					if(probVal>.0001){
						//calculate delta U in chained conditional probability when msg received node is activated
						dU[calcId] = VecTools.vecAdd(dU[calcId], VecTools.dotMultiply(infos.getValue()*Cons.beta*(1-probVal)/probVal, sumC));
						//-calculate delta C in chained conditional probability when msg received node is activated
						int len = sidList.size()-1;
						for(int i=len; i>=Math.max(0,len-Cons.paramL); i--) {
							dC[sidList.get(i)] = VecTools.vecAdd(dC[sidList.get(i)], VecTools.dotMultiply(infos.getValue()*Cons.beta*(1-probVal)/probVal, Cons.uMatrix[calcId]));
						}
						//-end of delta C calculated
						//end of delta U calculated
					}
					//calculate delta U in chained conditional probability when msg received node is activated
					if(sidList.size()>1) {
						int len = sidList.size()-1;
						for(int i=len; i>=1; i--) {
							List<Integer> subList = sidList.subList(Math.max(0, i-Cons.paramL-1),i);
							if(CollectionHelper.isEmpty(subList)) {
								continue;
							}
							//-calculate delta C in chained conditional probability when msg received node is activated
							for(Integer sid : subList) {
								dC[sid] = VecTools.vecAdd(dC[sid], VecTools.dotMultiply(-infos.getValue()*Cons.beta, Cons.uMatrix[calcId]));
							}
							//-end of delta C calculated
							sumC = getSumC(subList);
							dU[calcId] = VecTools.vecAdd(dU[calcId], VecTools.dotMultiply(-infos.getValue()*Cons.beta, sumC));
						}
					}


					//end of delta U calculated
				}
			}
		}
		
		private void calcNegCases(Integer calcId) {
			
			if(Cons.negNCasInfo.containsKey(calcId)) {
				Map<String, Integer> snInfos = Cons.negNCasInfo.get(calcId);
				for(Map.Entry<String, Integer> infos : snInfos.entrySet()) {
					List<Integer> sidList = new ArrayList<Integer>();
					String[] snIndices = infos.getKey().split("-");
					if(snIndices.length<1) {
						continue;
					}
					for(String val : snIndices) {
						if(getNumber(val)<0) {
							continue;
						}
						sidList.add(getNumber(val));
					}
					if(sidList.size()<1) {
						continue;
					}
					//calculate delta U in chained conditional probability when msg received node is activated
					int len = sidList.size();
					for(int i=len; i>=1; i--) {
						List<Integer> subList = sidList.subList(Math.max(0, i-Cons.paramL-1),i);
						if(CollectionHelper.isEmpty(subList)) {
							continue;
						}
						//-calculate delta C in chained conditional probability when msg received node is activated
						for(Integer sid : subList) {
							dC[sid] = VecTools.vecAdd(dC[sid], VecTools.dotMultiply(-infos.getValue()*Cons.beta, Cons.uMatrix[calcId]));
						}
						//-end of delta C calculated
						Double[] sumC = getSumC(subList);
						dU[calcId] = VecTools.vecAdd(dU[calcId], VecTools.dotMultiply(-infos.getValue()*Cons.beta, sumC));
					}
					//end of delta U calculated
				}
			}
		}
		
		private void calcDeltaUC(Integer calcId) {
			
			calcPosCases(calcId);
			calcNegCases(calcId);
			//minimize ||U[calcId]||*||C[calcId]|| - VecTools.vecMultiply(U[calcId],C[calcId]) s.t. let Ui and Ci to be similar in direction
			//derivative to Ui is Ui*||Ci||/||Ui|| - Ci
			Double UiNorm = VecTools.l2Norm(Cons.uMatrix[calcId]);
			Double CiNorm = VecTools.l2Norm(Cons.cMatrix[calcId]);
			if (UiNorm*CiNorm != 0) {
				Double[] gradUi = VecTools.vecAdd(VecTools.dotMultiply(CiNorm / UiNorm, Cons.uMatrix[calcId]), VecTools.dotMultiply(-1.0, Cons.cMatrix[calcId]));
				Double[] gradCi = VecTools.vecAdd(VecTools.dotMultiply(UiNorm / CiNorm, Cons.cMatrix[calcId]), VecTools.dotMultiply(-1.0, Cons.uMatrix[calcId]));
				dU[calcId] = VecTools.vecAdd(dU[calcId], VecTools.dotMultiply(Cons.phi, gradUi));
				dC[calcId] = VecTools.vecAdd(dC[calcId], VecTools.dotMultiply(Cons.phi, gradCi));
			}

		}
		
		private Integer missionDispatcher() {
			
			synchronized(missionNodes) {
				if(!CollectionHelper.isEmpty(missionNodes)) {
					return missionNodes.remove(0);
				}
			}
			
			return -1;
		}
		
		private void getCurrentLoss(Integer calcId) {
			
			if(Cons.posNCasInfo.containsKey(calcId)) {
				Map<String, Integer> snInfos = Cons.posNCasInfo.get(calcId);
				for(Map.Entry<String, Integer> infos : snInfos.entrySet()) {
					List<Integer> sidList = new ArrayList<Integer>();
					String[] snIndices = infos.getKey().split("-");
					if(snIndices.length<1) {
						continue;
					}
					for(String val : snIndices) {
						if(getNumber(val)<0) {
							continue;
						}
						sidList.add(getNumber(val));
					}
					Double[] sumC = getSumC(sidList);
					Double probVal = getProbVal(sumC, calcId);
					// To comment the synchronize, the algorithm can be speeded up
					// , however, the loss calculation may be inaccurate somehow.
					synchronized(Cons.lossEpoch) {
						Cons.lossEpoch[curEpoch] += infos.getValue()*Math.log(probVal+10e-7);
					}
					//calculate delta U in chained conditional probability when msg received node is activated
					if(sidList.size()>1) {
						int len = sidList.size()-1;
						for(int i=len; i>=1; i--) {
							List<Integer> subList = sidList.subList(Math.max(0, i-Cons.paramL-1),i);
							if(CollectionHelper.isEmpty(subList)) {
								continue;
							}
							sumC = getSumC(subList);
							// To comment the synchronize, the algorithm can be speeded up
							// , however, the loss calculation may be inaccurate somehow.
							synchronized(Cons.lossEpoch) { 
								Cons.lossEpoch[curEpoch] -= infos.getValue()*Cons.beta*VecTools.vecMultiply(Cons.uMatrix[calcId], sumC);
							}
						}
					}
					//end of delta U calculated
				}
			}
			
			if(Cons.negNCasInfo.containsKey(calcId)) {
				Map<String, Integer> snInfos = Cons.negNCasInfo.get(calcId);
				for(Map.Entry<String, Integer> infos : snInfos.entrySet()) {
					List<Integer> sidList = new ArrayList<Integer>();
					String[] snIndices = infos.getKey().split("-");
					if(snIndices.length<1) {
						continue;
					}
					for(String val : snIndices) {
						if(getNumber(val)<0) {
							continue;
						}
						sidList.add(getNumber(val));
					}
					//calculate delta U in chained conditional probability when msg received node is activated
					int len = sidList.size();
					for(int i=len; i>=1; i--) {
						List<Integer> subList = sidList.subList(Math.max(0, i-Cons.paramL-1),i);
						if(CollectionHelper.isEmpty(subList)) {
							continue;
						}
						Double[] sumC = getSumC(subList);
						// To comment the synchronize, the algorithm can be speeded up
						// , however, the loss calculation may be inaccurate somehow.
						synchronized(Cons.lossEpoch) {
							Cons.lossEpoch[curEpoch] -= infos.getValue()*Cons.beta*VecTools.vecMultiply(Cons.uMatrix[calcId], sumC);
						}
					}
					//end of delta U calculated
				}
			}

			synchronized (Cons.lossEpoch){
				Double UiNorm = VecTools.l2Norm(Cons.uMatrix[calcId]);
				Double CiNorm = VecTools.l2Norm(Cons.cMatrix[calcId]);
				Cons.lossEpoch[curEpoch] += 0.5*Cons.lbda*(1+Cons.mu)*(UiNorm+CiNorm)+Cons.phi*(UiNorm*CiNorm-VecTools.vecMultiply(Cons.uMatrix[calcId],Cons.cMatrix[calcId]));
			}
		}
		
		private void missionOver() {
			
			AdditiveInfModel_beta.missionOver();
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			
			while(isTraining && !CollectionHelper.isEmpty(missionNodes)) {
				Integer calNode = missionDispatcher();
				if(calNode==-1) {
					continue;
				}
				calcDeltaUC(calNode);
			}
			
			while(!isTraining && !CollectionHelper.isEmpty(missionNodes)) {
				Integer calNode = missionDispatcher();
				if(calNode!=-1) {
					getCurrentLoss(calNode);
				}
			}
			
			missionOver();
		}
	}
	
	public static void main(String[] args) {
		
		if(args.length<1) {
			System.out.println("Please input parameter like [configuration file path]!");
			return;
		}
		
		AdditiveInfModel_beta nb = new AdditiveInfModel_beta(args[0]);
		try {
			nb.learningProc();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
