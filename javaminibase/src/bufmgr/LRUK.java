/* File LRUK.java */

package bufmgr;

import diskmgr.*;
import global.*;
import java.lang.*;
import java.util.*;


/* Class for history string in the LRUK Replacement Algorithms*/
class history{
    
 /*
  * The history of all pages
 * 	each page has an array of time instances indicating when it was called for
 */
  private HashMap<Integer, Long[]> Hist;
    
   /*
   * The time instances for the last accesses for all pages
   * This is to help the algorithm indicate whether a new page is called
   * before or after the correlated reference period
   */ 
  private HashMap<Integer, Long> Last;
    
   /*
   * This period in milliseconds identifies
   * whether two consecutive accesses of the same page are correlated or not
   */  
  public long Correlated_Reference_Period=0;
    
    /*
   * The number of references that we go back to "identical to lastRef variable"
   */ 
  private int length;

  public history(int k){

      Hist = new HashMap<Integer, Long[]>();
      Last = new HashMap<Integer, Long>();
      length = k;
  }
  
   /*
   * returns the Kth access in the page's history in ms
   */
  public Long get_k_access(int p, int k){
    return Hist.get(p)[k];
  }

   /*
   * returns the most recent access of the page in ms
   */ 
  public Long get_last_access(int p){
    return Last.get(p);
  }
  
   /*
   * sets the Kth access in the page's history in ms
   */ 
  public void set_k_access(int p,int k,Long val){
    Long tmp[];
    tmp=Hist.get(p);
    tmp[k]=val;
    Hist.put(p,tmp);
  }
  
  /*
   * updates the most recent access of the page in ms
   */
  public void update_last_access(int p){
    Last.put(p,System.currentTimeMillis());
  }

    /*
   * updates the latest history element for page p
   */
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
  
    /*
   * updates the history by moving all the elements forward in the history
   * to make room for the new entry
   */
  public void updating_hist(int p){

    Long tmp[];
    tmp=Hist.get(p);
    for(int i=1;i<length;i++){
      tmp[i]=tmp[i-1];
    }
    Hist.put(p,tmp);
  }
  
    /*
   * updates all the history for the page according to its own correlation period
   */
  public void updating_hist_const(int p, Long constant){
    Long tmp[];
    tmp=Hist.get(p);
    for(int i=1;i<length;i++){
      tmp[i]=tmp[i-1]+constant;
    }
    Hist.put(p,tmp);
  }
    
    /*
   * allocate a new block for the page not previously existing in the history
   */
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
    
  //The value of K in LUR-K Algorithm
  private int lastRef;
  
   /*The ID of the page being currently referenced
   * used to generate history one page at a time
   */
  public int pageid;
    
  public int already_in_buffer;
    
    //The object that stores the access history and last accesses of all pages
  private history HIST;


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
    if((t-HIST.get_last_access(p)>= HIST.Correlated_Reference_Period)){
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
   * Initializing frames[] pointer = null.
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
   * or choosing a page to replace using LRUK policy
   *
   * @return 	return the frame number
   *		throws BufferPoolExceededException if failed
   */
 public int pick_victim()
		 throws BufferPoolExceededException
 {
   int numBuffers = mgr.getNumBuffers();
   int frame=-1;

    if ( nframes < numBuffers ) {
      // buffer is not full
        frame = nframes++;
        frames[frame] = pageid;
        state_bit[frame].state = Pinned;
        (mgr.frameTable())[frame].pin();
        //generate or update history for the page
        update(0);
        return frame;
    }
    // buffer is full
    Long min;
    Long t = System.currentTimeMillis();
    min = t;
    int victim;
    int q;
    for ( int i = 0; i < numBuffers; ++i ) {
         q = frames[i];
        if ( state_bit[i].state != Pinned ) {
            /*
            *find the page with the oldest last K reference
            *and having its correlated reference period already
            *passed
            */
          if((t-HIST.get_last_access(q))>=
          HIST.Correlated_Reference_Period && HIST.get_k_access(q,lastRef-1) <= min){
          victim = q;
          frame = i;
          min =  HIST.get_k_access(q,lastRef-1);

          }

            // state_bit[frame].state = Pinned;
            // (mgr.frameTable())[frame].pin();
            // update(0);
            // return frame;
        }
        if (frame>=0){
        state_bit[frame].state = Pinned;
        (mgr.frameTable())[frame].pin();
        //generate or update history for the page
        update(0);
        return frame;
      }
    }

	  throw new BufferPoolExceededException (null, "BUFMGR: BUFFER_EXCEEDED.");
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
 
    /*
 *returns the frames in the buffer pool
 */
 public int[] getFrames() {
	 return frames;
 }
 
    /*
 *returns the timestamp of the access of index k
 */
 public long get_history_access(int p, int k) {
	 return HIST.get_k_access(p,k);
 }
 
 /*
 *returns the timestamp of the last access
 */
 public long get_last_access(int p) {
	 return HIST.get_last_access(p);
 }

}
