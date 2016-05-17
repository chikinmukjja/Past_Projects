#include "vm/swap.h"

void swap_init(void);
void swap_in(size_t used_index, void* kaddr);
size_t swap_out(void *kaddr);



struct lock swap_lock;
struct block *swap_block;
struct bitmap *swap_map;

void swap_init(void)
{
	swap_block = block_get_role(BLOCK_SWAP);          // initial share variable
	swap_map = bitmap_create(block_size(swap_block)/SECTORS_PER_PAGE);
	bitmap_set_all(swap_map,SWAP_FREE);
	lock_init(&swap_lock);
}

size_t swap_out(void *kaddr)
{
	lock_acquire(&swap_lock);
	size_t free_index = bitmap_scan_and_flip(swap_map,0,1,SWAP_FREE);

	size_t i;
	for(i = 0;i <SECTORS_PER_PAGE;i++)// sectors_per_page = 4096 / 512
	{// move disk swap partition
		block_write(swap_block,free_index*SECTORS_PER_PAGE +i,
				(uint8_t*)kaddr + i*BLOCK_SECTOR_SIZE);
	}
	lock_release(&swap_lock);

	return free_index;  // return free index number
}

void swap_in(size_t used_index,void *kaddr)
{
	if(!swap_block||!swap_map)return ;   // handle exception

	lock_acquire(&swap_lock);
	if(bitmap_test(swap_map,used_index) == SWAP_FREE);
	
	bitmap_flip(swap_map,used_index);

	size_t i;
	for(i = 0;i< SECTORS_PER_PAGE;i++)
	{// restore on swap partition on disk
		block_read(swap_block,used_index*SECTORS_PER_PAGE +i,
				(uint8_t *)kaddr + i*BLOCK_SECTOR_SIZE);
	}
	lock_release(&swap_lock);
}
