package pomdp.utilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * 普通算法也可以照常使用 在调用它之前已经确保，b不在这里存在
 * 
 * @author default
 * 
 * @param <E>
 */
public class BeliefStateVector<E> extends Vector<E> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2174749846857971924L;

	/*
	 * 0：根 1：第一层 2：第二层
	 */
	private ArrayList<ArrayList<E>> treeLevelInfo = null;

	/*
	 * 如果只存放新的b，那么完整的tree信息，就放在这
	 */
	private ArrayList<ArrayList<E>> treeLevelInfoComplete = null;
	private boolean notComplete = false;

	public BeliefStateVector() {
		super();
		treeLevelInfo = new ArrayList<ArrayList<E>>();
	}

	public BeliefStateVector(Vector<E> vector) {
		super(vector);
	}

	public BeliefStateVector(BeliefStateVector<E> beliefStateVector) {
		super(beliefStateVector);
		ArrayList<ArrayList<E>> treeLevelInfoIn = beliefStateVector.getTreeLevelInfo();
		treeLevelInfo = new ArrayList<ArrayList<E>>();
		for(int i = 0; i < treeLevelInfoIn.size(); i++){
			treeLevelInfo.add(new ArrayList<E>());
			treeLevelInfo.get(i).addAll(treeLevelInfoIn.get(i));
		}
		
	}

	/**
	 * 如果只存放新的b，那么完整的tree信息，就要额外存储
	 * 
	 * @param beliefStateVector
	 * @param notComplete
	 */
	public BeliefStateVector(BeliefStateVector<E> beliefStateVector,
			boolean notCompleteIn) {
		this();
		ArrayList<ArrayList<E>> treeLevelInfoIn = beliefStateVector.getTreeLevelInfo();
		treeLevelInfoComplete = new ArrayList<ArrayList<E>>();
		for(int i = 0; i < treeLevelInfoIn.size(); i++){
			treeLevelInfoComplete.add(new ArrayList<E>());
			treeLevelInfoComplete.get(i).addAll(treeLevelInfoIn.get(i));
		}
		for (int i = 0; i < treeLevelInfoComplete.size(); i++) {
			treeLevelInfo.add(new ArrayList<E>());
		}
		notComplete = true;
	}

	/**
	 * 增加一个b时，带入parent信息 若为根，则parent为null 如果b已经存在，则不做处理
	 * 
	 * @param parent
	 * @param current
	 * @return
	 */
	public synchronized void add(E parent, E current) {
		super.add(current);
		// 为根
		if (parent == null) {
			treeLevelInfo.clear();
			ArrayList<E> levelOne = new ArrayList<E>();
			treeLevelInfo.add(levelOne);
			levelOne.add(current);
		}
		// 非根
		else {
			int pLevelNum = getLevelNum(parent);
			if (pLevelNum < 0) {
				Logger.getInstance()
						.logln("Error: BeliefStateVector.add(E parent, E current): parent not exist!");
				return;
			}
			// parent正常
			else {
				// parent的下一层放置current
				pLevelNum += 1;
				if (treeLevelInfo.size() <= pLevelNum) {
					ArrayList<E> newLevel = new ArrayList<E>();
					treeLevelInfo.add(newLevel);
					if (notComplete) {
						ArrayList<E> newLevel2 = new ArrayList<E>();
						treeLevelInfoComplete.add(newLevel2);
					}
				}
				treeLevelInfo.get(pLevelNum).add(current);
				if (notComplete)
					treeLevelInfoComplete.get(pLevelNum).add(current);
			}
		}
	}

	/**
	 * 默认调用这个方法，肯定就是notComplete==true
	 * 默认两个合并之后，就是完整的了
	 */
	@SuppressWarnings("unchecked")
	@Override
	public synchronized boolean addAll(Collection<? extends E> bsv) {
		// 为了支持普通vector的功能，所以做一个判断
		if (bsv instanceof BeliefStateVector<?>) {
			ArrayList<ArrayList<E>> treeLevelInfoIn = ((BeliefStateVector<E>) bsv)
					.getTreeLevelInfo();
			for (int i = 0; i < treeLevelInfoIn.size(); i++) {
				if (treeLevelInfo.size() <= i) {
					treeLevelInfo.add(new ArrayList<E>());
				}
				treeLevelInfo.get(i).addAll(treeLevelInfoIn.get(i));

			}
			notComplete = false;
		}
		return super.addAll(bsv);
	}

	/**
	 * 查询一个b的层数
	 * 
	 * @param e
	 * @return -1: not found
	 */
	public synchronized int getLevelNum(E e) {
		if (notComplete) {
			for (int i = 0; i < treeLevelInfoComplete.size(); i++) {
				if (treeLevelInfoComplete.get(i).contains(e))
					return i;
			}
		} else {
			for (int i = 0; i < treeLevelInfo.size(); i++) {
				if (treeLevelInfo.get(i).contains(e))
					return i;
			}
		}
		return -1;
	}

	public synchronized ArrayList<ArrayList<E>> getTreeLevelInfo() {
		return treeLevelInfo;
	}
	
	public synchronized ArrayList<ArrayList<E>> getTreeLevelInfoComplete() {
		return treeLevelInfoComplete;
	}

	/**
	 * 获得一个在tree上，从下往上扫描的iterator
	 * 
	 * @return
	 */
	public Iterator<E> getTreeDownUpIterator() {
		ArrayList<E> resultList = new ArrayList<E>();
		for (int i = treeLevelInfo.size() - 1; i >= 0; i--) {
			resultList.addAll(treeLevelInfo.get(i));
		}
		return resultList.iterator();
	}

	/**
	 * 肯定是complete的
	 * 特别的方法，为了从Vector中删除b的同时，从tree中也删除掉
	 */
	public void removeABeliefStateWithTree(E e) {
		super.remove(e);
		for (int i = 0; i < treeLevelInfo.size(); i++) {
			if (treeLevelInfo.get(i).contains(e))
				treeLevelInfo.get(i).remove(e);
		}
	}
}
