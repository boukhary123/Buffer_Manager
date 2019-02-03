# Database Management System Buffer Manager

## Introduction
One of the crucial task in database management is reading and writing pages from memory. Selection of subset of pages present in the memory is regarded as the buffer-pool. Buffer pool is organized into frames, where each frame holds a page from the disk. The buffer-manager is implemented to manage the allocation (pinning) and deallocation (unpinning) of pages from disk to the main memory and vice versa. Buffer manager maintains a pin counter, which stores the number of times a page is requested (but not released), and a dirty flag, which marks whether or not a page is updated. One important task of the buffer manager is to deploy a page replacement policy, when the buffer is full. Replacement policy is responsible for selecting the page to be flushed from the buffer-pool. Techniques like Least Recently Used [LRU], Mostly Recently Used [MRU], and Clock identify pages from the buffer-pool to be written to disk.

## Description
In this project, we implemented three different page replacement policies for buffer manager in [Minibase](http://research.cs.wisc.edu/coral/mini_doc/minibase.html), namely
1. First-In, First-Out [FIFO] is one of the elementary techniques used to carry-out page replacement in the buffer. It is a low-overhead algorithm with little bookkeeping that uses a queue to maintain the order in which pages are accessed in memory. When a page is required to be replaced, the page in the front of the queue is selected to be flushed to disk.
2. Last-In, First-out [LIFO] is a technique that works similar to FIFO but the page to be replaced is selected from the back of the queue.
3. Least Recently Used - k Reference [LRU-K] is a technique that keeps track of the number of times each page is request in the last k-references to pages in buffer pool.
The first two replacement policies are not discriminating against different pages whereas LRU-K discriminates between pages that are frequently and infrequently accessed in last k-references (k is a user supplied value). The pseudocode for this technique can be found in [1].

The code pieces that were worked on or edited are:

- bufmgr class in [BufMgr.java](https://github.com/boukhary123/Buffer_Manager/blob/master/javaminibase/src/bufmgr/BufMgr.java)
- FIFO class in [FIFO.java](https://github.com/boukhary123/Buffer_Manager/blob/master/javaminibase/src/bufmgr/FIFO.java)
- LIFO class in [LIFO.java](https://github.com/boukhary123/Buffer_Manager/blob/master/javaminibase/src/bufmgr/LIFO.java)
- LRUK class in [LRUK.java](https://github.com/boukhary123/Buffer_Manager/blob/master/javaminibase/src/bufmgr/LRUK.java)
- history class in [LRUK.java](https://github.com/boukhary123/Buffer_Manager/blob/master/javaminibase/src/bufmgr/LRUK.java)

## bufmgr class in BufMgr.java

- The constructor of this class is supposed to have a third integer argument called lastRef. The latter argument is related to the LRUK replacement algorithm and causes a conflict with the other pieces of code that we shouldn’t edit. Therefore, it was not added to the bufmgr constructor.
- We added three new cases (for FIFO,LIFO, and LRUK ) to the bufmgr class constructor and in the case of LRUK , lastRef = 2 was inserted into its object creation.

## FIFO class in FIFO.java

- As the other replacers, FIFO inherits from Replacer class.
- In this replacer, the frames array is like a queue where the index 0 is the head of the queue.
- When we want to pick a victim frame for a page that is not in the buffer
    - In the case where the buffer is not full we just choose and return the first empty frame in the array (the tail of the queue)
    - In the case where the buffer is full, we return the first frame from the head in which the
page in it is unpinned.
- In the latter case we move all the other frames forward towards the head of the queue to fill the place of the chosen frame and then move the chosen from to the tail of the queue.

## LIFO class in LIFO.java


- As the other replacers, LIFO inherits from Replacer class.
- In this replacer, the frames array is like a stack where the index 0 is the top of the stack.
- When we want to pick a victim frame for a page that is not in the buffer.
    - In the case where the buffer is not full we just choose and return the first empty frame in the array (the bottom of the stack)/empty frames have a negative value
    - In the case where the buffer is full, we return the first frame from the top in which the page in it is unpinned.
- In only these cases, we move all the other frames downwards away from the top of the stack to fill the place of the chosen frame and then move the chosen frame to the top of the stack at index 0.

## history class in LRUK.java

- This class consists of all the parameters and the operations required to store, manipulate and utilize all the required history information.
- The actual history is represented as a hash map called Hist where the keys are the page IDs and the values are arrays representing the last K time instances in which the page was referenced.
- The other hash map called Last also has page IDs as keys and each value is the timestamp in which the corresponding page is last called for.
- Last may not be found in Hist due to the Correlated_Reference_Period
- The hash map helps organize the pages’ history and is very efficient for this application.

## LRUK class in LRUK.java

- The replacement code is based on the pseudo-code in the paper [1].
- In this class we have several additional properties which are:
    - lastRef which holds the value of K (as in last K accesses of a page)
    - pageid which is the ID of the page which is currently being called for. This allows us to build the history one page at a time.
    - HIST object which is an instantiation of the history class to keep track of the history parameters
    - already_in_buffer integer that keeps track of the status of the page having ID pageid.
- If the page is in the buffer, the update(frameNo) is called to update its Hist and Last information.
- In the process of picking a victim, if the buffer is not full, the first free frame is returned and the history for that page is generated.
- If the buffer is full, we look for a page q that has the oldest Hist(q,K) and the time passed since its last reference List(q) is greater than Correlated_Reference_Period
- Then the update function will be called to either generate a new Hist and Last blocks for the new page, or just update these two pieces of information in the case where the page was referenced in some point in history.

## References
[1] [O’Neil, E., O’Neil, P., & Weikum, G. – The LRU-K page replacement algorithm for database disk buffering. – In SIGMOD, 1993.](http://www.cs.cmu.edu/~christos/courses/721-resources/p297-o_neil.pdf)