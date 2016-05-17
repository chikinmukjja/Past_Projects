#include "userprog/syscall.h"
#include <stdio.h>
#include <syscall-nr.h>
#include "threads/interrupt.h"
#include "threads/thread.h"
#include <devices/shutdown.h>
#include <filesys/filesys.h>
#include "userprog/process.h"
#include "threads/synch.h"
#include <string.h>
#include <devices/input.h>
#include "vm/page.h"
#include "threads/malloc.h"
#include "threads/vaddr.h"
#include "userprog/pagedir.h"
#include "threads/palloc.h"
#define CLOSE_ALL -1

int mmap(int fd,void *addr);
void munmap(int mapping);
void do_munmap(struct mmap_file *mmap_file);

static void syscall_handler (struct intr_frame *);

struct vm_entry* check_address(void *addr);
void check_valid_buffer(void *buffer, unsigned size, void *esp, bool to_write);
void check_valid_string(const void *str, void *esp);

void get_argument(void *esp, int *arg, int count);

// system call function 
void halt(void);
void exit(int status);
bool create(const char *file, unsigned initial_size);
bool remove(const char *file);
tid_t exec(const char *cmd_line);
int wait(tid_t tid);
int open(const char *file);
int filesize(int fd);
int read(int fd, void *buffer, unsigned size);
int write(int fd, void *buffer, unsigned size);
void seek(int fd, unsigned position);
unsigned tell(int fd);
void close(int fd);

extern struct lock file_lock;   // lock for read , write


int mmap(int fd, void *addr)
{

	struct thread *t = thread_current();
	struct mmap_file* mmap_f= malloc(sizeof(struct mmap_file)); // mmap_file allocate

	struct file *fp1 = process_get_file(fd);                    // get file for fd
	if(fp1 == NULL)
		return -1;


	if( !is_user_vaddr(addr)|| addr < (void *)0x08048000 ||
		   	   ((uint32_t)addr % PGSIZE)!= 0)return -1;
    // only on user address space, set address to aligned pagesize

	struct file *fp2 = file_reopen(fp1);       // fail for reopening
	if(fp2 == NULL || file_length(fp1) == 0)
	return -1;


	if(mmap_f != NULL){                        // fail for allocate space for mmap_file

			t->mapid++;
			mmap_f->file = fp2;
			mmap_f->mapid = t->mapid;
			list_init(&mmap_f->vme_list);

	}else return -1;

	int32_t offset = 0;
	uint32_t read_bytes = file_length(fp2);   //  to read the number of bytes
	
	uint32_t page_read_bytes;
	uint32_t page_zero_bytes;

	while(read_bytes > 0)
	{

		page_read_bytes = read_bytes < PGSIZE ? read_bytes : PGSIZE;  // over PGSIZE 4096
		page_zero_bytes = PGSIZE - page_read_bytes;

		// add_mmap_to_page
		struct vm_entry *entry = malloc(sizeof(struct vm_entry));
		if(entry == NULL)return -1;
		
		entry->file = fp2;                             // vme_entry initialize
		entry->offset = offset;
		entry->vaddr = addr;
		entry->read_bytes = page_read_bytes;
		entry->zero_bytes = page_zero_bytes;
		entry->is_loaded = false;
		entry->type = VM_FILE;
		entry->writable = true;
		
		list_push_back(&mmap_f->vme_list,&entry->mmap_elem);       // to push element to (mmap_file`s member) vme_list
		if(!insert_vme(&t->vm,entry))return -1;

		read_bytes -= page_read_bytes;                              // if read_bytes > PGSIZE -> decrease PGSIZE
		offset += page_read_bytes;
		addr += PGSIZE;

	}
	
	list_push_back(&t->mmap_list,&mmap_f->elem);                    //to push element to( struct thread`s member )mmap_list


	return t->mapid;
	
}

