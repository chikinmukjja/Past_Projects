
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
struct list lru_list;
struct list_elem* lru_clock;


extern struct lock file_lock;

void lru_list_init(void)
{

	lock_init(&lru_list_lock);
	list_init(&lru_list);
	lru_clock = NULL;
}

void add_page_to_lru_list(struct page *page)
{

	lock_acquire(&lru_list_lock);
	list_push_back(&lru_list,&page->lru_elem);
	lock_release(&lru_list_lock);

}

void del_page_from_lru_list(struct page *page)
{
	lock_acquire(&lru_list_lock);
	if(lru_clock == &page->lru_elem)
		lru_clock = list_remove(&page->lru_elem);
	else list_remove(&page->lru_elem);
	lock_release(&lru_list_lock);
}

/*struct page* alloc_page(enum palloc_flags flags)
{
	void *kaddr = palloc_get_page(flags);
	struct page *page_entry;
	printf("alloc_page\n");
	if(kaddr != NULL)
	{
		page_entry = malloc(sizeof(struct page));
		
		if(page_entry != NULL)
		{printf("alloc_page if \n");
			page_entry->kaddr = kaddr;
			page_entry->vme; //find? -> after
			page_entry->thread = thread_current();
		}
		else{ return NULL; }//error
	}		
	else{
		printf("alloc_page else \n");
		while(kaddr == NULL)kaddr = try_to_free_pages(flags);
		page_entry = malloc(sizeof(struct page));
		page_entry->kaddr = kaddr;
		page_entry->thread = thread_current();
	}//error
	
	add_page_to_lru_list(page_entry);
	
	return page_entry;
 
}
*/
struct page *alloc_page(enum palloc_flags flags)
{
	void *kaddr = palloc_get_page(flags);
	while(kaddr == NULL)
	{
		kaddr = try_to_free_pages(flags);
		lock_release(&lru_list_lock);

	}
	struct page *page = (struct page*)malloc(sizeof(struct page));
	if(page == NULL)return NULL;
	page->kaddr = kaddr;
	page->thread = thread_current();

	add_page_to_lru_list(page);

	return page;
}
/*void free_page(void *kaddr)
{
	struct list_elem *e;
	struct page *page_entry;
	lock_acquire(&lru_list_lock);
	for(e = list_begin(&lru_list);e != list_end(&lru_list);
		e = list_next(e))
	{
		page_entry = list_entry(e,struct page,lru_elem);
		if(page_entry->kaddr == kaddr)
		{
			lock_release(&lru_list_lock);
		 	__free_page(page_entry);
			return ;
		}
	}
	lock_release(&lru_list_lock);

}
*/
void free_page(void *kaddr)
{
	lock_acquire(&lru_list_lock);
	struct list_elem *ptr = list_begin(&lru_list);
	while(ptr != list_end(&lru_list))
	{
		struct page *page = list_entry(ptr,struct page,lru_elem);
		if(page->kaddr == kaddr)
		{
			lock_release(&lru_list_lock);
			__free_page(page);
		}
		ptr = list_next(ptr);
	}
}

void __free_page(struct page *page)
{
	del_page_from_lru_list(page);
	palloc_free_page(page->kaddr);
	free(page);
}

static struct list_elem* get_next_lru_clock()
{
	
	struct list_elem *e = lru_clock;
	if(e == list_end(&lru_list))
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

	if(list_empty(&lru_list));
	if(lru_clock == NULL)lru_clock = list_begin(&lru_list);

	while(lru_clock)
	{

		e = lru_clock;
		page_entry = list_entry(e,struct page,lru_elem);
		t = page_entry->thread;
		v = page_entry->vme;
		if(pagedir_is_accessed(t->pagedir,page_entry->vme->vaddr))
		{
			pagedir_set_accessed(t->pagedir,
					page_entry->vme->vaddr,false);
		}
		else
		{
			if(pagedir_is_dirty(t->pagedir,page_entry->vme->vaddr)
					| page_entry->vme->type == VM_ANON)
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
			free_page(page_entry->kaddr);

			return palloc_get_page(flags);
		}
		lru_clock = get_next_lru_clock();
	}
}
*/
void* try_to_free_pages(enum palloc_flags flags)
{
	lock_acquire(&lru_list_lock);
	struct list_elem *e = list_begin(&lru_list);

	while(true)
	{
		struct page *page_entry = list_entry(e,struct page,lru_elem);
		struct thread *t = page_entry->thread;

		if(pagedir_is_accessed(t->pagedir,page_entry->vme->vaddr))
		{
			pagedir_set_accessed(t->pagedir,page_entry->vme->vaddr,false);

		}
		else
		{
			if(pagedir_is_dirty(t->pagedir,page_entry->vme->vaddr)||
					page_entry->vme->type == VM_ANON)
			{
				if(page_entry->vme->type == VM_FILE)
				{
					lock_acquire(&file_lock);
					file_write_at(page_entry->vme->file,page_entry->kaddr,
									page_entry->vme->read_bytes,
									page_entry->vme->offset);
					lock_release(&file_lock);
				}
				else
				{
					page_entry->vme->type = VM_ANON;
					page_entry->vme->swap_slot = swap_out(page_entry->kaddr);

				}
			}

			page_entry->vme->is_loaded = false;
			list_remove(&page_entry->lru_elem);
			//printf("%d\n",t->pagedir);
			pagedir_clear_page(t->pagedir,page_entry->vme->vaddr);

			palloc_free_page(page_entry->kaddr);
			free(page_entry);
			return palloc_get_page(flags);
		}
		e = list_next(e);
		if(e == list_end(&lru_list))
		{
			e = list_begin(&lru_list);
		}
	}
}


