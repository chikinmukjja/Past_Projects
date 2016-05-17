#include "vm/page.h"
#include <hash.h>
#include "threads/palloc.h"
#include "threads/malloc.h"
#include "threads/vaddr.h"
#include "userprog/process.h"
#include "userprog/pagedir.h"
#include <string.h>
#include "filesys/file.h"
#include "vm/frame.h"
#include "vm/swap.h"

void vm_init(struct hash* vm);
void vm_destroy(struct hash *vm);
static unsigned vm_hash_func(const struct hash_elem *e, void *aux UNUSED);

static bool vm_less_func(const struct hash_elem *a,
			 const struct hash_elem *b, void *aux UNUSED);
static void vm_destroy_func(struct hash_elem *e, void *aux UNUSED);
struct vm_entry *find_vme(void *vaddr);
bool insert_vme(struct hash *vm, struct vm_entry *vme);
bool delete_vme(struct hash *vm, struct vm_entry *vme);

extern struct lock file_lock;

void vm_init(struct hash *vm)
{
	hash_init(vm,vm_hash_func,vm_less_func,NULL);
}
void vm_destroy(struct hash *vm) // to destroy hash table
{
	hash_destroy(vm,vm_destroy_func);
}
static unsigned vm_hash_func(const struct hash_elem *e, void *aux UNUSED) //to use vm_init
{  // static only declared in *.c file not *.h  only use in this file
	struct vm_entry *entry;
	entry = hash_entry(e, struct vm_entry, h_elem); 
	
	return hash_int((int)entry->vaddr);
}
static bool vm_less_func(const struct hash_elem *a,                      // to use vm_init
			const struct hash_elem *b, void *aux UNUSED)

{
	struct vm_entry *a_entry = hash_entry(a, struct vm_entry, h_elem);
	struct vm_entry *b_entry = hash_entry(b, struct vm_entry, h_elem);
	
	if(a_entry->vaddr < b_entry->vaddr)return true;
	else return false; 

}
static void vm_destroy_func(struct hash_elem *e,void *aux UNUSED)         // to use vm_destroy
{
	struct vm_entry *entry = hash_entry(e, struct vm_entry, h_elem);
	
	if(entry->is_loaded)
	{
		free_page(pagedir_get_page(                           // remove frame on physical memory
			thread_current()->pagedir,entry->vaddr));
		pagedir_clear_page(thread_current()->pagedir,entry->vaddr);  // remove page table entry
			
	}
	
	free(entry);
}
struct vm_entry *find_vme(void *vaddr) // search in hash, no -> return NULL
{
	struct vm_entry entry;
	entry.vaddr = pg_round_down(vaddr);
	
	struct hash_elem *e = hash_find(&thread_current()->vm,
					&entry.h_elem);

	if(!e)return NULL;
	return hash_entry(e, struct vm_entry, h_elem);
	
}

bool insert_vme(struct hash *vm, struct vm_entry *vme)  // insert vm_entry on thread`s vm (hash_list)
{
	struct hash_elem *e = &vme->h_elem;
	
	if(hash_insert(vm,e) != NULL)return false;
	else return true;

}

bool delete_vme(struct hash *vm, struct vm_entry* vme) // delete vm_entry on thread`s vm ( hash_list )
{
	struct hash_elem *e = &vme->h_elem;
	
	if(hash_delete(vm,e) == NULL)return false;
	else return true;
}

bool load_file(void *kaddr, struct vm_entry *vme)    // load file from disk to physical memory
{

		//lock_acquire(&file_lock);
		if((int)vme->read_bytes != file_read_at(vme->file,kaddr,
							vme->read_bytes,
							vme->offset))
		{
			//lock_release(&file_lock);
			free_page(kaddr);                // in case fail -> pagefree
			return false;
		}

		//lock_release(&file_lock);
		memset(kaddr+vme->read_bytes,0,vme->zero_bytes);  // fill zero

	return true;
}