void munmap(int mapping)                    // if mmaping == -1 -> close all
{
	struct list_elem *e;
	struct list_elem *next;
	struct thread *t = thread_current();
	struct mmap_file *mmap_f;	



	e = list_begin(&t->mmap_list);
    //printf("munmap start %d \n",e != list_end(&t->mmap_list));
	while(e != list_end(&t->mmap_list))
	{
		//printf("in while \n");
		next = list_next(e);               // case of current element removed
		mmap_f = list_entry(e,struct mmap_file,elem);

		if(mmap_f->mapid == mapping || mapping == -1 )
		{
			do_munmap(mmap_f);             // remove all entry of vme_list ( member of mmap_f )

			list_remove(e);                // remove element on mmap_list

			lock_acquire(&file_lock);
			file_close(mmap_f->file);      // file close for reopen
			lock_release(&file_lock);

			free(mmap_f);
		}
		e = next;
	}

//printf("mumap end\n");
}
void do_munmap(struct mmap_file *mmap_file)
{
	struct list_elem *e = list_begin(&mmap_file->vme_list);
	struct list_elem *next;
	struct vm_entry *entry;
	struct thread *t = thread_current();
	//printf("do_mumap start\n");
	while(e != list_end(&mmap_file->vme_list)) 
	{	next = list_next(e);

		entry = list_entry(e,struct vm_entry,mmap_elem);
		if(entry->is_loaded)                             // is_loaded true -> page free, false-> pass
		{
			if(pagedir_is_dirty(t->pagedir,entry->vaddr)) // dirty -> write, no dirty -> pass
			{
				lock_acquire(&file_lock);
				file_write_at(entry->file,entry->vaddr,
					entry->read_bytes,entry->offset);
				lock_release(&file_lock);
			}

			palloc_free_page(pagedir_get_page(t->pagedir,
						entry->vaddr));
			pagedir_clear_page(t->pagedir,entry->vaddr);
		}
		list_remove(e);                                  // remove element on vme_list
		delete_vme(&t->vm,entry);                        // remove element on vm (hash)
		free(entry);

		e = next;
	}
	//printf("mummap end\n");
}



void
syscall_init (void) 
{
  intr_register_int (0x30, 3, INTR_ON, syscall_handler, "syscall");
}

static void
syscall_handler (struct intr_frame *f UNUSED) 
{
  uint32_t *sp = f -> esp;
  check_address((void *)sp);               // check address of esp

  int syscall_n = *sp;                     // syscall_number
  int arg[5];                              // store argument 
//  printf("in syscall.c %d\n", syscall_n);
  switch(syscall_n)
  {
	    case SYS_HALT :                                    // system call 0 ~
		halt();	 
        case SYS_EXIT :                                             // exit
		get_argument(sp,arg,1);
		exit(arg[0]);
		break;
        case SYS_EXEC :                                 // make another process
		get_argument(sp,arg,1);
	//	check_address((void *)arg[0]);          // check string pointer
		check_valid_string((const void *)arg[0],NULL);
		f->eax = exec((const char *)arg[0]);
		break;
        case SYS_WAIT :                                
		get_argument(sp,arg,1);
		f->eax = wait(arg[0]);
		break; 
        case SYS_CREATE :                                //create new file
        get_argument(sp,arg,2);
	//	check_address((void *)arg[0]);
		check_valid_string((const void *)arg[0],NULL);
		f->eax = create((const char *)arg[0],arg[1]);
		break;
	case SYS_REMOVE :                                // remove file
		get_argument(sp,arg,1);
	//	check_address((void *)arg[0]);
		check_valid_string((const void *)arg[0],NULL);
		f->eax = remove((const char *)arg[0]);
		break;
	case SYS_OPEN :                              
		get_argument(sp,arg,1);
	//	check_address((void *)arg[0]);
		check_valid_string((const void *)arg[0],NULL);
		f->eax = open((const char *)arg[0]);     // return fd, not -1
		break;
	case SYS_FILESIZE :
		get_argument(sp,arg,1);
		f->eax = filesize(arg[0]);
		break;
	case SYS_READ  :

		// argument ( fd ,buffer ,size )
		get_argument(sp,arg,3);
	//	check_address((void *)arg[1]);
		check_valid_buffer((void *)arg[1],(unsigned)arg[2],NULL,true);
		f->eax = read(arg[0],(void *)arg[1],arg[2]);

		break;
 	case SYS_WRITE :                    // argument ( fd ,buffer ,size )
	//	printf("in write start\n");
 		get_argument(sp,arg,3);
	//	check_address((void *)arg[1]);
		check_valid_buffer((void *)arg[1],(unsigned)arg[2],NULL,false);
		f->eax = write(arg[0],(void *)arg[1],arg[2]);
	//	printf("in write end\n");
		break;
	case SYS_SEEK  :
		get_argument(sp,arg,2);
		seek(arg[0],arg[1]);
		break;
        case SYS_TELL  :
		get_argument(sp,arg,1);
		tell(arg[0]);
		break;    
	case SYS_CLOSE :
		get_argument(sp,arg,1);
		close(arg[0]);
		break;

	case SYS_MMAP:                     // if call check address -> call find_vme -> error
		get_argument(sp,arg,2);
		f->eax = mmap(arg[0],(void *)arg[1]);
		break;
	case SYS_MUNMAP:                  // syscall munmap
		get_argument(sp,arg,1);
		munmap(arg[0]);
		break;
	default :
	//	thread_exit(); 
		break;
  }
// printf ("system call!\n");
//  thread_exit ();
}

