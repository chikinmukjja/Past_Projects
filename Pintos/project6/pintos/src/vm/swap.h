#include "devices/block.h"
#include "threads/synch.h"
#include "threads/vaddr.h"
#include <bitmap.h>


#define SWAP_FREE 0

#define SECTORS_PER_PAGE (PGSIZE/BLOCK_SECTOR_SIZE)

