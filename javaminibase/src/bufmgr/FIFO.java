/* File FIFO.java */

package bufmgr;

import diskmgr.*;
import global.*;

  /**
   * class FIFO is a subclass of class Replacer using FIFO
   * algorithm for page replacement
   */
class FIFO extends  Replacer {

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

  /**
   * This pushes the given frame to the end of the list.
   * @param frameNo	the frame number
   */
  private void update(int frameNo)
  {
     int index;
     for ( index=0; index < nframes; ++index )
        if ( frames[index] == frameNo )
            break;
      /*
     *move all the frames forward in the queue to fill the place of the freed frame
     *place the freed frame on the tail of the queue
     */
    while ( ++index < nframes )
        frames[index-1] = frames[index];
        frames[nframes-1] = frameNo;
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
    public FIFO(BufMgr mgrArg)
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

 }

  /**
   * Finding a free frame in the buffer pool
   * or choosing a page to replace using FIFO policy
   *
   * @return 	return the frame number
   *		throws BufferPoolExceededException if failed
   */

 public int pick_victim()
		 throws BufferPoolExceededException
 {
   int numBuffers = mgr.getNumBuffers();
   int frame;
     
    if ( nframes < numBuffers ) {
        /*
     *the buffer is not full
     *we add the page to the end of the buffer pool
     *by returning the corresponding free frame
     */
        frame = nframes++;
        frames[frame] = frame;
        state_bit[frame].state = Pinned;
        (mgr.frameTable())[frame].pin();
        return frame;
    }
     /*
     *the buffer is full
     *we pick the first unpinned page in the buffer pool
     *following a FIFO manner
     */
    for ( int i = 0; i < numBuffers; ++i ) {
         frame = frames[i];
        if ( state_bit[frame].state != Pinned ) {
            state_bit[frame].state = Pinned;
            (mgr.frameTable())[frame].pin();
            /*
             *we update the frames queue
             *by moving the frame of the victim page
             *and placing it to the end of the array
             *i.e. the tail of the queue
             */
            update(frame);
            return frame;
        }
    }

    //return -1;
	  throw new BufferPoolExceededException (null, "BUFMGR: BUFFER_EXCEEDED.");

 }

  /**
   * get the page replacement policy name
   *
   * @return	return the name of replacement policy used
   */
    public String name() { return "FIFO"; }

  /**
   * print out the information of frame usage
   */
 public void info()
 {
    super.info();

    System.out.print( "FIFO REPLACEMENT");

    for (int i = 0; i < nframes; i++) {
        if (i % 5 == 0)
	System.out.println( );
	System.out.print( "\t" + frames[i]);

    }
    System.out.println();
 }

}
