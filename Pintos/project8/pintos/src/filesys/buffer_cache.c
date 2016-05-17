#include "filesys/buffer_cache.h"
#include "threads/palloc.h"
#include "devices/block.h"
#include <string.h>


#define BUFFER_CACHE_ENTRY_NB 64

void *p_buffer_cache;
struct buffer_head buffer_head_table[BUFFER_CACHE_ENTRY_NB];
int clock_hand;


void bc_init(void)
{
	p_buffer_cache = malloc(64*BLOCK_SECTOR_SIZE); // 8page 어떻게 선언해주나
	// 자료구조 초기화를 왜 하지 ? head 삽입할때 entry initial 해주면 되지않나
 	// 초기화를
	clock_hand = 0;
	int i;
	for(i =0; i < 64; i++)
	{

		buffer_head_table[i].sector = -1; //-1 Invalid
		buffer_head_table[i].dirty = false;
		buffer_head_table[i].clock_bit = 0;
		buffer_head_table[i].data = p_buffer_cache + i*BLOCK_SECTOR_SIZE;

		lock_init(&buffer_head_table[i].buffer_lock);
	}
}

struct buffer_head* bc_lookup(block_sector_t sector)
{
	int i;
	for(i = 0; i < 64; i++)
	{
		if(buffer_head_table[i].sector == sector)
			return &buffer_head_table[i];
	}

	return NULL;
}

void bc_flush_entry(struct buffer_head *p_flush_entry)
{
	//dirty check ?
	block_write(block_get_role(BLOCK_FILESYS),p_flush_entry->sector,p_flush_entry->data);
	// data size ?
	p_flush_entry->dirty = false;
}

void bc_flush_all_entries(void)
{
	int i;
	for(i=0;i<64;i++)
	{
		if(buffer_head_table[i].sector != -1 &&buffer_head_table[i].dirty == true)
		{
			bc_flush_entry(&buffer_head_table[i]);
		}
	}
}

struct buffer_head* bc_select_victim(void)
{
	int i;

	for(i=0;i<64;i++)
	{
		if(buffer_head_table[i].sector == -1)
		{
			return &buffer_head_table[i];
		}
	}

	while(true)
	{
		if(clock_hand == 63)clock_hand = 0;
		else clock_hand++;

		if(buffer_head_table[clock_hand].clock_bit == 0)
		{
			if(buffer_head_table[clock_hand].dirty == true)
			{
				bc_flush_entry(&buffer_head_table[clock_hand]);
			}
			buffer_head_table[clock_hand].sector = -1;
			buffer_head_table[clock_hand].clock_bit = 0;

			return &buffer_head_table[clock_hand];
		}
		else
		{
			buffer_head_table[clock_hand].clock_bit = 0;
		}
	}

/*	for(i = 0; i< 64; i++)
	{
		if(buffer_head_table[i].clock_bit == 1){    // set 1 -> 0
			buffer_head_table[i].clock_bit = 0;
		}
		else if(buffer_head_table[i].clock_bit == 0) // this entry is victim
		{
			if(buffer_head_table[i].dirty == true) //flush to disk
			{
				bc_flush_entry(&buffer_head_table[i]);
			}

			buffer_head_table[i].sector = -1;     // initialize entry
			buffer_head_table[i].dirty = false;
			buffer_head_table[i].accessed = false;


			struct buffer_head* ptr = &buffer_head_table[i];

			return ptr; //return victim entry
		}
	}

	return NULL;
	*/
}

void bc_term(void)
{
	bc_flush_all_entries(); // flush dirty space
	free(p_buffer_cache);   // free buffer cache space
}


bool bc_read(block_sector_t sector_idx, void *buffer, off_t bytes_read,
		int chunk_size, int sector_ofs)
{
	bool check = false;
	// sector_ofs ?
	struct buffer_head *buffer_entry = bc_lookup(sector_idx);

	while(buffer_entry == NULL)   // check is entry null?
	{
		buffer_entry = bc_select_victim();
		check = true;
		buffer_entry->sector =  sector_idx;
	}

	if(check)
		block_read(block_get_role(BLOCK_FILESYS),sector_idx,buffer_entry->data);
	// get disk data and store on buffer
	buffer_entry->clock_bit = 1;
	memcpy(buffer  + bytes_read,buffer_entry->data + sector_ofs,chunk_size);
	// buffer data stored in buffer cache space
	// chunk size?





	return true;
}

bool bc_write(block_sector_t sector_idx, void *buffer, off_t bytes_read,
		int chunk_size, int sector_ofs)
{
	bool success = false;
	bool check = false;
	struct buffer_head *buffer_entry = bc_lookup(sector_idx);

	while(buffer_entry == NULL)   // check is entry null?
	{
		buffer_entry = bc_select_victim();
		buffer_entry->sector = sector_idx;
		check = true;
	}
	if(check)
	{
		block_read(block_get_role(BLOCK_FILESYS),sector_idx,buffer_entry->data);

	}

	memcpy(buffer_entry->data + sector_ofs,buffer + bytes_read,chunk_size);


	lock_acquire(&buffer_entry->buffer_lock);
	buffer_entry->dirty = true;
	buffer_entry->clock_bit = 1;
	lock_release(&buffer_entry->buffer_lock);



	success = true;

	return success;
}

