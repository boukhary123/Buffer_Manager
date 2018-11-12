/* File LRU.java */

package bufmgr;

import diskmgr.*;
import global.*;
import java.lang.*;
import java.util.*;



class history{

  private HashMap<Integer, Long[]> Hist;
  private HashMap<Integer, Long> Last;
  public int Correlated_Reference_Period=0;
  private int length;

  public history(int k){

      Hist = new HashMap<Integer, Long[]>();
      Last = new HashMap<Integer, Long>();
      length = k;
  }
  public Long get_k_access(int p, int k){
    return Hist.get(p)[k];
  }

  public Long get_last_access(int p){
    return Last.get(p);
  }
  public void set_k_access(int p,int k,Long val){
    Long tmp[];
    tmp=Hist.get(p);
    tmp[k]=val;
    Hist.put(p,tmp);
  }
  public void update_last_access(int p){
    Last.put(p,System.currentTimeMillis());
  }

  public void update_last_access_hist(int p){
    Long tmp[];
    tmp=Hist.get(p);
    tmp[0]=System.currentTimeMillis();
    Hist.put(p,tmp);
  }
  public boolean is_page_present(int p){
    // check if a paricular page is present in history
    return Hist.containsKey(p);
  }
  public void updating_hist(int p){

    Long tmp[];
    tmp=Hist.get(p);
    for(int i=1;i<length;i++){
      tmp[i]=tmp[i-1];
    }
    Hist.put(p,tmp);
  }
  public void updating_hist_const(int p, Long constant){
    Long tmp[];
    tmp=Hist.get(p);
    for(int i=1;i<length;i++){
      tmp[i]=tmp[i-1]+constant;
    }
    Hist.put(p,tmp);
  }
  public void allocate_block(int p){
    Long tmp[] = new Long[length];
    Arrays.fill(tmp,0L);
    Hist.put(p,tmp);
  }
}



public class LRUK extends  Replacer {

  /**
   * private field
   * An array to hold number of frames in the buffer pool
   */

    private int  frames[];

  /**
   * private field
   * number of frames used
   */
  private int  nframes;
  private int lastRef;
  // varibale that stores hist and last data structures
  // variable used to track page id that is calling pin or
  // pick_victim
  public int pageid;
  public int already_in_buffer;
  public history HIST;


  /**
   * This pushes the given frame to the end of the list.
   * @param frameNo	the frame number
   */
  private void update(int frameNo)
  {

    if (already_in_buffer==0){
      // page not in buffer
    if (HIST.is_page_present(pageid)){
      // page is present in history
      HIST.updating_hist(pageid);
    }
    else{
      HIST.allocate_block(pageid);
    }
    HIST.update_last_access(pageid);
    HIST.update_last_access_hist(pageid);
  }

  else{
    // page in buffer
    // update history information of p
    Long t = System.currentTimeMillis();
    int p = frames[frameNo];
    Long correl_period_of_refd_page = 0L;
    if((t-HIST.get_last_access(p)> HIST.Correlated_Reference_Period)){
      // new uncorrelated reference
      correl_period_of_refd_page = HIST.get_last_access(p)-HIST.get_k_access(p,0);
      HIST.updating_hist_const(p,correl_period_of_refd_page);
      HIST.update_last_access(p);
      HIST.update_last_access_hist(p);

    }
    else{
      // correlated reference
      HIST.update_last_access(p);
    }

  }
  }

  /**
   * Calling super class the same method
   * Initializing the frames[] with number of buffer allocated
   * by buffer manager
   * set number of frame used to zero
   *
   * @param	mgr	a BufMgr object
   * @see	BufMgr
   * @see	Replacer
   */
    public void setBufferManager( BufMgr mgr )
     {
        super.setBufferManager(mgr);
	      frames = new int [ mgr.getNumBuffers() ];
	      nframes = 0;

     }

/* public methods */

  /**
   * Class constructor
   * Initializing frames[] pinter = null.
   */
    public LRUK(BufMgr mgrArg,int k)
    {
      super(mgrArg);
      frames = null;
      pageid=-1;
      already_in_buffer=0;
      lastRef=k;
      HIST = new history(lastRef);

    }

  /**
   * calll super class the same method
   * pin the page in the given frame number
   * move the page to the end of list
   *
   * @param	 frameNo	 the frame number to pin
   * @exception  InvalidFrameNumberException
   */
 public void pin(int frameNo) throws InvalidFrameNumberException
 {
    super.pin(frameNo);
    update(frameNo);

 }

  /**
   * Finding a free frame in the buffer pool
   * or choosing a page to replace using LRU policy
   *
   * @return 	return the frame number
   *		return -1 if failed
   */
 public int pick_victim()
 throws BufferPoolExceededException
 {
   int numBuffers = mgr.getNumBuffers();
   int frame=-1;

    if ( nframes < numBuffers ) {
      // buffer is not full
        frame = nframes++;
        // System.out.println(frame);
        frames[frame] = pageid;
        state_bit[frame].state = Pinned;
        // System.out.println(state_bit[frame].state);
        (mgr.frameTable())[frame].pin();
        update(0);
        return frame;
    }
    // buffer is full
    Long min;
    Long t = System.currentTimeMillis();
    min=t;
    int victim;
    int q;
    for ( int i = 0; i < numBuffers; ++i ) {
         q = frames[i];
        if ( state_bit[i].state != Pinned ) {
          if((t-HIST.get_last_access(q))>
          HIST.Correlated_Reference_Period && HIST.get_k_access(q,0) < min){
          // sSystem.out.println("State is pinned");
          victim = q;
          frame = i;
          min =  HIST.get_k_access(q,0);

          }

            // state_bit[frame].state = Pinned;
            // (mgr.frameTable())[frame].pin();
            // update(0);
            // return frame;
        }
        if (frame>=0){
        state_bit[frame].state = Pinned;
        (mgr.frameTable())[frame].pin();
        update(0);
        return frame;
      }
    }
    // No victims found!!
    throw new BufferPoolExceededException (null, "BUFMGR: BUFFER_EXCEEDED.");
    // return -1;
 }

  /**
   * get the page replacement policy name
   *
   * @return	return the name of replacement policy used
   */
    public String name() { return "LRUK"; }

    public int[] getFrames(){
      return frames;
    }

    public Long get_history_access(int p,int k){
      return HIST.get_k_access(p,k);
    }
  /**
   * print out the information of frame usage
   */
 public void info()
 {
    super.info();

    System.out.print( "LRUK REPLACEMENT");

    for (int i = 0; i < nframes; i++) {
        if (i % 5 == 0)
	System.out.println( );
	System.out.print( "\t" + frames[i]);

    }
    System.out.println();
 }

}
