
#include "vm/frame.h"
#include "userprog/pagedir.h"
#include "threads/thread.h"
#include "threads/synch.h"
#include "threads/malloc.h"
#include "threads/palloc.h"
#include "userprog/syscall.h"
#include "vm/page.h"
#include "vm/swap.h"

void lru_list_init(void);
void add_page_to_lru_list(struct page *page);
void del_page_from_lru_list(struct page *page);
static struct list_elem *get_next_lru_clock(void);

struct page* alloc_page(enum palloc_flags flags);
void free_page(void *kaddr);
void __free_page(struct page* page);

void *try_to_free_pages(enum palloc_flags flags);





struct lock lru_list_lock;
struct list lru_list;				// physical memory
struct list_elem* lru_clock;		// chose frame for swap out


extern struct lock file_lock;

void lru_list_init(void)
{

	lock_init(&lru_list_lock); // lock init
	list_init(&lru_list);      // lru list init
	lru_clock = NULL;
}

void add_page_to_lru_list(struct page *page)
{

	lock_acquire(&lru_list_lock);            // share value
	list_push_back(&lru_list,&page->lru_elem);
	lock_release(&lru_list_lock);

}

void del_page_from_lru_list(struct page *page)
{
	lock_acquire(&lru_list_lock);
	if(lru_clock == &page->lru_elem)          // current clock == removed elem
		lru_clock = list_remove(&page->lru_elem); // clock is next elem
	else list_remove(&page->lru_elem);
	lock_release(&lru_list_lock);
}

struct page *alloc_page(enum palloc_flags flags)
{

	void *kaddr = palloc_get_page(flags);
	while(kaddr == NULL) // memory is full
	{
		kaddr = try_to_free_pages(flags); //to swap out and alloc new page
		lock_release(&lru_list_lock);

	}
	struct page *page = (struct page*)malloc(sizeof(struct page));
	if(page == NULL)return NULL;
	page->kaddr = kaddr;
	page->thread = thread_current();

	add_page_to_lru_list(page); // insert to lru list

	return page;
}
void free_page(void *kaddr)
{
	struct list_elem *e;

	struct page *page_entry;
	lock_acquire(&lru_list_lock);
	for(e = list_begin(&lru_list);e != list_end(&lru_list);  // for all elem on lru list
		e = list_next(e))
	{

		page_entry = list_entry(e,struct page,lru_elem);
		if(page_entry->kaddr == kaddr)        // check
		{
			lock_release(&lru_list_lock);
		 	__free_page(page_entry);
			return ;                          // end when free for physical address

			//break;
		}

	}
	lock_release(&lru_list_lock);

}


void __free_page(struct page *page)
{
	del_page_from_lru_list(page);                  // delete on list
	palloc_free_page(page->kaddr);				   // free on memory
	free(page);									   // free for malloc
}

static struct list_elem* get_next_lru_clock()
{

	struct list_elem *e = lru_clock;
	e = list_next(lru_clock);
	if(e == list_end(&lru_list))				   // list end == list begin -> return null
	{
		e = list_begin(&lru_list);
		if(e == list_end(&lru_list))return NULL;
		return e;
	}
	return list_next(lru_clock); 

}

/*void *try_to_free_pages(enum palloc_flags flags)
{
	struct thread *t;
	struct list_elem *e;
	struct page *page_entry;
	struct vm_entry *v;
	lock_acquire(&lru_list_lock);

	//if(list_empty(&lru_list));
	if(lru_clock == NULL)lru_clock = list_begin(&lru_list);

	while(lru_clock)
	{

		e = lru_clock;
		page_entry = list_entry(e,struct page,lru_elem);
		t = page_entry->thread;
		v = page_entry->vme;
		if(v->pinned == false)
		if(pagedir_is_accessed(t->pagedir,page_entry->vme->vaddr))
		{
			pagedir_set_accessed(t->pagedir,
					page_entry->vme->vaddr,false);
		}
		else
		{
			if(pagedir_is_dirty(t->pagedir,page_entry->vme->vaddr)
					|| page_entry->vme->type == VM_ANON)
			{
				if(v->type==VM_FILE)
                {
					lock_acquire(&file_lock);
					file_write_at(v->file,page_entry->kaddr,
							v->read_bytes,
							v->offset);
					lock_release(&file_lock);
				}
				else
				{
					v->type = VM_ANON;
					page_entry->vme->swap_slot = swap_out(page_entry->kaddr);
				}
			}
			v->is_loaded = false;
			//pagedir_clear_page(t->pagedir,v->vaddr);
			list_remove(&page_entry->lru_elem);
			pagedir_clear_page(t->pagedir,page_entry->vme->vaddr);
			palloc_free_page(page_entry->kaddr);
			free(page_entry);
			return palloc_get_page(flags);


		}
		lru_clock = get_next_lru_clock();
	}
}
*/
void* try_to_free_pages(enum palloc_flags flags) // not using clock algorithm
{												 // no test error, so i go this
	lock_acquire(&lru_list_lock);
	struct list_elem *e = list_begin(&lru_list);

	while(true)
	{
		struct page *page_entry = list_entry(e,struct page,lru_elem);
		struct thread *t = page_entry->thread;
		if(page_entry->vme->pinned == false)
		if(pagedir_is_accessed(t->pagedir,page_entry->vme->vaddr))
		{// is this page accessed in nearly ?
			pagedir_set_accessed(t->pagedir,page_entry->vme->vaddr,false);
		  // set 1 -> 0
		}
		else
		{   // is changed?
			if(pagedir_is_dirty(t->pagedir,page_entry->vme->vaddr)||
					page_entry->vme->type == VM_ANON) // is swap type ?
			{
				if(page_entry->vme->type == VM_FILE)
				{// mmap file write before free

					lock_acquire(&file_lock);		// save on disk partition
					file_write_at(page_entry->vme->file,page_entry->kaddr,
									page_entry->vme->read_bytes,
									page_entry->vme->offset);
					lock_release(&file_lock);
				}
				else
				{
					page_entry->vme->type = VM_ANON;		//change type
					page_entry->vme->swap_slot = swap_out(page_entry->kaddr);

				}
			}

			page_entry->vme->is_loaded = false;				// delete entry for victim
			list_remove(&page_entry->lru_elem);
			pagedir_clear_page(t->pagedir,page_entry->vme->vaddr);
			palloc_free_page(page_entry->kaddr);
			free(page_entry);
			return palloc_get_page(flags);
		}
		e = list_next(e);									// move next element
		if(e == list_end(&lru_list))
		{
			e = list_begin(&lru_list);
		}
	}
}



