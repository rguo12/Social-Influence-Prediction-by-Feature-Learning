package alg.lis;

import alg.lis.cons.Cons;
import alg.lis.utils.CollectionHelper;
import alg.lis.utils.FileUtil;
import alg.lis.utils.MatrixTools;
import alg.lis.utils.VecTools;

import javax.swing.text.html.HTMLDocument;
import java.awt.geom.Arc2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rguo12 on 11/14/16.
 */
public class ModelPerformance {
    //read U and C from lis_uMatrix_xx and lis_cMatrix_xx
    //read 'testing' samples from the toy.dat
    //then we can modify getCurLoss a lil bit to do the 'classification'

    private DataInitiator initiator = new DataInitiator();
    private Map<String, String> config = new HashMap<String, String>();
    private static List<Integer> missionNodes = new ArrayList<Integer>();
    private static Integer missionOver = 0;
    private Boolean hasStopped = false;

    public ModelPerformance(String confPath){
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

    private void initialData(String confPath) throws IOException {

        setBasicConfig(confPath);
        System.out.println("Get configuration!");

        //Cons.uMatrix = initiator.getUniPolRandomMatrix(Cons.nodeSize, Cons.ftuLen, .3);
        //Cons.cMatrix = initiator.getUniPolRandomMatrix(Cons.nodeSize, Cons.ftuLen, .3);

        //System.out.println("Initialize matrices!");

        System.out.println("Start to fetch cascade date from file "+Cons.trainDatFile);
        initiator.setNodeCascadeInfo(Cons.trainDatFile);
        System.out.println("===========Complete getting cascade date, and be ready for testing===========");

    }

    private void setBasicConfig(String confPath) {
        try {
            config = getConfParams(confPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(config!=null && !config.isEmpty()) {
            Cons.trainDatFile = config.get("test_dat_file");
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

            Cons.moreNeg = Integer.parseInt(config.get("moreNeg"));
            Cons.neg = Integer.parseInt(config.get("neg"));
            Cons.phi = Double.parseDouble(config.get("phi"));
            Cons.probThres = Double.parseDouble(config.get("probThres"));

            if(config.get("has_stopped").equalsIgnoreCase("false")) {
                hasStopped = false;
            } else if(config.get("has_stopped").equalsIgnoreCase("true")) {
                hasStopped = true;
            }
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

    public void testingProcess() throws IOException{
        for (int i = 1; i*100 <= Cons.maxEpoch; i++){
            System.out.println("===========Loading the U and C matrices for epoch "+100*i+"=========");
            String uMatrixPath = Cons.resFilePathRoot+"_uMatrix_"+100*i;
            String cMatrixPath = Cons.resFilePathRoot+"_cMatrix_"+100*i;
            initiator.setUCMatrix(uMatrixPath,cMatrixPath);
            oneTurnTesting(i*100,Cons.threadNum);
        }
    }

    public void oneTurnTesting(Integer epoch, Integer threadNum) throws IOException{

        //Distribute the tasks over nodes
        initMissionNodes();
        getPred(Cons.threadNum,Cons.threadSleepSec);
        writeTableDouble(Cons.yPred,epoch);
        writeTableInt(Cons.yTrue,epoch);

    }

    private void writeTableDouble(Hashtable<Integer,ArrayList<Double>> myHashtable,Integer epoch) throws IOException{

        OutputStreamWriter osw = FileUtil.getOutputStreamWriter(Cons.resFilePathRoot+"probVals"+epoch);
        Set<Integer> keys = myHashtable.keySet();
        for (Integer key:keys){
            String wLine = "";
            Integer calcId = key;
            wLine += calcId + "|";
            ArrayList<Double> probVals = myHashtable.get(key);
            for (int i = 0; i < probVals.size(); i++) {
                //System.out.println(probVals.get(i));

                if (i==probVals.size()-1){
                    wLine += probVals.get(i);
                }
                else{
                    wLine+=probVals.get(i)+",";
                }
            }

            FileUtil.writeln(osw,wLine);

        }

        osw.close();
    }

    private void writeTableInt(Hashtable<Integer,ArrayList<Integer>> myHashtable,Integer epoch) throws IOException{

        OutputStreamWriter osw = FileUtil.getOutputStreamWriter(Cons.resFilePathRoot+"labels"+epoch);
        Set<Integer> keys = myHashtable.keySet();
        for (Integer key:keys){
            String wLine = "";
            Integer calcId = key;
            wLine += calcId + "|";
            ArrayList<Integer> probVals = myHashtable.get(key);
            for (int i = 0; i < probVals.size(); i++) {
                //System.out.println(probVals.get(i));

                if (i==probVals.size()-1){
                    wLine += probVals.get(i);
                }
                else{
                    wLine+=probVals.get(i)+",";
                }
            }

            FileUtil.writeln(osw,wLine);

        }

        osw.close();
    }



    public void writeProb() throws IOException{

    }

    private void getPred(Integer threadNum, Double sleepSec) {

        initMissionNodes();//multi-thread mission initiator

        ExecutorService exec = Executors.newCachedThreadPool();
        for (int i = 0; i < threadNum; i++) {
            exec.execute(new GetPrediction());
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

    class GetPrediction implements Runnable{

        //private boolean isTraining = true;
        //private Integer curEpoch = null;

        GetPrediction(){

        }

        private Integer getNumber(String val) {

            if(val.contains("*")) {
                return Integer.parseInt(val.substring(0, val.length()-1));
            }

            return Integer.parseInt(val);
        }


        private void getPredictionUC(Integer calcId){

            ArrayList<Double> probVals = new ArrayList<Double>();
            ArrayList<Integer>labels = new ArrayList<Integer>();


            if (Cons.posNCasInfo.containsKey(calcId)){
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

                    probVals.add(probVal);
                    labels.add(1);


                    if(sidList.size()>1 && Cons.moreNeg > 0) {
                        int len = sidList.size()-1;
                        for(int i=len; i>=1; i--) {
                            List<Integer> subList = sidList.subList(Math.max(0, i-Cons.paramL-1),i);
                            if(CollectionHelper.isEmpty(subList)) {
                                continue;
                            }
                            //-calculate delta C in chained conditional probability when msg received node is activated

                            //-end of delta C calculated
                            sumC = getSumC(subList);
                            probVal = getProbVal(sumC,calcId);
                            probVals.add(probVal);
                            labels.add(0);

                        }
                    }
                }
            }

            if (Cons.negNCasInfo.containsKey(calcId) && Cons.neg > 0){
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
                    Double[] sumC = getSumC(sidList);
                    Double probVal = getProbVal(sumC, calcId);

                    probVals.add(probVal);
                    labels.add(0);


                    if(sidList.size()>1 && Cons.moreNeg > 0) {
                        int len = sidList.size()-1;
                        for(int i=len; i>=1; i--) {
                            List<Integer> subList = sidList.subList(Math.max(0, i-Cons.paramL-1),i);
                            if(CollectionHelper.isEmpty(subList)) {
                                continue;
                            }
                            //-calculate delta C in chained conditional probability when msg received node is activated

                            //-end of delta C calculated
                            sumC = getSumC(subList);
                            probVal = getProbVal(sumC,calcId);
                            probVals.add(probVal);
                            labels.add(0);

                        }
                    }
                }

            }
            synchronized (Cons.yPred) {
                Cons.yPred.put(calcId, probVals);
            }
            synchronized (Cons.yTrue){
                Cons.yTrue.put(calcId, labels);
            }
        }

        private Double getProbVal(Double[] sumC, Integer recId) {

            Double[] recUVec = Cons.uMatrix[recId];
            Double ucMult = VecTools.vecMultiply(recUVec, sumC);

            return 1.-Math.exp(-Cons.beta*ucMult);
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

        private Integer missionDispatcher() {

            synchronized(missionNodes) {
                if(!CollectionHelper.isEmpty(missionNodes)) {
                    return missionNodes.remove(0);
                }
            }

            return -1;
        }

        private void missionOver() {

            ModelPerformance.missionOver();
        }


        public void run() {
            while(!CollectionHelper.isEmpty(missionNodes)) {
                Integer calNode = missionDispatcher();
                if(calNode==-1) {
                    continue;
                }
                getPredictionUC(calNode);
            }
            missionOver();
        }


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


    public static void main(String[] args) {

        if(args.length<1) {
            System.out.println("Please input parameter like [configuration file path]!");
            return;
        }

        ModelPerformance mp = new ModelPerformance(args[0]);
        try {
            mp.testingProcess();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