struct vm_entry* check_address(void *addr)
{
	if( 0x8048000 <(uint32_t *)addr &&(uint32_t *)addr < 0xc0000000 );
	else {
		exit(-1);
	}

	struct vm_entry *v_entry = find_vme(addr);     // no find -> error
	if(v_entry == NULL)
		{
		exit(-1);
		}
	else return v_entry;                           // return ( void -> vm_entry* )

}
void check_valid_buffer(void *buffer, unsigned size, void *esp UNUSED,
			 bool to_write)
{
	unsigned i;
	void *temp_buffer = buffer;
	struct vm_entry *v_temp = check_address(buffer);
	
	for(i = 0; i < size; i++) // for all buffer space
	{

		if(v_temp != NULL && to_write)     // case of read, to_write true, also writable true
		{                                  // case of write to_write false, also writable false
			if(!v_temp->writable)
			{
				exit(-1);
			}
		}

		temp_buffer++;                     // move next buffer
		v_temp = check_address(temp_buffer);
	}	
}

void check_valid_string(const void *str, void *esp UNUSED)
{

	check_address((void *)str);
	while(*(char *)str != 0)               // until to meet null byte
	{
		str = (char *)str + 1;
		check_address((void *)str);
	}	
}

void get_argument(void *esp, int *arg, int count)
{
	int i=count;
	void *pointer = esp +4;            // above system call number
	
	for(i = 0; i < count; i++)
	{
		arg[i] = *(int *)pointer;
		pointer = pointer +4;
		check_address(pointer);          
	}
	esp = esp + 4*(count+1);           // change esp poiter location
}

void halt()
{
	shutdown_power_off();
}

void exit(int status)
{
	thread_current()->exit_status = status;             //store exit status
	printf("%s: exit(%d)\n",thread_current()->name,status);
        thread_exit();
}

bool create(const char *file, unsigned initial_size)
{
	return filesys_create(file,initial_size);
}

bool remove(const char *file)
{
	return filesys_remove(file);
}

tid_t exec(const char *cmd_line)
{
	tid_t pid;
	struct thread *t;
	
	pid = process_execute(cmd_line);       // create child process
	t = get_child_process((int)pid);       // find child process
	
 	sema_down(&t->load_semaphore);         // wait child process load
	
	if(t->process_load)return pid;
	else return -1;
	
}

int wait(tid_t tid)
{
	return process_wait(tid);	
} 

int open(const char *file)
{
	struct file *f;
	
	if((f = filesys_open(file)) == NULL)
	return -1;
        else return process_add_file(f);

}
int filesize(int fd)
{
	struct file *f;
	if((f = process_get_file(fd)) == NULL)
		return -1;
	else return file_length(f);
}  

int read(int fd, void *buffer, unsigned size)
{
	unsigned count = 0;
	int read_size = -1;
	struct file *f;

	lock_acquire(&file_lock);

	if(fd == 1 || fd < 0);                       // handle exception 
	else if(fd == 0)                             // stdin 
	{
		for(count=0;count<size;count++)
				((char *)buffer)[count] = input_getc();
		read_size = count;
	}
	else 
	{
		if((f= process_get_file(fd)) != NULL)
			read_size = file_read(f,buffer,size);	
	}
	
	lock_release(&file_lock);
	return read_size;


} 
int write(int fd, void *buffer, unsigned size)
{
	struct file *f;
	unsigned write_size=-1;

	lock_acquire(&file_lock);     // lock to write alone
	
	if(fd < 1);                   // exception  return size 0;
	else if(fd == 1)              // write on stdout 
	{
		putbuf((char *)buffer,size);
		write_size = size;
	}
	else
	{
		if((f=process_get_file(fd)) != NULL )
			write_size = file_write(f,buffer,size);
	}
	lock_release(&file_lock);
	return write_size;
} 

void seek(int fd, unsigned position)
{
	struct file *f;
	if((f = process_get_file(fd)) != NULL)
		file_seek(f,position);
	
}
unsigned tell(int fd)
{
	struct file *f;
	if((f = process_get_file(fd)) !=  NULL)
		return (unsigned)file_tell(f);
	else return 0;
		
}

void close(int fd)
{
	process_close_file(fd);
}
