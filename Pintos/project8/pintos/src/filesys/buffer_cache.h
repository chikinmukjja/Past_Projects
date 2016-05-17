#include <list.h>
#include "threads/synch.h"
#include "devices/block.h"
#include "devices/ide.h"
#include <inttypes.h>
#include "threads/malloc.h"
#include <string.h>
#include "filesys/off_t.h"

struct buffer_head{


	struct inode* inode;
	bool dirty;  		// dirty flag
	bool used;		// used flag
	block_sector_t sector; // disk sector address of entry
	int clock_bit;		 // for clock algorithm
	struct lock buffer_lock;
	void *data;			 // point buffer cache space

};


bool bc_read(block_sector_t sector_idx, void *buffer, off_t bytes_read,
		int chunk_size, int sector_ofs);
bool bc_write(block_sector_t sector_idx, void *buffer, off_t bytes_written,
		int chunk_size, int sector_ofs);
void bc_init(void);
void bc_term(void);
struct buffer_head* bc_select_victim(void);
struct buffer_head* bc_lookup(block_sector_t sector);
void bc_flush_entry(struct buffer_head *p_flush_entry);
void bc_flush_all_entries(void);
