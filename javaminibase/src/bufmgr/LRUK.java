/* File LRU.java */

package bufmgr;

import diskmgr.*;
import global.*;
import java.lang.*;



class history{

  private Map<Integer, Long[]> Hist;
  private Map<Integer, Long> Last;
  public static final int Correlated_Reference_Period=100000;
  private int length;

  public history(){
      Map<Integer, Long[]> Hist = new HashMap<Integer, Long[]>();
      Map<Integer, Long[]> Last = new HashMap<Integer, Long>();
  }
  public int get_k_access(int p, int k){
    return Hist.get(p)[k];
  }

  public int get_last_access(int p){
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
    Hist.put(p,System.currentTimeMillis());
  }
  public void is_page_present(int p){
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
    Long tmp[];
    tmp = new Long[length];
    Hist.put(p,tmp);
  }
}



class LRUK extends  Replacer {

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
  private history hist;
  // variable used to track page id that is calling pin or
  // pick_victim
  public int pageid;

  public int already_in_buffer=0;


  /**
   * This pushes the given frame to the end of the list.
   * @param frameNo	the frame number
   */
  private void update(int frameNo)
  {
    //  int index;
    //  for ( index=0; index < nframes; ++index )
    //     if ( frames[index] == frameNo )
    //         break;
    //
    // while ( ++index < nframes )
    //     frames[index-1] = frames[index];
    //     frames[nframes-1] = frameNo;
    if (already_in_buffer==0){
      // page not in buffer
    if (HIST.is_page_present(pageid)){
      // page is present in history
      HIST.updating_hist(pageid);
    }
    else{
      HIST.allocate_block(pageid);
    }
    HIST.update_last_access(p);
    HIST.update_last_access_hist(p);
  }

  else{
    // page in buffer
    // update history information of p
    Long t = System.currentTimeMillis();
    int p = frames[frameNo];
    Long correl_period_of_refd_page = 0;
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
        pageid=-1;
        already_in_buffer=-1;
        lastRef=k;
        hist= new history();

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
 {
   int numBuffers = mgr.getNumBuffers();
   int frame;

    if ( nframes < numBuffers ) {
      // buffer is not full


        frame = nframes++;
        frames[frame] = pageid;
        state_bit[frame].state = Pinned;
        (mgr.frameTable())[frame].pin();
        update(0);
        return frame;
    }
    // buffer is full
    Long min = System.currentTimeMillis();
    Long t = System.currentTimeMillis();
    int victim;
    for ( int i = 0; i < numBuffers; ++i ) {
         q = frames[i];
        if ( state_bit[frame].state != Pinned ) {

          if((t-HIST.get_last_access(q))>
          HIST.Correlated_Reference_Period & HIST.get_k_access(q,0) < min){
          victim = q;
          frame = i;
          min =  HIST.get_k_access(q,0);

          }
            state_bit[frame].state = Pinned;
            (mgr.frameTable())[frame].pin();
            update(0);
            return frame;
        }
    }

    return -1;
 }

  /**
   * get the page replacement policy name
   *
   * @return	return the name of replacement policy used
   */
    public String name() { return "LRUK"; }

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
